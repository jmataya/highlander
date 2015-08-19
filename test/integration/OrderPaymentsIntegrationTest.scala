import akka.http.scaladsl.model.StatusCodes

import models.Order._
import models._
import services.{CannotUseInactiveCreditCard, CustomerHasInsufficientStoreCredit, CustomerManager, GiftCardIsInactive,
GiftCardNotEnoughBalance, GiftCardNotFoundFailure, NotFoundFailure, OrderNotFoundFailure}
import slick.driver.PostgresDriver.api._
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils._

class OrderPaymentsIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with AutomaticAuth {

  import concurrent.ExecutionContext.Implicits.global

  "gift cards" - {
    "when added as a payment method" - {
      "succeeds" in new GiftCardFixture {
        val payload = payloads.GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance)
        val response = POST(s"v1/orders/${order.referenceNumber}/payment-methods/gift-cards", payload)

        response.status must ===(StatusCodes.OK)
        val (p :: Nil) = OrderPayments.findAllByOrderId(order.id).result.run().futureValue.toList

        p.paymentMethodType must ===(PaymentMethod.GiftCard)
        p.amount must ===((Some(payload.amount)))
      }

      "fails if the order is not found" in new GiftCardFixture {
        val payload = payloads.GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance)
        val response = POST(s"v1/orders/ABC123/payment-methods/gift-cards", payload)

        response.status must === (StatusCodes.NotFound)
      }

      "fails if the giftCard is not found" in new GiftCardFixture {
        val payload = payloads.GiftCardPayment(code = giftCard.code ++ "xyz", amount = giftCard.availableBalance)
        val response = POST(s"v1/orders/${order.referenceNumber}/payment-methods/gift-cards", payload)

        response.status must === (StatusCodes.NotFound)
        parseErrors(response).get.head must === (GiftCardNotFoundFailure(payload.code).description.head)
      }

      "fails if the giftCard does not have sufficient available balance" in new GiftCardFixture {
        val payload = payloads.GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance + 1)
        val response = POST(s"v1/orders/${order.referenceNumber}/payment-methods/gift-cards", payload)

