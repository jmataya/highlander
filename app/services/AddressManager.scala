package services

import java.time.Instant

import scala.concurrent.{ExecutionContext, Future}

import cats.implicits._
import models.order._
import Order._
import models.customer._
import models.location._
import models.StoreAdmin
import payloads.CreateAddressPayload
import responses.Addresses._
import responses.{Addresses ⇒ Response, TheResponse}
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._

import models.activity.ActivityContext

object AddressManager {

  private def addressNotFound(id: Int): NotFoundFailure404 = NotFoundFailure404(Address, id)

  def findAllVisibleByCustomer(customerId: Int)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): Result[TheResponse[Seq[Root]]] = {
    val query = Addresses.findAllVisibleByCustomerIdWithRegions(customerId)

    Addresses.sortedAndPagedWithRegions(query).result.map(Response.buildMulti).toTheResponse.run()
  }

  def getByIdAndCustomer(addressId: Int, customer: Customer)
    (implicit db: Database, ec: ExecutionContext): Result[Root] = (for {
    address ← * <~ Addresses.findVisibleByIdAndCustomer(addressId, customer.id)
                            .mustFindOr(NotFoundFailure404(Address, addressId))
    region  ← * <~ Regions.mustFindById404(address.regionId)
  } yield Response.build(address, region)).run()

  def create(payload: CreateAddressPayload, customerId: Int, admin: Option[StoreAdmin] = None)
    (implicit ec: ExecutionContext, db: Database, ac: ActivityContext): Result[Root] = (for {

    customer  ← * <~ Customers.mustFindById404(customerId)
    address   ← * <~ Addresses.create(Address.fromPayload(payload).copy(customerId = customerId))
    region    ← * <~ Regions.findOneById(address.regionId).safeGet.toXor
    _         ← * <~ LogActivity.addressCreated(admin, customer, address, region)
  } yield Response.build(address, region)).runTxn()

  def edit(addressId: Int, customerId: Int, payload: CreateAddressPayload, admin: Option[StoreAdmin] = None)
    (implicit ec: ExecutionContext, db: Database, ac: ActivityContext): Result[Root] = (for {

    customer    ← * <~ Customers.mustFindById404(customerId)
    oldAddress  ← * <~ Addresses.findVisibleByIdAndCustomer(addressId, customerId).mustFindOr(addressNotFound(addressId))
    oldRegion   ← * <~ Regions.findOneById(oldAddress.regionId).safeGet.toXor
    address     ← * <~ Address.fromPayload(payload).copy(customerId = customerId, id = addressId).validate
    _           ← * <~ Addresses.insertOrUpdate(address).toXor
    region      ← * <~ Regions.findOneById(address.regionId).safeGet.toXor
    _           ← * <~ LogActivity.addressUpdated(admin, customer, address, region, oldAddress, oldRegion)
  } yield Response.build(address, region)).runTxn()

  def get(customerId: Int, addressId: Int)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = (for {

    address ← * <~ Addresses.findVisibleByIdAndCustomer(addressId, customerId).mustFindOr(addressNotFound(addressId))
    region  ← * <~ Regions.findOneById(address.regionId).safeGet.toXor
  } yield Response.build(address, region)).run()

  def remove(customerId: Int, addressId: Int, admin: Option[StoreAdmin] = None)
    (implicit ec: ExecutionContext, db: Database, ac: ActivityContext): Result[Unit] = (for {

    customer    ← * <~ Customers.mustFindById404(customerId)
    address     ← * <~ Addresses.findVisibleByIdAndCustomer(addressId, customerId).mustFindOr(addressNotFound(addressId))
    region      ← * <~ Regions.findOneById(address.regionId).safeGet.toXor
    softDelete  ← * <~ address.updateTo(address.copy(deletedAt = Instant.now.some, isDefaultShipping = false))
    updated     ← * <~ Addresses.update(address, softDelete)
    _           ← * <~ LogActivity.addressDeleted(admin, customer, address, region)
  } yield {}).runTxn()

  def setDefaultShippingAddress(customerId: Int, addressId: Int)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = (for {
    customer    ← * <~ Customers.mustFindById404(customerId)
    _           ← * <~ Addresses.findShippingDefaultByCustomerId(customerId).map(_.isDefaultShipping).update(false)
    address     ← * <~ Addresses.findVisibleByIdAndCustomer(addressId, customerId).mustFindOr(addressNotFound(addressId))
    newAddress  = address.copy(isDefaultShipping = true)
    address     ← * <~ Addresses.update(address, newAddress)
    region      ← * <~ Regions.findOneById(address.regionId).safeGet.toXor
  } yield Response.build(newAddress, region)).run()

  def removeDefaultShippingAddress(customerId: Int)
    (implicit ec: ExecutionContext, db: Database): Result[Int] =
    Addresses.findShippingDefaultByCustomerId(customerId).map(_.isDefaultShipping).update(false).run()
      .flatMap(Result.good)

  def getDisplayAddress(customer: Customer)
    (implicit ec: ExecutionContext, db: Database): Future[Option[Root]] = {

    defaultShipping(customer.id).run().flatMap {
      case Some((address, region)) ⇒
        Future.successful(Response.build(address, region).some)
      case None ⇒
        lastShippedTo(customer.id).run().map {
          case Some((ship, region)) ⇒ Response.buildOneShipping(ship, region, isDefault = false).some
          case None ⇒ None
        }
    }
  }

  def defaultShipping(customerId: Int): DBIO[Option[(Address, Region)]] = (for {
    address ← Addresses.findShippingDefaultByCustomerId(customerId)
    region  ← Regions if region.id === address.regionId
  } yield (address, region)).one

  def lastShippedTo(customerId: Int)
    (implicit db: Database, ec: ExecutionContext): DBIO[Option[(OrderShippingAddress, Region)]] = (for {
    order ← Orders.findByCustomerId(customerId)
      .filter(_.state =!= (Order.Cart: Order.State))
      .sortBy(_.id.desc)
    shipping ← OrderShippingAddresses if shipping.orderId === order.id
    region   ← Regions if region.id === shipping.regionId
  } yield (shipping, region)).take(1).one
}
