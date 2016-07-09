package services.orders

import java.time.Instant

import models.cord.{Order, Orders}
import services.Result
import utils.aliases._
import utils.db._

object TimeMachine {

  def changePlacedAt(refNum: String, placedAt: Instant)(implicit ec: EC, db: DB): Result[Order] =
    (for {
      order   ← * <~ Orders.mustFindByRefNum(refNum)
      updated ← * <~ Orders.update(order, order.copy(placedAt = placedAt))
    } yield updated).runTxn()
}
