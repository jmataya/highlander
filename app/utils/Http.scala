package utils

import scala.concurrent.{ExecutionContext, Future}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, ResponseEntity, HttpResponse, StatusCode}

import cats.data.Xor
import models.{Customer, Order, Orders}
import org.json4s.{Formats, jackson}
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.{write ⇒ json}
import services.{Failures, NotFoundFailure, OrderLockedFailure}
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._

object Http {
  import utils.JsonFormatters._

  implicit lazy val serialization: Serialization.type = jackson.Serialization
  implicit lazy val formats:       Formats = phoenixFormats

  val notFoundResponse:   HttpResponse  = HttpResponse(NotFound)
  val noContentResponse:  HttpResponse  = HttpResponse(NoContent)

  def renderGoodOrFailures[G <: AnyRef](or: Failures Xor G)
                                       (implicit ec: ExecutionContext): HttpResponse =
    or.fold(renderFailure(_), render(_))

  def renderNothingOrFailures(or: Failures Xor _)(implicit ec: ExecutionContext): HttpResponse =
    or.fold(renderFailure(_), _ ⇒ noContentResponse)

  def whenFound[A, G <: AnyRef](finder: Future[Option[A]])
    (handle: A ⇒ Future[Failures Xor G])
    (implicit ec: ExecutionContext, db: Database): Future[HttpResponse] =
    finder.flatMap { option ⇒
      option.map(handle(_).map(renderGoodOrFailures)).
        getOrElse(Future.successful(notFoundResponse))
    }

  def whenFoundDispatchToService[A, G <: AnyRef](finder: ⇒ Future[Option[A]])
                                                (bind:   A ⇒ Future[Failures Xor G])
                                                (implicit ec: ExecutionContext): Future[HttpResponse] = {
    finder.flatMap {
      case None    ⇒ Future.successful(notFoundResponse)
      case Some(v) ⇒ bind(v).map(renderGoodOrFailures)
    }
  }

  def whenOrderFoundAndEditable[G <: AnyRef](finder: Future[Option[Order]])
                                            (f: Order ⇒ Future[Failures Xor G])
                                            (implicit ec: ExecutionContext, db: Database): Future[HttpResponse] = {

    finder.flatMap {
      case Some(order) if !order.locked ⇒
        f(order).map(renderGoodOrFailures)
      case Some(order) if order.locked ⇒
        Future.successful(renderFailure(services.Failures(OrderLockedFailure(order.referenceNumber))))
      case None ⇒
        Future.successful(notFoundResponse)
    }
  }

  def whenOrderFoundAndEditable[G <: AnyRef](customer: Customer)
                                            (f: Order ⇒ Future[Failures Xor G])
                                            (implicit ec: ExecutionContext, db: Database): Future[HttpResponse] = {

    val finder = Orders._findActiveOrderByCustomer(customer).one.run()
    whenOrderFoundAndEditable(finder)(f)
  }

  def whenOrderFoundAndEditable[G <: AnyRef](refNumber: String)
                                            (f: Order ⇒ Future[Failures Xor G])
                                            (implicit ec: ExecutionContext, db: Database): Future[HttpResponse] = {

    val finder = Orders.findByRefNum(refNumber).one.run()
    whenOrderFoundAndEditable(finder)(f)
  }

  def renderOrNotFound[A <: AnyRef](resource: Future[Option[A]],
    onFound: (A ⇒ HttpResponse) = (r: A) => render(r))(implicit ec: ExecutionContext) = {
    resource.map {
      case Some(r) => onFound(r)
      case None => notFoundResponse
    }
  }

  def renderOrNotFound[A <: AnyRef](resource: Option[A])(implicit ec: ExecutionContext): HttpResponse =
    resource.fold(notFoundResponse)(render(_))

  def renderNotFoundFailure(f: NotFoundFailure): HttpResponse =
    notFoundResponse.copy(entity = jsonEntity("errors" → Seq(f.message)))

  def render[A <: AnyRef](resource: A, statusCode: StatusCode = OK) =
    HttpResponse(statusCode, entity = jsonEntity(resource))

  def renderFailure(failures: Failures, statusCode: ClientError = BadRequest): HttpResponse = {
    import services._
    val failuresList = failures.toList
    val notFound = failuresList.collectFirst { case f: NotFoundFailure ⇒ f }
    notFound.fold(HttpResponse(statusCode, entity = jsonEntity("errors" → failuresList.flatMap(_.description)))) { nf ⇒
      renderNotFoundFailure(nf)
    }
  }

  def jsonEntity[A <: AnyRef](resource: A): ResponseEntity = HttpEntity(ContentTypes.`application/json`,
    json(resource))
}
