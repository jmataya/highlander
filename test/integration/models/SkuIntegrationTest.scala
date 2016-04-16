package models

import models.product.{Mvp, SimpleContext}
import models.objects._
import models.order.lineitems.OrderLineItemSkus
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.Tables.Table
import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._
import utils.seeds.Seeds
import Seeds.Factories

import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.Tables.Table

class SkuModelIntegrationTest extends IntegrationTestBase {
  import api._
  import concurrent.ExecutionContext.Implicits.global

  "Skus" - {
    "a Postgres trigger creates a `order_line_item_skus` record after `skus` insert" in {
      val (product, liSku) = (for {
        context ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
        product     ← * <~ Mvp.insertProduct(context.id, Factories.products.head)
        liSku ← * <~ OrderLineItemSkus.safeFindBySkuId(product.skuId).toXor
      } yield (product, liSku)).runTxn().futureValue.rightVal

      product.skuId must === (liSku.skuId)
    }
  }
}

