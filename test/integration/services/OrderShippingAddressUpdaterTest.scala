package services

import models.StoreAdmins
import models.activity.ActivityContext
import models.customer.Customers
import models.location.Addresses
import models.order._
import models.traits.Originator
import payloads.AddressPayloads._
import services.orders.OrderShippingAddressUpdater._
import util.IntegrationTestBase
import utils.db.DbResultT._
import utils.seeds.Seeds.Factories

class OrderShippingAddressUpdaterTest extends IntegrationTestBase {
  import concurrent.ExecutionContext.Implicits.global

  implicit val ac = ActivityContext(
      userId = 1, userType = "b", transactionId = "c")

  "OrderUpdater" - {

    "Adds a shipping address by referencing an order that already exists" in new Fixture {
      val fullOrder = createShippingAddressFromAddressId(
          Originator(admin), address.id, Some(order.refNum)).futureValue.get
      fullOrder.result.shippingAddress must not be 'empty
      val orderAddress = fullOrder.result.shippingAddress.value

      orderAddress.name must ===(address.name)
      orderAddress.address1 must ===(address.address1)
      orderAddress.address2 must ===(address.address2)
      orderAddress.city must ===(address.city)
      orderAddress.zip must ===(address.zip)
    }

    "Adds a shipping address by creating a new address in the payload" in new Fixture {
      val newAddress = CreateAddressPayload(name = "Home Office",
                                            regionId = 1,
                                            address1 = "3000 Coolio Dr",
                                            city = "Seattle",
                                            zip = "55555")

      val fullOrder = createShippingAddressFromPayload(
          Originator(admin), newAddress, Some(order.refNum)).futureValue.get
      fullOrder.result.shippingAddress must not be 'empty
      val orderAddress = fullOrder.result.shippingAddress.value

      orderAddress.name must ===(newAddress.name)
      orderAddress.address1 must ===(newAddress.address1)
      orderAddress.address2 must ===(newAddress.address2)
      orderAddress.city must ===(newAddress.city)
      orderAddress.zip must ===(newAddress.zip)
    }

    "Updates a shipping address by referencing an order that already exists" in new UpdateAddressFixture {
      val fullOrder = createShippingAddressFromAddressId(
          Originator(admin), newAddress.id, Some(order.refNum)).futureValue.get
      fullOrder.result.shippingAddress must not be 'empty
      val orderAddress = fullOrder.result.shippingAddress.value

      orderAddress.name must ===(newAddress.name)
      orderAddress.address1 must ===(newAddress.address1)
      orderAddress.address2 must ===(newAddress.address2)
      orderAddress.city must ===(newAddress.city)
      orderAddress.zip must ===(newAddress.zip)
    }

    "Updates a shipping address by sending fields in the payload" in new UpdateAddressFixture {
      val payload = UpdateAddressPayload(name = Some("Don Keyhote"))
      val fullOrder = updateShippingAddressFromPayload(
          Originator(admin), payload, Some(order.refNum)).futureValue.get
      fullOrder.result.shippingAddress must not be 'empty
      val orderAddress = fullOrder.result.shippingAddress.value

      orderAddress.name must ===("Don Keyhote")
    }
  }

  trait Fixture {
    val (admin, customer, address, order) = (for {
      admin ← * <~ StoreAdmins.create(Factories.storeAdmin)
      customer ← * <~ Customers.create(Factories.customer)
      address ← * <~ Addresses.create(
                   Factories.address.copy(customerId = customer.id))
      order ← * <~ Orders.create(Factories.order.copy(customerId = customer.id,
                                                      state = Order.Cart))
    } yield (admin, customer, address, order)).runTxn().futureValue.rightVal
  }

  trait UpdateAddressFixture extends Fixture {
    val (orderShippingAddress, newAddress) = (for {
      orderShippingAddress ← * <~ OrderShippingAddresses.copyFromAddress(
                                address = address, orderId = order.id)
      newAddress ← * <~ Addresses.create(
                      Factories.address.copy(customerId = customer.id,
                                             name = "New Address",
                                             isDefaultShipping = false))
    } yield (orderShippingAddress, newAddress)).runTxn().futureValue.rightVal
  }
}
