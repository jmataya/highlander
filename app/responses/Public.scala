package responses

import scala.collection.immutable.Seq
import models.location.{Region, Country}
import org.json4s.JsonAST.{JField, JObject}
import org.json4s.{Formats, CustomSerializer, Extraction, JValue}
import utils.JsonFormatters

case class CountryWithRegions(country: Country, regions: Seq[Region])

object CountryWithRegions {
  implicit val formats: Formats = JsonFormatters.DefaultFormats

  val jsonFormat = new CustomSerializer[CountryWithRegions](
      format ⇒
        ({
      case obj: JObject ⇒
        CountryWithRegions(obj.extract[Country], (obj \ "regions").extract[Seq[Region]])
    }, {
      case CountryWithRegions(c, regions) ⇒
        import org.json4s.JsonDSL._
        Extraction.decompose(c).merge(JField("regions", Extraction.decompose(regions)): JValue)
    }))
}
