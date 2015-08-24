package services

import scala.concurrent.{ExecutionContext, Future}

import cats.Functor
import models.{Order, Address, Addresses, Region, Regions}
import org.scalactic.{Bad, Good, Or}
import payloads.CreateAddressPayload
import responses.Addresses.Root
import responses.{Addresses ⇒ Response}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}
import utils.Validation.Result.{Failure ⇒ Invalid, Success}

object AddressManager {
  def create(payload: CreateAddressPayload, customerId: Int)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = {
    val address = Address.fromPayload(payload).copy(customerId = customerId)
    address.validate match {
      case Success ⇒
        db.run(for {
          newAddress ← Addresses.save(address)
          region ← Regions.findById(newAddress.regionId)
        } yield (newAddress, region)).map {
          case (address, Some(region))  ⇒ Good(Response.build(address, region))
          case (_, None)                ⇒ Bad(NotFoundFailure(Region, address.regionId).single)
        }
      case f: Invalid ⇒ Result.failure(ValidationFailure(f))
    }
  }

  def edit(addressId: Int, customerId: Int, payload: CreateAddressPayload)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = {
    val address = Address.fromPayload(payload).copy(customerId = customerId, id = addressId)
    address.validate match {
      case Success ⇒
        db.run((for {
          rowsAffected ← Addresses.insertOrUpdate(address)
          region ← Regions.findById(address.regionId)
        } yield (rowsAffected, address, region)).transactionally).map {
          case (1, address, Some(region)) ⇒ Good(Response.build(address, region))
          case (_, address, Some(region)) ⇒ Bad(NotFoundFailure(address).single)
          case (_, _, None)               ⇒ Bad(NotFoundFailure(Region, address.regionId).single)
        }
      case f: Invalid ⇒ Result.failure(ValidationFailure(f))
    }
  }

  def setDefaultShippingAddress(customerId: Int, addressId: Int)
    (implicit ec: ExecutionContext, db: Database): Future[Option[Failure]] = {
    db.run((for {
      _ ← Addresses.findShippingDefaultByCustomerId(customerId).map(_.isDefaultShipping).update(false)
      newDefault ← Addresses._findById(addressId).extract.map(_.isDefaultShipping).update(true)
    } yield newDefault).transactionally).map {
      case rowsAffected if rowsAffected == 1 ⇒
        None
      case _ ⇒
        Some(NotFoundFailure(Address, addressId))
    }
  }

  def removeDefaultShippingAddress(customerId: Int)
    (implicit ec: ExecutionContext, db: Database): Future[Int] =
    db.run(Addresses.findShippingDefaultByCustomerId(customerId).map(_.isDefaultShipping).update(false))
}
