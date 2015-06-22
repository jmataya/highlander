package services

import models._

import org.scalactic._
import scala.concurrent.{Future, ExecutionContext}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}

import slick.driver.PostgresDriver.api._


object ShippingMethodsBuilder {
  case class ShippingMethodWithPrice(displayName: String, estimatedTime: String, price: Int)

  // Which shipping methods are active for this order?
  // 1) Do restriction check
  def availableShippingMethods(order: Order)
                              (implicit ec: ExecutionContext,
                               db: Database): Future[Seq[ShippingMethod]] = {
    val availMethods = ShippingMethods.filter(_.isActive).result
    db.run(availMethods).map(methods => methods)
  }


  def fullShippingMethodsForOrder(order: Order)
                                 (implicit ec: ExecutionContext,
                                   db: Database): Future[Seq[ShippingMethodWithPrice]] = {
    val baseMethods = availableShippingMethods(order)
    baseMethods.map{ _.map { shippingMethod =>
      ShippingPriceRules.shippingPriceRuesForShippingMethod(shippingMethod.id).map{
        _.map { sRule =>
          // TODO: AW: Come back and deal with SKU-specific criteria later.
          ShippingPriceRulesOrderCriteria.criteriaForPricingRule(sRule.id).map {
            _.map { oCriterion =>
              oCriterion match {
                case t: OrderPriceCriterion =>
                  t.priceType match {
                    case t: OrderPriceCriterion.GrandTotal.type =>
                    // This is the only implementation I'm working on for now -- to demonstrate how the functionality should generally work.
                    case t: OrderPriceCriterion.SubTotal.type =>
                      val exactApplies = oCriterion.exactMatch.exists(eMatch => (order.subTotal == eMatch))
                      val greaterApplies = oCriterion.greaterThan.exists(gThan => order.subTotal >= gThan)
                      val lessApplies = oCriterion.lessThan.exists(lThan => order.subTotal >= lThan)
                      if (exactApplies || greaterApplies || lessApplies) {
                        ShippingMethodWithPrice(displayName = shippingMethod.storefrontDisplayName, estimatedTime = "Long Time", price = sRule.flatPrice)
                      }
                    case t: OrderPriceCriterion.GrandTotalLessShipping.type =>
                    case t: OrderPriceCriterion.GrandTotalLessTax.type =>
                  }
                case unknown => //could not find inherited objects or case classes
              }
            }
          }
        }

      }
      ShippingMethodWithPrice(displayName = "donkey", estimatedTime = "FOREVER", price = 3333)
    }
    }
  }
  // What is the price of a certain shipping method based on the current order details?

}
