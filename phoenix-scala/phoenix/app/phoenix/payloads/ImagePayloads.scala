package phoenix.payloads

import akka.http.scaladsl.model.Uri

import cats.data.{Validated, ValidatedNel}
import cats.implicits._
import core.failures.Failure
import core.utils.Validation
import core.utils.Validation._
import objectframework.ObjectUtils._
import objectframework.models._
import objectframework.payloads.ObjectPayloads._
import phoenix.facades.ImageFacade.validateImageUrl
import phoenix.models.image._

object ImagePayloads {

  type Images = Option[Seq[ImagePayload]]

  case class ImagePayload(id: Option[Int] = None,
                          src: String,
                          baseUrl: Option[String] = None,
                          title: Option[String] = None,
                          alt: Option[String] = None,
                          scope: Option[String] = None)
      extends Validation[ImagePayload] {

    def formAndShadow: FormAndShadow = {
      val jsonBuilder: AttributesBuilder = optionalAttributes(Some(StringField("src", src)),
                                                              title.map(StringField("title", _)),
                                                              alt.map(StringField("alt", _)),
                                                              baseUrl.map(StringField("baseUrl", _)))

      (ObjectForm(kind = Image.kind, attributes = jsonBuilder.objectForm),
       ObjectShadow(attributes = jsonBuilder.objectShadow))
    }

    private def validateImageSrc(): ValidatedNel[Failure, Uri] =
      validateImageUrl(src).leftMap(_.head).toValidatedNel

    override def validate: ValidatedNel[Failure, ImagePayload] =
      validateImageSrc().map(_ ⇒ this)
  }

  case class AlbumPayload(scope: Option[String] = None,
                          id: Option[Int] = None,
                          name: Option[String] = None,
                          images: Images = None,
                          position: Option[Int] = None)
      extends Validation[AlbumPayload] {

    def formAndShadow: FormAndShadow = {
      val jsonBuilder: AttributesBuilder = optionalAttributes(name.map(StringField("name", _)))

      (ObjectForm(kind = Album.kind, attributes = jsonBuilder.objectForm),
       ObjectShadow(attributes = jsonBuilder.objectShadow))
    }

    override def validate: ValidatedNel[Failure, AlbumPayload] =
      validateIdsUnique(images).map(_ ⇒ this)
  }

  def validateIdsUnique(images: Images): ValidatedNel[Failure, Images] = images match {
    case Some(imgList) ⇒
      val duplicated = imgList.groupBy(_.id.getOrElse(0)).collect {
        case (id, items) if id != 0 && items.size > 1 ⇒ id
      }
      validExpr(duplicated.isEmpty, s"Image ID is duplicated ${duplicated.head}").map(_ ⇒ images)
    case None ⇒
      Validated.valid(images)

  }

  case class UpdateAlbumPositionPayload(albumId: Int, position: Int)
}
