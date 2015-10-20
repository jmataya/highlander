package routes

import scala.concurrent.{ExecutionContext, Future}
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer

import cats.data.Xor
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import models._

import payloads._
import responses.FullOrder
import services._
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives._
import utils.Slick.DbResult
import utils.Slick._
import utils.Slick.implicits._

object Customer {
  def routes(implicit ec: ExecutionContext, db: Database,
    mat: Materializer, customerAuth: AsyncAuthenticator[Customer]) = {
    import Json4sSupport._
    import utils.Http._

    authenticateBasicAsync(realm = "private customer routes", customerAuth) { customer ⇒
      pathPrefix("my") {
        (get & path("cart")) {
          complete {
            whenOrderFoundAndEditable(customer) { activeOrder ⇒
              FullOrder.fromOrder(activeOrder).run().map(Xor.right)
            }
          }
        } ~
        pathPrefix("addresses") {
          (get & sortAndPage) { implicit sortAndPage ⇒
            goodOrFailures {
              AddressManager.findAllVisibleByCustomer(customer.id)
            }
          } ~
          (post & entity(as[CreateAddressPayload])) { payload ⇒
            complete {
              AddressManager.create(payload, customer.id).map(renderGoodOrFailures)
            }
          }
        } ~
        pathPrefix("payment-methods") {
          pathPrefix("store-credits") {
            (get & pathEnd & sortAndPage) { implicit sortAndPage ⇒
              goodOrFailures {
                StoreCreditService.findAllByCustomer(customer.id)
              }
            } ~
            (get & path(IntNumber)) { storeCreditId ⇒
              complete {
                renderOrNotFound(StoreCredits.findByIdAndCustomerId(storeCreditId, customer.id).run())
              }
            }
          }
        } ~
        pathPrefix("order") {
          (post & path("checkout")) {
            complete {
              whenOrderFoundAndEditable(customer) {
                order ⇒ new Checkout(order).checkout
              }
            }
          } ~
          (post & path("line-items") & entity(as[Seq[UpdateLineItemsPayload]])) { reqItems ⇒
            goodOrFailures {
              LineItemUpdater.updateQuantitiesOnCustomersOrder(customer, reqItems)
            }
          } ~
          (get & path(PathEnd)) {
            goodOrFailures {
              val finder = Orders.findActiveOrderByCustomer(customer)
              finder.selectOne { order ⇒ DbResult.fromDbio(fullOrder(finder)) }
            }
          }
        }
      }
    }
  }
}

