package services.giftcards

import models.order.{OrderPayments, Orders}
import models.payment.giftcard.{GiftCardAdjustments, GiftCards}
import responses.GiftCardAdjustmentsResponse._
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object GiftCardAdjustmentsService {

  def forGiftCard(code: String)(implicit ec: EC, db: DB): DbResultT[Seq[Root]] = {

    def maybePaymentQ =
      for {
        pay   ← OrderPayments
        order ← Orders.filter(_.referenceNumber === pay.orderRef)
      } yield (pay, order.referenceNumber)

    def adjustmentQ(giftCardId: Int) = GiftCardAdjustments.filter(_.id === giftCardId)

    def joinedQ(giftCardId: Int) =
      adjustmentQ(giftCardId).joinLeft(maybePaymentQ).on {
        case (adj, (pay, _)) ⇒ adj.orderPaymentId === pay.id
      }

    for {
      giftCard ← * <~ GiftCards.mustFindByCode(code)
      records  ← * <~ joinedQ(giftCard.id).result.toXor
    } yield
      records.map {
        case (adj, Some((_, orderRef))) ⇒
          build(adj, Some(orderRef))
        case (adj, _) ⇒
          build(adj)
      }
  }
}
