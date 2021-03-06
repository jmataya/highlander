package phoenix.responses

import objectframework.IlluminateAlgorithm
import objectframework.models._
import phoenix.models.product._
import phoenix.utils.JsonFormatters

object VariantValueResponses {
  implicit val formats = JsonFormatters.phoenixFormats

  case class IlluminatedVariantValueResponse(id: Int,
                                             name: String,
                                             swatch: Option[String] = None,
                                             image: Option[String],
                                             skuCodes: Seq[String])

  object IlluminatedVariantValueResponse {

    def build(value: FullObject[VariantValue], skuCodes: Seq[String]): IlluminatedVariantValueResponse = {
      val model       = value.model
      val formAttrs   = value.form.attributes
      val shadowAttrs = value.shadow.attributes

      val name   = IlluminateAlgorithm.get("name", formAttrs, shadowAttrs).extract[String]
      val swatch = IlluminateAlgorithm.get("swatch", formAttrs, shadowAttrs).extractOpt[String]
      val image  = IlluminateAlgorithm.get("image", formAttrs, shadowAttrs).extractOpt[String]

      IlluminatedVariantValueResponse(id = model.formId,
                                      name = name,
                                      swatch = swatch,
                                      image = image,
                                      skuCodes)
    }
  }
}
