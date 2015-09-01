package services

import scala.concurrent.ExecutionContext

import cats.data.Xor
import models.{PaymentMethod, CreditCard, Orders, Order, OrderPayment, OrderPayments, GiftCards, GiftCard,
StoreCredits, StoreCredit, CreditCards}

import payloads.{GiftCardPayment, StoreCreditPayment}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}

object OrderPaymentUpdater {
  def addGiftCard(refNum: String, payload: GiftCardPayment)
    (implicit ec: ExecutionContext, db: Database): Result[Unit] = {

   val orderAndGiftCard = for {
     order ← Orders.findCartByRefNum(refNum).result.headOption
     giftCard ← GiftCards.findByCode(payload.code).result.headOption
   } yield (order, giftCard)

   db.run(orderAndGiftCard.flatMap {

     case (Some(order), Some(gc)) if gc.isActive ⇒
       if (gc.hasAvailable(payload.amount)) {
         val payment = OrderPayment.build(gc).copy(orderId = order.id, amount = Some(payload.amount))
         OrderPayments.save(payment).map(_ ⇒ Xor.right({}))
       } else {
         GiftCardNotEnoughBalance(gc, payload.amount).single.liftDBIOXor[Unit]
       }

     case (Some(_), Some(gc)) if !gc.isActive ⇒
       GiftCardIsInactive(gc).single.liftDBIOXor[Unit]

     case (None, _) ⇒
       orderNotFound(refNum).liftDBIOXor[Unit]

     case (_, None) ⇒
       giftCardNotFound(payload.code).liftDBIOXor[Unit]

   }.transactionally)
  }

  def addStoreCredit(refNum: String, payload: StoreCreditPayment)
    (implicit ec: ExecutionContext, db: Database): Result[Unit] = {

    db.run(for {
      order ← Orders.findCartByRefNum(refNum).result.headOption
      storeCredits ← order.map { o ⇒
        StoreCredits.findAllActiveByCustomerId(o.customerId).result
      }.getOrElse(DBIO.successful(Seq.empty[StoreCredit]))
    } yield (order, storeCredits)).flatMap {

      case (Some(order), storeCredits) ⇒
        val available = storeCredits.map(_.availableBalance).sum

        if (available < payload.amount) {
          val error = CustomerHasInsufficientStoreCredit(id = order.customerId, has = available, want = payload.amount)
          Result.left(error)
        } else {
          val payments = StoreCredit.processFifo(storeCredits.toList, payload.amount).map { case (sc, amount) ⇒
            OrderPayment.build(sc).copy(orderId = order.id, amount = Some(amount))
          }

          db.run(OrderPayments ++= payments).map(_ ⇒ Xor.right({}))
        }

      case (None, _) ⇒
        Result.left(OrderNotFoundFailure(refNum))
    }
  }

  def addCreditCard(refNum: String, id: Int)
    (implicit ec: ExecutionContext, db: Database): Result[Unit] = {

    val orderAndCreditCard = for {
      order ← Orders.findCartByRefNum(refNum).result.headOption
      creditCard ← CreditCards._findById(id).result.headOption
    } yield (order, creditCard)

    db.run(orderAndCreditCard.flatMap {

      case (Some(order), Some(cc)) if cc.isActive ⇒
        val payment = OrderPayment.build(cc).copy(orderId = order.id, amount = None)
        val delete = OrderPayments.creditCards.filter(_.orderId === order.id).delete

        (delete >> OrderPayments.save(payment)).map(_ ⇒ Xor.right({}))

      case (Some(_), Some(cc)) ⇒
        CannotUseInactiveCreditCard(cc).single.liftDBIOXor[Unit]

      case (None, _) ⇒
        orderNotFound(refNum).liftDBIOXor[Unit]

      case (_, None) ⇒
        creditCardNotFound(id).liftDBIOXor[Unit]

    }.transactionally)
  }

  def deleteCreditCard(refNum: String)(implicit ec: ExecutionContext, db: Database): Result[Unit] =
    deleteCreditCardOrStoreCredit(refNum, PaymentMethod.CreditCard)

  def deleteStoreCredit(refNum: String)(implicit ec: ExecutionContext, db: Database): Result[Unit] =
    deleteCreditCardOrStoreCredit(refNum, PaymentMethod.StoreCredit)

  private def deleteCreditCardOrStoreCredit(refNum: String, pmt: PaymentMethod.Type)
    (implicit ec: ExecutionContext, db: Database): Result[Unit] = {

    val order = Orders.findCartByRefNum(refNum).result.headOption

    db.run(order.flatMap {

      case None ⇒
        orderNotFound(refNum).liftDBIOXor[Unit]

      case Some(order) ⇒
        OrderPayments.byType(pmt).filter(_.orderId === order.id)
          .delete.map(deletedOrFailure(_, pmt))

    }.transactionally)
  }

  def deleteGiftCard(refNum: String, code: String)
    (implicit ec: ExecutionContext, db: Database): Result[Unit] = {

    val orderAndGiftCard = for {
      order ← Orders.findCartByRefNum(refNum).result.headOption
      giftCard ← GiftCards.findByCode(code).result.headOption
    } yield (order, giftCard)

    db.run(orderAndGiftCard.flatMap {

      case (Some(order), Some(giftCard)) ⇒
        OrderPayments.giftCards.filter(_.paymentMethodId === giftCard.id)
          .filter(_.orderId === order.id).delete.map(deletedOrFailure(_, PaymentMethod.GiftCard))

      case (None, _) ⇒
        orderNotFound(refNum).liftDBIOXor[Unit]

      case (_, None) ⇒
        giftCardNotFound(code).liftDBIOXor[Unit]

    }.transactionally)
  }

  private def deletedOrFailure(rowsDeleted: Int, pmt: PaymentMethod.Type): Failures Xor Unit = {
    if (rowsDeleted == 0)
      Xor.left(OrderPaymentNotFoundFailure(pmt).single)
    else
      Xor.right({})
  }

  private implicit class LiftDBIO[F](failures: Failures) {
    def liftDBIOXor[A]: DBIO[Failures Xor A] = {
      DBIO.successful(Xor.left(failures))
    }
  }

  private def orderNotFound(refNum: String)   = OrderNotFoundFailure(refNum).single
  private def giftCardNotFound(code: String)  = GiftCardNotFoundFailure(code).single
  private def creditCardNotFound(id: Int)     = NotFoundFailure(CreditCard, id).single
}
