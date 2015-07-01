package models

import com.wix.accord.{Failure ⇒ ValidationFailure, Success ⇒ ValidationSuccess}
import org.scalactic.{Bad, Good}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.TableDrivenPropertyChecks._
import payloads.CreateAddressPayload
import util.IntegrationTestBase
import utils.{Seeds, Validation}
import utils.Seeds.Factories

class OrdersTest extends IntegrationTestBase {
  import api._

  import concurrent.ExecutionContext.Implicits.global

  "Orders" - {
    "can only have one record in 'cart' status" in {
      val (customer, order) = (for {
        customer ← Customers.save(Factories.customer)
        order ← Orders.save(Factories.order.copy(customerId = customer.id, status = Order.Cart))
      } yield (customer, order)).run().futureValue

      val failure = Orders.save(order.copy(id = 0)).run().failed.futureValue
      failure.getMessage must include ("""value violates unique constraint "orders_has_only_one_cart"""")
    }
  }
}
