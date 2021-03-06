package models

import phoenix.models.account.Scope
import phoenix.models.cord._
import slick.jdbc.PostgresProfile.api._
import testutils._
import testutils.fixtures.BakedFixtures

class CordsIntegrationTest extends IntegrationTestBase with TestObjectContext with BakedFixtures {

  "should not override cart's reference_number" in new Customer_Seed with StoreAdmin_Seed {
    Carts
      .create(Cart(referenceNumber = "foo", scope = Scope.current, accountId = customer.accountId))
      .gimme
    val cord = Cords.result.headOption.gimme.value
    cord.referenceNumber must === ("foo")
    cord.isCart mustBe true
  }

  "should generate new & unique reference numbers if empty" in new Customer_Seed {

    implicit val au = customerAuthData

    val refNums = (1 to 3).map { i ⇒
      val cart = Carts.create(Cart(accountId = customer.accountId, scope = Scope.current)).gimme
      val cord = Cords.findOneByRefNum(cart.refNum).gimme.value
      cart.referenceNumber must === (cord.referenceNumber)
      cord.isCart mustBe true
      Orders.createFromCart(cart, None).gimme
      cord.referenceNumber
    }

    refNums.size must === (refNums.distinct.size)
  }

  "cord should be updated and cart should be deleted on order creation" in new Customer_Seed {
    implicit val au = customerAuthData
    val cart        = Carts.create(Cart(accountId = customer.accountId, scope = Scope.current)).gimme
    val order       = Orders.createFromCart(cart, None).gimme
    val cord        = Cords.result.headOption.gimme.value
    cart.referenceNumber must === (cord.referenceNumber)
    order.referenceNumber must === (cord.referenceNumber)
    cord.isCart mustBe false
    Carts.findOneByRefNum(cart.referenceNumber).gimme must not be defined
  }
}
