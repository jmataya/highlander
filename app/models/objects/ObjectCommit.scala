package models.objects

import java.time.Instant

import models.Aliases.Json
import monocle.macros.GenLens
import utils.ExPostgresDriver.api._
import utils.JsonFormatters
import utils.time.JavaTimeSlickMapper._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId, Validation}

/**
 * A ObjectCommit is a tree of commits, each pointing to a form shadow.
 */
case class ObjectCommit(id: Int = 0, formId: Int, shadowId: Int,
  reasonId: Option[Int] = None, previousId: Option[Int] = None, createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[ObjectCommit]
  with Validation[ObjectCommit]

class ObjectCommits(tag: Tag) extends GenericTable.TableWithId[ObjectCommit](tag, "object_commits")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def formId = column[Int]("form_id")
  def shadowId = column[Int]("shadow_id")
  def reasonId = column[Option[Int]]("reason_id")
  def previousId = column[Option[Int]]("previous_id")
  def createdAt = column[Instant]("created_at")

  def * = (id, formId, shadowId, reasonId, previousId, createdAt) <> ((ObjectCommit.apply _).tupled, ObjectCommit.unapply)

  def shadow = foreignKey(ObjectShadows.tableName, shadowId, ObjectShadows)(_.id)
  def form = foreignKey(ObjectForms.tableName, formId, ObjectForms)(_.id)

}

object ObjectCommits extends TableQueryWithId[ObjectCommit, ObjectCommits](
  idLens = GenLens[ObjectCommit](_.id))(new ObjectCommits(_)) {

  implicit val formats = JsonFormatters.phoenixFormats

  def filterByObject(formId: Int): QuerySeq = 
    filter(_.formId === formId)
}
