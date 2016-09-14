package services.inventory

import java.time.Instant

import cats.data._
import failures.ProductFailures._
import failures.{Failures, GeneralFailure, NotFoundFailure400}
import models.StoreAdmin
import models.inventory._
import models.objects._
import payloads.SkuPayloads._
import responses.ObjectResponses.ObjectContextResponse
import responses.SkuResponses._
import services.LogActivity
import services.image.ImageManager
import services.objects.ObjectManager
import slick.driver.PostgresDriver.api._
import utils.JsonFormatters
import utils.aliases._
import utils.db._

object SkuManager {
  implicit val formats = JsonFormatters.DefaultFormats

  def createSku(admin: StoreAdmin, contextName: String, payload: SkuPayload)(implicit ec: EC,
                                                          db: DB): DbResultT[SkuResponse.Root] =
    for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      sku     ← * <~ createSkuInner(context, payload)
      albums  ← * <~ ImageManager.getAlbumsForSkuInner(sku.model.code, context)
      response = SkuResponse.build(IlluminatedSku.illuminate(context, sku), albums)
      _ ← * <~ LogActivity.fullSkuCreated(Some(admin), response, ObjectContextResponse.build(context))
    } yield response

  def getSku(contextName: String, code: String)(implicit ec: EC,
                                                db: DB): DbResultT[SkuResponse.Root] =
    for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      sku     ← * <~ SkuManager.mustFindSkuByContextAndCode(context.id, code)
      form    ← * <~ ObjectForms.mustFindById404(sku.formId)
      shadow  ← * <~ ObjectShadows.mustFindById404(sku.shadowId)
      albums  ← * <~ ImageManager.getAlbumsForSkuInner(sku.code, context)
    } yield
      SkuResponse.build(IlluminatedSku.illuminate(context, FullObject(sku, form, shadow)), albums)

  def updateSku(admin: StoreAdmin, contextName: String, code: String, payload: SkuPayload)(
      implicit ec: EC,
      db: DB): DbResultT[SkuResponse.Root] =
    for {
      context    ← * <~ ObjectManager.mustFindByName404(contextName)
      sku        ← * <~ SkuManager.mustFindSkuByContextAndCode(context.id, code)
      updatedSku ← * <~ updateSkuInner(sku, payload)
      albums     ← * <~ ImageManager.getAlbumsForSkuInner(updatedSku.model.code, context)
      response = SkuResponse.build(IlluminatedSku.illuminate(context, updatedSku), albums)
      _ ← * <~ LogActivity.fullSkuUpdated(Some(admin), response, ObjectContextResponse.build(context))
    } yield response

  def archiveByContextAndId(contextName: String,
                            code: String)(implicit ec: EC, db: DB): DbResultT[SkuResponse.Root] =
    for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      fullSku ← * <~ ObjectManager.getFullObject(
                   SkuManager.mustFindSkuByContextAndCode(context.id, code))
      archivedSku ← * <~ Skus.update(fullSku.model,
                                     fullSku.model.copy(archivedAt = Some(Instant.now)))
      albumLinks ← * <~ SkuAlbumLinks.filter(_.leftId === archivedSku.id).result
      _ ← * <~ albumLinks.map { link ⇒
           SkuAlbumLinks.deleteById(link.id,
                                    DbResultT.unit,
                                    id ⇒ NotFoundFailure400(SkuAlbumLinks, id))
         }
      albums       ← * <~ ImageManager.getAlbumsForSkuInner(archivedSku.code, context)
      productLinks ← * <~ ProductSkuLinks.filter(_.rightId === archivedSku.id).result
      _ ← * <~ productLinks.map { link ⇒
           SkuAlbumLinks.deleteById(link.id,
                                    DbResultT.unit,
                                    id ⇒ NotFoundFailure400(SkuAlbumLinks, id))
         }
    } yield
      SkuResponse.build(
          IlluminatedSku.illuminate(
              context,
              FullObject(model = archivedSku, form = fullSku.form, shadow = fullSku.shadow)),
          albums)

  def createSkuInner(context: ObjectContext,
                     payload: SkuPayload)(implicit ec: EC, db: DB): DbResultT[FullObject[Sku]] = {

    val form   = ObjectForm.fromPayload(Sku.kind, payload.attributes)
    val shadow = ObjectShadow.fromPayload(payload.attributes)

    for {
      code ← * <~ mustGetSkuCode(payload)
      ins  ← * <~ ObjectUtils.insert(form, shadow)
      sku ← * <~ Skus.create(
               Sku(contextId = context.id,
                   code = code,
                   formId = ins.form.id,
                   shadowId = ins.shadow.id,
                   commitId = ins.commit.id))
    } yield FullObject(sku, ins.form, ins.shadow)
  }

  def updateSkuInner(sku: Sku, payload: SkuPayload)(implicit ec: EC,
                                                    db: DB): DbResultT[FullObject[Sku]] = {

    val newFormAttrs   = ObjectForm.fromPayload(Sku.kind, payload.attributes).attributes
    val newShadowAttrs = ObjectShadow.fromPayload(payload.attributes).attributes
    val code           = getSkuCode(payload.attributes).getOrElse(sku.code)

    for {
      oldForm   ← * <~ ObjectForms.mustFindById404(sku.formId)
      oldShadow ← * <~ ObjectShadows.mustFindById404(sku.shadowId)

      mergedAttrs = oldShadow.attributes.merge(newShadowAttrs)
      updated ← * <~ ObjectUtils
                 .update(oldForm.id, oldShadow.id, newFormAttrs, mergedAttrs, force = true)
      commit      ← * <~ ObjectUtils.commit(updated)
      updatedHead ← * <~ updateHead(sku, code, updated.shadow, commit)
    } yield FullObject(updatedHead, updated.form, updated.shadow)
  }

  def findOrCreateSku(skuPayload: SkuPayload)(implicit ec: EC, db: DB, oc: OC) =
    for {
      code ← * <~ mustGetSkuCode(skuPayload)
      sku ← * <~ Skus.filterByContextAndCode(oc.id, code).one.toXor.flatMap {
             case Some(sku) ⇒ SkuManager.updateSkuInner(sku, skuPayload)
             case None      ⇒ SkuManager.createSkuInner(oc, skuPayload)
           }
    } yield sku

  private def updateHead(sku: Sku,
                         code: String,
                         shadow: ObjectShadow,
                         maybeCommit: Option[ObjectCommit])(implicit ec: EC): DbResultT[Sku] =
    maybeCommit match {
      case Some(commit) ⇒
        Skus.update(sku, sku.copy(code = code, shadowId = shadow.id, commitId = commit.id))
      case None ⇒
        DbResultT.good(sku)
    }

  def mustGetSkuCode(payload: SkuPayload): Failures Xor String =
    getSkuCode(payload.attributes) match {
      case Some(code) ⇒ Xor.right(code)
      case None       ⇒ Xor.left(GeneralFailure("SKU code not found in payload").single)
    }

  def getSkuCode(attributes: Map[String, Json]): Option[String] =
    attributes.get("code").flatMap(json ⇒ (json \ "v").extractOpt[String])

  def mustFindSkuByContextAndCode(contextId: Int, code: String)(implicit ec: EC): DbResultT[Sku] =
    for {
      sku ← * <~ Skus
             .filterByContextAndCode(contextId, code)
             .mustFindOneOr(SkuNotFoundForContext(code, contextId))
    } yield sku

  def mustFindFullSkuById(id: Int)(implicit ec: EC, db: DB, oc: OC): DbResultT[FullObject[Sku]] =
    ObjectManager.getFullObject(Skus.filter(_.id === id).mustFindOneOr(SkuNotFound(id)))

  def illuminateSku(
      fullSku: FullObject[Sku])(implicit ec: EC, db: DB, oc: OC): DbResultT[SkuResponse.Root] =
    ImageManager
      .getAlbumsBySku(fullSku.model)
      .map(albums ⇒ SkuResponse.buildLite(IlluminatedSku.illuminate(oc, fullSku), albums))
}
