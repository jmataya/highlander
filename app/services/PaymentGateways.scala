package services

import models.{CreditCardGateway, Customer}
import payloads.CreditCardPayload

import com.stripe.Stripe
import com.stripe.model.{Token, Card => StripeCard, Charge => StripeCharge, Customer => StripeCustomer}
import com.stripe.net.{RequestOptions => StripeRequestOptions}
import org.scalactic.{Good, Bad, ErrorMessage, Or}
import collection.JavaConversions.mapAsJavaMap
import scala.concurrent.{ExecutionContext, Future}

abstract class PaymentGateway
case object BraintreeGateway extends PaymentGateway

// TODO(yax): do not default apiKey, it should come from store
case class StripeGateway(apiKey: String = "sk_test_eyVBk2Nd9bYbwl01yFsfdVLZ") extends PaymentGateway {
  // Creates a customer in Stripe along with their first CC
  def createCustomerAndCard(customer: Customer, card: CreditCardPayload)
                           (implicit ec: ExecutionContext): Future[StripeCustomer Or List[ErrorMessage]] = tryFutureWrap {

    val base = Map[String, Object](
      "description" -> "FoxCommerce",
      "email" -> customer.email
    )

    val source = Map[String, Object](
      "object" -> "card",
      "number" -> card.number,
      "exp_month" -> card.expMonth.toString,
      "exp_year" -> card.expYear.toString,
      "cvc" -> card.cvv.toString,
      "name" -> card.holderName
    )

    val params = card.address.fold(base.updated("source", mapAsJavaMap(source))) { address =>
      val sourceWithAddress = source ++ Map[String, Object](
        "address_line1" -> address.street1,
        "address_line2" -> address.street2.orNull,
        "address_city" -> address.city,
        "address_state" -> address.state.orNull,
        "address_zip" -> address.zip
      )
      base.updated("source", mapAsJavaMap(sourceWithAddress))
    }

    Good(StripeCustomer.create(mapAsJavaMap(params), options))
  }

  def authorizeAmount(card: CreditCardGateway, amount: Int)
                     (implicit ec: ExecutionContext): Future[String Or List[ErrorMessage]] = tryFutureWrap {
    val capture: java.lang.Boolean = false
    val chargeMap: Map[String, Object] = Map("amount" -> "100", "currency" -> "usd",
      "source" -> card.customerId.toString, "capture" -> capture)

    val charge = StripeCharge.create(mapAsJavaMap(chargeMap), options)
    /*
      TODO: https://stripe.com/docs/api#create_charge
      Since we're using tokenized, we presumably pass verification process, but might want to handle here
    */
    Good(charge.getId)
  }

  private [this] def tryFutureWrap[A](f: => A Or List[ErrorMessage])(implicit ec: ExecutionContext): Future[A Or List[ErrorMessage]] = {
    Future {
      try {
        f
      } catch {
        case t @ (_: com.stripe.exception.InvalidRequestException | _: com.stripe.exception.CardException) =>
          Bad(List(t.getMessage))
      }
    }
  }

  private [this] def options: StripeRequestOptions = StripeRequestOptions.builder().setApiKey(this.apiKey).build()
}
