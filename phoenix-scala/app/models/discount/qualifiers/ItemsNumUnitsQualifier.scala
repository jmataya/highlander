package models.discount.qualifiers

import cats.data.Xor
import failures.DiscountFailures._
import failures._
import models.discount._
import services.Result
import utils.ElasticsearchApi._
import utils.aliases._
import scala.util.Try

case class ItemsNumUnitsQualifier(numUnits: Int, search: Seq[ProductSearch])
    extends Qualifier
    with NonEmptySearch
    with ItemsQualifier {

  val qualifierType: QualifierType = ItemsNumUnits

  def check(input: DiscountInput)(implicit db: DB, ec: EC, es: ES, au: AU): Result[Unit] =
    checkInner(input)(search)

  def matchXor(input: DiscountInput)(xor: Failures Xor Buckets): Failures Xor Unit = xor match {
    case Xor.Right(buckets) ⇒
      val matchedProductFormIds =
        buckets.filter(_.docCount > 0).flatMap(b ⇒ Try(b.key.toInt).toOption).toSet
      if (numUnits >= unitsByProducts(input.lineItems, matchedProductFormIds)) Xor.Right(Unit)
      rejectXor(input, "Number of units is less than required")
    case _ ⇒
      Xor.Left(SearchFailure.single)
  }
}
