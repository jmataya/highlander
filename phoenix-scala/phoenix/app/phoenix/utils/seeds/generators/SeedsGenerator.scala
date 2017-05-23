package phoenix.utils.seeds.generators

import faker.Faker
import models.objects.ObjectContexts
import phoenix.models.account._
import phoenix.models.coupon._
import phoenix.models.customer._
import phoenix.models.inventory._
import phoenix.models.location.{Address, Addresses}
import phoenix.models.payment.creditcard.CreditCards
import phoenix.models.product.SimpleContext
import phoenix.models.promotion._
import phoenix.utils.aliases._
import slick.jdbc.PostgresProfile.api._
import utils.db._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

object SeedsGenerator
    extends CustomerGenerator
    with AddressGenerator
    with CreditCardGenerator
    with OrderGenerator
    with GiftCardGenerator
    with ProductGenerator
    with PromotionGenerator
    with CouponGenerator {

  def generateAddresses(customers: Seq[User]): Seq[Address] = {
    customers.flatMap { c ⇒
      generateAddress(customer = c, isDefault = true) +:
      ((0 to Random.nextInt(2)) map { i ⇒
            generateAddress(customer = c, isDefault = false)
          })
    }
  }

  def makePromotions(promotionCount: Int) =
    (1 to promotionCount).par.map { i ⇒
      generatePromotion(Random.nextInt(2) match {
        case 0 ⇒ Promotion.Auto
        case _ ⇒ Promotion.Coupon
      })
    }.toList

  def makeCoupons(promotions: Seq[SimplePromotion]) =
    promotions.par.map { p ⇒
      generateCoupon(p)
    }.toList

  def makeCouponCodes(promotions: Seq[SimpleCoupon]) =
    promotions.flatMap { c ⇒
      CouponCodes.generateCodes("CP", 12, 1 + Random.nextInt(5)).map { d ⇒
        CouponCode(couponFormId = c.formId, code = d)
      }
    }.toList

  def pickOne[T](vals: Seq[T]): T = vals(Random.nextInt(vals.length))

  def insertRandomizedSeeds(customersCount: Int,
                            appeasementCount: Int)(implicit ec: EC, db: DB, ac: AC, au: AU) = {
    Faker.locale("en")

    for {
      context     ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      shipMethods ← * <~ getShipmentRules
      skus        ← * <~ Skus.filter(_.contextId === context.id).result
      skuIds             = skus.map(_.id)
      generatedCustomers = generateCustomers(customersCount)
      accountIds ← * <~ Accounts.createAllReturningIds(generatedCustomers.map { _ ⇒
                    Account()
                  })
      accountCustomers = accountIds zip generatedCustomers
      customerIds ← * <~ Users.createAllReturningIds(accountCustomers.map {
                     case (accountId, customer) ⇒
                       customer.copy(accountId = accountId)
                   })
      customers ← * <~ Users.filter(_.id.inSet(customerIds)).result
      _ ← * <~ CustomersData.createAll(customers.map { c ⇒
           CustomerData(accountId = c.accountId, userId = c.id, scope = Scope.current)
         })
      _ ← * <~ Addresses.createAll(generateAddresses(customers))
      _ ← * <~ CreditCards.createAll(generateCreditCards(customers))
      orderedGcs ← * <~ randomSubset(customerIds).map { id ⇒
                    generateGiftCardPurchase(id, context)
                  }
      appeasements ← * <~ (1 to appeasementCount).map(i ⇒ generateGiftCardAppeasement)

      giftCards ← * <~ orderedGcs ++ appeasements
      unsavedPromotions = makePromotions(1)
      promotions     ← * <~ generatePromotions(unsavedPromotions)
      unsavedCoupons ← * <~ makeCoupons(promotions.filter(_.applyType == Promotion.Coupon))
      coupons        ← * <~ generateCoupons(unsavedCoupons)
      unsavedCodes   ← * <~ makeCouponCodes(coupons)
      _              ← * <~ CouponCodes.createAll(unsavedCodes)
      _ ← * <~ randomSubset(customerIds, customerIds.length).map { id ⇒
           generateOrders(id, context, skuIds, pickOne(giftCards))
         }
    } yield {}
  }
}