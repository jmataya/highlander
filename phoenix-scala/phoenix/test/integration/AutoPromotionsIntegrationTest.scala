import cats.implicits._
import org.json4s.JsonAST._
import org.json4s.jackson.JsonMethods._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.{when, _}
import org.scalactic.TolerantNumerics
import phoenix.models.Reasons
import phoenix.models.customer.CustomerGroup
import phoenix.models.customer.CustomerGroup.GroupType
import phoenix.models.promotion._
import phoenix.models.rules.QueryStatement
import phoenix.models.shipping.{ShippingMethod, ShippingMethods}
import phoenix.payloads.AddressPayloads.CreateAddressPayload
import phoenix.payloads.CartPayloads.CreateCart
import phoenix.payloads.CustomerGroupPayloads.{CustomerGroupMemberSyncPayload, CustomerGroupPayload}
import phoenix.payloads.LineItemPayloads.UpdateLineItemsPayload
import phoenix.payloads.PaymentPayloads.{CreateManualStoreCredit, StoreCreditPayment}
import phoenix.payloads.PromotionPayloads._
import phoenix.payloads.UpdateShippingMethod
import phoenix.responses.PromotionResponses.PromotionResponse
import phoenix.responses.cord.base.CartResponseTotals
import phoenix.responses.cord.{CartResponse, OrderResponse}
import phoenix.responses.{CustomerResponse, GroupResponses, PromotionResponses, StoreCreditResponse}
import phoenix.utils.ElasticsearchApi
import phoenix.utils.aliases._
import phoenix.utils.seeds.Factories
import testutils.PayloadHelpers.tv
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.api.PromotionPayloadBuilder.{PromoOfferBuilder, PromoQualifierBuilder}
import testutils.fixtures.api._
import testutils.fixtures.{BakedFixtures, PromotionFixtures}

import scala.concurrent.Future

class AutoPromotionsIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with DefaultJwtAdminAuth
    with TestActivityContext.AdminAC
    with BakedFixtures
    with ApiFixtures
    with ApiFixtureHelpers
    with PromotionFixtures {

  "with many available, the best one is chosen" in new ProductSku_ApiFixture {
    val percentOffs = List.fill(11)(scala.util.Random.nextInt(100))
    val percentOff  = percentOffs.max

    val promos = percentOffs.map { percentOff ⇒
      promotionsApi
        .create(
            PromotionPayloadBuilder.build(Promotion.Auto,
                                          PromoOfferBuilder.CartPercentOff(percentOff),
                                          PromoQualifierBuilder.CartAny))
        .as[PromotionResponse.Root]
    }

    val customer = api_newCustomer()

    val refNum =
      cartsApi.create(CreateCart(email = customer.email)).as[CartResponse].referenceNumber

    val cartWithProduct = cartsApi(refNum).lineItems
      .add(Seq(UpdateLineItemsPayload(skuCode, 1)))
      .asTheResult[CartResponse]

    cartWithProduct.promotion mustBe 'defined
    cartWithProduct.coupon mustBe 'empty

    import cartWithProduct.totals
    implicit val doubleEquality = TolerantNumerics.tolerantDoubleEquality(1.0)
    totals.adjustments.toDouble must === (sku.attributes.salePrice * (percentOff / 100.0))
    totals.total.toDouble must === (sku.attributes.salePrice * (1.0 - percentOff / 100.0))
  }

  def percentOff(p: PromotionResponse.Root): Int =
    (p.discounts.head.attributes \ "offer" \ "v" \ "orderPercentOff" \ "discount").extract[Int]

  // FIXME: un-ignore this test when you get historical promotions working for OrderResponse… @michalrus
  "keep correct promo versions for Carts & Orders after admin updates" ignore new ProductSku_ApiFixture
  with StoreAdmin_Seed {
    val percentOffInitial = 33
    val percentOffUpdated = 17

    val promo = promotionsApi
      .create(
          PromotionPayloadBuilder.build(Promotion.Auto,
                                        PromoOfferBuilder.CartPercentOff(percentOffInitial),
                                        PromoQualifierBuilder.CartAny))
      .as[PromotionResponse.Root]

    val customerA, customerB = api_newCustomer()

    // FIXME: use API
    val shippingMethod = UpdateShippingMethod(
        ShippingMethods
          .create(
              ShippingMethod(
                  adminDisplayName = "a",
                  storefrontDisplayName = "b",
                  code = "c",
                  price = 1000,
                  conditions = Some(
                      parse("""{"comparison": "and", "conditions": []}""").extract[QueryStatement])
              ))
          .gimme
          .id)

    // FIXME: use API
    val reason = Reasons.create(Factories.reason(storeAdmin.accountId)).gimme

    def cartPreCheckout(customer: CustomerResponse.Root): CartResponse = {
      val refNum =
        cartsApi.create(CreateCart(customerId = customer.id.some)).as[CartResponse].referenceNumber
      cartsApi(refNum).lineItems
        .add(Seq(UpdateLineItemsPayload(skuCode, 1)))
        .asTheResult[CartResponse]
      cartsApi(refNum).shippingAddress
        .create(
            CreateAddressPayload(name = "Home Office",
                                 regionId = 1,
                                 address1 = "3000 Coolio Dr",
                                 city = "Seattle",
                                 zip = "55555"))
        .asTheResult[CartResponse]
      val c = cartsApi(refNum).shippingMethod.update(shippingMethod).asTheResult[CartResponse]
      import c.totals.total
      customersApi(customer.id).payments.storeCredit
        .create(
            CreateManualStoreCredit(
                amount = total,
                reasonId = reason.id
            ))
        .as[StoreCreditResponse.Root]
      cartsApi(refNum).payments.storeCredit
        .add(StoreCreditPayment(total))
        .asTheResult[CartResponse]
    }

    val List(cartA, cartB) = List(customerA, customerB).map(cartPreCheckout)

    val orderA = cartsApi(cartA.referenceNumber).checkout().as[OrderResponse]

    // Now, let’s update the promotion and see if orderA’s one stays intact, while cartB’s is updated.

    val promoUpdated = promotionsApi(promo.id).update {
      val payload =
        PromotionPayloadBuilder.build(Promotion.Auto,
                                      PromoOfferBuilder.CartPercentOff(percentOffUpdated),
                                      PromoQualifierBuilder.CartAny)
      UpdatePromotion(
          applyType = payload.applyType,
          attributes = payload.attributes,
          discounts =
            Seq(UpdatePromoDiscount(promo.discounts.head.id, payload.discounts.head.attributes)))
    }.as[PromotionResponse.Root]

    val orderA2 = ordersApi(orderA.referenceNumber).get().asTheResult[OrderResponse]
    val cartB2  = cartsApi(cartB.referenceNumber).get().asTheResult[CartResponse]

    percentOff(orderA2.promotion.value) must === (percentOffInitial) // FIXME: this line fails @michalrus
    percentOff(cartB2.promotion.value) must === (percentOffUpdated)
  }

  "should be applied retroactively" in new ProductSku_ApiFixture {
    val customer = api_newCustomer()

    val refNum =
      cartsApi.create(CreateCart(email = customer.email)).as[CartResponse].referenceNumber

    val cartWithProduct = cartsApi(refNum).lineItems
      .add(Seq(UpdateLineItemsPayload(skuCode, 1)))
      .asTheResult[CartResponse]

    cartWithProduct.promotion mustBe 'empty

    val promo = promotionsApi
      .create(
          PromotionPayloadBuilder.build(Promotion.Auto,
                                        PromoOfferBuilder.CartPercentOff(37),
                                        PromoQualifierBuilder.CartAny))
      .as[PromotionResponse.Root]

    cartsApi(refNum).get.asTheResult[CartResponse].promotion mustBe 'defined
  }

  "after emptying a cart, no auto-promos are left" in new ProductSku_ApiFixture {
    val promo = promotionsApi
      .create(
          PromotionPayloadBuilder.build(Promotion.Auto,
                                        PromoOfferBuilder.CartPercentOff(37),
                                        PromoQualifierBuilder.CartAny))
      .as[PromotionResponse.Root]

    val customer = api_newCustomer()
    val refNum   = api_newCustomerCart(customer.id).referenceNumber

    cartsApi(refNum).lineItems
      .add(Seq(UpdateLineItemsPayload(skuCode, 1)))
      .asTheResult[CartResponse]
      .promotion mustBe 'defined

    cartsApi(refNum).lineItems.update(Seq(UpdateLineItemsPayload(skuCode, -1)))

    val finl = cartsApi(refNum).get.asTheResult[CartResponse]

    finl.promotion mustBe 'empty
    finl.totals must === (CartResponseTotals(0, 0, 0, 0, 0, 0))
  }

  "archived auto-apply promos are not applied" in new ProductSku_ApiFixture {
    val promo = promotionsApi
      .create(
          PromotionPayloadBuilder.build(Promotion.Auto,
                                        PromoOfferBuilder.CartPercentOff(37),
                                        PromoQualifierBuilder.CartAny))
      .as[PromotionResponse.Root]

    promotionsApi(promo.id).delete().mustBeOk()

    val customer = api_newCustomer()

    val refNum =
      cartsApi.create(CreateCart(email = customer.email)).as[CartResponse].referenceNumber

    cartsApi(refNum).lineItems
      .add(Seq(UpdateLineItemsPayload(skuCode, 1)))
      .asTheResult[CartResponse]
      .promotion mustBe 'empty
  }

  "promotions narrowed down to certain customer groups are applied only for them in" - {
    val DefaultPercentOff = 37

    def groupAndPromo(
        tpe: GroupType): (GroupResponses.GroupResponse.Root, PromotionResponse.Root) = {
      val group = customerGroupsApi
        .create(
            CustomerGroupPayload(name = faker.Lorem.sentence(),
                                 clientState = JNull,
                                 elasticRequest = JNull,
                                 groupType = tpe))
        .as[GroupResponses.GroupResponse.Root]

      val promo = promotionsApi
        .create(
            PromotionPayloadBuilder.build(
                Promotion.Auto,
                PromoOfferBuilder.CartPercentOff(DefaultPercentOff),
                PromoQualifierBuilder.CartAny,
                extraAttrs = Map(
                    "customerGroupIds" → tv(List(group.id),
                                            "tock673sjgmqbi5zlfx43o4px6jnxi7absotzjvxwir7jo2v")
                )))
        .as[PromotionResponse.Root]

      (group, promo)
    }

    "manual CGs" in {
      val (group, _) = groupAndPromo(CustomerGroup.Manual)

      val customer = api_newCustomer()
      val refNum   = api_newCustomerCart(customer.id).referenceNumber
      val skuCode  = new ProductSku_ApiFixture {}.skuCode

      cartsApi(refNum).lineItems
        .add(Seq(UpdateLineItemsPayload(skuCode, 1)))
        .asTheResult[CartResponse]
        .promotion mustBe 'empty

      customerGroupsMembersApi(group.id)
        .syncCustomers(CustomerGroupMemberSyncPayload(List(customer.id), List.empty))
        .mustBeEmpty()

      cartsApi(refNum).get().asTheResult[CartResponse].promotion mustBe 'defined
    }

    def dynamicCGCartPromo(numESHits: Long): Option[PromotionResponses.PromotionResponse.Root] = {
      reset(elasticSearchMock)
      when(elasticSearchMock.numResults(any[ElasticsearchApi.SearchView], any[Json]))
        .thenReturn(Future.successful(numESHits))

      groupAndPromo(CustomerGroup.Dynamic)

      val customer = api_newCustomer()
      val refNum   = api_newCustomerCart(customer.id).referenceNumber
      val skuCode  = new ProductSku_ApiFixture {}.skuCode

      cartsApi(refNum).lineItems
        .add(Seq(UpdateLineItemsPayload(skuCode, 1)))
        .asTheResult[CartResponse]
        .promotion
    }

    "dynamic CGs with a match" in { dynamicCGCartPromo(1L) mustBe 'defined }
    "dynamic CGs w/o matches" in { dynamicCGCartPromo(0L) mustBe 'empty }

    "and still the best promo is chosen among CG/non-CG ones" - {
      "lt" in bestIsApplied(DefaultPercentOff - 13)
      "gt" in bestIsApplied(DefaultPercentOff + 11)
      def bestIsApplied(otherPercentOff: Int) = {
        reset(elasticSearchMock)
        when(elasticSearchMock.numResults(any[ElasticsearchApi.SearchView], any[Json]))
          .thenReturn(Future.successful(1L))
        groupAndPromo(CustomerGroup.Dynamic)

        promotionsApi
          .create(
              PromotionPayloadBuilder.build(Promotion.Auto,
                                            PromoOfferBuilder.CartPercentOff(otherPercentOff),
                                            PromoQualifierBuilder.CartAny))
          .as[PromotionResponse.Root]

        val customer = api_newCustomer()
        val refNum   = api_newCustomerCart(customer.id).referenceNumber
        val skuCode  = new ProductSku_ApiFixture {}.skuCode

        val finalCart = cartsApi(refNum).lineItems
          .add(Seq(UpdateLineItemsPayload(skuCode, 1)))
          .asTheResult[CartResponse]

        percentOff(finalCart.promotion.value) must === (
            math.max(DefaultPercentOff, otherPercentOff))
      }
    }

    "and customerGroupIds are returned correctly" in {
      val (group, promo) = groupAndPromo(CustomerGroup.Dynamic)
      val ids            = (promo.attributes \ "customerGroupIds" \ "v").extract[List[Int]]
      val idsType        = (promo.attributes \ "customerGroupIds" \ "t").extract[String]

      info(s"Is List[Int] ~ $idsType?…")
      ids must === (List(group.id))
    }

  }

}