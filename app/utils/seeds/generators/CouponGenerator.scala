package utils.seeds.generators

import java.time.Instant

import scala.concurrent.ExecutionContext.Implicits.global

import models.objects._
import models.product.SimpleContext
import org.json4s._
import org.json4s.jackson.JsonMethods._
import payloads.CouponPayloads._
import services.coupon.CouponManager
import utils.aliases._
import utils.db._
import utils.seeds.generators.SimpleCoupon._

object SimpleCoupon {
  type Percent = Int
}

case class SimpleCoupon(formId: Int = 0,
                        shadowId: Int = 0,
                        percentOff: Percent,
                        totalAmount: Int,
                        promotionId: Int)

case class SimpleCouponForm(percentOff: Percent, totalAmount: Int) {

  val (keyMap, form) = ObjectUtils.createForm(parse(s"""
    {
      "name" : "$percentOff% off over $totalAmount",
      "storefrontName" : "Get $percentOff% off over $totalAmount dollars",
      "description" : "Get $percentOff% full order after spending more than $totalAmount dollars",
      "details" : "This offer applies only when you have a total amount $totalAmount dollars",
      "activeFrom" : "${Instant.now}",
      "activeTo" : null,
      "tags" : []
      }
    }"""))
}

case class SimpleCouponShadow(f: SimpleCouponForm) {

  val shadow = ObjectUtils.newShadow(
      parse("""
        {
          "name" : {"type": "string", "ref": "name"},
          "storefrontName" : {"type": "richText", "ref": "storefrontName"},
          "description" : {"type": "text", "ref": "description"},
          "details" : {"type": "richText", "ref": "details"},
          "activeFrom" : {"type": "date", "ref": "activeFrom"},
          "activeTo" : {"type": "date", "ref": "activeTo"},
          "tags" : {"type": "tags", "ref": "tags"}
        }"""),
      f.keyMap)
}

trait CouponGenerator {

  def generateCoupon(promotion: SimplePromotion): SimpleCoupon = {
    SimpleCoupon(percentOff = promotion.percentOff,
                 totalAmount = promotion.totalAmount,
                 promotionId = promotion.promotionId)
  }

  def generateCoupons(sourceData: Seq[SimpleCoupon])(implicit db: DB,
                                                     ac: AC): DbResultT[Seq[SimpleCoupon]] =
    for {
      context ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      coupons ← * <~ sourceData.map(source ⇒ {
                 val couponForm   = SimpleCouponForm(source.percentOff, source.totalAmount)
                 val couponShadow = SimpleCouponShadow(couponForm)
                 val payload = CreateCoupon(form = CreateCouponForm(attributes = couponForm.form),
                                            shadow =
                                              CreateCouponShadow(attributes = couponShadow.shadow),
                                            source.promotionId)
                 CouponManager.create(payload, context.name, None).map { newCoupon ⇒
                   source.copy(formId = newCoupon.form.id, shadowId = newCoupon.shadow.id)
                 }
               })
    } yield coupons
}
