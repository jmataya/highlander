package services

import java.time.Instant

import scala.concurrent.{ExecutionContext, Future}

import cats.data.Xor
import cats.implicits._

import com.stripe.model.{Card ⇒ StripeCard, Customer ⇒ StripeCustomer}
import models.OrderPayments.scope._
import models.Orders.scope._
import models.{Region, Regions, Address, Addresses, CreditCard, CreditCards, Customer, OrderPayments, Orders}
import payloads.{CreateAddressPayload, CreateCreditCard, EditCreditCard}

import slick.driver.PostgresDriver.api._
import utils.Apis
import utils.Slick.UpdateReturning._
import utils.Slick.implicits._
import utils.jdbc._

import utils.DbResultT._
import utils.DbResultT.implicits._

import utils.time.JavaTimeSlickMapper.instantAndTimestampWithoutZone

object CreditCardManager {
  private def gateway(implicit ec: ExecutionContext, apis: Apis): Stripe = Stripe()

  type Root = responses.CreditCardsResponse.Root

  def buildResponse(card: CreditCard, region: Region): Root =
    responses.CreditCardsResponse.build(card, region)

  def buildResponses(records: Seq[(CreditCard, Region)]): Seq[Root] =
    records.map((buildResponse _).tupled)

  def createCardThroughGateway(customer: Customer, payload: CreateCreditCard)
    (implicit ec: ExecutionContext, db: Database, apis: Apis): Result[Root] = {

    def saveCardAndAddress(stripeCustomer: StripeCustomer, stripeCard: StripeCard, address: Address):
    ResultT[DBIO[CreditCard]] = {
      val cc = CreditCard.build(customer.id, stripeCustomer, stripeCard, payload, address)
      val saveAddress = if (address.isNew)
        Addresses.saveNew(address.copy(customerId = customer.id))
      else
        DBIO.successful(address)

      ResultT.rightAsync(saveAddress >> CreditCards.saveNew(cc))
    }

    def getExistingStripeIdAndAddress: ResultT[(Option[String], Address)] = {
      val queries = for {
        stripeId  ← CreditCards.filter(_.customerId === customer.id).map(_.gatewayCustomerId).one
        address   ← getAddressFromPayload(payload.addressId, payload.address)
      } yield (stripeId, address)

      ResultT(queries.run().map {
        case (stripeId, Some(address))  ⇒ Xor.right((stripeId, address))
        case (_, None)                  ⇒ Xor.left(CreditCardMustHaveAddress.single)
      })
    }

    val transformer = for {
      _       ← ResultT.fromXor(payload.validate.toXor)
      res     ← getExistingStripeIdAndAddress
      res2    ← res match { case (sId, address) ⇒
        ResultT(gateway.createCard(customer.email, payload, sId, address))
      }
      newCard ← res2 match { case (sCustomer, sCard) ⇒
        saveCardAndAddress(sCustomer, sCard, res._2)
      }
    } yield newCard

    transformer.value.flatMap { res ⇒
      res.fold(
        Result.left,
        _.flatMap { newCard ⇒
          DBIO.successful(newCard).zip(Regions.findOneById(newCard.regionId).safeGet)
        }.transactionally.run().flatMap { case (cc, r) ⇒ Result.good(buildResponse(cc, r)) }
      )
    }
  }

  def toggleCreditCardDefault(customerId: Int, cardId: Int, isDefault: Boolean)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = (for {

    _       ← * <~ CreditCards.findDefaultByCustomerId(customerId).map(_.isDefault).update(false)
    cc      ← * <~ CreditCards.mustFindByIdAndCustomer(cardId, customerId)
    // TODO: please fucking replace me with diffing update
    default = cc.copy(isDefault = true)
    _       ← * <~ CreditCards.filter(_.id === cardId).map(_.isDefault).update(true)
    region  ← * <~ Regions.findOneById(cc.regionId).safeGet.toXor
  } yield buildResponse(default, region)).runT()

  def deleteCreditCard(customerId: Int, id: Int)
    (implicit ec: ExecutionContext, db: Database): Result[Unit] = {

    val updateCc = CreditCards.findById(id).extract
      .filter(_.customerId === customerId)
      .map { cc ⇒ (cc.inWallet, cc.deletedAt) }
      .update((false, Some(Instant.now())))

    db.run(updateCc.map { rows ⇒
      if (rows == 1) Xor.right(Unit) else Xor.left(creditCardNotFound(id))
    })
  }

