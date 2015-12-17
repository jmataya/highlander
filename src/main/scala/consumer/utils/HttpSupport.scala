package consumer.utils

import scala.concurrent.Await.result
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._

import cats.std.future._

import akka.actor.ActorSystem
import akka.http.ConnectionPoolSettings
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.Authorization
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse}
import akka.util.ByteString
import akka.stream.{ActorMaterializer, Materializer}

import consumer.JsonProcessor
import consumer.AvroJsonHelper

import org.json4s.Formats

import scala.language.postfixOps

final case class PhoenixConnectionInfo(
  uri: String,
  user: String,
  pass: String)

case class Phoenix(conn: PhoenixConnectionInfo)
  (implicit ec: ExecutionContext, ac: ActorSystem, mat: Materializer, cp: ConnectionPoolSettings) {

    def get(suffix: String) : Future[HttpResponse] = { 
      val uri  = fullUri(suffix)
      val request = HttpRequest(HttpMethods.GET,uri).addHeader(
        Authorization(BasicHttpCredentials(conn.user, conn.pass)))

      Http().singleRequest(request, cp)
    }

    def post(suffix: String, body: String) : Future[HttpResponse]= { 
      val uri  = fullUri(suffix)
      val request = HttpRequest(
        method = HttpMethods.POST,
        uri    = uri,
        entity = HttpEntity.Strict(
          ContentTypes.`application/json`,
          ByteString(body)
        )).addHeader(Authorization(BasicHttpCredentials(conn.user, conn.pass)))

      Http().singleRequest(request, cp)
    }

    private def fullUri(suffix: String) = s"${conn.uri}/${suffix}"
}

/**
 * Stolen from phoenix
 */
object HttpResponseExtensions {
  implicit class RichHttpResponse(val res: HttpResponse) extends AnyVal {
    import org.json4s.jackson.JsonMethods._

    def bodyText(implicit ec: ExecutionContext, mat: Materializer): String =
      result(res.entity.toStrict(10 seconds).map(_.data.utf8String), 1 minute)

    def as[A <: AnyRef]
    (implicit ec:ExecutionContext, 
      fm: Formats, 
      mf: scala.reflect.Manifest[A], 
      mat: Materializer): A =
      parse(bodyText).extract[A]
  }
}
