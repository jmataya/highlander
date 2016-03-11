package services.assignments

import models.Assignment._
import models.{StoreAdmins, StoreAdmin, Assignment, Assignments, NotificationSubscription}
import payloads.{AssignmentPayload, BulkAssignmentPayload}
import responses.{StoreAdminResponse, ResponseItem, BatchMetadataSource, BatchMetadata, TheResponse}
import responses.StoreAdminResponse.{build ⇒ buildAdmin}
import services.Util._
import services._
import slick.driver.PostgresDriver.api._
import utils.DbResultT._
import utils.{TableQueryWithId, ModelWithIdParameter}
import utils.DbResultT.implicits._
import utils.Slick._
import utils.Slick.implicits._
import utils.aliases._

trait AssignmentsManager[K, M <: ModelWithIdParameter[M]] {
  // Define this methods in inherit object
  //def modelInstance(): ModelWithIdParameter[M]
  //def tableInstance(): TableQueryWithId[M, T]
  //def responseBuilder(): M ⇒ ResponseItem

  def assignmentType(): AssignmentType
  def referenceType(): ReferenceType
  def notifyDimension(): String

  private def notifyReason(): NotificationSubscription.Reason = assignmentType() match {
    case Assignee ⇒ NotificationSubscription.Assigned
    case Watcher  ⇒ NotificationSubscription.Watching
  }

  def fetchEntity(key: K)(implicit ec: EC, db: DB, ac: AC): DbResult[M]
  //def fetchMulti(keys: Seq[K])(implicit ec: EC, db: DB, ac: AC): DbResult[Seq[M]]

  // Use this methods wherever you want
  def assign(key: K, payload: AssignmentPayload, originator: StoreAdmin)
    (implicit ec: EC, db: DB, ac: AC): Result[Unit] = (for {
    // Validation + assign
    entity         ← * <~ fetchEntity(key)
    admins         ← * <~ StoreAdmins.filter(_.id.inSetBind(payload.assignees)).result
    adminIds       = admins.map(_.id)
    assignees      ← * <~ Assignments.assigneesFor(assignmentType(), entity, referenceType()).result.toXor
    newAssigneeIds = adminIds.diff(assignees.map(_.id))
    _              ← * <~ Assignments.createAll(build(entity, newAssigneeIds))
    assignedAdmins = admins.filter(a ⇒ newAssigneeIds.contains(a.id)).map(buildAdmin)
    // TODO Prepare response
    // ...
    notFoundAdmins  = diffToFailures(payload.assignees, adminIds, StoreAdmin)
    // Activity log + notifications subscription
    _         ← * <~ LogActivity.assigned(originator, entity, assignedAdmins)
    _         ← * <~ NotificationManager.subscribe(adminIds = assignedAdmins.map(_.id), dimension = notifyDimension(),
      reason = notifyReason(), objectIds = Seq(key.toString))
  } yield ()).runTxn()

  def unassign(key: K, assigneeId: Int, originator: StoreAdmin)
    (implicit ec: EC, db: DB, ac: AC): Result[Unit] = (for {
    // Validation + unassign
    entity     ← * <~ fetchEntity(key)
    admin      ← * <~ StoreAdmins.mustFindById404(assigneeId)
    querySeq   = Assignments.byEntityAndAdmin(assignmentType(), entity, referenceType(), admin)
    assignment ← * <~ querySeq.one.mustFindOr(AssigneeNotFound(entity, key, assigneeId))
    _          ← * <~ querySeq.delete
    // TODO - Prepare response
    // ...
    // Activity log + notifications subscription
    _         ← * <~ LogActivity.unassigned(originator, entity, admin)
    _         ← * <~ NotificationManager.unsubscribe(adminIds = Seq(assigneeId), dimension = notifyDimension(),
      reason = notifyReason(), objectIds = Seq(key.toString))
  } yield ()).runTxn()

  /*
  def assignBulk(admin: StoreAdmin, payload: BulkAssignmentPayload)
    (implicit ec: EC, db: DB, ac: AC, sortAndPage: SortAndPage): Result[Unit] = (for {

    admin      ← * <~ StoreAdmins.mustFindById404(payload.assigneeId)
    entities   ← * <~ fetchMulti(payload.entityIds)
    newEntries = buildNewMulti(entities, payload.assigneeId)
    _          ← * <~ Assignments.createAll(newEntries)

    // TODO - .findAll generalized
    success   = filterSuccess(entities, newEntries)

    // TODO - LogActivity + notifications generalization
    // ...

    // TODO - Append proper class names
    batchFailures  = diffToBatchErrors(payload.entityIds, entities.map(_.id), modelInstance())
    batchMetadata  = BatchMetadata(BatchMetadataSource(modelInstance(), success.map(_.toString), batchFailures))
  } yield ()).runTxn()

  def unassignBulk(admin: StoreAdmin, payload: BulkAssignmentPayload)
    (implicit ec: EC, db: DB, ac: AC, sortAndPage: SortAndPage): Result[Unit] = (for {

    admin    ← * <~ StoreAdmins.mustFindById404(payload.assigneeId)
    entities ← * <~ fetchMulti(payload.entityIds)
    _        ← * <~ Assignments.filter(_.storeAdminId === payload.assigneeId)
      .filter(_.referenceType === referenceType()).filter(_.referenceId.inSetBind(entities.map(_.id))).delete

    // TODO - .findAll generalized
    success   = entities.filter(c ⇒ payload.entityIds.contains(c.id)).map(_.id)

    // TODO - LogActivity + notifications generalization
    // ...

    // Prepare batch response + proper class names
    batchFailures  = diffToBatchErrors(payload.entityIds, entities.map(_.id), modelInstance())
    batchMetadata  = BatchMetadata(BatchMetadataSource(modelInstance(), success.map(_.toString), batchFailures))
  } yield ()).runTxn()
  */

  // Helpers
  private def build(entity: M, newAssigneeIds: Seq[Int]): Seq[Assignment] =
    newAssigneeIds.map(adminId ⇒ Assignment(assignmentType = assignmentType(),
      storeAdminId = adminId, referenceType = referenceType(), referenceId = entity.id))

  private def buildMulti(entities: Seq[M], storeAdminId: Int): Seq[Assignment] =
    for (e ← entities) yield Assignment(assignmentType = assignmentType(), storeAdminId = storeAdminId,
      referenceType = referenceType(), referenceId = e.id)

  private def filterSuccess(entities: Seq[M], newEntries: Seq[Assignment]): Seq[M#Id] =
    entities.filter(e ⇒ newEntries.map(_.referenceId).contains(e.id)).map(_.id)
}