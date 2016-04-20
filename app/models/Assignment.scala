package models

import java.time.Instant

import com.pellucid.sealerate
import models.Assignment.{AssignmentType, ReferenceType}
import monocle.macros.GenLens
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils.aliases._
import utils.db._
import utils.ADT

case class Assignment(id: Int = 0, assignmentType: AssignmentType, storeAdminId: Int, referenceId: Int,
  referenceType: ReferenceType, createdAt: Instant = Instant.now)
  extends FoxModel[Assignment] {

}

object Assignment {
  sealed trait AssignmentType
  case object Assignee extends AssignmentType
  case object Watcher extends AssignmentType

  object AssignmentType extends ADT[AssignmentType] {
    def types = sealerate.values[AssignmentType]
  }

  implicit val assignemtTypeColumnType: JdbcType[AssignmentType] with BaseTypedType[AssignmentType] =
    AssignmentType.slickColumn

  sealed trait ReferenceType
  case object Order extends ReferenceType
  case object GiftCard extends ReferenceType
  case object Customer extends ReferenceType
  case object Rma extends ReferenceType
  case object Sku extends ReferenceType
  case object Product extends ReferenceType
  case object Promotion extends ReferenceType
  case object Coupon extends ReferenceType

  object ReferenceType extends ADT[ReferenceType] {
    def types = sealerate.values[ReferenceType]
  }

  implicit val refTypeColumnType: JdbcType[ReferenceType] with BaseTypedType[ReferenceType] =
    ReferenceType.slickColumn
}

class Assignments(tag: Tag) extends FoxTable[Assignment](tag, "assignments")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def assignmentType = column[AssignmentType]("assignment_type")
  def storeAdminId = column[Int]("store_admin_id")
  def referenceId = column[Int]("reference_id")
  def referenceType = column[ReferenceType]("reference_type")
  def createdAt = column[Instant]("created_at")

  def * = (id, assignmentType, storeAdminId, referenceId,
    referenceType, createdAt) <> ((Assignment.apply _).tupled, Assignment.unapply)

  def storeAdmin = foreignKey(StoreAdmins.tableName, storeAdminId, StoreAdmins)(_.id)
}

object Assignments extends FoxTableQuery[Assignment, Assignments](
  idLens = GenLens[Assignment](_.id)
)(new Assignments(_)) {

  def byType(assignType: AssignmentType, refType: ReferenceType): QuerySeq =
    filter(_.assignmentType === assignType).filter(_.referenceType === refType)

  def byAdmin[T <: FoxModel[T]](assignType: AssignmentType, refType: ReferenceType, admin: StoreAdmin): QuerySeq =
    byType(assignType, refType).filter(_.storeAdminId === admin.id)

  def byEntity[T <: FoxModel[T]](assignType: AssignmentType, model: T,
    refType: ReferenceType): QuerySeq = byType(assignType, refType).filter(_.referenceId === model.id)

  def byEntityAndAdmin[T <: FoxModel[T]]
    (assignType: AssignmentType, model: T, refType: ReferenceType, admin: StoreAdmin): QuerySeq =
    byEntity(assignType, model, refType).filter(_.storeAdminId === admin.id)

  def byEntitySeqAndAdmin[T <: FoxModel[T]](assignType: AssignmentType, models: Seq[T],
    refType: ReferenceType, admin: StoreAdmin): QuerySeq =
    byType(assignType, refType).filter(_.referenceId.inSetBind(models.map(_.id))).filter(_.storeAdminId === admin.id)

  def assigneesFor[T <: FoxModel[T]](assignType: AssignmentType, entity: T, refType: ReferenceType): StoreAdmins.QuerySeq = for {
      assignees ← byEntity(assignType, entity, refType).map(_.storeAdminId)
      admins    ← StoreAdmins.filter(_.id === assignees)
    } yield admins
}
