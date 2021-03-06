import akka.http.scaladsl.model.HttpResponse
import cats.implicits._
import core.failures.Failure
import objectframework.models.ObjectContexts
import org.scalatest.AppendedClues
import phoenix.failures.CartFailures._
import phoenix.models.account.Scope
import phoenix.models.cord._
import phoenix.models.inventory.Skus
import phoenix.models.location.Addresses
import phoenix.models.payment.creditcard.CreditCards
import phoenix.models.payment.giftcard._
import phoenix.models.payment.storecredit._
import phoenix.models.product.{Mvp, SimpleContext}
import phoenix.models.shipping.ShippingMethods
import phoenix.payloads.AddressPayloads._
import phoenix.payloads.LineItemPayloads.UpdateLineItemsPayload
import phoenix.payloads.PaymentPayloads._
import phoenix.payloads.UpdateShippingMethod
import phoenix.responses.cord.CartResponse
import phoenix.utils.aliases._
import phoenix.utils.seeds.{CouponSeeds, Factories}
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import core.db._

class CartValidatorIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AppendedClues
    with DefaultJwtAdminAuth
    with BakedFixtures {

  "Cart validator must be applied to" - {

    "/v1/carts/:refNum/payment-methods/gift-cards" in new GiftCardFixture {
      val api = cartsApi(refNum).payments.giftCard
      checkResponse(api.add(GiftCardPayment(giftCard.code)), expectedWarnings)
      checkResponse(api.delete(giftCard.code), expectedWarnings)
    }

    "/v1/carts/:refNum/payment-methods/store-credit" in new StoreCreditFixture {
      val api = cartsApi(refNum).payments.storeCredit
      checkResponse(api.add(StoreCreditPayment(500)), expectedWarnings)
      checkResponse(api.delete(), expectedWarnings)
    }

    "/v1/carts/:refNum/payment-methods/credit-cards" in new CreditCardFixture {
      val api = cartsApi(refNum).payments.creditCard
      checkResponse(api.add(CreditCardPayment(creditCard.id)), expectedWarnings)
      checkResponse(api.delete(), expectedWarnings)
    }

    "/v1/carts/:refNum/shipping-address" in new ShippingAddressFixture {
      val api = cartsApi(refNum).shippingAddress

      checkResponse(api.create(CreateAddressPayload("a", 1, "b", None, "c", "11111")), expectedWarnings)

      checkResponse(api.update(UpdateAddressPayload(name = "z".some)), expectedWarnings)

      checkResponse(api.delete(), expectedWarnings :+ NoShipAddress(refNum))

      val address = Addresses.create(Factories.address.copy(accountId = customer.accountId)).gimme
      checkResponse(api.updateFromAddress(address.id), expectedWarnings)
    }

    "/v1/carts/:refNum/shipping-method" in new ShippingMethodFixture {
      val api = cartsApi(refNum).shippingMethod
      checkResponse(api.update(UpdateShippingMethod(shipMethod.id)),
                    Seq(EmptyCart(refNum), InsufficientFunds(refNum)))
      checkResponse(api.delete(), Seq(EmptyCart(refNum), NoShipMethod(refNum)))
    }

    "/v1/carts/:refNum/line-items" in new LineItemFixture {

      checkResponse(cartsApi(refNum).lineItems.add(Seq(UpdateLineItemsPayload(sku.code, 1))),
                    Seq(InsufficientFunds(refNum), NoShipAddress(refNum), NoShipMethod(refNum)))
    }

    "/v1/carts/:refNum/coupon" in new CouponFixture with LineItemFixture {
      override def refNum = super[CouponFixture].refNum
      cartsApi(refNum).lineItems.add(Seq(UpdateLineItemsPayload(sku.code, 1)))
      override def expectedWarnings =
        Seq(NoShipAddress(refNum), NoShipMethod(refNum), InsufficientFunds(refNum))
      checkResponse(cartsApi(refNum).coupon.add(couponCode), expectedWarnings)
      checkResponse(cartsApi(refNum).coupon.delete(), expectedWarnings)
    }

    "must validate funds with line items:" - {
      "must return warning when credit card is removed" in new LineItemAndFundsFixture {
        val api = cartsApi(refNum)
        api.lineItems.add(Seq(UpdateLineItemsPayload(sku.code, 1))).mustBeOk()
        api.payments.creditCard.add(CreditCardPayment(creditCard.id)).mustBeOk()
        checkResponse(api.payments.creditCard.delete(),
                      Seq(NoShipAddress(refNum), NoShipMethod(refNum), InsufficientFunds(refNum)))
      }

      "must return warning when store credits are removed" in new LineItemAndFundsFixture {
        cartsApi(refNum).lineItems.add(Seq(UpdateLineItemsPayload(sku.code, 1))).mustBeOk()
        cartsApi(refNum).payments.storeCredit.add(StoreCreditPayment(500)).mustBeOk()
        checkResponse(cartsApi(refNum).payments.storeCredit.delete(),
                      Seq(NoShipAddress(refNum), NoShipMethod(refNum), InsufficientFunds(refNum)))
      }

      "must return warning when gift card is removed" in new LineItemAndFundsFixture {
        cartsApi(refNum).lineItems.add(Seq(UpdateLineItemsPayload(sku.code, 1))).mustBeOk()
        cartsApi(refNum).payments.giftCard.add(GiftCardPayment(giftCard.code)).mustBeOk()
        checkResponse(cartsApi(refNum).payments.giftCard.delete(giftCard.code),
                      Seq(NoShipAddress(refNum), NoShipMethod(refNum), InsufficientFunds(refNum)))
      }
    }

    def checkResponse(response: HttpResponse, expectedWarnings: Seq[Failure])(implicit line: SL,
                                                                              file: SF): Unit = {
      val warnings = response.asThe[CartResponse].warnings
      warnings.value must not be empty
      warnings.value must contain theSameElementsAs expectedWarnings.map(_.description)
    }
  }

  trait CouponFixture
      extends CouponSeeds
      with TestActivityContext.AdminAC
      with ExpectedWarningsForPayment
      with EmptyCustomerCart_Baked
      with StoreAdmin_Seed {

    def refNum = cart.refNum
    val (couponCode) = (for {
      search     ← * <~ Factories.createSharedSearches(storeAdmin.accountId)
      discounts  ← * <~ Factories.createDiscounts(search)
      promotions ← * <~ Factories.createCouponPromotions(discounts)
      coupons    ← * <~ Factories.createCoupons(promotions)
      couponCode = coupons.head._2.head.code
    } yield couponCode).gimme
  }

  trait LineItemFixture extends EmptyCustomerCart_Baked {
    val (sku) = (for {
      product ← * <~ Mvp.insertProduct(ctx.id, Factories.products.head)
      sku     ← * <~ Skus.mustFindById404(product.skuId)
    } yield sku).gimme
    def refNum = cart.refNum
  }

  trait ShippingMethodFixture extends EmptyCustomerCart_Baked {
    val (shipMethod, address) = (for {
      address    ← * <~ Addresses.create(Factories.address.copy(accountId = customer.accountId, regionId = 4129))
      _          ← * <~ address.bindToCart(cart.refNum)
      shipMethod ← * <~ ShippingMethods.create(Factories.shippingMethods.head)
    } yield (shipMethod, address)).gimme
    val refNum = cart.refNum
  }

  trait ShippingAddressFixture extends EmptyCustomerCart_Baked {
    val refNum           = cart.refNum
    val expectedWarnings = Seq(EmptyCart(refNum), NoShipMethod(refNum))
  }

  trait GiftCardFixture extends ExpectedWarningsForPayment with EmptyCustomerCart_Baked with Reason_Baked {
    val giftCard = (for {
      origin ← * <~ GiftCardManuals.create(
                GiftCardManual(adminId = storeAdmin.accountId, reasonId = reason.id))
      giftCard ← * <~ GiftCards.create(Factories.giftCard.copy(originId = origin.id, state = GiftCard.Active))
    } yield giftCard).gimme
    val refNum = cart.refNum
  }

  trait StoreCreditFixture extends ExpectedWarningsForPayment with EmptyCustomerCart_Baked with Reason_Baked {
    (for {
      manual ← * <~ StoreCreditManuals.create(
                StoreCreditManual(adminId = storeAdmin.accountId, reasonId = reason.id))
      _ ← * <~ StoreCredits.create(
           Factories.storeCredit
             .copy(state = StoreCredit.Active, accountId = customer.accountId, originId = manual.id))
    } yield {}).gimme
    val refNum = cart.refNum
  }

  trait CreditCardFixture
      extends ExpectedWarningsForPayment
      with EmptyCustomerCart_Baked
      with CustomerAddress_Raw {
    val creditCard =
      CreditCards.create(Factories.creditCard.copy(accountId = customer.accountId)).gimme
    val refNum = cart.refNum
  }

  trait LineItemAndFundsFixture extends Reason_Baked with Customer_Seed {
    val (refNum, sku, creditCard, giftCard) = (for {
      _          ← * <~ Addresses.create(Factories.address.copy(accountId = customer.accountId))
      cc         ← * <~ CreditCards.create(Factories.creditCard.copy(accountId = customer.accountId))
      productCtx ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      cart       ← * <~ Carts.create(Factories.cart(Scope.current).copy(accountId = customer.accountId))
      product    ← * <~ Mvp.insertProduct(productCtx.id, Factories.products.head)
      sku        ← * <~ Skus.mustFindById404(product.skuId)
      manual ← * <~ StoreCreditManuals.create(
                StoreCreditManual(adminId = storeAdmin.accountId, reasonId = reason.id))
      _ ← * <~ StoreCredits.create(
           Factories.storeCredit
             .copy(state = StoreCredit.Active, accountId = customer.accountId, originId = manual.id))
      origin ← * <~ GiftCardManuals.create(
                GiftCardManual(adminId = storeAdmin.accountId, reasonId = reason.id))
      giftCard ← * <~ GiftCards.create(Factories.giftCard.copy(originId = origin.id, state = GiftCard.Active))
    } yield (cart.refNum, sku, cc, giftCard)).gimme
  }

  trait ExpectedWarningsForPayment {
    def refNum: String
    def expectedWarnings = Seq(EmptyCart(refNum), NoShipAddress(refNum), NoShipMethod(refNum))
  }
}