        response.status must === (StatusCodes.BadRequest)
        parseErrors(response).get.head must === (GiftCardNotEnoughBalance(giftCard, payload.amount).description.head)
      }

      "fails if the giftCard is inactive" in new GiftCardFixture {
        GiftCards.findByCode(giftCard.code).map(_.status).update(GiftCard.Canceled).run().futureValue
        val payload = payloads.GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance + 1)
        val response = POST(s"v1/orders/${order.referenceNumber}/payment-methods/gift-cards", payload)

        response.status must === (StatusCodes.BadRequest)
        parseErrors(response).get.head must === (GiftCardIsInactive(giftCard).description.head)
      }
    }
  }

  "store credit" - {
    "when added as a payment method" - {
      "succeeds" in new StoreCreditFixture {
        val payload = payloads.StoreCreditPayment(amount = 100)
        val response = POST(s"v1/orders/${order.refNum}/payment-methods/store-credit", payload)

        response.status must ===(StatusCodes.OK)
        val payments = OrderPayments.findAllByOrderId(order.id).result.run().futureValue.toList

        payments must have size(2)
        payments.filter(_.paymentMethodType === PaymentMethod.StoreCredit) must have size(2)
      }

      "fails if the order is not found" in new Fixture {
        val notFound = order.copy(referenceNumber = "ABC123")
        val payload = payloads.StoreCreditPayment(amount = 50)
        val response = POST(s"v1/orders/${notFound.refNum}/payment-methods/store-credit", payload)

        response.status must === (StatusCodes.NotFound)
        parseErrors(response).get.head must === (OrderNotFoundFailure(notFound).description.head)
      }

      "fails if the customer has no active store credit" in new Fixture {
        val payload = payloads.StoreCreditPayment(amount = 50)
        val response = POST(s"v1/orders/${order.refNum}/payment-methods/store-credit", payload)

        response.status must === (StatusCodes.BadRequest)
        val error = CustomerHasInsufficientStoreCredit(customer.id, 0, 50).description.head
        parseErrors(response).get.head must === (error)
      }

      "fails if the customer has insufficient available store credit" in new StoreCreditFixture {
        val payload = payloads.StoreCreditPayment(amount = 101)
        val response = POST(s"v1/orders/${order.refNum}/payment-methods/store-credit", payload)

        response.status must === (StatusCodes.BadRequest)
        val has = storeCredits.map(_.availableBalance).sum
        val error = CustomerHasInsufficientStoreCredit(customer.id, has, payload.amount).description.head
        parseErrors(response).get.head must === (error)
      }
    }
  }

  "deleting a payment" - {
    "successfully deletes" in new GiftCardFixture {
      val payload = payloads.GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance)
      val payment = services.OrderUpdater.addGiftCard(order.referenceNumber, payload).futureValue.get

      val response = DELETE(s"v1/orders/${order.referenceNumber}/payment-methods/${payment.id}")
      response.status must ===(StatusCodes.NoContent)

      val payments = OrderPayments.findAllByOrderId(order.id).result.run().futureValue
      payments must have size (0)
    }

    "fails if the order is not found" in new GiftCardFixture {
      val response = DELETE(s"v1/orders/ABCAYXADSF/payment-methods/1")
      response.status must ===(StatusCodes.NotFound)
    }

    "fails if the payment is not found" in new GiftCardFixture {
      val response = DELETE(s"v1/orders/${order.referenceNumber}/payment-methods/1")
      response.status must ===(StatusCodes.NotFound)
    }
  }

  "credit cards" - {
    "when added as a payment method" - {
      "succeeds" in new CreditCardFixture {
        val response = POST(s"v1/orders/${order.referenceNumber}/payment-methods/credit-cards/${creditCard.id}")

        response.status must === (StatusCodes.OK)
        val (p :: Nil) = OrderPayments.findAllByOrderId(order.id).result.run().futureValue.toList

        p.paymentMethodType must === (PaymentMethod.CreditCard)
        p.amount must === (None)
      }

      "fails if the order is not found" in new CreditCardFixture {
        val response = POST(s"v1/orders/99/payment-methods/credit-cards/${creditCard.id}")
        response.status must === (StatusCodes.NotFound)
      }

      "fails if the giftCard is not found" in new CreditCardFixture {
        val response = POST(s"v1/orders/${order.referenceNumber}/payment-methods/credit-cards/99")
        response.status must === (StatusCodes.NotFound)
        parseErrors(response).get.head must === (NotFoundFailure(CreditCard, 99).description.head)
      }

      "fails if the creditCard is inActive" in new CreditCardFixture {
        CustomerManager.deleteCreditCard(customerId = customer.id, adminId = admin.id, id = creditCard.id).futureValue
        val response = POST(s"v1/orders/${order.referenceNumber}/payment-methods/credit-cards/${creditCard.id}")
        response.status must ===(StatusCodes.BadRequest)
        parseErrors(response).get.head must ===(CannotUseInactiveCreditCard(creditCard).description.head)
      }

      "fails if the order is not in cart status" in new CreditCardFixture {
        Orders.findCartByRefNum(order.referenceNumber).map(_.status).update(Order.RemorseHold)
        val first = POST(s"v1/orders/${order.referenceNumber}/payment-methods/credit-cards/${creditCard.id}")
        first.status must ===(StatusCodes.OK)

        val response = POST(s"v1/orders/${order.referenceNumber}/payment-methods/credit-cards/${creditCard.id}")
        response.status must === (StatusCodes.BadRequest)

        val payments = OrderPayments.findAllCreditCardsForOrder(order.id).result.run().futureValue
        payments must have size(1)
      }

      "fails if the cart already has a credit card" in new CreditCardFixture {
        val first = POST(s"v1/orders/${order.referenceNumber}/payment-methods/credit-cards/${creditCard.id}")
        first.status must ===(StatusCodes.OK)

        val response = POST(s"v1/orders/${order.referenceNumber}/payment-methods/credit-cards/${creditCard.id}")
        response.status must === (StatusCodes.BadRequest)

        val payments = OrderPayments.findAllCreditCardsForOrder(order.id).result.run().futureValue
        payments must have size(1)
      }
    }
  }

  trait Fixture {
    val (order, admin, customer) = (for {
      customer ← Customers.save(Factories.customer)
      order ← Orders.save(Factories.order.copy(customerId = customer.id, status = Order.Cart))
      admin ← StoreAdmins.save(authedStoreAdmin)
    } yield (order, admin, customer)).run().futureValue
  }

  trait AddressFixture extends Fixture {
    val address = Addresses.save(Factories.address.copy(customerId = customer.id)).run().futureValue
  }

  trait GiftCardFixture extends Fixture {
    val giftCard = (for {
      reason ← Reasons.save(Factories.reason.copy(storeAdminId = admin.id))
      origin ← GiftCardManuals.save(Factories.giftCardManual.copy(adminId = admin.id, reasonId = reason.id))
      giftCard ← GiftCards.save(Factories.giftCard.copy(originId = origin.id, status = GiftCard.Active))
    } yield giftCard).run().futureValue
  }

  trait StoreCreditFixture extends Fixture {
    val storeCredits = (for {
      reason ← Reasons.save(Factories.reason.copy(storeAdminId = admin.id))
      _ ← StoreCreditManuals ++= (1 to 2).map { _ ⇒
        Factories.storeCreditManual.copy(adminId = admin.id, reasonId = reason.id)
      }
      _ ← StoreCredits ++= (1 to 2).map { i ⇒
        Factories.storeCredit.copy(status = StoreCredit.Active, customerId = customer.id, originId = i)
      }
      storeCredits ← StoreCredits._findAllByCustomerId(customer.id)
    } yield storeCredits).run().futureValue
  }

  trait CreditCardFixture extends Fixture {
    val creditCard = (for {
      address ← Addresses.save(Factories.address.copy(customerId = customer.id))
      cc ← CreditCards.save(Factories.creditCard.copy(customerId = customer.id, billingAddressId = address.id))
    } yield cc).run().futureValue
  }
}

