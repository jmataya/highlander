package consumer.activity

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings.FieldType._

import consumer.AvroJsonHelper
import consumer.elastic.AvroTransformer

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.language.postfixOps

import akka.actor.ActorSystem
import akka.http.ConnectionPoolSettings
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.Authorization
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.model.{ContentTypes, HttpMethods, HttpRequest, HttpResponse}
import akka.util.ByteString
import akka.stream.{ActorMaterializer, Materializer}

import org.json4s.JsonAST.JInt
import org.json4s.jackson.JsonMethods.parse
import org.json4s.DefaultFormats

import consumer.elastic.JsonTransformer
import consumer.elastic.AvroTransformers

import consumer.utils.PhoenixConnectionInfo
import consumer.utils.Phoenix
import consumer.utils.HttpResponseExtensions._
import akka.http.scaladsl.model.StatusCodes


final case class ActivityConnectionTransformer(conn: PhoenixConnectionInfo)
(implicit ec:ExecutionContext, mat: Materializer, ac: ActorSystem, cp: ConnectionPoolSettings) extends JsonTransformer { 

  implicit val formats: DefaultFormats.type = DefaultFormats

  val phoenix = Phoenix(conn)

  def mapping = "activity_connections" as (
        "id"                   typed IntegerType,
        "dimensionId"          typed IntegerType,
        "objectId"             typed StringType index "not_analyzed",
        "trailId"              typed IntegerType,
        "activity" nested(
          "id"                 typed IntegerType,
          "createdAt"          typed DateType format AvroTransformers.strictDateFormat,
          "kind"               typed StringType index "not_analyzed",
          "context" nested(
            "transactionId"    typed StringType index "not_analyzed",
            "userId"           typed IntegerType,
            "userType"         typed StringType index "not_analyzed"
            ),
          "data"               typed ObjectType
          ),
        "previousId"           typed IntegerType,
        "nextId"               typed IntegerType,
        "data"                 typed ObjectType,
        "connectedBy"          typed ObjectType,
        "createdAt"            typed DateType format AvroTransformers.strictDateFormat)

  def transform(json: String) : Future[String] = {

    Console.out.println(json)

    parse(json) \ "id" \ "int" match {
      case JInt(id) ⇒ queryPhoenixForConnection(id)
      case _ ⇒  throw new IllegalArgumentException("Activity connection is missing id")
    }
  }

  private def queryPhoenixForConnection(id: BigInt) : Future[String] = {
    val uri = s"connections/$id"
    Console.err.println(s"Requesting Phoenix $uri")

    phoenix.get(uri).flatMap { _.bodyText}
  }

}
