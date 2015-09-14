package models

import util.IntegrationTestBase
import utils.Seeds.Factories
import utils.Slick.implicits._

class CreditCardTest extends IntegrationTestBase {
  import api._

  import concurrent.ExecutionContext.Implicits.global

  "CreditCard" - {
    "has only one default per customer" in new Fixture {
      val anotherDefault = CreditCards.save(cc.copy(id = 0, isDefault = true)).run().failed.futureValue
      anotherDefault.getMessage must include ("violates unique constraint \"credit_cards_default_idx\"")
    }
  }

  trait Fixture {
    val (customer, cc) = (for {
      customer ← Customers.save(Factories.customer)
      address ← Addresses.save(Factories.address.copy(customerId = customer.id))
      cc ← CreditCards.save(Factories.creditCard.copy(customerId = customer.id))
    } yield (customer, cc)).run().futureValue
  }
}
