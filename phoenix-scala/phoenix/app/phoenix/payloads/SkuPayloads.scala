package phoenix.payloads

import phoenix.payloads.ImagePayloads.AlbumPayload
import phoenix.utils.aliases._

object SkuPayloads {
  case class SkuPayload(scope: Option[String] = None,
                        attributes: Map[String, Json],
                        schema: Option[String] = None,
                        albums: Option[Seq[AlbumPayload]] = None)
}
