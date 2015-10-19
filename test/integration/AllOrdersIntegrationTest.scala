import java.time.Instant

import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.model.StatusCodes

import Extensions._
import models.Order._
import models._
import payloads.{BulkAssignment, BulkUpdateOrdersPayload}
import responses.{BulkAssignmentResponse, StoreAdminResponse, FullOrder, BulkOrderUpdateResponse, AllOrders}
import services.{OrderQueries, NotFoundFailure, OrderNotFoundFailure, OrderUpdateFailure}
import util.IntegrationTestBase
import util.SlickSupport.implicits._
import utils.Seeds
import utils.Seeds.Factories
import utils.Slick.implicits._
import utils.time._

class AllOrdersIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with SortingAndPaging[responses.AllOrders.Root]
  with AutomaticAuth {

  // paging and sorting API
  def uriPrefix = "v1/orders"

  def responseItems = (1 to 30).map { i ⇒
    val customer = Customers.save(Seeds.Factories.generateCustomer).futureValue
    val order = Orders.save(Factories.order.copy(
      customerId = customer.id,
      referenceNumber = Seeds.Factories.randomString(10),
      status = Order.RemorseHold,
      remorsePeriodEnd = Some(Instant.now.plusMinutes(30)))).futureValue
    responses.AllOrders.build(order, customer, None).futureValue
  }
  val sortColumnName = "referenceNumber"

  def responseItemsSort(items: IndexedSeq[responses.AllOrders.Root]) = items.sortBy(_.referenceNumber)

  def mf = implicitly[scala.reflect.Manifest[responses.AllOrders.Root]]
  // paging and sorting API end

  "GET /v1/orders" - {
    "find all" in {
      val cId = Customers.save(Factories.customer).run().futureValue.id
      Orders.save(Factories.order.copy(customerId = cId)).run().futureValue

      val responseJson = GET(s"v1/orders")
      responseJson.status must === (StatusCodes.OK)

      val allOrders = responseJson.as[Seq[AllOrders.Root]]
      allOrders.size must === (1)

      val actual = allOrders.head

      val expected = responses.AllOrders.Root(
        referenceNumber = "ABCD1234-11",
        email = "yax@yax.com",
        orderStatus = Order.ManualHold,
        placedAt = None,
        total = 27,
        paymentStatus = None,
        remorsePeriodEnd = None)

      actual must === (expected)
    }
  }

  "PATCH /v1/orders" - {
    "bulk update statuses" in new StatusUpdateFixture {
      val responseJson = PATCH(
        "v1/orders",
          BulkUpdateOrdersPayload(Seq("foo", "bar", "qux", "nonExistent"), FulfillmentStarted)
      )

      responseJson.status must === (StatusCodes.OK)

      val all = responseJson.as[BulkOrderUpdateResponse]
      val allOrders = all.orders.map(o ⇒ (o.referenceNumber, o.orderStatus))

      allOrders must contain allOf(
        ("foo", FulfillmentStarted),
        ("bar", RemorseHold),
        ("baz", ManualHold))

      all.failures must contain allOf(
        OrderUpdateFailure("bar", "Order is locked"),
        OrderUpdateFailure("nonExistent", "Not found"))
    }

    "bulk update statuses with paging and sorting" in new StatusUpdateFixture {
      val responseJson = PATCH(
        "v1/orders?pageSize=2&pageNo=2&sortBy=referenceNumber",
        BulkUpdateOrdersPayload(Seq("foo", "bar", "qux", "nonExistent"), FulfillmentStarted)
      )

      responseJson.status must === (StatusCodes.OK)

      val all = responseJson.as[BulkOrderUpdateResponse]
      val allOrders = all.orders.map(o ⇒ (o.referenceNumber, o.orderStatus))

      allOrders must contain theSameElementsInOrderAs Seq(
        ("foo", FulfillmentStarted))

      all.failures must contain allOf(
        OrderUpdateFailure("bar", "Order is locked"),
        OrderUpdateFailure("nonExistent", "Not found"))
    }
  }

  "POST /v1/orders/assignees" - {
    "assigns successfully ignoring duplicates" in new BulkAssignmentFixture {
      val assignResponse1 = POST(s"v1/orders/assignees", BulkAssignment(Seq(orderRef1), adminId))
      assignResponse1.status must === (StatusCodes.OK)
      val responseObj1 = assignResponse1.as[BulkOrderUpdateResponse]
      responseObj1.orders.map(_.referenceNumber) contains allOf("foo", "bar")
      responseObj1.failures mustBe empty

      val updOrderResponse1 = GET(s"v1/orders/$orderRef1")
      val updOrder1 = updOrderResponse1.as[FullOrder.Root]
      updOrder1.assignees.map(_.assignee) must === (Seq(StoreAdminResponse.build(admin)))

      // Don't complain about duplicates
      val assignResponse2 = POST(s"v1/orders/assignees", BulkAssignment(Seq(orderRef1, orderRef2), adminId))
      assignResponse2.status must === (StatusCodes.OK)
      val responseObj2 = assignResponse2.as[BulkOrderUpdateResponse]
      responseObj2.orders.map(_.referenceNumber) contains allOf("foo", "bar")
      responseObj2.failures mustBe empty

      val updOrderResponse2 = GET(s"v1/orders/$orderRef1")
      val updOrder2 = updOrderResponse2.as[FullOrder.Root]
      updOrder2.assignees.map(_.assignee) must === (Seq(StoreAdminResponse.build(admin)))

      val updOrderResponse3 = GET(s"v1/orders/$orderRef2")
      val updOrder3 = updOrderResponse3.as[FullOrder.Root]
      updOrder3.assignees.map(_.assignee) must === (Seq(StoreAdminResponse.build(admin)))
    }

    "happens successfully ignoring duplicates with sorting and paging" in new BulkAssignmentFixture {
      val assignResponse1 = POST(s"v1/orders/assignees?pageSize=1&pageNo=2&sortBy=referenceNumber",
        BulkAssignment(Seq(orderRef1), adminId))
      assignResponse1.status must === (StatusCodes.OK)
      val responseObj1 = assignResponse1.as[BulkOrderUpdateResponse]
      responseObj1.orders.map(_.referenceNumber) must contain theSameElementsInOrderAs Seq("foo")
      responseObj1.failures mustBe empty
    }

    "warns when order to assign not found" in new BulkAssignmentFixture {
      val response = POST(s"v1/orders/assignees", BulkAssignment(Seq(orderRef1, "NOPE"), adminId))
      response.status must === (StatusCodes.OK)
      val responseObj = response.as[BulkAssignmentResponse]
      responseObj.orders must === (OrderQueries.findAll.run().futureValue)
      responseObj.ordersNotFound must === (Seq(OrderNotFoundFailure("NOPE")))
      responseObj.adminNotFound must not be defined
    }

    "warns when admin to assign not found" in new BulkAssignmentFixture {
      val response = POST(s"v1/orders/assignees", BulkAssignment(Seq(orderRef1), 777))
      response.status must === (StatusCodes.OK)
      val responseObj = response.as[BulkAssignmentResponse]
      responseObj.orders must === (OrderQueries.findAll.run().futureValue)
      responseObj.ordersNotFound mustBe empty
      responseObj.adminNotFound.value must === (NotFoundFailure(StoreAdmin, 777))
    }
  }

  "POST /v1/orders/assignees" - {
    "unassigns successfully ignoring wrong attempts" in new BulkAssignmentFixture {
      // Should pass
      val unassign1 = POST(s"v1/orders/assignees/delete", BulkAssignment(Seq(orderRef1), adminId))
      unassign1.status must === (StatusCodes.OK)

      POST(s"v1/orders/assignees", BulkAssignment(Seq(orderRef1, orderRef2), adminId))

      val unassign2 = POST(s"v1/orders/assignees/delete", BulkAssignment(Seq(orderRef1), adminId))
      unassign2.status must === (StatusCodes.OK)

      val updOrder1 = GET(s"v1/orders/$orderRef1").as[FullOrder.Root]
      updOrder1.assignees mustBe empty

      val updOrder2 = GET(s"v1/orders/$orderRef2").as[FullOrder.Root]
      updOrder2.assignees.map(_.assignee) must === (Seq(StoreAdminResponse.build(admin)))
    }

    "unassigns successfully ignoring wrong attempts with sorting and paging" in new BulkAssignmentFixture {
      POST(s"v1/orders/assignees", BulkAssignment(Seq(orderRef1, orderRef2), adminId))

      val unassign = POST(s"v1/orders/assignees/delete?pageSize=1&pageNo=2&sortBy=referenceNumber",
        BulkAssignment(Seq(orderRef1), adminId))
      unassign.status must === (StatusCodes.OK)

      val responseObj = unassign.as[BulkOrderUpdateResponse]
      responseObj.orders.map(_.referenceNumber) must contain theSameElementsInOrderAs Seq("foo")
      responseObj.failures mustBe empty
    }

    "warns when order to unassign not found" in new BulkAssignmentFixture {
      val response = POST(s"v1/orders/assignees/delete", BulkAssignment(Seq(orderRef1, "NOPE"), adminId))
      response.status must === (StatusCodes.OK)
      val responseObj = response.as[BulkAssignmentResponse]
      responseObj.orders must === (OrderQueries.findAll.run().futureValue)
      responseObj.ordersNotFound must === (Seq(OrderNotFoundFailure("NOPE")))
      responseObj.adminNotFound must not be defined
    }

    "warns when admin to unassign not found" in new BulkAssignmentFixture {
      val response = POST(s"v1/orders/assignees/delete", BulkAssignment(Seq(orderRef1), 777))
      response.status must === (StatusCodes.OK)
      val responseObj = response.as[BulkAssignmentResponse]
      responseObj.orders must === (OrderQueries.findAll.run().futureValue)
      responseObj.ordersNotFound mustBe empty
      responseObj.adminNotFound.value must === (NotFoundFailure(StoreAdmin, 777))
    }
  }

  trait StatusUpdateFixture {
    (for {
      customer ← Customers.save(Factories.customer)
      foo ← Orders.save(Factories.order.copy(customerId = customer.id, referenceNumber = "foo", status = FraudHold))
      bar ← Orders.save(Factories.order.copy(customerId = customer.id, referenceNumber = "bar", status = RemorseHold,
        locked = true))
      baz ← Orders.save(Factories.order.copy(customerId = customer.id, referenceNumber = "baz", status = ManualHold))
    } yield (customer, foo, bar)).run().futureValue
  }

  trait BulkAssignmentFixture {
    val (order1, order2, admin) = (for {
      customer ← Customers.save(Factories.customer)
      order1 ← Orders.save(Factories.order.copy(id = 1, referenceNumber = "foo", customerId = customer.id))
      order2 ← Orders.save(Factories.order.copy(id = 2, referenceNumber = "bar", customerId = customer.id))
      admin ← StoreAdmins.save(Factories.storeAdmin)
    } yield (order1, order2, admin)).run().futureValue
    val orderRef1 = order1.referenceNumber
    val orderRef2 = order2.referenceNumber
    val adminId = admin.id
  }
}
