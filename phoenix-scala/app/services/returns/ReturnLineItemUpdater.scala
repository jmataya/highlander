package services.returns

import failures.ReturnFailures.{ReturnReasonNotFoundFailure, ReturnShippingCostExceeded}
import models.cord.Orders
import models.objects._
import models.returns._
import payloads.ReturnPayloads._
import responses.ReturnResponse
import services.LogActivity
import services.inventory.SkuManager
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object ReturnLineItemUpdater {

  def addLineItem(refNum: String, payload: ReturnLineItemPayload)(
      implicit ec: EC,
      db: DB,
      ac: AC,
      oc: OC): DbResultT[ReturnResponse.Root] =
    for {
      rma ← * <~ Returns.mustFindActiveByRefNum404(refNum)
      reason ← * <~ ReturnReasons
                .filter(_.id === payload.reasonId)
                .mustFindOneOr(ReturnReasonNotFoundFailure(payload.reasonId))
      _        ← * <~ processAddLineItem(rma, reason, payload)
      updated  ← * <~ Returns.refresh(rma)
      response ← * <~ ReturnResponse.fromRma(updated)
    } yield response

  private def processAddLineItem(
      rma: Return,
      reason: ReturnReason,
      payload: ReturnLineItemPayload)(implicit ec: EC, db: DB, ac: AC, oc: OC) = {
    payload match {
      case giftCard: ReturnGiftCardLineItemPayload     ⇒ addGiftCardItem(rma, reason, giftCard)
      case shipping: ReturnShippingCostLineItemPayload ⇒ addShippingCostItem(rma, reason, shipping)
      case sku: ReturnSkuLineItemPayload               ⇒ addSkuLineItem(rma, reason, sku)
    }
  }

  private def addGiftCardItem(
      rma: Return,
      reason: ReturnReason,
      payload: ReturnGiftCardLineItemPayload)(implicit ec: EC, db: DB): DbResultT[ReturnLineItem] =
    ??? // TODO add gift card handling

  private def validateMaxShippingCost(rma: Return, amount: Int)(implicit ec: EC,
                                                                db: DB): DbResultT[Unit] =
    for {
      order ← * <~ Orders.mustFindByRefNum(rma.orderRef)
      orderShippingTotal = order.shippingTotal
      previouslyReturnedCost ← * <~ Returns
                                .findPrevious(rma)
                                .join(ReturnLineItemShippingCosts)
                                .on(_.id === _.returnId)
                                .map { case (_, shippingCost) ⇒ shippingCost.amount }
                                .sum
                                .getOrElse(0)
                                .result
      maxAmount = orderShippingTotal - previouslyReturnedCost
      _ ← * <~ failIf(amount > maxAmount,
                      ReturnShippingCostExceeded(refNum = rma.referenceNumber,
                                                 amount = amount,
                                                 maxAmount = maxAmount))
    } yield ()

  private def addShippingCostItem(rma: Return,
                                  reason: ReturnReason,
                                  payload: ReturnShippingCostLineItemPayload)(
      implicit ec: EC,
      ac: AC,
      db: DB): DbResultT[ReturnLineItem] =
    for {
      _ ← * <~ validateMaxShippingCost(rma, payload.amount)
      _ ← * <~ ReturnLineItemShippingCosts.findByRmaId(rma.id).deleteAll
      origin ← * <~ ReturnLineItemShippingCosts.create(
                  ReturnLineItemShippingCost(returnId = rma.id, amount = payload.amount))
      _  ← * <~ ReturnLineItems.filter(_.originId === origin.id).deleteAll
      li ← * <~ ReturnLineItems.create(ReturnLineItem.buildShippingCost(rma, reason, origin))
      _  ← * <~ LogActivity().returnShippingCostItemAdded(rma, reason, payload)
    } yield li

  private def addSkuLineItem(rma: Return, reason: ReturnReason, payload: ReturnSkuLineItemPayload)(
      implicit ec: EC,
      db: DB,
      ac: AC,
      oc: OC): DbResultT[ReturnLineItem] =
    for {
      sku       ← * <~ SkuManager.mustFindSkuByContextAndCode(oc.id, payload.sku)
      skuShadow ← * <~ ObjectShadows.mustFindById404(sku.shadowId)
      origin ← * <~ ReturnLineItemSkus.create(
                  ReturnLineItemSku(returnId = rma.id, skuId = sku.id, skuShadowId = skuShadow.id))
      li ← * <~ ReturnLineItems.create(ReturnLineItem.buildSku(rma, reason, origin, payload))
      _  ← * <~ LogActivity().returnSkuLineItemAdded(rma, reason, payload)
    } yield li

  def deleteLineItem(refNum: String, lineItemId: Int)(implicit ec: EC,
                                                      ac: AC,
                                                      db: DB): DbResultT[ReturnResponse.Root] =
    for {
      rma      ← * <~ Returns.mustFindActiveByRefNum404(refNum)
      li       ← * <~ ReturnLineItems.mustFindById404(lineItemId)
      _        ← * <~ processDeleteLineItem(li, li.originType)
      _        ← * <~ ReturnLineItems.filter(_.id === lineItemId).deleteAll
      updated  ← * <~ Returns.refresh(rma)
      response ← * <~ ReturnResponse.fromRma(updated)
    } yield response

  private def processDeleteLineItem(
      lineItem: ReturnLineItem,
      originType: ReturnLineItem.OriginType)(implicit ec: EC, ac: AC, db: DB): DbResultT[Unit] =
    originType match {
      case ReturnLineItem.GiftCardItem ⇒ deleteGiftCardLineItem(lineItem)
      case ReturnLineItem.ShippingCost ⇒ deleteShippingCostLineItem(lineItem)
      case ReturnLineItem.SkuItem      ⇒ deleteSkuLineItem(lineItem)
    }

  private def deleteGiftCardLineItem(lineItem: ReturnLineItem)(implicit ec: EC,
                                                               db: DB): DbResultT[Unit] =
    ReturnLineItemGiftCards.filter(_.id === lineItem.originId).deleteAll.meh

  private def deleteShippingCostLineItem(
      lineItem: ReturnLineItem)(implicit ec: EC, ac: AC, db: DB): DbResultT[Unit] = {
    LogActivity().returnShippingCostItemDeleted(lineItem)
    ReturnLineItemShippingCosts.filter(_.id === lineItem.originId).deleteAll.meh
  }

  private def deleteSkuLineItem(
      lineItem: ReturnLineItem)(implicit ec: EC, ac: AC, db: DB): DbResultT[Unit] = {
    LogActivity().returnSkuLineItemDeleted(lineItem)
    ReturnLineItemSkus.filter(_.id === lineItem.originId).deleteAll.meh
  }
}
