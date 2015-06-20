package models

import utils.{GenericTable, Validation, TableQueryWithId, ModelWithIdParameter, RichTable}

import com.wix.accord.dsl.{validator => createValidator}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import org.scalactic._
import com.wix.accord.{Failure => ValidationFailure, Validator}
import com.wix.accord.dsl._
import scala.concurrent.{ExecutionContext, Future}


case class OrderWeightCriterion(id:Int = 0, greaterThan: Int, lessThan: Int, exactMatch: Int, unitOfMeasure: String, exclude: Boolean) extends ModelWithIdParameter

class OrderWeightCriteria(tag: Tag) extends GenericTable.TableWithId[OrderWeightCriterion](tag, "shipping_methods") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def greaterThan = column[Int]("greater_than")
  def lessThan = column[Int]("less_than")
  def exactMatch = column[Int]("exact_match") // Doesn't seem likely that anyone would use this.  But the pattern applies..
  def UnitOfMeasure = column[String]("unit_of_measure") // pounds, kg, etc.  Should this be an int?  How do we handle enumerables?
  def exclude = column[Boolean]("exclude") // Is this an inclusion or exclusion rule?

  def * = (id, greaterThan, lessThan, exactMatch, unitOfMeasure, exclude) <> ((OrderWeightCriterion.apply _).tupled, OrderWeightCriterion.unapply)
}

object OrderWeightCriteria extends TableQueryWithId[OrderWeightCriterion, OrderWeightCriteria](
  idLens = GenLens[OrderWeightCriteria](_.id)
)(new OrderWeightCriteria(_))