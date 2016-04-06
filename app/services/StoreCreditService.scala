package services

import scala.concurrent.Future

import cats.implicits._
import models.payment.storecredit._
import StoreCredit.Canceled
import StoreCreditSubtypes.scope._
import failures.{NotFoundFailure400, NotFoundFailure404, OpenTransactionsFailure}
import models.customer.{Customer, Customers}
import models.{Reason, Reasons, StoreAdmin}
import payloads.{StoreCreditBulkUpdateStateByCsr, StoreCreditUpdateStateByCsr}
import responses.StoreCreditBulkResponse._
import responses.StoreCreditResponse._
import responses.{StoreCreditResponse, StoreCreditSubTypesResponse, TheResponse}
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.DbResultT
import utils.Slick._
import utils.Slick.implicits._
import utils.DbResultT.implicits._
import utils.DbResultT._
import utils.aliases._

object StoreCreditService {
  type QuerySeq = StoreCredits.QuerySeq

  def getOriginTypes(implicit ec: EC, db: DB): Result[Seq[StoreCreditSubTypesResponse.Root]] = {
    val types = StoreCredit.OriginType.types.--(Seq(StoreCredit.Loyalty, StoreCredit.Custom))
    Result.fromFuture(StoreCreditSubtypes.result.map { subTypes ⇒
      StoreCreditSubTypesResponse.build(types.toSeq, subTypes)
    }.run())
  }

  private def checkSubTypeExists(subTypeId: Option[Int],
    originType: StoreCredit.OriginType)(implicit ec: EC): DbResult[Unit] = {
    subTypeId.fold(DbResult.unit) { subtypeId ⇒
      StoreCreditSubtypes.byOriginType(originType).filter(_.id === subtypeId).one.flatMap(_.fold {
        DbResult.failure[Unit](NotFoundFailure400(StoreCreditSubtype, subtypeId))
      } { _ ⇒ DbResult.unit })
    }
  }

  def findAllByCustomer(customerId: Int)(implicit ec: EC, db: DB, sp: SortAndPage): Result[TheResponse[WithTotals]] = (for {

    _           ← * <~ Customers.mustFindById404(customerId)
    query       = StoreCredits.findAllByCustomerId(customerId)
    paginated   = StoreCredits.sortedAndPaged(query)

    sc          ← * <~ query.result.map(StoreCreditResponse.build).toXor
    totals      ← * <~ fetchTotalsForCustomer(customerId).toXor
    withTotals  = WithTotals(storeCredits = sc, totals = totals)
    response    ← * <~ ResultWithMetadata(result = DbResult.good(withTotals), metadata = paginated.metadata).toTheResponse

  } yield response).run()

  def totalsForCustomer(customerId: Int)(implicit ec: EC, db: DB): Result[StoreCreditResponse.Totals] = (for {
    _       ← * <~ Customers.mustFindById404(customerId)
    totals  ← * <~ fetchTotalsForCustomer(customerId).toXor
  } yield totals).map(_.getOrElse(Totals(0, 0))).value.run()

  def fetchTotalsForCustomer(customerId: Int)(implicit ec: EC, db: DB): DBIO[Option[Totals]] = {
    StoreCredits.findAllActiveByCustomerId(customerId)
      .groupBy(_.customerId)
      .map { case (_, q) ⇒ (q.map(_.availableBalance).sum, q.map(_.currentBalance).sum) }
      .one
      .map(_.map { case (avail, curr) ⇒ StoreCreditResponse.Totals(avail.getOrElse(0), curr.getOrElse(0)) })
  }

  def createManual(admin: StoreAdmin, customerId: Int, payload: payloads.CreateManualStoreCredit)
    (implicit ec: EC, db: DB, ac: AC): Result[Root] = (for {
    customer ← * <~ Customers.mustFindById404(customerId)
    _ ← * <~ Reasons.findById(payload.reasonId).extract.one.mustFindOr(NotFoundFailure400(Reason, payload.reasonId))
    // Check subtype only if id is present in payload; discard actual model
    _ ← * <~ checkSubTypeExists(payload.subTypeId, StoreCredit.CsrAppeasement)
    manual = StoreCreditManual(adminId = admin.id, reasonId = payload.reasonId, subReasonId = payload.subReasonId)
    origin ← * <~ StoreCreditManuals.create(manual)
    appeasement = StoreCredit.buildAppeasement(customerId = customer.id, originId = origin.id, payload = payload)
    storeCredit ← * <~ StoreCredits.create(appeasement)
    _ ← * <~ LogActivity.scCreated(admin, customer, storeCredit)
  } yield build(storeCredit)).runTxn

