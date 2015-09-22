package models

import java.time.Instant

import cats.data.ValidatedNel
import services._
import utils.Litterbox._
import utils.Validation

import scala.concurrent.{ExecutionContext, Future}

import cats.data.Validated.{invalidNel, valid}
import cats.data.ValidatedNel

import com.pellucid.sealerate
import models.StoreCredit.{CsrAppeasement, Active, Status, OriginType}
import monocle.macros.GenLens
import services.Result
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
//import utils.Joda._
import utils.Money._
import utils.{ADT, FSM, GenericTable, ModelWithIdParameter, NewModel, TableQueryWithId}
import utils.Slick.implicits._
import cats.syntax.apply._

final case class StoreCredit(id: Int = 0, customerId: Int, originId: Int, originType: OriginType = CsrAppeasement,
  currency: Currency = Currency.USD, originalBalance: Int, currentBalance: Int = 0, availableBalance:Int = 0,
  status: Status = Active, canceledAmount: Option[Int] = None, canceledReason: Option[Int] = None,
  createdAt: Instant = Instant.now())
  extends PaymentMethod
  with ModelWithIdParameter
  with FSM[StoreCredit.Status, StoreCredit]
  with NewModel
  with Validation[StoreCredit] {

  import StoreCredit._
  import Validation._

  def isNew: Boolean = id == 0

  def validate: ValidatedNel[Failure, StoreCredit] = {
    val canceledWithReason: ValidatedNel[Failure, Unit] = (status, canceledAmount, canceledReason) match {
      case (Canceled, None, _) ⇒ invalidNel(GeneralFailure("canceledAmount must be present when canceled"))
      case (Canceled, _, None) ⇒ invalidNel(GeneralFailure("canceledReason must be present when canceled"))
      case _                   ⇒ valid({})
    }

    (canceledWithReason
      |@| invalidExpr(originalBalance < currentBalance, "originalBalance cannot be less than currentBalance")
      |@| invalidExpr(originalBalance < availableBalance, "originalBalance cannot be less than availableBalance")
      |@| invalidExpr(originalBalance < 0, "originalBalance must be greater than zero")
    ).map { case _ ⇒ this }
  }

  def stateLens = GenLens[StoreCredit](_.status)

  val fsm: Map[Status, Set[Status]] = Map(
    OnHold → Set(Active, Canceled),
    Active → Set(OnHold, Canceled)
  )

  // TODO: not sure we use this polymorphically
  def authorize(amount: Int)(implicit ec: ExecutionContext): Result[String] =
    Result.good("authenticated")

  def isActive: Boolean = activeStatuses.contains(status)
}

object StoreCredit {
  sealed trait Status
  case object OnHold extends Status
  case object Active extends Status
  case object Canceled extends Status

  sealed trait OriginType
  case object GiftCardTransfer extends OriginType
  case object CsrAppeasement extends OriginType
  case object ReturnProcess extends OriginType

  object Status extends ADT[Status] {
    def types = sealerate.values[Status]
  }

  object OriginType extends ADT[OriginType] {
    def types = sealerate.values[OriginType]
  }

  val activeStatuses = Set[Status](Active)

  implicit val statusColumnType: JdbcType[Status] with BaseTypedType[Status] = Status.slickColumn
  implicit val originTypeColumnType: JdbcType[OriginType] with BaseTypedType[OriginType] = OriginType.slickColumn

  def processFifo(storeCredits: List[StoreCredit], requestedAmount: Int): Map[StoreCredit, Int] = {
    val fifo = storeCredits.sortBy(_.createdAt)
    fifo.foldLeft(Map.empty[StoreCredit, Int]) { case (amounts, sc) ⇒
      val total = amounts.values.sum
      val missing = requestedAmount - total

      if (total < requestedAmount) {
        if ((total + sc.availableBalance) >= requestedAmount) {
          amounts.updated(sc, missing)
        } else {
          amounts.updated(sc, sc.availableBalance)
        }
      } else {
        amounts
      }
    }
  }
}

class StoreCredits(tag: Tag) extends GenericTable.TableWithId[StoreCredit](tag, "store_credits")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def originId = column[Int]("origin_id")
  def originType = column[StoreCredit.OriginType]("origin_type")
  def customerId = column[Int]("customer_id")
  def currency = column[Currency]("currency")
  def originalBalance = column[Int]("original_balance")
  def currentBalance = column[Int]("current_balance")
  def availableBalance = column[Int]("available_balance")
  def status = column[StoreCredit.Status]("status")
  def canceledAmount = column[Option[Int]]("canceled_amount")
  def canceledReason = column[Option[Int]]("canceled_reason")
  def createdAt = column[Instant]("created_at")

  def * = (id, customerId, originId, originType, currency, originalBalance, currentBalance,
    availableBalance, status, canceledAmount, canceledReason, createdAt) <> ((StoreCredit.apply _).tupled, StoreCredit
    .unapply)
}

object StoreCredits extends TableQueryWithId[StoreCredit, StoreCredits](
  idLens = GenLens[StoreCredit](_.id)
  )(new StoreCredits(_)){

  import StoreCredit._
  import models.{StoreCreditAdjustment ⇒ Adj, StoreCreditAdjustments ⇒ Adjs}

  def auth(storeCredit: StoreCredit, orderPaymentId: Option[Int], amount: Int = 0)
    (implicit ec: ExecutionContext): DBIO[Adj] =
    debit(storeCredit = storeCredit, orderPaymentId = orderPaymentId, amount = amount, status = Adj.Auth)

  def capture(storeCredit: StoreCredit, orderPaymentId: Option[Int], amount: Int = 0)
    (implicit ec: ExecutionContext): DBIO[Adj] =
    debit(storeCredit = storeCredit, orderPaymentId = orderPaymentId, amount = amount, status = Adj.Capture)

  def findAllByCustomerId(customerId: Int)(implicit ec: ExecutionContext, db: Database): Future[Seq[StoreCredit]] =
    _findAllByCustomerId(customerId).run()

  def _findAllByCustomerId(customerId: Int)(implicit ec: ExecutionContext): DBIO[Seq[StoreCredit]] =
    filter(_.customerId === customerId).result

  def findAllActiveByCustomerId(customerId: Int): Query[StoreCredits, StoreCredit, Seq] =
    filter(_.customerId === customerId).filter(_.status === (Active: Status)).filter(_.availableBalance > 0)

  def findByIdAndCustomerId(id: Int, customerId: Int)
    (implicit ec: ExecutionContext, db: Database): Future[Option[StoreCredit]] =
    _findByIdAndCustomerId(id, customerId).run()

  def _findByIdAndCustomerId(id: Int, customerId: Int)(implicit ec: ExecutionContext): DBIO[Option[StoreCredit]] =
    filter(_.customerId === customerId).filter(_.id === id).one

  private def debit(storeCredit: StoreCredit, orderPaymentId: Option[Int], amount: Int = 0,
    status: Adj.Status = Adj.Auth)
    (implicit ec: ExecutionContext): DBIO[Adj] = {
    val adjustment = Adj(storeCreditId = storeCredit.id, orderPaymentId = orderPaymentId,
      debit = amount, status = status)
    Adjs.save(adjustment)
  }
}
