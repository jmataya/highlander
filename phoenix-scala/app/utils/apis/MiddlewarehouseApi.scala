package utils.apis

import cats.implicits._
import com.ning.http.client
import com.typesafe.scalalogging.LazyLogging
import dispatch._
import failures.MiddlewarehouseFailures.MiddlewarehouseError
import failures.{Failures, MiddlewarehouseFailures}
import io.circe.generic.semiauto._
import io.circe.jackson.syntax._
import io.circe.parser.parse
import io.circe.syntax._
import io.circe.{Encoder, Json}
import payloads.AuthPayload
import utils.aliases._
import utils.db._

case class SkuInventoryHold(sku: String, qty: Int)
case class OrderInventoryHold(refNum: String, items: Seq[SkuInventoryHold])
object OrderInventoryHold {
  implicit val encode: Encoder[OrderInventoryHold] = deriveEncoder[OrderInventoryHold]
}

trait MiddlewarehouseApi {

  def hold(reservation: OrderInventoryHold)(implicit ec: EC, au: AU): Result[Unit]
  def cancelHold(orderRefNum: String)(implicit ec: EC, au: AU): Result[Unit]

}

class Middlewarehouse(url: String) extends MiddlewarehouseApi with LazyLogging {

  def parseMwhErrors(message: String): Failures = {
    val errorString =
      parse(message).getOrElse(Json.Null).hcursor.downField("errors").as[List[String]]
    errorString.toOption
      .flatMap(errors ⇒ Failures(errors.map(MiddlewarehouseError): _*))
      .getOrElse(MiddlewarehouseError(message).single)
  }

  override def hold(reservation: OrderInventoryHold)(implicit ec: EC, au: AU): Result[Unit] = {

    val reqUrl = dispatch.url(s"$url/v1/private/reservations/hold")
    val body   = reservation.asJson.jacksonPrint
    val jwt    = AuthPayload.jwt(au.token)
    val req    = reqUrl.setContentType("application/json", "UTF-8") <:< Map("JWT" → jwt) << body
    logger.info(s"middlewarehouse hold: $body")

    val f = Http(req.POST > AsMwhResponse).either.map {
      case Right(MwhResponse(status, _)) if status / 100 == 2 ⇒ Either.right(())
      case Right(MwhResponse(_, message))                     ⇒ Either.left(parseMwhErrors(message))
      case Left(_)                                            ⇒ Either.left(MiddlewarehouseFailures.UnableToHoldLineItems.single)
    }
    Result.fromFEither(f)
  }

  //Note cart ref becomes order ref num after cart turns into order
  override def cancelHold(orderRefNum: String)(implicit ec: EC, au: AU): Result[Unit] = {

    val reqUrl = dispatch.url(s"$url/v1/private/reservations/hold/${orderRefNum}")
    val jwt    = AuthPayload.jwt(au.token)
    val req    = reqUrl.setContentType("application/json", "UTF-8") <:< Map("JWT" → jwt)
    logger.info(s"middlewarehouse cancel hold: $orderRefNum")
    val f = Http(req.DELETE OK as.String).either.map {
      case Right(_)    ⇒ Either.right(())
      case Left(error) ⇒ Either.left(MiddlewarehouseFailures.UnableToCancelHoldLineItems.single)
    }
    Result.fromFEither(f)
  }
}

case class MwhResponse(statusCode: Int, content: String)

object AsMwhResponse extends (client.Response ⇒ MwhResponse) {

  override def apply(r: client.Response): MwhResponse =
    MwhResponse(r.getStatusCode, r.getResponseBody)

}
