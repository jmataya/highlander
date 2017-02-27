import cats.implicits._
import models.returns.Return
import models.returns.Return.{Pending, Processing, ReturnType}
import org.json4s.JObject
import payloads.ReturnPayloads.{ReturnMessageToCustomerPayload, ReturnUpdateStatePayload}
import responses.ReturnResponse
import testutils.fixtures.{BakedFixtures, ReturnsFixtures}
import testutils._

case class ReturnsSearchViewResult(
    id: Int,
    referenceNumber: String,
    orderId: Int,
    orderRef: String,
    state: Return.State,
    messageToAccount: Option[String],
    returnType: ReturnType,
    customer: JObject
)

case class CustomerSearchViewResult(
    id: Int,
    name: String,
    email: String,
    isBlacklisted: Boolean,
    joinedAt: String,
    revenue: Int
)

class ReturnsSearchViewTest
    extends SearchViewTestBase
    with TestSeeds
    with BakedFixtures
    with ReturnsFixtures {

  type SearchViewResult = ReturnsSearchViewResult
  val searchViewName: String = "returns_search_view"
  val searchKeyName: String  = "id"

  "smoke test search view" - {
    "should work against fixture return" in new Fixture {
      val rmaSearchView = findOneInSearchView(rma.id)

      private val customerSearchViewResult =
        rmaSearchView.customer.extract[CustomerSearchViewResult]

      {
        import rmaSearchView._

        id must === (rma.id)
        referenceNumber must === (rma.refNum)
        orderId must === (rma.orderId)
        orderRef must === (rma.orderRef)
        state must === (rma.state)
        messageToAccount must === (rma.messageToAccount)
        returnType must === (rma.returnType)

        customerSearchViewResult.id must === (rma.accountId)
      }
    }
  }

  "update search view" - {
    "should update state" in new Fixture {
      assert(rma.state == Pending)
      findOneInSearchView(rma.id).state must === (rma.state)

      private val payload = ReturnUpdateStatePayload(state = Processing)
      returnsApi(rma.referenceNumber).update(payload).as[ReturnResponse.Root].state must === (
          Processing)

      findOneInSearchView(rma.id).state must === (Processing)
    }

    "should update message to customer" in new Fixture {
      findOneInSearchView(rma.id).messageToAccount must === (None)

      val payload = ReturnMessageToCustomerPayload(message = "Hello!")
      returnsApi(rma.refNum)
        .message(payload)
        .as[ReturnResponse.Root]
        .messageToCustomer
        .head must === (payload.message)

      findOneInSearchView(rma.id).messageToAccount must === ("Hello!".some)
    }
  }

}
