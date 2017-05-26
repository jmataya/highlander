package phoenix.routes.admin

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.Unmarshaller

import phoenix.facades.ImageFacade
import phoenix.facades.ImageHelpers
import phoenix.failures.ImageFailures.ImageNotFoundInPayload
import phoenix.models.account.User
import phoenix.payloads.ImagePayloads._
import phoenix.services.image.ImageManager
import phoenix.services.Authenticator.AuthData
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis
import phoenix.utils.http.CustomDirectives._
import phoenix.utils.http.Http._
import phoenix.utils.http.JsonSupport._
import akka.http.scaladsl.unmarshalling.PredefinedFromEntityUnmarshallers._

object ImageRoutes {
  def routes(implicit ec: EC, db: DB, auth: AuthData[User], apis: Apis, sys: ActorSystem): Route = {
    activityContext(auth) { implicit ac ⇒
      pathPrefix("images" / Segment) { context ⇒
        extractRequestContext { ctx ⇒
          implicit val materializer = ctx.materializer
          implicit val ec           = ctx.executionContext

          (post & pathEnd & entityOr(as[Multipart.FormData], ImageNotFoundInPayload)) { formData ⇒
            mutateOrFailures {
              ImageFacade.uploadImagesFromMultiPart(context, formData)
            }
          }
        }
      } ~
      pathPrefix("albums") {
        pathPrefix(Segment) { context ⇒
          (post & pathEnd & entity(as[AlbumPayload])) { payload ⇒
            mutateOrFailures {
              ImageManager.createAlbum(payload, context)
            }
          } ~
          pathPrefix(IntNumber) { albumId ⇒
            (get & pathEnd) {
              getOrFailures {
                ImageManager.getAlbum(albumId, context)
              }
            } ~
            (patch & pathEnd & entity(as[AlbumPayload])) { payload ⇒
              mutateOrFailures {
                ImageManager.updateAlbum(albumId, payload, context)
              }
            } ~
            (delete & pathEnd) {
              mutateOrFailures {
                ImageManager.archiveByContextAndId(albumId, context)
              }
            } ~
            pathPrefix("images") {
              extractRequestContext { ctx ⇒
                implicit val materializer = ctx.materializer
                implicit val ec           = ctx.executionContext

                (post & pathEnd & entityOr(as[Multipart.FormData], ImageNotFoundInPayload)) {
                  formData ⇒
                    mutateOrFailures {
                      ImageFacade.uploadImagesFromMultipartToAlbum(albumId, context, formData)
                    }
                } ~
                (path("by-url") & post & entity(as[ImagePayload])) { payload ⇒
                  mutateOrFailures {
                    ImageFacade.uploadImagesFromPayloadToAlbum(albumId, context, payload)
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
