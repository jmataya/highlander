package concepts

import concepts.discounts._
import concepts.discounts.qualifiers._
import failures.DiscountCompilerFailures._
import util.TestBase

class QualifierAstCompilerTest extends TestBase {

  "QualifierAstCompiler" - {

    "succeeds for case object with valid, but empty attributes" in new OrderAnyValidFixture {
      rightValue(compiler.compile()) must === (OrderAnyQualifier)
    }

    "succeeds for case class with valid attributes" in new OrderTotalAmountValidFixture {
      rightValue(compiler.compile()) must === (OrderTotalAmountQualifier(totalAmount = 1))
    }

    "fails when typo in configuration found" in new OrderTotalAmountTypoFixture {
      leftValue(compiler.compile()) must === (QualifierAttributesExtractionFailure(typeName, attributes).single)
    }

    "fails when invalid json provided" in new InvalidJsonFixture {
      leftValue(compiler.compile()) must === (QualifierAstParseFailure(json).single)
    }

    "fails when invalid json format provided" in new InvalidJsonFormatFixture {
      leftValue(compiler.compile()) must === (QualifierAstInvalidFormatFailure.single)
    }
  }

  def getTuple(json: String): (String, QualifierAstCompiler) =
    (json, QualifierAstCompiler(json))

  trait OrderAnyValidFixture {
    val (json, compiler) = getTuple("""{"orderAny": {}}""")
  }

  trait OrderTotalAmountValidFixture {
    val (json, compiler) = getTuple("""{"orderTotalAmount": {"totalAmount": 1}}""")
  }

  trait OrderTotalAmountTypoFixture {
    val typeName          = "orderTotalAmount"
    val attributes        = """{"totalAmounts":1}""" // compact()
    val (json, compiler)  = getTuple(s"""{"$typeName": $attributes}""")
  }

  trait InvalidJsonFixture {
    val (json, compiler) = getTuple("""""")
  }

  trait InvalidJsonFormatFixture {
    val (json, compiler) = getTuple("""{"orderAny": [1, 2, 3]}""")
  }
}
