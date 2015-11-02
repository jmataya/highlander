package services

import scala.concurrent.ExecutionContext

import models._
import responses.RmaResponse._
import responses.StoreAdminResponse
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.Slick._
import utils.Slick.DbResult
import utils.Slick.implicits._

object RmaService {
  def getByRefNum(refNum: String)(implicit db: Database, ec: ExecutionContext): Result[Root] = {
    val finder = Rmas.findByRefNum(refNum)
    finder.selectOne(rma ⇒ DbResult.fromDbio(fromRma(rma)))
  }

  def getExpandedByRefNum(refNum: String)(implicit db: Database, ec: ExecutionContext): Result[RootExpanded] = {
    val finder = Rmas.findByRefNum(refNum)
    finder.selectOne(rma ⇒ DbResult.fromDbio(fromRmaExpanded(rma)))
  }

  def findAll(admin: StoreAdmin)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): ResultWithMetadata[Seq[Root]] = {
    val mockAdmin = Some(StoreAdminResponse.build(admin))
    Rmas.queryAll.result.map(_.map(build(_, None, mockAdmin)))
  }

  def findByOrderRef(admin: StoreAdmin, refNum: String)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): ResultWithMetadata[Seq[Root]] = {

    //val finder = Orders.findByRefNum(refNum)
    //finder.selectOneWithMetaData(_ ⇒ DbResult.good(Rmas.queryByOrderRefNum(refNum).result.map(_.map(build(_)))))

    // TODO: Replace with upper when `selectOneWithMetaData` will be merged
    val mockAdmin = Some(StoreAdminResponse.build(admin))
    Rmas.queryByOrderRefNum(refNum).result.map(_.map(build(_, None, mockAdmin)))
  }

  def findByCustomerId(admin: StoreAdmin, customerId: Int)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): ResultWithMetadata[Seq[Root]] = {

    //val finder = Customers.filter(_.id === customerId)
    //finder.selectOneWithMetaData(_ ⇒ DbResult.good(Rmas.queryByCustomerId(customerId).result.map(_.map(build(_)))))

    // TODO: Replace with upper when `selectOneWithMetaData` will be merged
    val mockAdmin = Some(StoreAdminResponse.build(admin))
    Rmas.queryByCustomerId(customerId).result.map(_.map(build(_, None, mockAdmin)))
  }
}