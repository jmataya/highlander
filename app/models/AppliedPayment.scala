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
import com.stripe.model.{Customer => StripeCustomer}

case class AppliedPayment(id: Int = 0,
                          cartId: Int,
                          paymentMethodId: Int,
                          paymentMethodType: String,
                          appliedAmount: Int,
                          status: String,
                          responseCode: String)

object AppliedPayment {
  def fromStripeCustomer(stripeCustomer: StripeCustomer, cart: Cart): AppliedPayment = {
    AppliedPayment(cartId = cart.id, paymentMethodId = 1, // TODO: would do a lookup
      paymentMethodType = "stripe",
      appliedAmount = 0, status = Auth.toString, // TODO: use type and marshalling
      responseCode = "ok" // TODO: make this real
    )
  }
}

class AppliedPayments(tag: Tag) extends Table[AppliedPayment](tag, "applied_payments") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def cartId = column[Int]("cart_id")
  def paymentMethodId = column[Int]("payment_method_id")
  def paymentMethodType = column[String]("payment_method_type")
  def appliedAmount = column[Int]("applied_amount")
  def status = column[String]("status")
  def responseCode = column[String]("response_code")
  def * = (id, cartId, paymentMethodId, paymentMethodType, appliedAmount, status, responseCode) <> ((AppliedPayment.apply _).tupled, AppliedPayment.unapply )
}

object AppliedPayments {
  val table = TableQuery[AppliedPayments]
  val returningId = table.returning(table.map(_.id))
}
