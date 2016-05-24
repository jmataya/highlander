package models.product

import java.time.Instant

import models.objects._
import shapeless._
import slick.driver.PostgresDriver.api._
import slick.lifted.Tag
import utils.db._
import utils.{JsonFormatters, Validation}

object Variant {
  val kind = "variant"
}

/**
  * A Variant represents the attributes that are used to collect SKUs into a
  * product. One Variant will contain an array of many values, which define the
  * individual attributes that can be used to select a SKU.
  */
case class Variant(id: Int = 0,
                   variantType: String,
                   contextId: Int,
                   shadowId: Int,
                   formId: Int,
                   commitId: Int,
                   updatedAt: Instant = Instant.now,
                   createdAt: Instant = Instant.now)
    extends FoxModel[Variant]
    with Validation[Variant]

class Variants(tag: Tag) extends ObjectHeads[Variant](tag, "variants") {
  def variantType = column[String]("variant_type")

  def * =
    (id, variantType, contextId, shadowId, formId, commitId, updatedAt, createdAt) <> ((Variant.apply _).tupled, Variant.unapply)
}

object Variants
    extends FoxTableQuery[Variant, Variants](new Variants(_))
    with ReturningId[Variant, Variants] {

  val returningLens: Lens[Variant, Int] = lens[Variant].id

  def filterByContext(contextId: Int): QuerySeq =
    filter(_.contextId === contextId)

  def filterByFormId(formId: Int): QuerySeq =
    filter(_.formId === formId)

  def filterByContextAndFormIds(contextId: Int, formIds: Seq[Int]): QuerySeq =
    filterByContext(contextId).filter(_.id.inSet(formIds))

  def filterByContextAndFormId(contextId: Int, formId: Int): QuerySeq =
    filterByContext(contextId).filter(_.formId === formId)
}
