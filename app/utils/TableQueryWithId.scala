package utils

import monocle.Lens
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import Strings._

import scala.concurrent.ExecutionContext

trait Model {
  def modelName: String = getClass.getCanonicalName.lowerCaseFirstLetter
}

trait ModelWithIdParameter extends Model {
  type Id = Int

  def id: Id
}

trait TableWithIdColumn[I] {
  def id: Rep[I]
}

private[utils] abstract class TableWithIdInternal[M <: ModelWithIdParameter, I](tag: Tag, name: String)
  extends Table[M](tag, name) with TableWithIdColumn[I]

object GenericTable {
  /** This allows us to enforce that tables have the same ID column as their case class. */
  type TableWithId[MODEL <: ModelWithIdParameter] = TableWithIdInternal[MODEL, MODEL#Id]
}

abstract class TableQueryWithId[M <: ModelWithIdParameter, T <: GenericTable.TableWithId[M]]
  (idLens: Lens[M, M#Id])
  (construct: Tag ⇒ T)
  (implicit ev: BaseTypedType[M#Id]) extends TableQuery[T](construct) {

  val returningId =
    this.returning(map(_.id))

  private val compiledById = this.findBy(_.id)

  def _findById(i: M#Id) = compiledById(i)

  def findById(i: M#Id): DBIO[Option[M]] =
    _findById(i).result.headOption

  def save(model: M)(implicit ec: ExecutionContext): DBIO[M] = for {
    id ← returningId += model
  } yield idLens.set(id)(model)

  def deleteById(i: M#Id): DBIO[Int] =
    _findById(i).delete
}

