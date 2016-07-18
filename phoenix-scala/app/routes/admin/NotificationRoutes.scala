package routes.admin

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import de.heikoseeberger.akkasse.EventStreamMarshalling._
import facades.NotificationFacade
import models.StoreAdmin
import payloads.CreateNotification
import services.NotificationManager
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

object NotificationRoutes {

  def routes(implicit ec: EC, db: DB, mat: Mat, admin: StoreAdmin) = {
    activityContext(admin) { implicit ac ⇒
      pathPrefix("notifications") {
        (get & pathEnd) {
          complete {
            NotificationFacade.streamByAdminId(admin.id)
          }
        } ~
        (post & pathEnd & entity(as[CreateNotification])) { payload ⇒
          mutateOrFailures {
            NotificationManager.createNotification(payload)
          }
        } ~
        (post & path("last-seen" / IntNumber) & pathEnd) { activityId ⇒
          mutateOrFailures {
            NotificationManager.updateLastSeen(admin.id, activityId)
          }
        }
      }
    }
  }
}
