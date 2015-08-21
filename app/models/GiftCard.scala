package models

import com.pellucid.sealerate
import services.{Result, Failures, Failure}
import slick.dbio
import slick.dbio.Effect.{Read, Write}
import slick.profile.FixedSqlStreamingAction
import utils.Money._
import utils.{ADT, GenericTable, Validation, TableQueryWithId, ModelWithIdParameter, RichTable}
import validators.nonEmptyIf
import scala.concurrent.{ExecutionContext, Future}

import com.pellucid.sealerate
import com.wix.accord.dsl.{validator ⇒ createValidator, _}
import monocle.macros.GenLens
import org.scalactic._
import services.Failures
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}
import utils.Money._
import utils.{ADT, FSM, GenericTable, ModelWithIdParameter, RichTable, TableQueryWithId, Validation}
import validators.nonEmptyIf
import GiftCard.{Status, OnHold}

final case class GiftCard(id: Int = 0, originId: Int, originType: String, code: String,
  currency: Currency, status: Status = OnHold, originalBalance: Int, currentBalance: Int = 0,
  availableBalance: Int = 0, canceledReason: Option[String] = None, reloadable: Boolean = false)
  extends PaymentMethod
  with ModelWithIdParameter
  with Validation[GiftCard]
  with FSM[GiftCard.Status, GiftCard] {

  import GiftCard._

  override def validator = createValidator[GiftCard] { giftCard =>
    giftCard.status as "canceledReason" is nonEmptyIf(giftCard.status == Canceled, giftCard.canceledReason)
    giftCard.originalBalance should be >= 0
    giftCard.currentBalance should be >= 0
    giftCard.code is notEmpty
  }

  def stateLens = GenLens[GiftCard](_.status)

  val fsm: Map[Status, Set[Status]] = Map(
    OnHold → Set(Active, Canceled),
    Active → Set(OnHold, Canceled)
  )

  def authorize(amount: Int)(implicit ec: ExecutionContext): Result[String] = {
    Future.successful(Good("authenticated"))
  }

  def isActive: Boolean = activeStatuses.contains(status)

  def hasAvailable(amount: Int): Boolean = availableBalance >= amount
}

object GiftCard {
  sealed trait Status
  case object OnHold extends Status
  case object Active extends Status
  case object Canceled extends Status

  object Status extends ADT[Status] {
    def types = sealerate.values[Status]
  }

  val activeStatuses = Set[Status](Active)

  implicit val statusColumnType = Status.slickColumn
}

class GiftCards(tag: Tag) extends GenericTable.TableWithId[GiftCard](tag, "gift_cards") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def originId = column[Int]("origin_id")
  def originType = column[String]("origin_type")
  def code = column[String]("code")
  def status = column[GiftCard.Status]("status")
  def currency = column[Currency]("currency")
  def originalBalance = column[Int]("original_balance")
  def currentBalance = column[Int]("current_balance")
  def availableBalance = column[Int]("available_balance")
  def canceledReason = column[Option[String]]("canceled_reason")
  def reloadable = column[Boolean]("reloadable")

  def * = (id, originId, originType, code, currency, status, originalBalance, currentBalance,
    availableBalance, canceledReason, reloadable) <> ((GiftCard.apply _).tupled, GiftCard.unapply)
}

object GiftCards extends TableQueryWithId[GiftCard, GiftCards](
  idLens = GenLens[GiftCard](_.id)
  )(new GiftCards(_)){

  import models.{GiftCardAdjustment ⇒ Adj, GiftCardAdjustments ⇒ Adjs}
  import GiftCard._

  def auth(giftCard: GiftCard, orderPaymentId: Int, debit: Int = 0, credit: Int = 0)
    (implicit ec: ExecutionContext): DBIO[Adj] =
    adjust(giftCard, orderPaymentId, debit = debit, credit = credit, status = Adj.Auth)

  def capture(giftCard: GiftCard, orderPaymentId: Int, debit: Int = 0, credit: Int = 0)
    (implicit ec: ExecutionContext): DBIO[Adj] =
    adjust(giftCard, orderPaymentId, debit = debit, credit = credit, status = Adj.Capture)

  def findByCode(code: String): Query[GiftCards, GiftCard, Seq] =
    filter(_.code === code)

  def findActiveByCode(code: String): Query[GiftCards, GiftCard, Seq] =
    findByCode(code).filter(_.status inSet activeStatuses)

  override def save(giftCard: GiftCard)(implicit ec: ExecutionContext): DBIO[GiftCard] = for {
    (id, cb, ab) ← this.returning(map { gc ⇒ (gc.id, gc.currentBalance, gc.availableBalance) }) += giftCard
  } yield giftCard.copy(id = id, currentBalance = cb, availableBalance = ab)

  private def adjust(giftCard: GiftCard, orderPaymentId: Int, debit: Int = 0, credit: Int = 0,
    status: Adj.Status = Adj.Auth)
    (implicit ec: ExecutionContext): DBIO[Adj] = {
    val adjustment = Adj(giftCardId = giftCard.id, orderPaymentId = orderPaymentId,
      debit = debit, credit = credit, status = status)
    Adjs.save(adjustment)
  }
}
