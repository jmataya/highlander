package services

import cats.data.Xor
import models.{Address, Addresses, OrderPayment, OrderPayments, CreditCard, CreditCards, Customer, Customers, Order, OrderLineItem, OrderLineItems, Orders}

import org.scalatest.Inside
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils.Slick.implicits._

class CheckoutTest extends IntegrationTestBase with Inside {
  import concurrent.ExecutionContext.Implicits.global

  "Checkout" - {
    "checkout" - {
      "returns a new Order with the 'Cart' status" ignore { /** Needs Stripe mocks */
        val (order, _) = testData()

        val lineItemStub1 = OrderLineItem(id = 0, orderId = 0, originId = 1, originType = OrderLineItem.SkuItem)
        val lineItemStub2 = OrderLineItem(id = 0, orderId = 0, originId = 2, originType = OrderLineItem.SkuItem)

        val actions = for {
          _ ← OrderLineItems.returningId += lineItemStub1.copy(orderId = order.id)
          _ ← OrderLineItems.returningId += lineItemStub2.copy(orderId = order.id)
        } yield ()

        actions.run()

        val checkout = new Checkout(order)
        val result   = checkout.checkout.futureValue

        val newOrder = result.get
        newOrder.status must === (Order.Cart)
        newOrder.id     must !== (order.id)
      }

      "returns an errors if it has no line items" in {
        val (order, _) = testData()
        val checkout = new Checkout(order)

        val result = checkout.checkout.futureValue
        result mustBe 'left

        inside(result) {
          case Xor.Left(nel) ⇒
            inside(nel.head) {
              case NotFoundFailure404(message) ⇒
                message must include ("No Line Items")
            }
        }
      }

      /** Test data leak? */

      "returns an error if authorizePayments fails" in {
        pending /** Need a way to mock Stripe */

        val (order, _) = testData(gatewayCustomerId = "")

        val lineItemStub1 = OrderLineItem(id = 0, orderId = 0, originId = 1, originType = OrderLineItem.SkuItem)
        val lineItemStub2 = OrderLineItem(id = 0, orderId = 0, originId = 2, originType = OrderLineItem.SkuItem)

        val actions = for {
          _ ← OrderLineItems.returningId += lineItemStub1.copy(orderId = order.id)
          _ ← OrderLineItems.returningId += lineItemStub2.copy(orderId = order.id)
        } yield ()

        actions.run()

        val checkout = new Checkout(order)
        val result   = checkout.checkout.futureValue

        inside(result) {
          case Xor.Left(nel) ⇒
            inside(nel.head) {
              case StripeFailure(errorMessage) ⇒
                errorMessage.getMessage must include ("cannot set 'customer' to an empty string.")
            }
        }

      }
    }

    "Authorizes each payment" in pendingUntilFixed { /** Needs Stripe mocks */
      fail("fix me")
    }
  }

  def testData(gatewayCustomerId:String = "cus_6Rh139qdpaFdRP") = {
    val customerStub = Customer(email = "yax@yax.com", password = Some("password"), name = Some("Yax Fuentes"))
    val orderStub    = Order(id = 0, customerId = 0)
    val addressStub  = Address(id = 0, customerId = 0, regionId = 1, name = "Yax Home", address1 = "555 E Lake Union " +
      "St.", address2 = None, city = "Seattle", zip = "12345", phoneNumber = None)
    val gatewayStub  = Factories.creditCard.copy(gatewayCustomerId = gatewayCustomerId, lastFour = "4242",
      expMonth = 11, expYear = 2018)

    val (payment, order) = (for {
      customer ← (Customers.returningId += customerStub).map(id ⇒ customerStub.copy(id = id))
      order    ← Orders.saveNew(orderStub.copy(customerId = customer.id))
      address  ← Addresses.saveNew(addressStub.copy(customerId = customer.id))
      creditCard ← CreditCards.saveNew(gatewayStub.copy(customerId = customer.id))
      payment  ← OrderPayments.saveNew(Factories.orderPayment.copy(orderId = order.id, paymentMethodId = creditCard.id))
    } yield (payment, order)).run().futureValue

    (order, payment)
  }
}
