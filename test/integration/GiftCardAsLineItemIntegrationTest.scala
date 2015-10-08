import akka.http.scaladsl.model.StatusCodes
import models._
import responses._
import org.scalatest.BeforeAndAfterEach
import services._
import slick.driver.PostgresDriver.api._
import util.IntegrationTestBase
import utils.Money._
import utils.Seeds.Factories
import utils.Slick.implicits._

class GiftCardAsLineItemIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with AutomaticAuth
  with BeforeAndAfterEach {

  import concurrent.ExecutionContext.Implicits.global
  import Extensions._
  import org.json4s.jackson.JsonMethods._

  "POST /v1/orders/:refNum/gift-cards" - {
    "successuflly creates new GC as line item" in new LineItemFixture {
      val response = POST(s"v1/orders/${order.refNum}/gift-cards", payloads.AddGiftCardLineItem(balance = 100))
      val root = response.as[FullOrder.Root]

      response.status must ===(StatusCodes.OK)
      root.lineItems.giftCards.size must === (2)

      val newGiftCard = root.lineItems.giftCards.tail.head
      newGiftCard.originalBalance must === (100)
      newGiftCard.currentBalance must === (100)
      newGiftCard.availableBalance must === (100)
      newGiftCard.status must === (GiftCard.Cart)
    }

    "fails to create new GC as line item if no cart order is present" in new LineItemFixture {
      Orders._findActiveOrderByCustomer(customer).map(_.status).update(Order.ManualHold).run().futureValue
      val response = POST(s"v1/orders/${order.refNum}/gift-cards", payloads.AddGiftCardLineItem(balance = 100))

      response.status must ===(StatusCodes.NotFound)
      response.errors must ===(GeneralFailure("Not found").description)
    }

    "fails to create new GC with invalid balance" in new LineItemFixture {
      Orders._findActiveOrderByCustomer(customer).map(_.status).update(Order.ManualHold).run().futureValue
      val response = POST(s"v1/orders/${order.refNum}/gift-cards", payloads.AddGiftCardLineItem(balance = -100))

      response.status must ===(StatusCodes.BadRequest)
      response.errors must ===(GeneralFailure("Balance must be greater than zero").description)
    }
  }

  "PATCH /v1/orders/:refNum/gift-cards/:code" - {
    "successuflly updates GC as line item" in new LineItemFixture {
      val response = PATCH(s"v1/orders/${order.refNum}/gift-cards/${giftCard.code}", payloads.AddGiftCardLineItem(balance = 555))
      val root = response.as[FullOrder.Root]

      response.status must ===(StatusCodes.OK)
      root.lineItems.giftCards.size must === (1)

      val newGiftCard = root.lineItems.giftCards.head
      newGiftCard.originalBalance must === (555)
      newGiftCard.currentBalance must === (555)
      newGiftCard.availableBalance must === (555)
      newGiftCard.status must === (GiftCard.Cart)
    }

    "fails to update GC as line item for order not in cart state" in new LineItemFixture {
      Orders._findActiveOrderByCustomer(customer).map(_.status).update(Order.ManualHold).run().futureValue
      val response = PATCH(s"v1/orders/${order.refNum}/gift-cards/${giftCard.code}", payloads.AddGiftCardLineItem(balance = 100))

      response.status must ===(StatusCodes.NotFound)
      response.errors must ===(GeneralFailure("Order not found").description)
    }

    "fails to update GC setting invalid balance" in new LineItemFixture {
      val response = PATCH(s"v1/orders/${order.refNum}/gift-cards/${giftCard.code}", payloads.AddGiftCardLineItem(balance = -100))

      response.status must ===(StatusCodes.BadRequest)
      response.errors must ===(GeneralFailure("Balance must be greater than zero").description)
    }
  }

  "DELETE /v1/orders/:refNum/gift-cards/:code" - {
    "successuflly deletes GC as line item" in new LineItemFixture {
      val response = DELETE(s"v1/orders/${order.refNum}/gift-cards/${giftCard.code}")
      val root = response.as[FullOrder.Root]

      response.status must ===(StatusCodes.OK)
      root.lineItems.giftCards.size must === (0)

      GiftCards.findByCode(giftCard.code).one.run().futureValue.isEmpty mustBe true
    }

    "fails to delete GC as line item for order not in cart state" in new LineItemFixture {
      Orders._findActiveOrderByCustomer(customer).map(_.status).update(Order.ManualHold).run().futureValue
      val response = DELETE(s"v1/orders/${order.refNum}/gift-cards/${giftCard.code}")

      response.status must ===(StatusCodes.NotFound)
      response.errors must ===(GeneralFailure("Order not found").description)
    }
  }

  "POST /v1/orders/:refNum/payments-methods/gift-cards" - {
    "fails to add GC with cart status as payment method" in new LineItemFixture {
      val payload = payloads.GiftCardPayment(code = giftCard.code, amount = 15)
      val response = POST(s"v1/orders/${order.refNum}/payment-methods/gift-cards", payload)

      response.status must ===(StatusCodes.NotFound)
      response.errors must ===(GiftCardNotFoundFailure(giftCard.code).description)
    }
  }

  trait LineItemFixture {
    val (customer, order, giftCard) = (for {
      customer ← Customers.save(Factories.customer)
      order ← Orders.save(Factories.order.copy(customerId = customer.id, status = Order.Cart))

      gcOrigin ← GiftCardOrders.save(GiftCardOrder(orderId = order.id))
      giftCard ← GiftCards.save(GiftCard.buildLineItem(balance = 150, originId = gcOrigin.id, currency = Currency.USD))
      lineItemGc ← OrderLineItemGiftCards.save(OrderLineItemGiftCard(giftCardId = giftCard.id, orderId = order.id))
      lineItem ← OrderLineItems.save(OrderLineItem.buildGiftCard(order, lineItemGc))
    } yield (customer, order, giftCard)).run().futureValue
  }
}
