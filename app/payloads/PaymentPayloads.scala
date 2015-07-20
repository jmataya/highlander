package payloads

import utils.Validation

import com.wix.accord.dsl.{validator => createValidator}
import com.wix.accord.{Failure => ValidationFailure, Validator}
import com.wix.accord.dsl._

final case class CreditCardPayload(holderName: String, number: String, cvv: String, expYear: Int,
  expMonth: Int, address: Option[CreateAddressPayload] = None, isDefault: Boolean = false)
  extends Validation[CreditCardPayload] {

  override def validator = createValidator[CreditCardPayload] { cc =>
    cc.holderName is notEmpty
    cc.number should matchRegex("[0-9]+")
    cc.cvv should matchRegex("[0-9]{3,4}")
    cc.expYear is between(2015, 2050)
    cc.expMonth is between(1, 12)
    // TODO: this is why we'd use implicit validators
    // cc.address.map { a => a is valid }
  }

  def lastFour: String = this.number.takeRight(4)
}

final case class PaymentMethodPayload(cardholderName: String, cardNumber: String,  cvv: Int, expiration: String)
