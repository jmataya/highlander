package utils.seeds

import scala.util.Random

import cats.implicits._
import models.customer.{CustomerDynamicGroup, Customers, Customer}
import models.inventory.Skus
import models.location.{Addresses, Address}
import models.order._
import models.coupon._
import models.promotion._
import models.payment.PaymentMethod
import models.payment.creditcard.{CreditCards, CreditCard}
import models.product.SimpleContext
import models.objects.{ObjectContext, ObjectContexts}
import utils.seeds.generators._
import utils.aliases._

import utils.ModelWithIdParameter
import utils.DbResultT
import utils.DbResultT._
import utils.DbResultT.implicits._
import Seeds.Factories

import slick.driver.PostgresDriver.api._
import faker.Faker
import org.json4s.JObject

object RankingSeedsGenerator {
  def fakeJson = JObject()

  def generateCustomer: Customer = Customer.build(email = s"${randomString(10)}@email.com",
    password = Some(randomString(10)), name = Some(randomString(10)))

  def generateOrder(state: Order.State, customerId: Int, context: ObjectContext): Order = {
    Order(customerId = customerId,
      referenceNumber = randomString(8) + "-17",
      state = state, contextId = context.id)
  }

  def generateOrderPayment[A <: PaymentMethod with ModelWithIdParameter[A]](orderId: Int,
    paymentMethod: A, amount: Int = 100): OrderPayment = {
      Factories.orderPayment.copy(orderId = orderId, amount = Some(amount),
        paymentMethodId = paymentMethod.id)
  }

  def generateAddress: Address = Address(customerId = 0, regionId = 4177, name = randomString(10),
    address1 = randomString(30), address2 = None, city = "Seattle", zip = "12345", isDefaultShipping = false,
    phoneNumber = None)

  def generateGroup(ownerId: Int): CustomerDynamicGroup = CustomerDynamicGroup(name = s"${randomString(10)}Group",
    createdBy = ownerId, clientState = fakeJson, elasticRequest = fakeJson,
    customersCount = Some(Random.nextInt))

  def insertRankingSeeds(customersCount: Int)(implicit db: Database) = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val location = "Arkham"

    def makeOrders(c: Customer, context: ObjectContext) = {
      (1 to 5 + Random.nextInt(20)).map { i ⇒ generateOrder(Order.Shipped, c.id, context) }
    }

    def makePayment(o: Order, pm: CreditCard) = {
      generateOrderPayment(o.id, pm, Random.nextInt(20000) + 100)
    }

    def insertCustomers() = Customers.createAll((1 to customersCount).map { i ⇒
      val s = randomString(15)
      Customer.build(name = s.some, email = s"$s-$i@email.com", password = s.some, location = location.some)
    })

    def insertOrders() = for {
      context ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      customers ← * <~ Customers.filter(_.location === location).result
      newCreditCards ← * <~  customers.map { c ⇒
        Factories.creditCard.copy(customerId = c.id, holderName = c.name.getOrElse(""))
      }
      _ ← * <~ CreditCards.createAll(newCreditCards)
      orders ← * <~ customers.flatMap{c ⇒ makeOrders(c, context)}
      _  ← * <~  Orders.createAll(orders)
    } yield {}

    def insertPayments() = {
      val action = (for {
        (o, cc) ← Orders.join(CreditCards).on(_.customerId === _.customerId)
      } yield (o, cc)).result

      action.flatMap { ordersWithCc ⇒
        OrderPayments.createAll(ordersWithCc.map { case (order, cc) ⇒ makePayment(order, cc) })
      }
    }

    for {
      _ ← * <~ insertCustomers
      _ ← * <~ insertOrders
      _ ← * <~ insertPayments
    } yield {}
  }

  def randomOrderState: Order.State = {
    val types = Order.State.types.filterNot(_ == Order.Cart)
    val index = Random.nextInt(types.size)
    types.drop(index).head
  }

  def randomString(len: Int) = Random.alphanumeric.take(len).mkString.toLowerCase
}

object SeedsGenerator extends CustomerGenerator with AddressGenerator
  with CreditCardGenerator with OrderGenerator with InventoryGenerator with InventorySummaryGenerator
  with GiftCardGenerator with ProductGenerator with PromotionGenerator with CouponGenerator {

  def generateAddresses(customers: Seq[Customer]): Seq[Address] = {
    customers.flatMap { c ⇒
        generateAddress(customer = c, isDefault = true) +:
        ((0 to Random.nextInt(2)) map { i ⇒
          generateAddress(customer = c, isDefault = false)
        })
    }
  }

  def makeProducts(productCount: Int) = (1 to productCount).par.map { i ⇒  
    generateProduct 
  }.toList

  def makePromotions(promotionCount: Int) = (1 to promotionCount).par.map { i ⇒  
    generatePromotion(Random.nextInt(2) match {
      case 0 ⇒ Promotion.Auto
      case _ ⇒ Promotion.Coupon
    })
  }.toList
  
  def makeCoupons(promotions: Seq[SimplePromotion]) = promotions.par.map { 
    p ⇒  generateCoupon(p) 
  }.toList

  def makeCouponCodes(promotions: Seq[SimpleCoupon]) = promotions.flatMap { c ⇒  
    CouponCodes.generateCodes("CP", 12, 1 + Random.nextInt(5)).map { d ⇒ 
      CouponCode(couponFormId = c.formId, code = d)
    }
  }.toList

  def pickOne[T](vals: Seq[T]) : T = vals(Random.nextInt(vals.length))

  def insertRandomizedSeeds(customersCount: Int, productCount: Int)(implicit ec: EC, db: DB) = {
    Faker.locale("en")
    val location = "Random"

    for {
      context ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      shipMethods ← * <~ getShipmentRules
      warehouseIds ← * <~ generateWarehouses
      unsavedProducts = makeProducts(productCount)
      products ← * <~ generateProducts(unsavedProducts)
      _ ← * <~ generateInventories(products, warehouseIds)
      customerIds ← * <~ Customers.createAllReturningIds(generateCustomers(customersCount, location))
      customers  ← * <~ Customers.filter(_.id.inSet(customerIds)).result
      _ ← * <~ Addresses.createAll(generateAddresses(customers))
      _ ← * <~ CreditCards.createAll(generateCreditCards(customers))
      orderedGcs ← * <~ DbResultT.sequence(randomSubset(customerIds).map { id ⇒ generateGiftCardPurchase(id, context)})
      appeasementCount = Math.max(productCount / 8, Random.nextInt(productCount))
      appeasements  ← * <~ DbResultT.sequence((1 to appeasementCount).map { i ⇒ generateGiftCardAppeasement})
      giftCards  ← * <~  orderedGcs ++ appeasements
      unsavedPromotions = makePromotions(5)
      promotions ← * <~ generatePromotions(unsavedPromotions)
      unsavedCoupons ← * <~ makeCoupons(promotions.filter(_.applyType == Promotion.Coupon))
      coupons ← * <~ generateCoupons(unsavedCoupons)
      unsavedCodes ← * <~ makeCouponCodes(coupons)
      _  ← * <~ CouponCodes.createAll(unsavedCodes)
      _ ← * <~ DbResultT.sequence(
        randomSubset(customerIds, customerIds.length).map{
          id ⇒ generateOrders(id, context, products, pickOne(giftCards))
        })
    } yield {}
  }

}
