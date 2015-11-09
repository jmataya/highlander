package models

import utils.Seeds.Factories
import util.IntegrationTestBase
import utils.Slick.implicits._

class InventoryAdjustmentIntegrationTest extends IntegrationTestBase {
  import api._
  import concurrent.ExecutionContext.Implicits.global

  def seed(): (Sku, OrderLineItemSku, Order) = {
    val sku = Skus.saveNew(Factories.skus.head.copy(price = 5)).run().futureValue
    val order = Orders.saveNew(Order(id = 0, customerId = 1)).run().futureValue
    val lineItemSku = OrderLineItemSkus.saveNew(OrderLineItemSku(skuId = sku.id, orderId = order.id)).run().futureValue
    (sku, lineItemSku, order)
  }

  "InventoryAdjustment" - {
    "createAdjustmentsForOrder creates an adjustment with the correct reservation based on line items" in {
      // Simulate `order_line_item_skus` offset, to make SKU ID different from relation ID
      // This is required to properly test query in `InventoryAdjustment.createAdjustmentsForOrder()`
      seed()
      Orders.findByCustomerId(1).map(_.status).update(Order.Shipped).run().futureValue

      // Start actual testing
      val (sku, lineItemSku, order) = seed()

      (OrderLineItems.returningId ++= (1 to 5).map { _ ⇒
        OrderLineItem(orderId = order.id, originId = lineItemSku.id, originType = OrderLineItem.SkuItem)
      }).run().futureValue

      InventoryAdjustments.createAdjustmentsForOrder(order).run().futureValue
      val numAdjustments = InventoryAdjustments.filter(_.inventoryEventId === order.id).length.result.run().futureValue
      val summary = InventorySummaries.findBySkuId(sku.id).one.run().futureValue.value

      numAdjustments mustBe 1
      summary.availableOnHand must === (-5)
    }
  }
}

