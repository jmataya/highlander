package services.orders

import cats.implicits._
import models.objects.ObjectContext
import models.customer.{Customers, Customer}
import models.order.{Orders, Order}
import models.StoreAdmin
import payloads.OrderPayloads.CreateOrder
import responses.order.FullOrder
import responses.order.FullOrder.Root
import services.{LogActivity, Result, ResultT}
import utils.aliases._
import utils.db._
import utils.db.DbResultT._

object OrderCreator {

  def createCart(admin: StoreAdmin, payload: CreateOrder, context: ObjectContext)(
      implicit db: DB, ec: EC, ac: AC): Result[Root] = {

    def existingCustomerOrNewGuest: Result[Root] = (payload.customerId, payload.email) match {
      case (Some(customerId), _) ⇒ createCartForCustomer(customerId)
      case (_, Some(email))      ⇒ createCartAndGuest(email)
      case _                     ⇒ ???
    }

    def createCartForCustomer(customerId: Int): Result[Root] =
      (for {
        customer ← * <~ Customers.mustFindById400(customerId)
        fullOrder ← * <~ OrderQueries.findOrCreateCartByCustomerInner(
                       customer, context, Some(admin))
      } yield fullOrder).runTxn()

    def createCartAndGuest(email: String): Result[Root] =
      (for {
        guest ← * <~ Customers.create(Customer.buildGuest(email = email))
        cart  ← * <~ Orders.create(Order.buildCart(guest.id, context.id))
        _     ← * <~ LogActivity.cartCreated(Some(admin), root(cart, guest))
      } yield root(cart, guest)).runTxn()

    (for {
      _    ← ResultT.fromXor(payload.validate.toXor)
      root ← ResultT(existingCustomerOrNewGuest)
    } yield root).value
  }

  private def root(order: Order, customer: Customer): Root =
    FullOrder.build(order = order, customer = customer.some)
}
