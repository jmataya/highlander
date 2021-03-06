package phoenix.services.notes

import java.time.Instant

import core.db._
import core.failures.NotFoundFailure404
import phoenix.models.Notes.scope._
import phoenix.models.account._
import phoenix.models.{Note, Notes}
import phoenix.payloads.NotePayloads._
import phoenix.responses.AdminNoteResponse
import phoenix.services._
import phoenix.utils.FoxConfig.config
import phoenix.utils.aliases._
import slick.jdbc.PostgresProfile.api._

trait NoteManager[K, T <: Identity[T]] {

  // TODO: FIXME
  // Notes are context indepedent, but for Activities for now we can store only one entity
  // We have decided to store entity with default context for now
  val defaultContextId: Int = config.app.defaultContextId

  // Define this methods in inherit object
  def noteType(): Note.ReferenceType
  def fetchEntity(key: K)(implicit ec: EC, db: DB, ac: AC): DbResultT[T]
  def getEntityId(e: T): Int = e.id

  // Use this methods wherever you want
  def list(key: K)(implicit ec: EC, db: DB, ac: AC): DbResultT[Seq[AdminNoteResponse]] =
    for {
      entity   ← * <~ fetchEntity(key)
      response ← * <~ forModel(entityQuerySeq(getEntityId(entity)))
    } yield response

  def create(key: K,
             author: User,
             payload: CreateNote)(implicit ec: EC, db: DB, ac: AC, au: AU): DbResultT[AdminNoteResponse] =
    for {
      entity ← * <~ fetchEntity(key)
      note   ← * <~ createInner(entity, noteType(), author, payload)
    } yield AdminNoteResponse.build(note, author)

  def update(key: K, noteId: Int, author: User, payload: UpdateNote)(implicit ec: EC,
                                                                     db: DB,
                                                                     ac: AC): DbResultT[AdminNoteResponse] =
    for {
      entity ← * <~ fetchEntity(key)
      note   ← * <~ updateInner(entity, noteId, author, payload)
    } yield note

  def delete(key: K, noteId: Int, author: User)(implicit ec: EC, db: DB, ac: AC): DbResultT[Unit] =
    for {
      entity ← * <~ fetchEntity(key)
      _      ← * <~ deleteInner(entity, noteId, author)
    } yield ()

  // Inner methods
  private def entityQuerySeq(entityId: Int)(implicit ec: EC, db: DB, ac: AC): Notes.QuerySeq =
    Notes.filter(_.referenceType === noteType()).filter(_.referenceId === entityId).notDeleted

  private def createInner(entity: T,
                          refType: Note.ReferenceType,
                          author: User,
                          payload: CreateNote)(implicit ec: EC, db: DB, ac: AC, au: AU): DbResultT[Note] =
    for {
      note ← * <~ Notes.create(
              Note(storeAdminId = author.accountId,
                   referenceId = getEntityId(entity),
                   referenceType = refType,
                   body = payload.body,
                   scope = Scope.current))
      _ ← * <~ LogActivity().noteCreated(author, entity, note)
    } yield note

  private def updateInner(entity: T, noteId: Int, author: User, payload: UpdateNote)(
      implicit ec: EC,
      db: DB,
      ac: AC): DbResultT[AdminNoteResponse] =
    for {
      oldNote ← * <~ Notes
                 .filterByIdAndAdminId(noteId, author.accountId)
                 .mustFindOneOr(NotFoundFailure404(Note, noteId))
      newNote ← * <~ Notes.update(oldNote, oldNote.copy(body = payload.body))
      _       ← * <~ LogActivity().noteUpdated(author, entity, oldNote, newNote)
    } yield AdminNoteResponse.build(newNote, author)

  private def deleteInner(entity: T, noteId: Int, admin: User)(implicit ec: EC,
                                                               db: DB,
                                                               ac: AC): DbResultT[Unit] =
    for {
      note ← * <~ Notes.mustFindById404(noteId)
      _    ← * <~ Notes.update(note, note.copy(deletedAt = Some(Instant.now), deletedBy = Some(admin.accountId)))
      _    ← * <~ LogActivity().noteDeleted(admin, entity, note)
    } yield ()

  private def forModel[M <: FoxModel[M]](
      finder: Notes.QuerySeq)(implicit ec: EC, db: DB, ac: AC): DbResultT[Seq[AdminNoteResponse]] = {
    val query = for {
      notes   ← finder
      authors ← notes.author
    } yield (notes, authors)
    DbResultT.fromF(query.result.map(_.map {
      case (note, author) ⇒ AdminNoteResponse.build(note, author)
    }))
  }
}
