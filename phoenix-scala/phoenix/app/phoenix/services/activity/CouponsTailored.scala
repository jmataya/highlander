package phoenix.services.activity

import phoenix.models.coupon.Coupon
import phoenix.responses.CouponResponses.CouponResponse
import phoenix.responses.UserResponse

object CouponsTailored {
  case class CouponCreated(coupon: CouponResponse.Root, admin: Option[UserResponse.Root])
      extends ActivityBase[CouponCreated]

  case class CouponUpdated(coupon: CouponResponse.Root, admin: Option[UserResponse.Root])
      extends ActivityBase[CouponUpdated]

  case class SingleCouponCodeGenerated(coupon: Coupon, admin: Option[UserResponse.Root])
      extends ActivityBase[SingleCouponCodeGenerated]

  case class MultipleCouponCodesGenerated(coupon: Coupon, admin: Option[UserResponse.Root])
      extends ActivityBase[MultipleCouponCodesGenerated]
}