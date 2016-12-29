package services.inventory

import java.time.Instant

import com.github.tminglei.slickpg.LTree
import cats.data._
import failures.ProductFailures._
import failures.{Failures, GeneralFailure, NotFoundFailure400}
import models.account._
import models.inventory._
import models.objects._
import payloads.ImagePayloads.AlbumPayload
import payloads.ProductVariantPayloads._
import responses.AlbumResponses.AlbumResponse.{Root ⇒ AlbumRoot}
import responses.AlbumResponses._
import responses.ObjectResponses.ObjectContextResponse
import responses.ProductVariantResponses._
import services.LogActivity
import services.image.ImageManager
import services.image.ImageManager.FullAlbumWithImages
import services.objects.ObjectManager
import slick.driver.PostgresDriver.api._
import utils.JsonFormatters
import utils.aliases._
import utils.db._

object ProductVariantManager {
  implicit val formats = JsonFormatters.DefaultFormats

  def create(admin: User, payload: ProductVariantPayload)(
      implicit ec: EC,
      db: DB,
      ac: AC,
      oc: OC,
      au: AU): DbResultT[ProductVariantResponse.Root] = {
    val albumPayloads = payload.albums.getOrElse(Seq.empty)

    for {
      variant ← * <~ createInner(oc, payload)
      albums  ← * <~ findOrCreateAlbumsForVariant(variant.model, albumPayloads)
      albumResponse = albums.map { case (album, images) ⇒ AlbumResponse.build(album, images) }
      response = ProductVariantResponse
        .build(IlluminatedVariant.illuminate(oc, variant), albumResponse)
      _ ← * <~ LogActivity
           .fullVariantCreated(Some(admin), response, ObjectContextResponse.build(oc))
    } yield response
  }

  def getBySkuCode(
      code: String)(implicit ec: EC, db: DB, oc: OC): DbResultT[ProductVariantResponse.Root] =
    for {
      variant ← * <~ ProductVariantManager.mustFindByContextAndCode(oc.id, code)
      form    ← * <~ ObjectForms.mustFindById404(variant.formId)
      shadow  ← * <~ ObjectShadows.mustFindById404(variant.shadowId)
      albums  ← * <~ ImageManager.getAlbumsForVariantInner(variant.code, oc)
    } yield
      ProductVariantResponse
        .build(IlluminatedVariant.illuminate(oc, FullObject(variant, form, shadow)), albums)

  def update(admin: User, code: String, payload: ProductVariantPayload)(
      implicit ec: EC,
      db: DB,
      ac: AC,
      oc: OC,
      au: AU): DbResultT[ProductVariantResponse.Root] =
    for {
      variant        ← * <~ ProductVariantManager.mustFindByContextAndCode(oc.id, code)
      updatedVariant ← * <~ updateInner(variant, payload)
      albums         ← * <~ updateAssociatedAlbums(updatedVariant.model, payload.albums)
      response = ProductVariantResponse
        .build(IlluminatedVariant.illuminate(oc, updatedVariant), albums)
      _ ← * <~ LogActivity
           .fullVariantUpdated(Some(admin), response, ObjectContextResponse.build(oc))
    } yield response

  def archiveByCode(
      code: String)(implicit ec: EC, db: DB, oc: OC): DbResultT[ProductVariantResponse.Root] =
    for {
      fullVariant ← * <~ ObjectManager.getFullObject(
                       ProductVariantManager.mustFindByContextAndCode(oc.id, code))
      _ ← * <~ fullVariant.model.mustNotBePresentInCarts
      archivedVariant ← * <~ ProductVariants.update(
                           fullVariant.model,
                           fullVariant.model.copy(archivedAt = Some(Instant.now)))
      albumLinks ← * <~ VariantAlbumLinks.filter(_.leftId === archivedVariant.id).result
      _ ← * <~ albumLinks.map { link ⇒
           VariantAlbumLinks.deleteById(link.id,
                                        DbResultT.unit,
                                        id ⇒ NotFoundFailure400(VariantAlbumLinks, id))
         }
      albums       ← * <~ ImageManager.getAlbumsForVariantInner(archivedVariant.code, oc)
      productLinks ← * <~ ProductVariantLinks.filter(_.rightId === archivedVariant.id).result
      _ ← * <~ productLinks.map { link ⇒
           ProductVariantLinks.deleteById(link.id,
                                          DbResultT.unit,
                                          id ⇒ NotFoundFailure400(ProductVariantLinks, id))
         }
    } yield
      ProductVariantResponse.build(
          IlluminatedVariant.illuminate(oc,
                                        FullObject(model = archivedVariant,
                                                   form = fullVariant.form,
                                                   shadow = fullVariant.shadow)),
          albums)

  def createInner(context: ObjectContext, payload: ProductVariantPayload)(
      implicit ec: EC,
      db: DB,
      au: AU): DbResultT[FullObject[ProductVariant]] = {

    val form   = ObjectForm.fromPayload(ProductVariant.kind, payload.attributes)
    val shadow = ObjectShadow.fromPayload(payload.attributes)

    for {
      scope ← * <~ Scope.resolveOverride(payload.scope)
      code  ← * <~ mustGetSkuCode(payload)
      ins   ← * <~ ObjectUtils.insert(form, shadow, payload.schema)
      variant ← * <~ ProductVariants.create(
                   ProductVariant(scope = scope,
                                  contextId = context.id,
                                  code = code,
                                  formId = ins.form.id,
                                  shadowId = ins.shadow.id,
                                  commitId = ins.commit.id))
    } yield FullObject(variant, ins.form, ins.shadow)
  }

