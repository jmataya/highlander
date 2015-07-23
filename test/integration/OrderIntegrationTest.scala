import akka.http.scaladsl.model.StatusCodes
import models._
import org.joda.time.DateTime
import org.scalatest.time.{Milliseconds, Seconds, Span}
import payloads.{CreateAddressPayload, CreditCardPayload}
import responses.{AdminNotes, FullOrder}
import services.NoteManager
import util.{IntegrationTestBase, StripeSupport}
import utils.Seeds.Factories

/**
 * The Server is shut down by shutting down the ActorSystem
 */
class OrderIntegrationTest extends IntegrationTestBase
  with HttpSupport /** FIXME: Add to IntegrationTestBase once they no longer live in the root package */
  with AutomaticAuth {

  import concurrent.ExecutionContext.Implicits.global
  import org.json4s.jackson.JsonMethods._
  import Extensions._

  "returns new items" in {
    pending
    val orderId = db.run(Orders.returningId += Order(id = 0, customerId = 1)).futureValue

    val response = POST(
      s"v1/orders/$orderId/line-items",
       """
         | [ { "skuId": 1, "quantity": 1 },
         |   { "skuId": 5, "quantity": 2 } ]
       """.stripMargin)

    val order = parse(response.bodyText).extract[FullOrder.Root]
    order.lineItems.map(_.skuId).sortBy(identity) must === (List(1, 5, 5))
  }

  "handles credit cards" - {
    val today = new DateTime
    val customerStub = Customer(email = "yax@yax.com", password = "password", firstName = "Yax", lastName = "Fuentes")
    val payload = CreditCardPayload(holderName = "Jax", number = StripeSupport.successfulCard, cvv = "123",
      expYear = today.getYear + 1, expMonth = today.getMonthOfYear, isDefault = true)

    "fails if the order is not found" in {
      val response = POST(
        s"v1/orders/5/payment-methods/credit-card",
        payload)

      response.status must === (StatusCodes.NotFound)
    }

    "fails if the payload is invalid" in {
      val orderId = db.run(Orders.returningId += Order(id = 0, customerId = 1)).futureValue
      val response = POST(
        s"v1/orders/$orderId/payment-methods/credit-card",
        payload.copy(cvv = "", holderName = ""))

      val errors = parse(response.bodyText).extract[Map[String, Seq[String]]]

      errors must === (Map("errors" -> Seq("holderName must not be empty", "cvv must match regular expression " +
        "'[0-9]{3,4}'")))
      response.status must === (StatusCodes.BadRequest)
    }

    "fails if the card is invalid according to Stripe" ignore {
      val orderId = db.run(Orders.returningId += Order(id = 0, customerId = 1)).futureValue
      val customerId = db.run(Customers.returningId += customerStub).futureValue
      val response = POST(
        s"v1/orders/$orderId/payment-methods/credit-card",
        payload.copy(number = StripeSupport.declinedCard))

      val body = response.bodyText
      val errors = parse(body).extract[Map[String, Seq[String]]]

      errors must === (Map("errors" -> Seq("Your card was declined.")))
      response.status must === (StatusCodes.BadRequest)
    }

    "successfully creates records" ignore {
      val orderId = db.run(Orders.returningId += Order(id = 0, customerId = 1)).futureValue
      val customerId = db.run(Customers.returningId += customerStub).futureValue
      val customer = customerStub.copy(id = customerId)
      val addressPayload = CreateAddressPayload(name = "Home", stateId = 46, state = Some("VA"), street1 = "500 Blah",
        city = "Richmond", zip = "50000")
      val payloadWithAddress = payload.copy(address = Some(addressPayload))

      val response = POST(
        s"v1/orders/$orderId/payment-methods/credit-card",
        payloadWithAddress)

      val body = response.bodyText

      val cc = CreditCards.findById(1).futureValue.get
      val payment = OrderPayments.findAllByOrderId(orderId).futureValue.head
      val (address, billingAddress) = BillingAddresses.findByPaymentId(payment.id).futureValue.get

      val order = parse(body).extract[FullOrder.Root]

      cc.customerId must === (customerId)
      cc.lastFour must === (payload.lastFour)
      cc.expMonth must === (payload.expMonth)
      cc.expYear must === (payload.expYear)
      cc.isDefault must === (true)

      payment.appliedAmount must === (0)
      payment.orderId must === (orderId)
      payment.status must === ("auth")

      response.status must === (StatusCodes.OK)

      address.stateId must === (addressPayload.stateId)
      address.customerId must === (customerId)
    }
  }

  "notes" - {
    "can be created by an admin for an order" in new Fixture {
      val response = POST(s"v1/orders/${order.id}/notes", payloads.CreateNote(body = "Hello, FoxCommerce!"))

      response.status must === (StatusCodes.OK)

      val note = parse(response.bodyText).extract[AdminNotes.Root]

      note.body must === ("Hello, FoxCommerce!")
      note.author must === (AdminNotes.buildAuthor(storeAdmin))
    }

    "returns a validation error if failed to create" in new Fixture {
      val response = POST(s"v1/orders/${order.id}/notes", payloads.CreateNote(body = ""))

      response.status must === (StatusCodes.BadRequest)
      response.bodyText must include ("errors")
    }

    "returns a 404 if the order is not found" in new Fixture {
      val response = POST(s"v1/orders/99/notes", payloads.CreateNote(body = ""))

      response.status must === (StatusCodes.NotFound)
      response.bodyText must be ('empty)
    }

//    "are soft deleted" in {
//      val response = DELETE(s"v1/orders/${order.id}/notes/${note.id}")
//    }

    "can be listed" in new Fixture {
      List("abc", "123", "xyz").map { body ⇒
        NoteManager.createOrderNote(order, storeAdmin, payloads.CreateNote(body = body)).futureValue
      }

      val response = GET(s"v1/orders/${order.id}/notes")
      response.status must === (StatusCodes.OK)

      val notes = parse(response.bodyText).extract[Seq[AdminNotes.Root]]

      notes must have size (3)
      notes.map(_.body).toSet must === (Set("abc", "123", "xyz"))
    }

    "can update the body text" in new Fixture {
      val rootNote = NoteManager.createOrderNote(order, storeAdmin,
        payloads.CreateNote(body = "Hello, FoxCommerce!")).futureValue.get

      val response = PATCH(s"v1/orders/${order.id}/notes/${rootNote.id}", payloads.UpdateNote(body = "donkey"))
      response.status must === (StatusCodes.OK)

      val note = parse(response.bodyText).extract[AdminNotes.Root]
      note.body must === ("donkey")
    }

    trait Fixture {
      val (order, storeAdmin) = (for {
        order ← Orders.save(Factories.order)
        storeAdmin ← StoreAdmins.save(authedStoreAdmin)
      } yield (order, storeAdmin)).run().futureValue
    }
  }
}