  def editCreditCard(customerId: Int, id: Int, payload: EditCreditCard)
    (implicit ec: ExecutionContext, db: Database, apis: Apis): Result[Root] = {

    def update(cc: CreditCard): ResultT[DBIO[CreditCard]] = {
      if (!cc.inWallet)
        ResultT.leftAsync(CannotUseInactiveCreditCard(cc).single)
      else {
        val updated = cc.copy(
          parentId = Some(cc.id),
          holderName = payload.holderName.getOrElse(cc.holderName),
          expYear = payload.expYear.getOrElse(cc.expYear),
          expMonth = payload.expMonth.getOrElse(cc.expMonth)
        )

        val newVersion = CreditCards.saveNew(updated)
        val deactivate = CreditCards.findById(cc.id).extract.map(_.inWallet).update(false)

        ResultT(gateway.editCard(updated).map {
          case Xor.Left(f)  ⇒ Xor.left(f)
          case Xor.Right(_) ⇒ Xor.right(deactivate >> newVersion)
        })
      }
    }

    def createNewAddressIfProvided(dbio: DBIO[CreditCard]): ResultT[DBIO[CreditCard]] = ResultT.rightAsync(
      if (payload.address.isDefined)
        dbio.flatMap { cc ⇒
          Addresses.saveNew(Address.fromCreditCard(cc).copy(customerId = customerId)) >> DBIO.successful(cc)
        }
      else
        dbio
    )

    def cascadeChangesToCarts(edits: DBIO[CreditCard]): ResultT[DBIO[CreditCard]] = {
      val action = edits.flatMap { updated ⇒
        val paymentIds = for {
          orders ← Orders.findByCustomerId(customerId).cartOnly
          pmts ← OrderPayments.filter(_.paymentMethodId === updated.parentId).creditCards if pmts.orderId === orders.id
        } yield pmts.id

        OrderPayments.filter(_.id in paymentIds).map(_.paymentMethodId).update(updated.id).map(_ ⇒ updated)
      }

      ResultT.rightAsync(action)
    }

    val getCardAndAddressChange: ResultT[CreditCard] = {
      val queries = for {
        creditCard  ← getCard(customerId, id)
        address     ← getAddressFromPayload(payload.addressId, payload.address)
      } yield (creditCard, address)

      ResultT(queries.run().map {
        case (Some(cc), address)  ⇒ Xor.right(address.fold(cc)(cc.copyFromAddress))
        case (None, _)            ⇒ Xor.left(creditCardNotFound(id))
      })
    }

    val transformer: ResultT[DBIO[CreditCard]] = for {
      _       ← ResultT.fromXor(payload.validate.toXor)
      cc      ← getCardAndAddressChange
      updated ← update(cc)
      withAddress ← createNewAddressIfProvided(updated)
      payment ← cascadeChangesToCarts(withAddress)
    } yield payment

    transformer.value.flatMap { res ⇒
      res.fold(
        Result.left,
        _.flatMap { newCard ⇒
          DBIO.successful(newCard).zip(Regions.findOneById(newCard.regionId).safeGet)
        }.transactionally.run().flatMap { case (cc, r) ⇒ Result.good(buildResponse(cc, r)) }
      )
    }
  }

  def creditCardsInWalletFor(customerId: Int)
    (implicit ec: ExecutionContext, db: Database): Future[Seq[Root]] = (for {
    cc      ← CreditCards.findInWalletByCustomerId(customerId)
    region  ← cc.region
  } yield (cc, region)).result.map(buildResponses).run()

  private def getCard(customerId: Int, id: Int)
    (implicit ec: ExecutionContext, db: Database): DBIO[Option[CreditCard]] =
    CreditCards.findById(id).extract.filter(_.customerId === customerId).one

  private def getAddressFromPayload(id: Option[Int], payload: Option[CreateAddressPayload]): DBIO[Option[Address]] = {
    (id, payload) match {
      case (Some(addressId), _) ⇒
        Addresses.findById(addressId).extract.one

      case (_, Some(createAddress)) ⇒
        DBIO.successful(Address.fromPayload(createAddress).some)

      case _ ⇒
        DBIO.successful(None)
    }
  }

  private def creditCardNotFound(id: Int) = NotFoundFailure404(CreditCard, id).single
}
