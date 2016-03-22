import models.customer.{Customer, Customers}
import util.IntegrationTestBase
import utils.seeds.Seeds
import Seeds.Factories
import utils.Slick.implicits._
import utils.DbResultT
import DbResultT.implicits._
import utils.DbResultT._
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

import failures.DatabaseFailure

class DbResultSequenceIntegrationTest extends IntegrationTestBase {

  "DbResultT#sequence" - {
    "must convert Seq[DbResultT[A]] into DbResultT[Seq[A]]" in {
      val sux: Seq[DbResultT[Customer]] = Seq(1, 2, 3).map { i ⇒
        DbResultT(Customers.create(Factories.customer.copy(email = s"$i")))
      }
      val cool: DbResultT[Seq[Customer]] = DbResultT.sequence(sux)
      cool.runTxn().futureValue.rightVal

      val allCustomers = Customers.result.run().futureValue
      allCustomers must have size 3
      allCustomers.map(_.email) must contain allOf("1", "2", "3")
    }

    "must rollback transaction on errors" in {
      val sux: Seq[DbResultT[Customer]] = Seq(1, 2, 3).map { i ⇒
        DbResultT(Customers.create(Factories.customer.copy(email = "nope")))
      }
      val cool: DbResultT[Seq[Customer]] = DbResultT.sequence(sux)

      val result = cool.runTxn().futureValue.leftVal

      val allCustomers = Customers.result.run().futureValue
      allCustomers mustBe empty
    }

    "must collect all errors" in {
      val sux: Seq[DbResultT[Customer]] = Seq(1, 2, 3).map { i ⇒
        DbResultT(Customers.create(Factories.customer.copy(email = "boom")))
      }
      val cool: DbResultT[Seq[Customer]] = DbResultT.sequence(sux)

      val failures = cool.run().futureValue.leftVal
      val expectedFailure = DatabaseFailure(
        "ERROR: duplicate key value violates unique constraint \"customers_active_non_guest_email\"\n" +
          "  Detail: Key (email, is_disabled, is_guest)=(boom, f, f) already exists.")
      failures must === (expectedFailure.single)

      val allCustomers = Customers.result.run().futureValue
      allCustomers must have size 1
      allCustomers.head.email must === ("boom")
    }
  }
}
