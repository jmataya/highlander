package services.carts

import models.StoreAdmin
import models.cord.{CartLockEvent, CartLockEvents, Carts}
import responses.cart.FullCart
import services._
import utils.aliases._
import utils.db._

object CartLockUpdater {

  def lock(refNum: String, admin: StoreAdmin)(implicit ec: EC, db: DB): Result[FullCart.Root] =
    (for {
      cart ← * <~ Carts.mustFindByRefNum(refNum)
      _    ← * <~ cart.mustBeActive
      _    ← * <~ cart.mustNotBeLocked
      _    ← * <~ Carts.update(cart, cart.copy(isLocked = true))
      _    ← * <~ CartLockEvents.create(CartLockEvent(cartRef = cart.refNum, lockedBy = admin.id))
      resp ← * <~ FullCart.buildRefreshed(cart)
    } yield resp).runTxn()

  def unlock(refNum: String)(implicit ec: EC, db: DB): Result[FullCart.Root] =
    (for {
      cart     ← * <~ Carts.mustFindByRefNum(refNum)
      _        ← * <~ cart.mustBeActive
      _        ← * <~ cart.mustBeLocked
      _        ← * <~ Carts.update(cart, cart.copy(isLocked = false))
      response ← * <~ FullCart.buildRefreshed(cart)
    } yield response).runTxn()
}
