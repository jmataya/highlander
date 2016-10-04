import java.time.Instant

import akka.http.scaladsl.model.StatusCodes

import Extensions._
import cats.implicits._
import failures.UserFailures._
import failures.NotFoundFailure404
import failures.ShippingMethodFailures.ShippingMethodNotFoundByName
import models.Reasons
import models.cord.Order.RemorseHold
import models.cord._
import models.account._
import models.customer._
import models.inventory._
import models.location.Addresses
import models.payment.giftcard._
import models.product.Mvp
import models.shipping._
import payloads.GiftCardPayloads.GiftCardCreateByCsr
import payloads.LineItemPayloads.UpdateLineItemsPayload
import payloads.OrderPayloads.CreateCart
import payloads.PaymentPayloads.GiftCardPayment
import payloads.UpdateShippingMethod
import responses.GiftCardResponse
import responses.cord._
import slick.driver.PostgresDriver.api._
import util._
import util.fixtures.BakedFixtures
import utils.db._
import utils.seeds.Seeds.Factories

class CheckoutIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with AutomaticAuth
    with BakedFixtures {

  "POST v1/orders/:refNum/checkout" - {

    "places order as admin" in new Fixture {
      // Create cart
      val createCart = POST("v1/orders", CreateCart(Some(customer.accountId)))
      createCart.status must === (StatusCodes.OK)
      val refNum = createCart.as[CartResponse].referenceNumber
      // Add line items
      POST(s"v1/orders/$refNum/line-items", Seq(UpdateLineItemsPayload(sku.code, 2))).status must === (
          StatusCodes.OK)
      // Set address
      PATCH(s"v1/orders/$refNum/shipping-address/${address.id}").status must === (StatusCodes.OK)
      // Set shipping method
      val setShipMethod =
        PATCH(s"v1/orders/$refNum/shipping-method", UpdateShippingMethod(shipMethod.id))
      setShipMethod.status must === (StatusCodes.OK)
      val grandTotal = setShipMethod.ignoreFailuresAndGiveMe[CartResponse].totals.total
      // Pay
      val createGiftCard = POST("v1/gift-cards", GiftCardCreateByCsr(grandTotal, reason.id))
      createGiftCard.status must === (StatusCodes.OK)
      val gcCode    = createGiftCard.as[GiftCardResponse.Root].code
      val gcPayload = GiftCardPayment(gcCode, grandTotal.some)
      POST(s"v1/orders/$refNum/payment-methods/gift-cards", gcPayload).status must === (
          StatusCodes.OK)

      // Checkout!
      val checkout = POST(s"v1/orders/$refNum/checkout")
      checkout.status must === (StatusCodes.OK)

      val orderResponse = checkout.as[OrderResponse]

      // Checkout:
      // Triggers cart → order transition
      Orders.findOneByRefNum(refNum).gimme mustBe defined
      Carts.findOneByRefNum(refNum).gimme must not be defined

      // Properly creates an order
      orderResponse.orderState must === (Order.RemorseHold)
      orderResponse.remorsePeriodEnd.value.isAfter(Instant.now) mustBe true

      // Authorizes payments
      GiftCardAdjustments.map(_.state).gimme must contain only GiftCardAdjustment.Auth
    }

    "fails if customer's credentials are empty" in new Fixture {
      // Create cart
      val createCart = POST("v1/orders", CreateCart(Some(customer.accountId)))
      createCart.status must === (StatusCodes.OK)
      val refNum = createCart.as[CartResponse].referenceNumber

      // Update customer
      Users.update(customer, customer.copy(email = None)).run().futureValue

      // Checkout!
      val checkout = POST(s"v1/orders/$refNum/checkout")
      checkout.error must === (UserMustHaveCredentials.description)
    }

    "fails if AFS is zero" in new Fixture {
      // FIXME #middlewarehouse
      pending

      //Create cart
      val refNum =
        POST("v1/orders", CreateCart(Some(customer.accountId))).as[OrderResponse].referenceNumber

      POST(s"v1/orders/$refNum/line-items", Seq(UpdateLineItemsPayload(sku.code, 2))).status must === (
          StatusCodes.OK)

      // Set address
      PATCH(s"v1/orders/$refNum/shipping-address/${address.id}").status must === (StatusCodes.OK)
      // Set shipping method
      val setShipMethod =
        PATCH(s"v1/orders/$refNum/shipping-method", UpdateShippingMethod(shipMethod.id))
      setShipMethod.status must === (StatusCodes.OK)
      val grandTotal = setShipMethod.ignoreFailuresAndGiveMe[OrderResponse].totals.total

      // Pay
      val createGiftCard = POST("v1/gift-cards", GiftCardCreateByCsr(grandTotal, reason.id))
      createGiftCard.status must === (StatusCodes.OK)
      val gcCode    = createGiftCard.as[GiftCardResponse.Root].code
      val gcPayload = GiftCardPayment(gcCode, grandTotal.some)
      POST(s"v1/orders/$refNum/payment-methods/gift-cards", gcPayload).status must === (
          StatusCodes.OK)

      // Checkout!
      val checkout = POST(s"v1/orders/$refNum/checkout")
      checkout.status must === (StatusCodes.OK)

      val order = Orders.findOneByRefNum(refNum).gimme.value
      order.state must === (RemorseHold)
    }

    "errors 404 if no cart found by reference number" in {
      val response = POST("v1/orders/NOPE/checkout")
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Cart, "NOPE").description)
    }

    "fails if customer is blacklisted" in new BlacklistedFixture {
      val createCart = POST("v1/orders", CreateCart(Some(customer.accountId)))
      createCart.status must === (StatusCodes.OK)
      val refNum = createCart.as[CartResponse].referenceNumber
      // Add line items
      POST(s"v1/orders/$refNum/line-items", Seq(UpdateLineItemsPayload(sku.code, 2))).status must === (
          StatusCodes.OK)
      // Set address
      PATCH(s"v1/orders/$refNum/shipping-address/${address.id}").status must === (StatusCodes.OK)
      // Set shipping method
      val setShipMethod =
        PATCH(s"v1/orders/$refNum/shipping-method", UpdateShippingMethod(shipMethod.id))
      setShipMethod.status must === (StatusCodes.OK)
      val grandTotal = setShipMethod.ignoreFailuresAndGiveMe[CartResponse].totals.total
      // Pay
      val createGiftCard = POST("v1/gift-cards", GiftCardCreateByCsr(grandTotal, reason.id))
      createGiftCard.status must === (StatusCodes.OK)
      val gcCode    = createGiftCard.as[GiftCardResponse.Root].code
      val gcPayload = GiftCardPayment(gcCode, grandTotal.some)
      POST(s"v1/orders/$refNum/payment-methods/gift-cards", gcPayload).status must === (
          StatusCodes.OK)

      // Checkout!
      val checkout = POST(s"v1/orders/$refNum/checkout")
      checkout.status must === (StatusCodes.BadRequest)

      checkout.error must === (UserIsBlacklisted(customer.accountId).description)
    }
  }

  trait FullCartWithGcPayment
      extends Reason_Baked
      with EmptyCartWithShipAddress_Baked
      with FullCart_Raw
      with GiftCard_Raw
      with CartWithGiftCardPayment_Raw

  trait Fixture extends StoreAdmin_Seed with CustomerAddress_Baked {

    implicit val au = storeAdminAuthData

    val (shipMethod, product, sku, reason) = (for {
      _ ← * <~ Factories.shippingMethods.map(ShippingMethods.create)
      shipMethodName = ShippingMethod.expressShippingNameForAdmin
      shipMethod ← * <~ ShippingMethods
                    .filter(_.adminDisplayName === shipMethodName)
                    .mustFindOneOr(ShippingMethodNotFoundByName(shipMethodName))
      product ← * <~ Mvp.insertProduct(ctx.id, Factories.products.head)
      sku     ← * <~ Skus.mustFindById404(product.skuId)
      reason  ← * <~ Reasons.create(Factories.reason(storeAdmin.accountId))
    } yield (shipMethod, product, sku, reason)).gimme
  }

  trait BlacklistedFixture extends StoreAdmin_Seed {

    implicit val au = storeAdminAuthData

    val (customer, address, shipMethod, product, sku, reason) = (for {
      account ← * <~ Accounts.create(Account())
      customer ← * <~ Users.create(
                    Factories.customer.copy(accountId = account.id,
                                            isBlacklisted = true,
                                            blacklistedBy = Some(storeAdmin.accountId)))
      custUser ← * <~ CustomerUsers.create(
                    CustomerUser(userId = customer.id, accountId = account.id))
      address ← * <~ Addresses.create(Factories.usAddress1.copy(accountId = customer.accountId))
      _       ← * <~ Factories.shippingMethods.map(ShippingMethods.create)
      shipMethod ← * <~ ShippingMethods
                    .filter(_.adminDisplayName === ShippingMethod.expressShippingNameForAdmin)
                    .mustFindOneOr(
                        ShippingMethodNotFoundByName(ShippingMethod.expressShippingNameForAdmin))
      product ← * <~ Mvp.insertProduct(ctx.id, Factories.products.head)
      sku     ← * <~ Skus.mustFindById404(product.skuId)
      reason  ← * <~ Reasons.create(Factories.reason(storeAdmin.accountId))
    } yield (customer, address, shipMethod, product, sku, reason)).gimme
  }
}
