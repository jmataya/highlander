package services

import models._

import responses.FullOrder
import payloads.CreateCreditCard

import org.scalactic._
import scala.concurrent.{Future, ExecutionContext}
import slick.dbio.Effect.All
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import com.stripe.model.{Token, Card => StripeCard, Customer => StripeCustomer}
import com.stripe.net.{RequestOptions => StripeRequestOptions}
import collection.JavaConversions.mapAsJavaMap

import slick.profile.FixedSqlAction
import utils.Validation
import utils.Validation.Result.{ Success}
import utils.{ Validation ⇒ validation }

// TODO(yax): make this abstract to handle multiple Gateways
final case class CreditCardPaymentCreator(order: Order, customer: Customer, cardPayload: CreateCreditCard)
  (implicit ec: ExecutionContext, db: Database) {

  val gateway = StripeGateway()
  import CreditCardPaymentCreator._

  def run(): Response = cardPayload.validate match {
    case failure @ validation.Result.Failure(violations) ⇒
      Future.successful(Bad(List(ValidationFailure(failure))))
    case Success ⇒
      // creates the customer, card, and gives us getDefaultCard as the token
      gateway.createCustomerAndCard(customer, this.cardPayload).flatMap {
        case Good(stripeCustomer) =>
          createRecords(stripeCustomer, order, customer).flatMap { optOrder =>
            optOrder.map { (o: Order) =>
              FullOrder.fromOrder(o).map { root =>
                root.map(Good(_)).getOrElse(Bad(List(GeneralFailure("could not render order"))))
              }
            }.getOrElse(Future.successful(Bad(List(NotFoundFailure(order)))))
          }

        case Bad(errors) ⇒ Future.successful(Bad(errors))
      }
  }

  // creates CreditCardGateways, uses its id for an AppliedPayment record, and attempts to associate billing info
  // from stripe to a BillingAddress
  private [this] def createRecords(stripeCustomer: StripeCustomer, order: Order, customer: Customer)
    (implicit ec: ExecutionContext, db: Database): Future[Option[Order]] = {

    val appliedPayment = OrderPayment.fromStripeCustomer(stripeCustomer, order)
    val cc = CreditCard.build(stripeCustomer, this.cardPayload).copy(customerId = customer.id)
    val billingAddress = this.cardPayload.address.map(Address.fromPayload(_).copy(customerId = customer.id))
    val orderBillingAddress = billingAddress.map(OrderBillingAddress.buildFromAddress(_))

    val noAddress: DBIOAction[Option[Address], NoStream, All] = DBIO.successful(None)

    val c: FixedSqlAction[CreditCard#Id, NoStream, Effect.Write] = CreditCards.returningId += cc
    val d = c.andFinally

    val queries = for {
      ba ← billingAddress.fold(noAddress)(Addresses.save(_).map(Some(_)))
      _ ← billingAddress.map(Addresses.save(_)).getOrElse(DBIO.successful(None))
      _ ← ba.fold(None)((a: Address) ⇒ CreditCards.returningId += cc.copy(billingAddressId = a.id))
      ccId ← CreditCards.returningId += cc
      appliedPaymentId ← OrderPayments.returningId += appliedPayment.copy(paymentMethodId = ccId)
      _ ← orderBillingAddress.map(OrderBillingAddresses.save(_)).getOrElse(DBIO.successful(None))
      c ← Orders._findById(order.id).result.headOption
    } yield c

    db.run(queries.transactionally)
  }
}

object CreditCardPaymentCreator {
  type Response = Future[FullOrder.Root Or Failures]

  def run(order: Order, customer: Customer, payload: CreateCreditCard)
    (implicit ec: ExecutionContext, db: Database): Response = {
    new CreditCardPaymentCreator(order, customer, payload).run()
  }
}

