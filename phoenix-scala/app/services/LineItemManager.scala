package services

import cats.data._
import cats.implicits._
import failures.ProductFailures.NoProductFoundForVariant
import models.cord.lineitems._
import models.image.{AlbumImageLinks, Albums, Images}
import models.inventory.ProductVariant
import models.objects._
import models.product._
import org.json4s.JsonAST.JString
import services.image.ImageManager
import services.inventory.ProductVariantManager
import services.objects.ObjectManager
import services.product.ProductManager
import slick.driver.PostgresDriver.api._
import utils._
import utils.aliases._
import utils.db._

object LineItemManager {
  def getCartLineItems(refNum: String)(implicit ec: EC,
                                       db: DB): DbResultT[Seq[CartLineItemProductData]] =
    for {
      lineItems    ← * <~ CartLineItems.filter(_.cordRef === refNum).result
      lineItemData ← * <~ lineItems.map(getCartLineItem)
    } yield lineItemData

  def getOrderLineItems(refNum: String)(implicit ec: EC,
                                        db: DB): DbResultT[Seq[OrderLineItemProductData]] =
    for {
      lineItems    ← * <~ OrderLineItems.filter(_.cordRef === refNum).result
      lineItemData ← * <~ lineItems.map(getOrderLineItem)
    } yield lineItemData

  private def getCartLineItem(cartLineItem: CartLineItem)(implicit ec: EC, db: DB) =
    for {
      variant ← * <~ ProductVariantManager.mustFindFullById(cartLineItem.variantId)
      product ← * <~ getProductForVariant(variant.model)
      image   ← * <~ getLineItemImage(variant.model, product.model)
    } yield
      CartLineItemProductData(variant = variant.model,
                              variantForm = variant.form,
                              variantShadow = variant.shadow,
                              productForm = product.form,
                              productShadow = product.shadow,
                              image = image,
                              lineItem = cartLineItem,
                              attributes = cartLineItem.attributes)

  private def getOrderLineItem(orderLineItem: OrderLineItem)(implicit ec: EC, db: DB) =
    for {
      variant ← * <~ ProductVariantManager.mustFindFullByIdAndShadowId(
                   orderLineItem.variantId,
                   orderLineItem.variantShadowId)
      product ← * <~ getProductForVariant(variant.model)
      image   ← * <~ getLineItemImage(variant.model, product.model)
    } yield
      OrderLineItemProductData(variant = variant.model,
                               variantForm = variant.form,
                               variantShadow = variant.shadow,
                               productForm = product.form,
                               productShadow = product.shadow,
                               image = image,
                               lineItem = orderLineItem,
                               attributes = orderLineItem.attributes)

  private def getProductForVariant(variant: ProductVariant)(implicit ec: EC, db: DB) =
    for {
      productId ← * <~ ProductVariantLinks.filter(_.rightId === variant.id).one.dbresult.flatMap {
                   case Some(productLink) ⇒
                     DbResultT.good(productLink.leftId)
                   case None ⇒
                     for {
                       valueLink ← * <~ ProductValueVariantLinks
                                    .filter(_.rightId === variant.id)
                                    .mustFindOneOr(NoProductFoundForVariant(variant.id))
                       variantLink ← * <~ ProductOptionValueLinks
                                      .filter(_.rightId === valueLink.leftId)
                                      .mustFindOneOr(NoProductFoundForVariant(variant.id))
                       productLink ← * <~ ProductOptionLinks
                                      .filter(_.rightId === variantLink.leftId)
                                      .mustFindOneOr(NoProductFoundForVariant(variant.id))
                     } yield productLink.leftId
                 }
      product ← * <~ ProductManager.mustFindFullProductById(productId)
    } yield product

  private def getLineItemImage(variant: ProductVariant, product: Product)(implicit ec: EC,
                                                                          db: DB) =
    for {
      image ← * <~ getLineItemAlbumId(variant, product).flatMap {
               case Some(albumId) ⇒
                 for {
                   album ← * <~ Albums.mustFindById404(albumId)
                   image ← * <~ ImageManager.getFirstImageForAlbum(album)
                 } yield image

               case None ⇒
                 DbResultT.none[String]
             }
    } yield image

  private def getLineItemAlbumId(variant: ProductVariant, product: Product)(implicit ec: EC,
                                                                            db: DB) =
    for {
      albumId ← * <~ VariantAlbumLinks.filterLeft(variant).one.dbresult.flatMap {
                 case Some(albumLink) ⇒
                   DbResultT.good(albumLink.rightId.some)
                 case None ⇒
                   for {
                     albumLink ← * <~ ProductAlbumLinks.filterLeft(product).one.dbresult
                   } yield albumLink.map(_.rightId)
               }
    } yield albumId
}
