package models

import utils.{Model, RichTable}

import com.wix.accord.dsl.{validator => createValidator}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}

import com.wix.accord.{Failure => ValidationFailure, Validator}
import com.wix.accord.dsl._
import scala.concurrent.{ExecutionContext, Future}

final case class Adjustment(id: Int, amount: Int, sourceId: Int, sourceType: String, reason: String) extends Model

