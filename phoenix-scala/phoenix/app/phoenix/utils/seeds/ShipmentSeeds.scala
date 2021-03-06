package phoenix.utils.seeds

import core.db._
import org.json4s.Formats
import org.json4s.jackson.JsonMethods._
import phoenix.models.location.Country.unitedStatesId
import phoenix.models.rules._
import phoenix.models.shipping.ShippingMethod._
import phoenix.models.shipping._
import phoenix.utils.JsonFormatters
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global

trait ShipmentSeeds {

  implicit val formats: Formats = JsonFormatters.phoenixFormats

  type ShippingMethods =
    (ShippingMethod#Id, ShippingMethod#Id, ShippingMethod#Id, ShippingMethod#Id)

  def getShipmentRules(implicit db: DB): DbResultT[ShippingMethods] =
    for {
      methods ← * <~ ShippingMethods.findActive.result
    } yield
      methods.seq.toList match {
        case m1 :: m2 :: m3 :: m4 :: Nil ⇒ (m1.id, m2.id, m3.id, m4.id)
        case _                           ⇒ ???
      }

  def createShipmentRules: DbResultT[ShippingMethods] =
    for {
      methods ← * <~ ShippingMethods.createAllReturningIds(shippingMethods)
    } yield
      methods.seq.toList match {
        case m1 :: m2 :: m3 :: m4 :: Nil ⇒ (m1, m2, m3, m4)
        case _                           ⇒ ???
      }

  def shippingMethods =
    Seq(
      ShippingMethod(
        adminDisplayName = standardShippingNameForAdmin,
        storefrontDisplayName = standardShippingName,
        code = standardShippingCode,
        price = 300,
        isActive = true,
        conditions = Some(under50Bucks)
      ),
      ShippingMethod(
        adminDisplayName = standardShippingNameForAdmin,
        storefrontDisplayName = standardShippingName,
        code = standardShippingFreeCode,
        price = 0,
        isActive = true,
        conditions = Some(over50Bucks)
      ),
      ShippingMethod(
        adminDisplayName = expressShippingNameForAdmin,
        storefrontDisplayName = expressShippingName,
        code = expressShippingCode,
        price = 1500,
        isActive = true,
        conditions = Some(usOnly)
      ),
      ShippingMethod(
        adminDisplayName = overnightShippingNameForAdmin,
        storefrontDisplayName = overnightShippingName,
        code = overnightShippingCode,
        price = 3000,
        isActive = true,
        conditions = Some(usOnly)
      )
    )

  def lowConditions: QueryStatement =
    parse("""
      | {
      |   "comparison": "and",
      |   "conditions": [{
      |     "rootObject": "Order", "field": "grandtotal", "operator": "greaterThan", "valInt": 25
      |   }]
      | }
    """.stripMargin).extract[QueryStatement]

  def usOnly = parse(s"""
    | {
    |   "comparison": "and",
    |   "conditions": [{
    |     "rootObject": "ShippingAddress", "field": "countryId", "operator": "equals", "valInt": $unitedStatesId
    |   }]
    | }
  """.stripMargin).extract[QueryStatement]

  def over50Bucks = parse(s"""
    | {
    |   "comparison": "and",
    |   "statements": [{
    |     "comparison": "and",
    |     "conditions": [{
    |       "rootObject": "ShippingAddress", "field": "countryId", "operator": "equals", "valInt": $unitedStatesId
    |     }]
    |   }, {
    |     "comparison": "and",
    |     "conditions": [{
    |       "rootObject": "Order", "field": "subtotal", "operator": "greaterThanOrEquals", "valInt": 5000
    |     }]
    |   }]
    | }
  """.stripMargin).extract[QueryStatement]

  def under50Bucks = parse(s"""
     | {
     |   "comparison": "and",
     |   "statements": [{
     |     "comparison": "and",
     |     "conditions": [{
     |       "rootObject": "ShippingAddress", "field": "countryId", "operator": "equals", "valInt": $unitedStatesId
     |     }]
     |   }, {
     |     "comparison": "and",
     |     "conditions": [{
     |       "rootObject": "Order", "field": "subtotal", "operator": "lessThan", "valInt": 5000
     |     }]
     |   }]
     | }
  """.stripMargin).extract[QueryStatement]

  def shipment = Shipment(1, "boo", Some(1), Some(1))

  def condition =
    Condition(rootObject = "Order", field = "subtotal", operator = Condition.Equals, valInt = Some(50))
}
