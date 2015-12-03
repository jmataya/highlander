package services

import cats.data.Validated.{Invalid, Valid}
import cats.data.Xor
import cats.implicits._
import models.Customers.scope._
import models.{Customer, Customers, Orders, StoreAdmin, javaTimeSlickMapper}
import payloads.{ActivateCustomerPayload, CreateCustomerPayload, CustomerSearchForNewOrder, UpdateCustomerPayload}
import responses.CustomerResponse._
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.UpdateReturning._
import utils.Slick.implicits._
import utils.jdbc._

import scala.concurrent.ExecutionContext

object CustomerManager {

  private def customerNotFound(id: Int): NotFoundFailure404 = NotFoundFailure404(Customer, id)

  def toggleDisabled(customerId: Int, disabled: Boolean, admin: StoreAdmin)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = (for {
      customer ← * <~ Customers.mustFindById(customerId, customerNotFound)
      updated ← * <~ Customers.update(customer, customer.copy(isDisabled = disabled, disabledBy = Some(admin.id)))
    } yield build(updated)).runT()


  // TODO: add blacklistedReason later
  def toggleBlacklisted(customerId: Int, blacklisted: Boolean, admin: StoreAdmin)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = (for {
      customer ← * <~ Customers.mustFindById(customerId, customerNotFound)
      updated ← * <~ Customers.update(customer, customer.copy(isBlacklisted = blacklisted, blacklistedBy = Some(admin
        .id)))
    } yield build(updated)).runT()

  def findAll(implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): ResultWithMetadata[Seq[Root]] = {
    val query = Customers.withRegionsAndRank

    val queryWithMetadata = query.withMetadata.sortAndPageIfNeeded { case (s, (customer, _, _, _)) ⇒
      s.sortColumn match {
        case "id"                ⇒ if(s.asc) customer.id.asc                 else customer.id.desc
        case "isDisabled"        ⇒ if(s.asc) customer.isDisabled.asc         else customer.isDisabled.desc
        case "disabledBy"        ⇒ if(s.asc) customer.disabledBy.asc         else customer.disabledBy.desc
        case "isBlacklisted"     ⇒ if(s.asc) customer.isBlacklisted.asc      else customer.isBlacklisted.desc
        case "blacklistedBy"     ⇒ if(s.asc) customer.blacklistedBy.asc      else customer.blacklistedBy.desc
        case "blacklistedReason" ⇒ if(s.asc) customer.blacklistedReason.asc  else customer.blacklistedReason.desc
        case "email"             ⇒ if(s.asc) customer.email.asc              else customer.email.desc
        case "name"              ⇒ if(s.asc) customer.name.asc               else customer.name.desc
        case "phoneNumber"       ⇒ if(s.asc) customer.phoneNumber.asc        else customer.phoneNumber.desc
        case "location"          ⇒ if(s.asc) customer.location.asc           else customer.location.desc
        case "modality"          ⇒ if(s.asc) customer.modality.asc           else customer.modality.desc
        case "isGuest"           ⇒ if(s.asc) customer.isGuest.asc            else customer.isGuest.desc
        case "createdAt"         ⇒ if(s.asc) customer.createdAt.asc          else customer.createdAt.desc
        case other               ⇒ invalidSortColumn(other)
      }
    }


    queryWithMetadata.result.map {
      _.map {
        case (customer, shipRegion, billRegion, rank) ⇒
          build(customer = customer, shippingRegion = shipRegion, billingRegion = billRegion, rank = rank)
      }
    }
  }

  def searchForNewOrder(payload: CustomerSearchForNewOrder)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): ResultWithMetadata[Seq[Root]] = {

    def customersAndNumOrders = {
      val likeQuery = s"%${payload.term}%".toLowerCase
      val query = if (payload.term.contains("@"))
          Customers.filter(_.email.toLowerCase like likeQuery)
        else
          Customers.filter { c ⇒ c.email.toLowerCase.like(likeQuery) || c.name.toLowerCase.like(likeQuery) }

      val withNumOrders = query.joinLeft(Orders).on(_.id === _.customerId).groupBy(_._1.id).map {
        case (id, q) ⇒ (id, q.length)
      }

      for {
        c ← query
        (id, count) ← withNumOrders if id === c.id
      } yield (c, count)
    }

    payload.validate match {
      case Valid(_) ⇒
        customersAndNumOrders.withMetadata.result.map(_.map { case (customer, numOrders) ⇒
          build(customer = customer, numOrders = Some(numOrders))
        })
      case Invalid(errors) ⇒ ResultWithMetadata.fromFailures(errors)
    }
  }

  def getById(id: Int)(implicit db: Database, ec: ExecutionContext): Result[Root] = {
    val query = Customers.filter(_.id === id).withRegionsAndRank
    query.result.headOption.run().flatMap {
      case Some((customer, shipRegion, billRegion, rank)) ⇒
        Result.right(build(customer = customer, shippingRegion = shipRegion, billingRegion = billRegion, rank = rank))
      case _ ⇒
        Result.failure(NotFoundFailure404(Customer, id))
    }
  }

  def create(payload: CreateCustomerPayload)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = {
    val customer = Customer.buildFromPayload(payload)

    def result = {
      swapDatabaseFailure {
        Customers.create(customer).run()
      } { (NotUnique, CustomerEmailNotUnique) }.flatMap {
        case Xor.Right(c) ⇒ Result.good(build(c))
        case Xor.Left(eh) ⇒ Result.failures(eh)
      }
    }

    (for {
      _    ← ResultT.fromXor(customer.validate.toXor)
      root ← ResultT(result)
    } yield root).value
  }

  def update(customerId: Int, payload: UpdateCustomerPayload)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = {

    swapDatabaseFailure {
      (for {
        _ ← * <~ payload.validate.toXor
        customer ← * <~ Customers.mustFindById(customerId, customerNotFound)
        updated ← * <~ Customers.update(customer, customer.copy(
          name = payload.name.fold(customer.name)(Some(_)),
          email = payload.email.getOrElse(customer.email),
          phoneNumber = payload.phoneNumber.fold(customer.phoneNumber)(Some(_))
        ))
      } yield build(updated)).runT()
    } { (NotUnique, CustomerEmailNotUnique) }

  }


  def activate(customerId: Int, payload: ActivateCustomerPayload)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = {

    def result = {
      val finder = Customers.filter(_.id === customerId)
      val result = swapDatabaseFailure {
        finder.selectOneForUpdate { customer ⇒
          finder.map { c ⇒ (c.name, c.isGuest) }
            .updateReturningHead(Customers.map(identity), (Some(payload.name), false))
            .map(_.map(build(_)))
        }
      } { (NotUnique, CustomerEmailNotUnique) }

      result.flatMap(_.fold(Result.failures, Result.good))
    }

    (for {
      _    ← ResultT.fromXor(payload.validate.toXor)
      root ← ResultT(result)
    } yield root).value
  }
}
