package services

import models.customer.{Customer, Customers}
import models.inventory.{SkuShadows, Skus}
import models.product.{ProductContext, ProductShadows, Products}
import models.{SaveForLater, SaveForLaters}
import responses.{SaveForLaterResponse, TheResponse}
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.DbResult
import utils.Slick.implicits._
import utils.aliases._
import cats.data.Xor
import scala.concurrent.ExecutionContext

import failures.{AlreadySavedForLater, Failures, NotFoundFailure404}
import failures.ProductFailures.SkuNotFoundForContext
import slick.driver.PostgresDriver.api._

object SaveForLaterManager {

  type SavedForLater = TheResponse[Seq[SaveForLaterResponse.Root]]

  def findAll(customerId: Int, productContextId: Int )(implicit db: DB, ec: EC): Result[SavedForLater] = (for {
    customer ← * <~ Customers.mustFindById404(customerId)
    response ← * <~ findAllDbio(customer, productContextId).toXor
  } yield response).run()

  def saveForLater(customerId: Int, skuCode: String, productContext: ProductContext)
    (implicit db: DB, ec: EC): Result[SavedForLater] = (for {
    customer ← * <~ Customers.mustFindById404(customerId)
    sku ← * <~ Skus.mustFindByCode(skuCode)
    skuShadow ← * <~ SkuShadows.filter(_.skuId === sku.id).filter(_.productContextId === productContext.id).one
                .mustFindOr(SkuNotFoundForContext(sku.code, productContext.name))
    _   ← * <~ SaveForLaters.find(customerId = customer.id, skuId = sku.id).one
                 .mustNotFindOr(AlreadySavedForLater(customerId = customer.id, skuId = sku.id))
    _   ← * <~ SaveForLaters.create(SaveForLater(customerId = customer.id, 
                skuId = sku.id, skuShadowId = skuShadow.id))
    response ← * <~ findAllDbio(customer, productContext.id).toXor
  } yield response).runTxn()

  def deleteSaveForLater(id: Int)(implicit ec: EC, db: DB): Result[Unit] =
    SaveForLaters.deleteById(id, DbResult.unit, i ⇒ NotFoundFailure404(SaveForLater, i)).run()

  private def findAllDbio(customer: Customer, productContextId: Int)(implicit ec: EC, db: DB): DBIO[SavedForLater] = for {
    sfls ← SaveForLaters.filter(_.customerId === customer.id).result
    xors ← DBIO.sequence(sfls.map(_.skuId).map(skuId ⇒ SaveForLaterResponse.forSkuId(skuId, productContextId).value))

    fails = xors.collect { case Xor.Left(f) ⇒ f }.flatMap(_.toList)
    roots = xors.collect { case Xor.Right(r) ⇒ r }
  } yield TheResponse.build(roots, warnings = Failures(fails: _*))
}
