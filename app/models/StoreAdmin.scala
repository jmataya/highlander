package models

import cats.data.ValidatedNel
import cats.implicits._
import utils.Litterbox._
import utils.Checks

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import utils.{TableQueryWithId, GenericTable, ModelWithIdParameter, RichTable}

import scala.concurrent.{ExecutionContext, Future}

final case class StoreAdmin(id: Int = 0, email: String, password: String,
                      firstName: String, lastName: String,
                      department: Option[String] = None)
  extends ModelWithIdParameter {

  def validateNew: ValidatedNel[String, StoreAdmin] = {
    ( Checks.notEmpty(firstName, "firstName")
      |@| Checks.notEmpty(lastName, "lastName")
      |@| Checks.notEmpty(email, "email")
      ).map { case _ ⇒ this }
  }
}

class StoreAdmins(tag: Tag) extends GenericTable.TableWithId[StoreAdmin](tag, "store_admins") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def email = column[String]("email")
  def password = column[String]("hashed_password")
  def firstName = column[String]("first_name")
  def lastName = column[String]("last_name")
  def department = column[Option[String]]("department")

  def * = (id, email, password, firstName, lastName, department) <> ((StoreAdmin.apply _).tupled, StoreAdmin.unapply)
}

object StoreAdmins extends TableQueryWithId[StoreAdmin, StoreAdmins](
  idLens = GenLens[StoreAdmin](_.id)
)(new StoreAdmins(_)){

  def findByEmail(email: String)(implicit ec: ExecutionContext, db: Database): Future[Option[StoreAdmin]] = {
    db.run(filter(_.email === email).result.headOption)
  }

  def findById(id: Int)(implicit db: Database): Future[Option[StoreAdmin]] = {
    db.run(_findById(id).result.headOption)
  }

  def _findById(id: Rep[Int]) = { filter(_.id === id) }
}
