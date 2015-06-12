package models

import utils.{Validation, RichTable}
import payloads.CreateAddressPayload

import com.wix.accord.dsl.{validator => createValidator}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import org.scalactic._
import com.wix.accord.{Failure => ValidationFailure, Validator}
import com.wix.accord.dsl._
import scala.concurrent.{ExecutionContext, Future}

case class Address(id: Int = 0, accountId: Int, stateId: Int, name: String, street1: String, street2: Option[String],
                   city: String, zip: String) extends Validation {
  override def validator[T] = {
    createValidator[Address] { address =>
      address.name is notEmpty
      address.street1 is notEmpty
      address.city is notEmpty
      address.zip should matchRegex("[0-9]{5}")
    }
  }.asInstanceOf[Validator[T]] // TODO: fix me
}

class Addresses(tag: Tag) extends Table[Address](tag, "addresses") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def accountId = column[Int]("account_id")
  def stateId = column[Int]("state_id")
  def name = column[String]("name")
  def street1 = column[String]("street1")
  def street2 = column[Option[String]]("street2")
  def city = column[String]("city")
  def zip = column[String]("zip")

  def * = (id, accountId, stateId, name, street1, street2, city, zip) <> ((Address.apply _).tupled, Address.unapply)

  def state = foreignKey("addresses_state_id_fk", stateId, TableQuery[States])(_.id)
}

object Addresses {
  val table = TableQuery[Addresses]
  val returningId = table.returning(table.map(_.id))

  def findAllByAccount(account: User)(implicit db: Database): Future[Seq[Address]] = {
    db.run(table.filter(_.accountId === account.id).result)
  }

  def findById(db: Database, id: Int): Future[Option[Address]] = {
    db.run(table.filter(_.id === id).result.headOption)
  }

  def createFromPayload(account: User,
                        payload: Seq[CreateAddressPayload])
                       (implicit ec: ExecutionContext,
                        db: Database): Future[Seq[Address] Or Map[Address, Set[ErrorMessage]]] = {

    val addresses = payload.map { a =>
      Address(id = 0, accountId = account.id, stateId = a.stateId, name = a.name,
              street1 = a.street1, street2 = a.street2, city = a.city, zip = a.zip)
    }

    create(account, addresses)
  }

  def create(account: User, addresses: Seq[Address])
            (implicit ec: ExecutionContext,
             db: Database): Future[Seq[Address] Or Map[Address, Set[ErrorMessage]]] = {

    val validatedAddresses = addresses.map { a => (a, a.validate) }
    val failures = validatedAddresses.filter { case (_, result) => result.isInstanceOf[ValidationFailure] }

    if (failures.nonEmpty) {
      val acc = Map[Address, Set[ErrorMessage]]()
      val errorMap = failures.foldLeft(acc) { case (map, (address, failure: ValidationFailure)) =>
        map.updated(address, Validation.validationFailureToSet(failure))
      }
      Future.successful(Bad(errorMap))
    } else {
      db.run(for {
        _ <- table ++= addresses
        addresses <- table.filter(_.accountId === account.id).result
      } yield Good(addresses))
    }
  }
}