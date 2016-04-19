package models.coupon

import java.time.Instant

import scala.util.Random

import monocle.macros.GenLens
import utils.ExPostgresDriver.api._
import utils.time.JavaTimeSlickMapper._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId, Validation}

/**
 * A coupon code is a way to reference a coupon from the outside world.
 * Multiple codes may point to the same coupon.
 */
case class CouponCode(id: Int = 0, code: String, couponFormId: Int, createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[CouponCode]
  with Validation[CouponCode]

class CouponCodes(tag: Tag) extends GenericTable.TableWithId[CouponCode](tag, "coupon_codes")  {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def code = column[String]("code")
  def couponFormId = column[Int]("coupon_form_id")
  def createdAt = column[Instant]("created_at")

  def * = (id, code, couponFormId, createdAt) <> ((CouponCode.apply _).tupled, CouponCode.unapply)

}

object CouponCodes extends TableQueryWithId[CouponCode, CouponCodes](
  idLens = GenLens[CouponCode](_.id))(new CouponCodes(_)) {

  def charactersGivenQuantity(quantity: Int) : Int = Math.ceil(Math.log10(quantity.toDouble)).toInt

  def isCharacterLimitValid(prefixSize: Int, quantity: Int, requestedLimit: Int) : Boolean = {
    val minSuffixSize = charactersGivenQuantity(quantity)
    requestedLimit >= prefixSize + minSuffixSize
  }

  def generateCodes(prefix: String, codeCharacterLength: Int, quantity: Int, attempt: Int = 0) : Seq[String]  = {
    require(quantity > 0)

    val minNumericLength = charactersGivenQuantity(quantity)
    require(codeCharacterLength >= prefix.length + minNumericLength)

    val numericLength = codeCharacterLength - prefix.length
    val largestNum = Math.pow(10, numericLength.toDouble).toInt
    val codes = (1 to quantity).map { i ⇒
      generateCode(prefix, Random.nextInt(largestNum), largestNum, numericLength)
    }.distinct

    //if we produced fewer codes then desired, attempt to do it again.
    if(codes.length < quantity && attempt < MAX_ATTEMPTS)
      generateCodes(prefix, codeCharacterLength, quantity, attempt +1)
    else codes
  }

  private val MAX_ATTEMPTS = 10

  private def generateCode(prefix: String, number: Int, largestNum: Int, numericLength: Int) : String = {
    require(numericLength >= 0)
    require(largestNum >= 0)
    require(number <= largestNum)
    val num = s"%0${numericLength}d".format(number)
    s"${prefix}${num}"
  }
}
