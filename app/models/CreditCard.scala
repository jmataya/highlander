package models

import scala.concurrent.{ExecutionContext, Future}

import com.stripe.model.{Customer ⇒ StripeCustomer}
import com.wix.accord.dsl.{validator ⇒ createValidator, _}
import monocle.macros.GenLens
import org.joda.time.DateTime
import com.github.tototoshi.slick.JdbcJodaSupport._

import payloads.CreateCreditCard
import services.{Failures, StripeGateway}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}
import utils._
import validators._
import services.Result

final case class CreditCard(id: Int = 0, parentId: Option[Int] = None, customerId: Int, billingAddressId: Int = 0,
  gatewayCustomerId: String, lastFour: String, expMonth: Int, expYear: Int, isDefault: Boolean = false,
  inWallet: Boolean = true, deletedAt: Option[DateTime] = None)
  extends PaymentMethod
  with ModelWithIdParameter
  with Validation[CreditCard] {

  def authorize(amount: Int)(implicit ec: ExecutionContext): Result[String] = {
    new StripeGateway().authorizeAmount(gatewayCustomerId, amount)
  }

  override def validator = createValidator[CreditCard] { cc =>
    cc.lastFour should matchRegex("[0-9]{4}")
    cc.expYear as "credit card" is notExpired(year = cc.expYear, month = cc.expMonth)
    cc.expYear as "credit card" is withinTwentyYears(year = cc.expYear, month = cc.expMonth)
  }

  def isActive: Boolean = deletedAt.isEmpty && inWallet
}

object CreditCard {
  def build(c: StripeCustomer, payload: CreateCreditCard): CreditCard = {
    CreditCard(customerId = 0, gatewayCustomerId = c.getId, lastFour = payload.lastFour,
      expMonth = payload.expMonth, expYear = payload.expYear, isDefault = payload.isDefault)
  }
}

class CreditCards(tag: Tag)
  extends GenericTable.TableWithId[CreditCard](tag, "credit_cards")
  with RichTable {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def parentId = column[Option[Int]]("parent_id")
  def customerId = column[Int]("customer_id")
  def billingAddressId = column[Int]("billing_address_id")
  def gatewayCustomerId = column[String]("gateway_customer_id")
  def lastFour = column[String]("last_four")
  def expMonth = column[Int]("exp_month")
  def expYear = column[Int]("exp_year")
  def isDefault = column[Boolean]("is_default")
  def inWallet = column[Boolean]("in_wallet")
  def deletedAt = column[Option[DateTime]]("deleted_at")

  def * = (id, parentId, customerId, billingAddressId, gatewayCustomerId,
    lastFour, expMonth, expYear, isDefault, inWallet, deletedAt) <> ((CreditCard.apply _).tupled, CreditCard
    .unapply)

  def customer        = foreignKey(Customers.tableName, customerId, Customers)(_.id)
  def billingAddress  = foreignKey(Addresses.tableName, billingAddressId, Addresses)(_.id)
}

object CreditCards extends TableQueryWithId[CreditCard, CreditCards](
  idLens = GenLens[CreditCard](_.id)
)(new CreditCards(_)) {

  def findById(id: Int)(implicit db: Database): Future[Option[CreditCard]] = {
    db.run(_findById(id).result.headOption)
  }

  def findAllByCustomerId(customerId: Int)(implicit db: Database): Future[Seq[CreditCard]] =
    _findAllByCustomerId(customerId).run()

  def _findAllByCustomerId(customerId: Int): DBIO[Seq[CreditCard]] =
    filter(_.customerId === customerId).result

  def findDefaultByCustomerId(customerId: Int)(implicit db: Database): Future[Option[CreditCard]] =
    _findDefaultByCustomerId(customerId).run()

  def _findDefaultByCustomerId(customerId: Int): DBIO[Option[CreditCard]] =
    filter(_.customerId === customerId).filter(_.isDefault === true).result.headOption

  def _findByIdAndIsDefault(id: Int, isDefault: Boolean): DBIO[Option[CreditCard]] =
    _findById(id).extract.filter(_.isDefault === isDefault).result.headOption
}

