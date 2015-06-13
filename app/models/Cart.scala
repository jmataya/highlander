package models

import utils.{Validation, RichTable}

import com.wix.accord.dsl.{validator => createValidator}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import org.scalactic._
import com.wix.accord.{Failure => ValidationFailure, Validator}
import com.wix.accord.dsl._
import scala.concurrent.{ExecutionContext, Future}

case class Cart(id: Int, accountId: Option[Int] = None, status: Cart.Status = Cart.Status.Active) {
  val lineItems: Seq[LineItem] = Seq.empty
  //val payments: Seq[AppliedPayment] = Seq.empty
  // val fulfillments: Seq[Fulfillment] = Seq.empty

  //  def coupons: Seq[Coupon] = Seq.empty
  //  def adjustments: Seq[Adjustment] = Seq.empty

  // TODO: how do we handle adjustment/coupon
  // specifically, promotions are handled at the checkout level, but need to display in the cart
  //def addCoupon(coupon: Coupon) = {}

  // carts support guest checkout
  def isGuest = this.accountId.isDefined

  // TODO: service class it?

  def payments: Future[Seq[AppliedPayment]] = {
    Carts.findPaymentMethods(this)
  }

  def subTotal: Int = {
    10000 //in cents?
  }

  def grandTotal: Int = {
    12550
  }

  def toMap: Map[String, Any] = {
    val fields = this.getClass.getDeclaredFields.map(_.getName)
    val values = Cart.unapply(this).get.productIterator.toSeq
    fields.zip(values).toMap
  }
}

object Cart {
  sealed trait Status
  object Status {
    case object Active extends Status  // most will be here
    case object Ordered extends Status // after order
    case object Removed extends Status // admin could do this
  }

  implicit val StatusColumnType = MappedColumnType.base[Status, String](
  { status =>
     status match {
        case t: Status.Active.type => "active"
        case t: Status.Ordered.type => "ordered"
        case t: Status.Removed.type => "removed"
        case _ => "unknown"
      }
  },
  { str =>
    str match {
      case "active" => Status.Active
      case "ordered" => Status.Ordered
      case "removed" => Status.Removed
      case _ => Status.Active
    }

  }
  )
}

class Carts(tag: Tag) extends Table[Cart](tag, "carts") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def customerId = column[Option[Int]]("customer_id")
  def status = column[Cart.Status]("status")
  def * = (id, customerId, status) <> ((Cart.apply _).tupled, Cart.unapply)
}

object Carts {
  val carts = TableQuery[Carts]

  val cartsTable = TableQuery[Carts]
  val returningId = cartsTable.returning(cartsTable.map(_.id))
  val tokenCardsTable = TableQuery[TokenizedCreditCards]
  val appliedPaymentsTable = TableQuery[AppliedPayments]

  // What do we return here?  I still don't have a clear STI approach in mind.  So maybe just tokenized cards for now.
  // Ideally, we would return a generic list of payment methods of all types (eg. giftcards, creditcards, store-credit)
  def findPaymentMethods(cart: Cart): Future[Seq[AppliedPayment]] = {
    val appliedpayment = AppliedPayment(id = 1, cartId = cart.id, paymentMethodId = 1, paymentMethodType = "TokenizedCard", appliedAmount = 10000, status = Applied.toString, responseCode = "")
    val appliedpayment2 = appliedpayment.copy(appliedAmount = 2550, paymentMethodId = 2)


    Future.successful(Seq(appliedpayment, appliedpayment2))

  }

  def addPaymentMethod(cartId: Int, paymentMethod: PaymentMethod)(implicit db: Database): Boolean = {
    true
  }

  def findById(id: Int)(implicit db: Database): Future[Option[Cart]] = {
    db.run(_findById(id).result.headOption)
  }

  def _findById(id: Rep[Int]) = { carts.filter(_.id === id) }

  // If the user doesn't have a cart yet, let's create one.
  def findByCustomer(customer: Customer)(implicit ec: ExecutionContext, db: Database): Future[Option[Cart]] = {
    // TODO: AW needs Help on conditionally creating the user.  Went fast, moving on...

//    val cartCount = carts.filter(_.customerId === customer.id).length
//    val newCart = cartCount.result.flatMap { count =>
//      if (count < 1) {
//        val freshCart = Cart(id = 0, accountId = Some(customer.id))
//        (returningId += freshCart) map { insertId =>
//          freshCart.copy(id = insertId)
//        }
//      } else {
//        carts.filter(_.customerId === customer.id).result.headOption
//      }
//    }
//
//    db.run(newCart)
    db.run(_findByCustomer(customer).result.headOption)
  }

  def _findByCustomer(cust: Customer) = {carts.filter(_.customerId === cust.id)}
}
