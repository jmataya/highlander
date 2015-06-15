package models

import utils.{Validation, RichTable}
import payloads.CreateAddressPayload

import com.wix.accord.dsl.{validator => createValidator}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import org.scalactic._
import com.wix.accord.{Failure => ValidationFailure, Validator}
import com.wix.accord.dsl._
import scala.concurrent.{ExecutionContext, Future}


case class Order(id: Int = 0, customerId: Int, status: Order.Status, locked: Int) {
  var lineItems: Seq[CartLineItem] = Seq.empty
}

object Order {
  sealed trait Status
  case object New extends Status
  case object FraudHold extends Status
  case object RemorseHold extends Status
  case object ManualHold extends Status
  case object Canceled extends Status
  case object FulfillmentStarted extends Status
  case object PartiallyShipped extends Status
  case object Shipped extends Status

  implicit val StatusColumnType = MappedColumnType.base[Status, String]({
    case t @ (New | FraudHold | RemorseHold | ManualHold | Canceled |
             FulfillmentStarted | PartiallyShipped | Shipped) => t.toString.toLowerCase
    case unknown => throw new IllegalArgumentException(s"cannot map status column to type $unknown")
  },
  {
    case "new" => New
    case "fraudhold" => FraudHold
    case "remorsehold" => RemorseHold
    case "manualhold" => ManualHold
    case "canceled" => Canceled
    case "fulfillmen_started" => FulfillmentStarted
    case "partiallyshipped" => PartiallyShipped
    case "shipped" => Shipped
    case unknown => throw new IllegalArgumentException(s"cannot map status column to type $unknown")
  })
}

class Orders(tag: Tag) extends Table[Order](tag, "orders") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def customer_id = column[Int]("customer_id")
  def status = column[Order.Status]("status")
  def locked = column[Int]("locked") // 0 for no; 1 for yes
  def * = (id, customer_id, status, locked) <> ((Order.apply _).tupled, Order.unapply)
}

object Orders {
  val table = TableQuery[Orders]
  val returningId = table.returning(table.map(_.id))

  def _create(order: Order)(implicit ec: ExecutionContext, db: Database): DBIOAction[models.Order, NoStream, Effect.Write] = {
   for {
     newId <- Orders.returningId += order
   } yield order.copy(id = newId)
  }
}