  def updateInner(variant: ProductVariant, payload: ProductVariantPayload)(
      implicit ec: EC,
      db: DB): DbResultT[FullObject[ProductVariant]] = {

    val newFormAttrs   = ObjectForm.fromPayload(ProductVariant.kind, payload.attributes).attributes
    val newShadowAttrs = ObjectShadow.fromPayload(payload.attributes).attributes
    val code           = getSkuCode(payload.attributes).getOrElse(variant.code)

    for {
      oldForm   ← * <~ ObjectForms.mustFindById404(variant.formId)
      oldShadow ← * <~ ObjectShadows.mustFindById404(variant.shadowId)

      mergedAttrs = oldShadow.attributes.merge(newShadowAttrs)
      updated ← * <~ ObjectUtils
                 .update(oldForm.id, oldShadow.id, newFormAttrs, mergedAttrs, force = true)
      commit      ← * <~ ObjectUtils.commit(updated)
      updatedHead ← * <~ updateHead(variant, code, updated.shadow, commit)
    } yield FullObject(updatedHead, updated.form, updated.shadow)
  }

  def findOrCreate(variantPayload: ProductVariantPayload)(
      implicit ec: EC,
      db: DB,
      oc: OC,
      au: AU): DbResultT[FullObject[ProductVariant]] =
    for {
      code ← * <~ mustGetSkuCode(variantPayload)
      variant ← * <~ ProductVariants.filterByContextAndCode(oc.id, code).one.dbresult.flatMap {
                 case Some(variant) ⇒ ProductVariantManager.updateInner(variant, variantPayload)
                 case None          ⇒ ProductVariantManager.createInner(oc, variantPayload)
               }
    } yield variant

  private def updateHead(
      variant: ProductVariant,
      code: String,
      shadow: ObjectShadow,
      maybeCommit: Option[ObjectCommit])(implicit ec: EC): DbResultT[ProductVariant] =
    maybeCommit match {
      case Some(commit) ⇒
        ProductVariants
          .update(variant, variant.copy(code = code, shadowId = shadow.id, commitId = commit.id))
      case None ⇒
        DbResultT.good(variant)
    }

  def mustGetSkuCode(payload: ProductVariantPayload): Failures Xor String =
    getSkuCode(payload.attributes) match {
      case Some(code) ⇒ Xor.right(code)
      case None       ⇒ Xor.left(GeneralFailure("SKU code not found in payload").single)
    }

  def getSkuCode(attributes: Map[String, Json]): Option[String] =
    attributes.get("code").flatMap(json ⇒ (json \ "v").extractOpt[String])

  def findOrCreateAlbumsForVariant(variant: ProductVariant, payload: Seq[AlbumPayload])(
      implicit ec: EC,
      db: DB,
      oc: OC,
      au: AU): DbResultT[Seq[FullAlbumWithImages]] =
    for {
      albums ← * <~ payload.map(ImageManager.updateOrCreateAlbum)
      _ ← * <~ VariantAlbumLinks.syncLinks(variant, albums.map {
           case (fullAlbum, _) ⇒ fullAlbum.model
         })
    } yield albums

  private def updateAssociatedAlbums(variant: ProductVariant,
                                     albumsPayload: Option[Seq[AlbumPayload]])(
      implicit ec: EC,
      db: DB,
      oc: OC,
      au: AU): DbResultT[Seq[AlbumRoot]] =
    albumsPayload match {
      case Some(payloads) ⇒
        findOrCreateAlbumsForVariant(variant, payloads).map(_.map(AlbumResponse.build))
      case None ⇒
        ImageManager.getAlbumsForVariantInner(variant.code, oc)
    }

  def mustFindByContextAndCode(contextId: Int, code: String)(
      implicit ec: EC): DbResultT[ProductVariant] =
    for {
      sku ← * <~ ProductVariants
             .filterByContextAndCode(contextId, code)
             .mustFindOneOr(ProductVariantNotFoundForContext(code, contextId))
    } yield sku

  def mustFindFullById(id: Int)(implicit ec: EC, db: DB): DbResultT[FullObject[ProductVariant]] =
    ObjectManager.getFullObject(
        ProductVariants.filter(_.id === id).mustFindOneOr(ProductVariantNotFound(id)))

  def mustFindFullByIdAndShadowId(skuId: Int, shadowId: Int)(
      implicit ec: EC,
      db: DB): DbResultT[FullObject[ProductVariant]] =
    for {
      shadow  ← * <~ ObjectShadows.mustFindById404(shadowId)
      form    ← * <~ ObjectForms.mustFindById404(shadow.formId)
      variant ← * <~ ProductVariants.mustFindById404(skuId)
    } yield FullObject(variant, form, shadow)

  def illuminateVariant(fullVariant: FullObject[ProductVariant])(
      implicit ec: EC,
      db: DB,
      oc: OC): DbResultT[ProductVariantResponse.Root] =
    ImageManager
      .getAlbumsByVariant(fullVariant.model)
      .map(albums ⇒
            ProductVariantResponse.buildLite(IlluminatedVariant.illuminate(oc, fullVariant),
                                             albums))
}
