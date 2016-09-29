package models.taxonomy

import java.time.Instant

import _root_.utils.aliases.{EC, OC}
import failures.TaxonomyFailures.NoTermInTaxonomy
import models.objects.ObjectForm
import models.objects.ObjectHeadLinks._
import shapeless._
import utils.db.ExPostgresDriver.api._
import slick.lifted.Tag
import utils.Validation
import utils.db._
import com.github.tminglei.slickpg.LTree

case class TaxonomyTaxonLink(id: Int = 0,
                             index: Int,
                             taxonomyId: Int,
                             taxonId: Int,
                             position: Int,
                             path: LTree,
                             updatedAt: Instant = Instant.now,
                             createdAt: Instant = Instant.now,
                             archivedAt: Option[Instant] = None)
    extends FoxModel[TaxonomyTaxonLink]
    with Validation[TaxonomyTaxonLink]
    with ObjectHeadLink[TaxonomyTaxonLink] {
  override def leftId: Id = taxonomyId

  override def rightId: Id = taxonId

  def childPath: LTree =
    if (path.value == List("")) LTree(List(index.toString))
    else LTree(path.value ::: List(index.toString))

  def parentIndex: Option[Int] = path.value.lastOption.filter(!_.isEmpty).map(_.toInt)
}

class TaxonomyTaxonLinks(tag: Tag)
    extends ObjectHeadLinks[TaxonomyTaxonLink](tag, "taxonomy_taxon_links") {

  def archivedAt = column[Option[Instant]]("archived_at")
  def index      = column[Int]("index")
  def position   = column[Int]("position")
  def path       = column[LTree]("path")
  def taxonomyId = column[Int]("taxonomy_id")
  def taxonId    = column[Int]("taxon_id")

  override def leftId  = taxonomyId
  override def rightId = taxonId

  def * =
    (id, index, taxonomyId, taxonId, position, path, updatedAt, createdAt, archivedAt) <> ((TaxonomyTaxonLink.apply _).tupled, TaxonomyTaxonLink.unapply)
}

object TaxonomyTaxonLinks
    extends ObjectHeadLinkQueries[TaxonomyTaxonLink, TaxonomyTaxonLinks, Taxonomy, Taxon](
        new TaxonomyTaxonLinks(_),
        Taxonomies,
        Taxons)
    with ReturningId[TaxonomyTaxonLink, TaxonomyTaxonLinks] {

  def hasChildren(link: TaxonomyTaxonLink) =
    active().filter(l ⇒ l.path @> link.path && !(l.path === link.path)).exists

  val returningLens: Lens[TaxonomyTaxonLink, Int] = lens[TaxonomyTaxonLink].id

  private def shiftPositions(taxonomyId: Int, path: LTree, position: Int): DBIO[Int] = {
    val pathString = path.toString()
    sqlu"""UPDATE taxonomy_taxon_links SET position = position + 1
           WHERE taxonomy_id = $taxonomyId
           AND path=text2ltree($pathString)
           AND position >= $position
           and archived_at is null"""
  }

  private def shrinkPositions(taxonId: Int, path: LTree, position: Int): DBIO[Int] = {
    val pathString = path.toString()
    sqlu"""update taxonomy_taxon_links set position = position - 1 where taxonomy_id = $taxonId
           and path=text2ltree($pathString)
           and position >= $position
           and archived_at is null"""
  }

  def moveTaxonAfter(link: TaxonomyTaxonLink, after: TaxonomyTaxonLink)(
      implicit ec: EC): DbResultT[TaxonomyTaxonLink] = {
    require(link.id != after.id)
    require(link.position < 0)
    require(after.position >= 0)
    require(link.parentIndex == after.parentIndex)

    val newPosition: Int = after.position + 1
    for {
      _       ← * <~ shiftPositions(link.taxonomyId, link.path, newPosition)
      newLink ← * <~ TaxonomyTaxonLinks.update(link, link.copy(position = newPosition))
      _ = assert(newLink.id == link.id && newLink.position == after.position + 1)
    } yield newLink
  }

  def archivate(link: TaxonomyTaxonLink)(implicit ec: EC): DbResultT[Unit] =
    for {
      _ ← * <~ (if (link.archivedAt.isDefined) DbResultT.good(link)
                else TaxonomyTaxonLinks.update(link, link.copy(archivedAt = Some(Instant.now))))
      _ ← * <~ shrinkPositions(link.taxonomyId, link.path, link.position)
    } yield {}

  def updatePath(taxonomyId: Int, oldPrefix: LTree, newPrefix: LTree): DBIO[Int] = {
    val patternLength = oldPrefix.value.length
    sqlu"""UPDATE taxonomy_taxon_links AS t
             SET path = (text2ltree(${newPrefix.toString}) || subpath(t.path, $patternLength))
             WHERE t.taxonomy_id=$taxonomyId
             AND (t.path <@ text2ltree(${oldPrefix.toString})
             and not t.path = text2ltree(${oldPrefix.toString}))
             and t.archived_at is null""".andThen {
      sqlu"""UPDATE taxonomy_taxon_links AS t
             SET path = text2ltree(${newPrefix.toString})
             WHERE t.taxon_id=$taxonomyId
             AND t.path = text2ltree(${oldPrefix.toString})
             and t.archived_at is null"""
    }
  }
  import scope._

  def getNextPosition(taxonomyId: Int, path: LTree): Rep[Int] =
    filter(link ⇒ link.taxonomyId === taxonomyId && (link.path ~ path.toString())).nonArchived
      .map(_.position)
      .max
      .map(_ + 1)
      .getOrElse(0)

  def active(active: Boolean = true): QuerySeq = filter(_.archivedAt.isEmpty === active)

  def nextIndex(taxonomyId: Taxonomy#Id)(implicit ec: EC): Rep[Int] =
    filter(_.taxonomyId === taxonomyId).nonArchived.map(_.index).max.map(_ + 1).getOrElse(0)

  def filterByTaxonFormId(taxonFormId: ObjectForm#Id)(implicit oc: OC): QuerySeq =
    join(Taxons).on { case (link, term) ⇒ link.taxonId === term.id }.filter {
      case (_, term) ⇒
        term.formId ===
          taxonFormId && term.contextId === oc.id
    }.map { case (link, _) ⇒ link }

  def filterByTaxonomyAndTaxonFormId(taxonomyId: Taxonomy#Id, taxonFormId: ObjectForm#Id)(
      implicit oc: OC): QuerySeq =
    filterByTaxonFormId(taxonFormId).filter(_.taxonomyId === taxonomyId)

  def build(left: Taxonomy, right: Taxon): TaxonomyTaxonLink =
    TaxonomyTaxonLink(0, 0, left.id, right.id, 0, LTree(""))

  override protected def filterLeftId(leftId: Int): TaxonomyTaxonLinks.QuerySeq =
    super.filterLeftId(leftId).filter(_.archivedAt.isEmpty)

  override protected def filterRightId(rightId: Int): TaxonomyTaxonLinks.QuerySeq =
    super.filterRightId(rightId).filter(_.archivedAt.isEmpty)

  object scope {
    implicit class ExtractLinks(q: QuerySeq) {
      def nonArchived = q.filter(_.archivedAt.isEmpty)

      def mustFindByTaxonomyAndTaxonFormId(taxonomy: Taxonomy,
                                           taxonFormId: ObjectForm#Id)(implicit oc: OC, ec: EC) =
        filterByTaxonomyAndTaxonFormId(taxonomy.id, taxonFormId)
          .mustFindOneOr(NoTermInTaxonomy(taxonomy.formId, taxonFormId))
    }
  }
}
