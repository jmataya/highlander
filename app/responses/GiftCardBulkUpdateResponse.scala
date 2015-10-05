package responses

import cats.data.Xor
import services.Failures
import scala.collection.immutable.Seq

object GiftCardBulkUpdateResponse {
  final case class ItemResult(
    code: String,
    success: Boolean = false,
    giftCard: Option[GiftCardResponse.Root] = None,
    errors: Option[Failures] = None)

  final case class BulkResponse(itemResults: Seq[ItemResult])

  def buildItemResult(code: String, result: Failures Xor GiftCardResponse.Root): ItemResult = {
    result.fold(errors ⇒ ItemResult(code = code, errors = Some(errors)),
      gc ⇒ ItemResult(code = code, success = true, giftCard = Some(gc)))
  }

  def buildBulkResponse(itemResults: Seq[ItemResult]): BulkResponse = BulkResponse(itemResults = itemResults)
}