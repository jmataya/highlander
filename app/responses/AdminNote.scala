package responses

import models._

import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import scala.concurrent.{ExecutionContext, Future}

object AdminNote {

  type NoteResponse = Future[Option[Seq[Root]]]

  case class Root(id: Int, noteText: String, author: NoteAuthor)
  case class NoteAuthor(firstName: String, lastName: String, email: String)

  def build(note: OrderNote, author: StoreAdmin): Root = {
    Root(id = note.id, noteText = note.noteText, author = NoteAuthor(firstName = author.firstName, lastName = author.lastName, email = author.email))
  }

  def findNotesByOrderId(id: Int)
                        (implicit ec: ExecutionContext, db:Database): NoteResponse = {

    val query = for {
      orderNotes ← OrderNotes.filter(_.orderId === id)
      noteAuthors ← orderNotes.author
    } yield (orderNotes, noteAuthors)

    db.run(query.result).map { results ⇒
      Some(results.map {
        case (orderNote, noteAuthor) ⇒
          build(orderNote, noteAuthor)
      })
    }

  }
}

