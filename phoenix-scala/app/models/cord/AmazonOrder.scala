package models.cord

import java.time.Instant

import shapeless._
import utils.Money.Currency
import utils.db.ExPostgresDriver.api._
import utils.db._
import utils.aliases._
import failures._
import payloads.AmazonOrderPayloads.CreateAmazonOrderPayload
import com.github.tminglei.slickpg.LTree
import models.account._

case class AmazonOrder(id: Int = 0,
                       amazonOrderId: String = "",
                       orderTotal: Int = 0,
                       paymentMethodDetail: String = "",
                       orderType: String = "",
                       currency: Currency = Currency.USD,
                       orderStatus: String = "",
                       purchaseDate: Instant,
                       scope: LTree,
                       accountId: Int,
                       createdAt: Instant = Instant.now,
                       updatedAt: Instant = Instant.now)
    extends FoxModel[AmazonOrder]

object AmazonOrder {
  def build(payload: CreateAmazonOrderPayload, accountId: Int)(implicit ec: EC): AmazonOrder =
    AmazonOrder(id = 0,
                amazonOrderId = payload.amazonOrderId,
                orderTotal = payload.orderTotal,
                paymentMethodDetail = payload.paymentMethodDetail,
                orderType = payload.orderType,
                currency = payload.currency,
                orderStatus = payload.orderStatus,
                purchaseDate = payload.purchaseDate,
                scope = payload.scope,
                accountId = accountId,
                createdAt = Instant.now,
                updatedAt = Instant.now)

  def fromExistingAmazonOrder(existingOrder: AmazonOrder): AmazonOrder =
    AmazonOrder(id = existingOrder.id,
                amazonOrderId = existingOrder.amazonOrderId,
                orderTotal = existingOrder.orderTotal,
                paymentMethodDetail = existingOrder.paymentMethodDetail,
                orderType = existingOrder.orderType,
                currency = existingOrder.currency,
                orderStatus = existingOrder.orderStatus,
                purchaseDate = existingOrder.purchaseDate,
                scope = existingOrder.scope,
                accountId = existingOrder.accountId,
                createdAt = existingOrder.createdAt,
                updatedAt = existingOrder.updatedAt)
}

object AmazonOrders
    extends FoxTableQuery[AmazonOrder, AmazonOrders](new AmazonOrders(_))
    with ReturningId[AmazonOrder, AmazonOrders]
    with SearchByAmazonOrderId[AmazonOrder, AmazonOrders] {

  val returningLens: Lens[AmazonOrder, Int] = lens[AmazonOrder].id

  def findOneByAmazonOrderId(amazonOrderId: String): DBIO[Option[AmazonOrder]] =
    filter(_.amazonOrderId === amazonOrderId).one

  def mustFindOneOr404(amazonOrderId: String)(implicit ec: EC): DbResultT[AmazonOrder] =
    findOneByAmazonOrderId(amazonOrderId).mustFindOr(
        NotFoundFailure404(s"Amazon order with id=$amazonOrderId not found"))
}

class AmazonOrders(tag: Tag) extends FoxTable[AmazonOrder](tag, "amazon_orders") {
  def id                  = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def amazonOrderId       = column[String]("amazon_order_id")
  def orderTotal          = column[Int]("order_total")
  def paymentMethodDetail = column[String]("payment_method_detail")
  def orderType           = column[String]("order_type")
  def currency            = column[Currency]("currency")
  def orderStatus         = column[String]("order_status")
  def purchaseDate        = column[Instant]("purchase_date")
  def scope               = column[LTree]("scope")
  def accountId           = column[Int]("account_id")
  def createdAt           = column[Instant]("created_at")
  def updatedAt           = column[Instant]("updated_at")

  def * =
    (id,
     amazonOrderId,
     orderTotal,
     paymentMethodDetail,
     orderType,
     currency,
     orderStatus,
     purchaseDate,
     scope,
     accountId,
     createdAt,
     updatedAt) <> ((AmazonOrder.apply _).tupled, AmazonOrder.unapply)
}