package testutils.fixtures.api

import io.circe.Json
import java.time.Instant
import models.promotion.Promotion.ApplyType
import payloads.DiscountPayloads.CreateDiscount
import payloads.PromotionPayloads.CreatePromotion
import testutils.PayloadHelpers._
import utils.aliases.Json

object PromotionPayloadBuilder {

  def build(applyType: ApplyType,
            offer: PromoOfferBuilder,
            qualifier: PromoQualifierBuilder,
            tags: PromoTagsBuilder = PromoTagsBuilder.Empty,
            title: String = faker.Lorem.sentence(),
            extraAttrs: Map[String, Json] = Map.empty,
            description: String = faker.Lorem.sentence()): CreatePromotion = {

    val discountAttrs = Map[String, Json](
        "title"       → tv(title),
        "description" → tv(description),
        "tags"        → tags.payloadJson,
        "qualifier"   → qualifier.payloadJson,
        "offer"       → offer.payloadJson
    )

    CreatePromotion(applyType = applyType,
                    attributes = Map(
                          "name"       → tv(faker.Lorem.sentence(1)),
                          "activeFrom" → tv(Instant.now, "datetime"),
                          "activeTo"   → tv(Json.Null, "datetime")
                      ) ++ extraAttrs,
                    discounts = Seq(CreateDiscount(discountAttrs)))
  }

  sealed trait PromoQualifierBuilder extends Jsonable

  object PromoQualifierBuilder {

    case object CartAny extends PromoQualifierBuilder {
      def payloadJson: Json = tv(parseJson("""{ "orderAny": {} }"""), "qualifier")
    }

    case class CartTotalAmount(qualifiedSubtotal: Int) extends PromoQualifierBuilder {
      def payloadJson: Json =
        tv(parseJson(s"""{ "orderTotalAmount": { "totalAmount" : $qualifiedSubtotal } }"""),
           "qualifier")
    }

    case class CartNumUnits(qualifiedNumUnits: Int) extends PromoQualifierBuilder {
      def payloadJson: Json =
        tv(parseJson(s"""{ "orderNumUnits": { "numUnits": $qualifiedNumUnits } }"""), "qualifier")
    }
  }

  sealed trait PromoOfferBuilder extends Jsonable

  object PromoOfferBuilder {

    case class CartPercentOff(percentOff: Int) extends PromoOfferBuilder {
      def payloadJson: Json =
        tv(parseJson(s"""{ "orderPercentOff" : { "discount" : $percentOff } }"""), "offer")
    }
  }

  sealed trait PromoTagsBuilder extends Jsonable

  object PromoTagsBuilder {

    case object Empty extends PromoTagsBuilder {
      def payloadJson: Json =
        tv(JArray(List.empty[Json]), "tags")
    }
  }

  trait Jsonable {
    def payloadJson: Json
  }
}
