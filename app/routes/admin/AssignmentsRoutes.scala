package routes.admin

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.StoreAdmin
import models.cord.Cord.cordRefNumRegex
import models.inventory.Sku.skuCodeRegex
import models.payment.giftcard.GiftCard.giftCardCodeRegex
import models.returns.Return.returnRefNumRegex
import payloads.AssignmentPayloads._
import services.assignments._
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

object AssignmentsRoutes {

  def routes(implicit ec: EC, db: DB, admin: StoreAdmin) = {

    activityContext(admin) { implicit ac ⇒
      pathPrefix("customers") {
        pathPrefix("assignees") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              goodOrFailures {
                CustomerAssignmentsManager.assignBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              goodOrFailures {
                CustomerAssignmentsManager.unassignBulk(admin, payload)
              }
            }
          }
        } ~
        pathPrefix("watchers") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              goodOrFailures {
                CustomerWatchersManager.assignBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              goodOrFailures {
                CustomerWatchersManager.unassignBulk(admin, payload)
              }
            }
          }
        }
      } ~
      pathPrefix("customers" / IntNumber) { customerId ⇒
        pathPrefix("assignees") {
          (get & pathEnd) {
            goodOrFailures {
              CustomerAssignmentsManager.list(customerId)
            }
          } ~
          (post & pathEnd & entity(as[AssignmentPayload])) { payload ⇒
            goodOrFailures {
              CustomerAssignmentsManager.assign(customerId, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            goodOrFailures {
              CustomerAssignmentsManager.unassign(customerId, assigneeId, admin)
            }
          }
        } ~
        pathPrefix("watchers") {
          (get & pathEnd) {
            goodOrFailures {
              CustomerWatchersManager.list(customerId)
            }
          } ~
          (post & pathEnd & entity(as[AssignmentPayload])) { payload ⇒
            goodOrFailures {
              CustomerWatchersManager.assign(customerId, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            goodOrFailures {
              CustomerWatchersManager.unassign(customerId, assigneeId, admin)
            }
          }
        }
      } ~
      pathPrefix("gift-cards") {
        pathPrefix("assignees") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              goodOrFailures {
                GiftCardAssignmentsManager.assignBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              goodOrFailures {
                GiftCardAssignmentsManager.unassignBulk(admin, payload)
              }
            }
          }
        } ~
        pathPrefix("watchers") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              goodOrFailures {
                GiftCardWatchersManager.assignBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              goodOrFailures {
                GiftCardWatchersManager.unassignBulk(admin, payload)
              }
            }
          }
        }
      } ~
      pathPrefix("gift-cards" / giftCardCodeRegex) { code ⇒
        pathPrefix("assignees") {
          (get & pathEnd) {
            goodOrFailures {
              GiftCardAssignmentsManager.list(code)
            }
          } ~
          (post & pathEnd & entity(as[AssignmentPayload])) { payload ⇒
            goodOrFailures {
              GiftCardAssignmentsManager.assign(code, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            goodOrFailures {
              GiftCardAssignmentsManager.unassign(code, assigneeId, admin)
            }
          }
        } ~
        pathPrefix("watchers") {
          (get & pathEnd) {
            goodOrFailures {
              GiftCardWatchersManager.list(code)
            }
          } ~
          (post & pathEnd & entity(as[AssignmentPayload])) { payload ⇒
            goodOrFailures {
              GiftCardWatchersManager.assign(code, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            goodOrFailures {
              GiftCardWatchersManager.unassign(code, assigneeId, admin)
            }
          }
        }
      } ~
      pathPrefix("orders") {
        pathPrefix("assignees") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              goodOrFailures {
                OrderAssignmentsManager.assignBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              goodOrFailures {
                OrderAssignmentsManager.unassignBulk(admin, payload)
              }
            }
          }
        } ~
        pathPrefix("watchers") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              goodOrFailures {
                OrderWatchersManager.assignBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              goodOrFailures {
                OrderWatchersManager.unassignBulk(admin, payload)
              }
            }
          }
        }
      } ~
      pathPrefix("orders" / cordRefNumRegex) { refNum ⇒
        pathPrefix("assignees") {
          (get & pathEnd) {
            goodOrFailures {
              OrderAssignmentsManager.list(refNum)
            }
          } ~
          (post & pathEnd & entity(as[AssignmentPayload])) { payload ⇒
            goodOrFailures {
              OrderAssignmentsManager.assign(refNum, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            goodOrFailures {
              OrderAssignmentsManager.unassign(refNum, assigneeId, admin)
            }
          }
        } ~
        pathPrefix("watchers") {
          (get & pathEnd) {
            goodOrFailures {
              OrderWatchersManager.list(refNum)
            }
          } ~
          (post & pathEnd & entity(as[AssignmentPayload])) { payload ⇒
            goodOrFailures {
              OrderWatchersManager.assign(refNum, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            goodOrFailures {
              OrderWatchersManager.unassign(refNum, assigneeId, admin)
            }
          }
        }
      } ~
      pathPrefix("returns") {
        pathPrefix("assignees") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              goodOrFailures {
                ReturnAssignmentsManager.assignBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              goodOrFailures {
                ReturnAssignmentsManager.unassignBulk(admin, payload)
              }
            }
          }
        } ~
        pathPrefix("watchers") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              goodOrFailures {
                ReturnWatchersManager.assignBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              goodOrFailures {
                ReturnWatchersManager.unassignBulk(admin, payload)
              }
            }
          }
        }
      } ~
      pathPrefix("returns" / returnRefNumRegex) { refNum ⇒
        pathPrefix("assignees") {
          (get & pathEnd) {
            goodOrFailures {
              ReturnAssignmentsManager.list(refNum)
            }
          } ~
          (post & entity(as[AssignmentPayload])) { payload ⇒
            goodOrFailures {
              ReturnAssignmentsManager.assign(refNum, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            goodOrFailures {
              ReturnAssignmentsManager.unassign(refNum, assigneeId, admin)
            }
          }
        } ~
        pathPrefix("watchers") {
          (get & pathEnd) {
            goodOrFailures {
              ReturnWatchersManager.list(refNum)
            }
          } ~
          (post & entity(as[AssignmentPayload])) { payload ⇒
            goodOrFailures {
              ReturnWatchersManager.assign(refNum, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            goodOrFailures {
              ReturnWatchersManager.unassign(refNum, assigneeId, admin)
            }
          }
        }
      } ~
      pathPrefix("products") {
        pathPrefix("assignees") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              goodOrFailures {
                ProductAssignmentsManager.assignBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              goodOrFailures {
                ProductAssignmentsManager.unassignBulk(admin, payload)
              }
            }
          }
        } ~
        pathPrefix("watchers") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              goodOrFailures {
                ProductWatchersManager.assignBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              goodOrFailures {
                ProductWatchersManager.unassignBulk(admin, payload)
              }
            }
          }
        }
      } ~
      pathPrefix("products" / IntNumber) { productId ⇒
        pathPrefix("assignees") {
          (get & pathEnd) {
            goodOrFailures {
              ProductAssignmentsManager.list(productId)
            }
          } ~
          (post & pathEnd & entity(as[AssignmentPayload])) { payload ⇒
            goodOrFailures {
              ProductAssignmentsManager.assign(productId, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            goodOrFailures {
              ProductAssignmentsManager.unassign(productId, assigneeId, admin)
            }
          }
        } ~
        pathPrefix("watchers") {
          (get & pathEnd) {
            goodOrFailures {
              ProductWatchersManager.list(productId)
            }
          } ~
          (post & pathEnd & entity(as[AssignmentPayload])) { payload ⇒
            goodOrFailures {
              ProductWatchersManager.assign(productId, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            goodOrFailures {
              ProductWatchersManager.unassign(productId, assigneeId, admin)
            }
          }
        }
      } ~
      pathPrefix("skus") {
        pathPrefix("assignees") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              goodOrFailures {
                SkuAssignmentsManager.assignBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              goodOrFailures {
                SkuAssignmentsManager.unassignBulk(admin, payload)
              }
            }
          }
        } ~
        pathPrefix("watchers") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              goodOrFailures {
                SkuWatchersManager.assignBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              goodOrFailures {
                SkuWatchersManager.unassignBulk(admin, payload)
              }
            }
          }
        }
      } ~
      pathPrefix("skus" / skuCodeRegex) { refNum ⇒
        pathPrefix("assignees") {
          (get & pathEnd) {
            goodOrFailures {
              SkuAssignmentsManager.list(refNum)
            }
          } ~
          (post & entity(as[AssignmentPayload])) { payload ⇒
            goodOrFailures {
              SkuAssignmentsManager.assign(refNum, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            goodOrFailures {
              SkuAssignmentsManager.unassign(refNum, assigneeId, admin)
            }
          }
        } ~
        pathPrefix("watchers") {
          (get & pathEnd) {
            goodOrFailures {
              SkuWatchersManager.list(refNum)
            }
          } ~
          (post & entity(as[AssignmentPayload])) { payload ⇒
            goodOrFailures {
              SkuWatchersManager.assign(refNum, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            goodOrFailures {
              SkuWatchersManager.unassign(refNum, assigneeId, admin)
            }
          }
        }
      } ~
      pathPrefix("promotions") {
        pathPrefix("assignees") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              goodOrFailures {
                PromotionAssignmentsManager.assignBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              goodOrFailures {
                PromotionAssignmentsManager.unassignBulk(admin, payload)
              }
            }
          }
        } ~
        pathPrefix("watchers") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              goodOrFailures {
                PromotionWatchersManager.assignBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              goodOrFailures {
                PromotionWatchersManager.unassignBulk(admin, payload)
              }
            }
          }
        }
      } ~
      pathPrefix("promotions" / IntNumber) { promotionId ⇒
        pathPrefix("assignees") {
          (get & pathEnd) {
            goodOrFailures {
              PromotionAssignmentsManager.list(promotionId)
            }
          } ~
          (post & pathEnd & entity(as[AssignmentPayload])) { payload ⇒
            goodOrFailures {
              PromotionAssignmentsManager.assign(promotionId, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            goodOrFailures {
              PromotionAssignmentsManager.unassign(promotionId, assigneeId, admin)
            }
          }
        } ~
        pathPrefix("watchers") {
          (get & pathEnd) {
            goodOrFailures {
              PromotionWatchersManager.list(promotionId)
            }
          } ~
          (post & pathEnd & entity(as[AssignmentPayload])) { payload ⇒
            goodOrFailures {
              PromotionWatchersManager.assign(promotionId, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            goodOrFailures {
              PromotionWatchersManager.unassign(promotionId, assigneeId, admin)
            }
          }
        }
      } ~
      pathPrefix("coupons") {
        pathPrefix("assignees") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              goodOrFailures {
                CouponAssignmentsManager.assignBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              goodOrFailures {
                CouponAssignmentsManager.unassignBulk(admin, payload)
              }
            }
          }
        } ~
        pathPrefix("watchers") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              goodOrFailures {
                CouponWatchersManager.assignBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              goodOrFailures {
                CouponWatchersManager.unassignBulk(admin, payload)
              }
            }
          }
        }
      } ~
      pathPrefix("coupons" / IntNumber) { couponId ⇒
        pathPrefix("assignees") {
          (get & pathEnd) {
            goodOrFailures {
              CouponAssignmentsManager.list(couponId)
            }
          } ~
          (post & pathEnd & entity(as[AssignmentPayload])) { payload ⇒
            goodOrFailures {
              CouponAssignmentsManager.assign(couponId, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            goodOrFailures {
              CouponAssignmentsManager.unassign(couponId, assigneeId, admin)
            }
          }
        } ~
        pathPrefix("watchers") {
          (get & pathEnd) {
            goodOrFailures {
              CouponWatchersManager.list(couponId)
            }
          } ~
          (post & pathEnd & entity(as[AssignmentPayload])) { payload ⇒
            goodOrFailures {
              CouponWatchersManager.assign(couponId, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            goodOrFailures {
              CouponWatchersManager.unassign(couponId, assigneeId, admin)
            }
          }
        }
      }
    }
  }
}
