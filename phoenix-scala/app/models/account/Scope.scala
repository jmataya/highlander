package models.account

import java.time.Instant

import cats.data.{Validated, ValidatedNel, Xor}
import cats.implicits._
import failures._
import shapeless._
import utils.Validation
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

case class Scope(id: Int = 0, source: String, parentPath: String) extends FoxModel[Scope] {
  def path = s"$parentPath.$id"
}

class Scopes(tag: Tag) extends FoxTable[Scope](tag, "scopes") {
  def id         = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def source     = column[String]("source")
  def parentPath = column[String]("parent_path")

  def * =
    (id, source, parentPath) <> ((Scope.apply _).tupled, Scope.unapply)
}

object Scopes extends FoxTableQuery[Scope, Scopes](new Scopes(_)) with ReturningId[Scope, Scopes] {

  val returningLens: Lens[Scope, Int] = lens[Scope].id
}
