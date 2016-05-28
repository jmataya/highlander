import java.time.Instant

import akka.http.scaladsl.model.StatusCodes

import Extensions._
import models.activity.ActivityContext
import models.payment.giftcard._
import models.{Notes, _}
import responses.AdminNotes
import services.notes.GiftCardNoteManager
import util.IntegrationTestBase
import utils.seeds.Seeds
import Seeds.Factories
import utils.db._
import utils.time.RichInstant
import scala.concurrent.ExecutionContext.Implicits.global

import failures.NotFoundFailure404
import payloads.NotePayloads._

class GiftCardNotesIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with AutomaticAuth {

  implicit val ac = ActivityContext(
      userId = 1, userType = "b", transactionId = "c")

  "POST /v1/notes/gift-card/:code" - {
    "can be created by an admin for a gift card" in new Fixture {
      val response = POST(s"v1/notes/gift-card/${giftCard.code}",
                          CreateNote(body = "Hello, FoxCommerce!"))

      response.status must ===(StatusCodes.OK)

      val note = response.as[AdminNotes.Root]
      note.body must ===("Hello, FoxCommerce!")
      note.author must ===(AdminNotes.buildAuthor(admin))
    }

    "returns a validation error if failed to create" in new Fixture {
      val response =
        POST(s"v1/notes/gift-card/${giftCard.code}", CreateNote(body = ""))

      response.status must ===(StatusCodes.BadRequest)
      response.error must ===("body must not be empty")
    }

    "returns a 404 if the gift card is not found" in new Fixture {
      val response = POST(s"v1/notes/gift-card/999999", CreateNote(body = ""))

      response.status must ===(StatusCodes.NotFound)
      response.error must ===(NotFoundFailure404(GiftCard, 999999).description)
    }
  }

  "GET /v1/notes/gift-card/:code" - {

    "can be listed" in new Fixture {
      List("abc", "123", "xyz").map { body ⇒
        GiftCardNoteManager
          .create(giftCard.code, admin, CreateNote(body = body))
          .futureValue
      }

      val response = GET(s"v1/notes/gift-card/${giftCard.code}")
      response.status must ===(StatusCodes.OK)

      val notes = response.as[Seq[AdminNotes.Root]]
      notes must have size 3
      notes.map(_.body).toSet must ===(Set("abc", "123", "xyz"))
    }
  }

  "PATCH /v1/notes/gift-card/:code/:noteId" - {

    "can update the body text" in new Fixture {
      val rootNote = rightValue(GiftCardNoteManager
            .create(
                giftCard.code, admin, CreateNote(body = "Hello, FoxCommerce!"))
            .futureValue)

      val response =
        PATCH(s"v1/notes/gift-card/${giftCard.code}/${rootNote.id}",
              UpdateNote(body = "donkey"))
      response.status must ===(StatusCodes.OK)

      val note = response.as[AdminNotes.Root]
      note.body must ===("donkey")
    }
  }

  "DELETE /v1/notes/gift-card/:code/:noteId" - {

    "can soft delete note" in new Fixture {
      val createResp = POST(s"v1/notes/gift-card/${giftCard.code}",
                            CreateNote(body = "Hello, FoxCommerce!"))
      val note = createResp.as[AdminNotes.Root]

      val response = DELETE(s"v1/notes/gift-card/${giftCard.code}/${note.id}")
      response.status must ===(StatusCodes.NoContent)
      response.bodyText mustBe empty

      val updatedNote = Notes.findOneById(note.id).run().futureValue.value
      updatedNote.deletedBy.value === 1

      withClue(updatedNote.deletedAt.value → Instant.now) {
        updatedNote.deletedAt.value.isBeforeNow === true
      }

      // Deleted note should not be returned
      val allNotesResponse = GET(s"v1/notes/gift-card/${giftCard.code}")
      allNotesResponse.status must ===(StatusCodes.OK)
      val allNotes = allNotesResponse.as[Seq[AdminNotes.Root]]
      allNotes.map(_.id) must not contain note.id

      val getDeletedNoteResponse =
        GET(s"v1/notes/gift-card/${giftCard.code}/${note.id}")
      getDeletedNoteResponse.status must ===(StatusCodes.NotFound)
    }
  }

  trait Fixture {
    val (admin, giftCard) = (for {
      admin ← StoreAdmins.create(authedStoreAdmin).map(rightValue)
      reason ← Reasons
                .create(Factories.reason.copy(storeAdminId = admin.id))
                .map(rightValue)
      origin ← GiftCardManuals
                .create(
                    GiftCardManual(adminId = admin.id, reasonId = reason.id))
                .map(rightValue)
      giftCard ← GiftCards
                  .create(Factories.giftCard.copy(originId = origin.id,
                                                  state = GiftCard.Active))
                  .map(rightValue)
    } yield (admin, giftCard)).run().futureValue
  }
}
