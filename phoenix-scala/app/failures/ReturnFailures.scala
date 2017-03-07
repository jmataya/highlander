package failures

import models.returns.{Return, ReturnReason}
import utils.friendlyClassName

object ReturnFailures {

  case class NoReturnsFoundForOrder(refNum: String) extends Failure {
    override def description = s"no return for order $refNum was found"
  }

  case class EmptyReturn(refNum: String) extends Failure {
    override def description = s"return with referenceNumber=$refNum has no line items"
  }

  object ReturnPaymentNotFoundFailure {
    def apply[M](m: M): NotFoundFailure400 =
      NotFoundFailure400(s"${friendlyClassName(m)} payment not found")
  }

  object ReturnReasonNotFoundFailure {
    def apply(id: ReturnReason#Id): NotFoundFailure400 =
      NotFoundFailure400(s"Return reason $id not found")
  }

  case class ReturnShippingCostExceeded(refNum: String, amount: Int, maxAmount: Int)
      extends Failure {
    def description: String =
      s"Returned shipping cost ($amount) cannot be greater than $maxAmount for return $refNum"
  }

  case class ReturnPaymentExceeded(refNum: String, amount: Int, maxAmount: Int) extends Failure {
    def description: String =
      s"Returned payment ($amount) cannot be greater than $maxAmount for return $refNum"
  }

  case class ReturnCCPaymentExceeded(refNum: String, amount: Int, maxAmount: Int) extends Failure {
    def description: String =
      s"Returned credit card payment ($amount) cannot be greater than $maxAmount for return $refNum"
  }

  case class ReturnCCPaymentViolation(refNum: String, issued: Int, allowed: Int) extends Failure {
    def description: String =
      s"Issued credit card payment ($issued) is different than $allowed for return $refNum"
  }
}
