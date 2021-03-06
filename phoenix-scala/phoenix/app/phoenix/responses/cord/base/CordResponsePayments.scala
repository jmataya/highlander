package phoenix.responses.cord.base

import java.time.Instant

import phoenix.models.cord.OrderPayments
import phoenix.models.payment.PaymentMethod._
import phoenix.models.payment.creditcard.CreditCards
import phoenix.responses.AddressResponse
import phoenix.responses.cord.base.CordResponseStoreCreditPayment.CordResponseApplePayPayment
import phoenix.utils.aliases._
import slick.jdbc.PostgresProfile.api._
import core.utils.Money._

sealed trait CordResponsePayments {
  def `type`: Type
}

object CordResponsePayments {

  def fetchAll(cordRef: String)(implicit ec: EC): DBIO[Seq[CordResponsePayments]] =
    for {
      gc ← CordResponseGiftCardPayment.fetch(cordRef)
      cc ← CordResponseCreditCardPayment.fetch(cordRef)
      sc ← CordResponseStoreCreditPayment.fetch(cordRef)
      ap ← CordResponseApplePayPayment.fetch(cordRef)
    } yield cc ++ gc ++ sc ++ ap
}

case class CordResponseCreditCardPayment(id: Int,
                                         customerId: Int,
                                         holderName: String,
                                         lastFour: String,
                                         expMonth: Int,
                                         expYear: Int,
                                         brand: String,
                                         address: AddressResponse,
                                         `type`: Type = CreditCard,
                                         createdAt: Instant)
    extends CordResponsePayments

object CordResponseCreditCardPayment {

  def fetch(cordRef: String)(implicit ec: EC): DBIO[Seq[CordResponseCreditCardPayment]] =
    (for {
      payment    ← OrderPayments.findAllByCordRef(cordRef)
      creditCard ← CreditCards.filter(_.id === payment.paymentMethodId)
      region     ← creditCard.region
    } yield (creditCard, region)).result.map(_.map {
      case (creditCard, region) ⇒
        CordResponseCreditCardPayment(
          id = creditCard.id,
          customerId = creditCard.accountId,
          holderName = creditCard.holderName,
          lastFour = creditCard.lastFour,
          expMonth = creditCard.expMonth,
          expYear = creditCard.expYear,
          brand = creditCard.brand,
          createdAt = creditCard.createdAt,
          address = AddressResponse.buildFromCreditCard(creditCard, region)
        )
    })
}

case class CordResponseGiftCardPayment(code: String,
                                       amount: Long,
                                       currentBalance: Long,
                                       availableBalance: Long,
                                       createdAt: Instant,
                                       `type`: Type = GiftCard)
    extends CordResponsePayments

object CordResponseGiftCardPayment {

  def fetch(cordRef: String)(implicit ec: EC): DBIO[Seq[CordResponseGiftCardPayment]] =
    OrderPayments
      .findAllGiftCardsByCordRef(cordRef)
      .result
      .map(_.map {
        case (pmt, gc) ⇒
          CordResponseGiftCardPayment(code = gc.code,
                                      amount = pmt.amount.getOrElse(0),
                                      currentBalance = gc.currentBalance,
                                      availableBalance = gc.availableBalance,
                                      createdAt = gc.createdAt)
      })
}

case class CordResponseStoreCreditPayment(id: Int,
                                          amount: Long,
                                          currentBalance: Long,
                                          availableBalance: Long,
                                          createdAt: Instant,
                                          `type`: Type = StoreCredit)
    extends CordResponsePayments

object CordResponseStoreCreditPayment {

  def fetch(cordRef: String)(implicit ec: EC): DBIO[Seq[CordResponseStoreCreditPayment]] =
    OrderPayments
      .findAllStoreCreditsByCordRef(cordRef)
      .result
      .map(_.map {
        case (pmt, sc) ⇒
          CordResponseStoreCreditPayment(id = sc.id,
                                         amount = pmt.amount.getOrElse(0),
                                         currentBalance = sc.currentBalance,
                                         availableBalance = sc.availableBalance,
                                         createdAt = sc.createdAt)
      })

  case class CordResponseApplePayPayment(id: Int,
                                         accountId: Int,
                                         cordRef: String,
                                         createdAt: Instant,
                                         `type`: Type = ApplePay)
      extends CordResponsePayments

  object CordResponseApplePayPayment {

    def fetch(cordRef: String)(implicit ec: EC): DBIO[Seq[CordResponseApplePayPayment]] =
      for {
        orderPayment ← OrderPayments.findAllApplePaysByCordRef(cordRef).result
        response = orderPayment.map {
          case (pmt, ap) ⇒
            CordResponseApplePayPayment(id = ap.id,
                                        accountId = ap.accountId,
                                        cordRef = pmt.cordRef,
                                        createdAt = ap.createdAt)
        }
      } yield response

  }
}
