package models

import utils.{GenericTable, TableQueryWithId, ModelWithIdParameter, RichTable}

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import scala.concurrent.{ExecutionContext, Future}

case class GiftCardAdjustment(id: Int = 0, giftCardId: Int, credit: Int, debit: Int)
  extends ModelWithIdParameter {
}

object GiftCardAdjustment {}


class GiftCardAdjustments(tag: Tag) extends GenericTable.TableWithId[GiftCardAdjustment](tag, "gift_card_balances") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def giftCardId = column[Int]("gift_card_id")
  def credit = column[Int]("credit")
  def debit = column[Int]("debit")

  def * = (id, giftCardId, credit, debit) <> ((GiftCardAdjustment.apply _).tupled, GiftCardAdjustment.unapply)
}

object GiftCardAdjustments extends TableQueryWithId[GiftCardAdjustment, GiftCardAdjustments](
  idLens = GenLens[GiftCardAdjustment](_.id)
  )(new GiftCardAdjustments(_)){
}
