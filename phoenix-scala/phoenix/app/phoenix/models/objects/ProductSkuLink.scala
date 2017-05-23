package phoenix.models.objects

import java.time.Instant

import models.objects.ObjectHeadLinks._
import phoenix.models.inventory._
import phoenix.models.product._
import shapeless._
import utils.db.ExPostgresDriver.api._
import utils.db._

case class ProductSkuLink(id: Int = 0,
                          leftId: Int,
                          rightId: Int,
                          createdAt: Instant = Instant.now,
                          updatedAt: Instant = Instant.now)
    extends FoxModel[ProductSkuLink]
    with ObjectHeadLink[ProductSkuLink]

class ProductSkuLinks(tag: Tag) extends ObjectHeadLinks[ProductSkuLink](tag, "product_sku_links") {

  def * =
    (id, leftId, rightId, createdAt, updatedAt) <> ((ProductSkuLink.apply _).tupled, ProductSkuLink.unapply)

  def left  = foreignKey(Products.tableName, leftId, Products)(_.id)
  def right = foreignKey(Skus.tableName, rightId, Skus)(_.id)
}

object ProductSkuLinks
    extends ObjectHeadLinkQueries[ProductSkuLink, ProductSkuLinks, Product, Sku](
        new ProductSkuLinks(_),
        Products,
        Skus)
    with ReturningId[ProductSkuLink, ProductSkuLinks] {

  val returningLens: Lens[ProductSkuLink, Int] = lens[ProductSkuLink].id

  def build(left: Product, right: Sku) = ProductSkuLink(leftId = left.id, rightId = right.id)
}