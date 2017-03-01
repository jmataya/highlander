package services.assignments

import models.{Assignment, NotificationSubscription}
import models.inventory.{ProductVariant, ProductVariants}
import responses.ProductVariantResponses.SkuHeadResponse.{Root, build}
import slick.driver.PostgresDriver.api._
import utils.db._
import utils.aliases._

object VariantWatchersManager extends AssignmentsManager[String, ProductVariant] {

  val assignmentType  = Assignment.Watcher
  val referenceType   = Assignment.Variant
  val notifyDimension = models.activity.Dimension.variant
  val notifyReason    = NotificationSubscription.Watching

  def buildResponse(model: ProductVariant): Root = build(model)

  def fetchEntity(code: String)(implicit ec: EC, db: DB, ac: AC): DbResultT[ProductVariant] =
    ProductVariants.mustFindByCode(code)

  def fetchSequence(
      codes: Seq[String])(implicit ec: EC, db: DB, ac: AC): DbResultT[Seq[ProductVariant]] =
    ProductVariants.filter(_.code.inSetBind(codes)).result.dbresult
}