  def createFromExtension(admin: StoreAdmin, customerId: Int, payload: payloads.CreateExtensionStoreCredit)
    (implicit ec: EC, db: DB, ac: AC): Result[Root] = (for {
    customer ← * <~ Customers.mustFindById404(customerId)
    // Check subtype only if id is present in payload; discard actual model
    _ ← * <~ checkSubTypeExists(payload.subTypeId, StoreCredit.Custom)
    // TODO: fixme originType
    customStoreCredit = StoreCredit.buildFromExtension(customerId = customer.id, payload = payload,
      originType = StoreCredit.Loyalty)
    storeCredit ← * <~ StoreCredits.create(customStoreCredit)
    _ ← * <~ LogActivity.scCreated(admin, customer, storeCredit)
  } yield build(storeCredit)).runTxn

  def getById(id: Int)(implicit ec: EC, db: DB): Result[Root] = (for {
    storeCredit ← * <~ StoreCredits.mustFindById404(id)
  } yield StoreCreditResponse.build(storeCredit)).run()

  def getByIdAndCustomer(storeCreditId: Int, customer: Customer)(implicit ec: EC, db: DB): Result[Root] = (for {
    storeCredit ← * <~ StoreCredits.findByIdAndCustomerId(storeCreditId, customer.id)
                                   .mustFindOr(NotFoundFailure404(StoreCredit, storeCreditId))
  } yield StoreCreditResponse.build(storeCredit)).run()

  def bulkUpdateStateByCsr(payload: StoreCreditBulkUpdateStateByCsr, admin: StoreAdmin)
    (implicit ec: EC, db: DB, ac: AC): Result[Seq[ItemResult]] = (for {
    _        ← ResultT.fromXor(payload.validate.toXor)
    response ← ResultT.right(Future.sequence(payload.ids.map { id ⇒
                 val itemPayload = StoreCreditUpdateStateByCsr(payload.state, payload.reasonId)
                 updateStateByCsr(id, itemPayload, admin).map(buildItemResult(id, _))
               }))
  } yield response).value

  def updateStateByCsr(id: Int, payload: StoreCreditUpdateStateByCsr, admin: StoreAdmin)
    (implicit ec: EC, db: DB, ac: AC): Result[Root] = (for {
    _           ← * <~ payload.validate
    storeCredit ← * <~ StoreCredits.mustFindById404(id)
    updated     ← * <~ cancelOrUpdate(storeCredit, payload.state, payload.reasonId, admin)
    _           ← * <~ LogActivity.scUpdated(admin, storeCredit, payload)
  } yield StoreCreditResponse.build(updated)).runTxn()

  private def cancelOrUpdate(storeCredit: StoreCredit, newState: StoreCredit.State, reasonId: Option[Int],
    admin: StoreAdmin)(implicit ec: EC, db: DB) = newState match {
    case Canceled ⇒ for {
      _   ← * <~ StoreCreditAdjustments.lastAuthByStoreCreditId(storeCredit.id).one.mustNotFindOr(OpenTransactionsFailure)
      _   ← * <~ reasonId.map(id ⇒ Reasons.mustFindById400(id)).getOrElse(DbResult.unit)
      upd ← * <~ StoreCredits.update(storeCredit, storeCredit.copy(state = newState, canceledReason = reasonId,
                   canceledAmount = storeCredit.availableBalance.some))
      _   ← * <~ StoreCredits.cancelByCsr(storeCredit, admin)
    } yield upd

    case _ ⇒ DbResultT(StoreCredits.update(storeCredit, storeCredit.copy(state = newState)))
  }
}
