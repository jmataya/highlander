import akka.http.scaladsl.model.StatusCodes.ClientError
import akka.http.scaladsl.model.{ContentTypes, ErrorInfo, IllegalRequestException, StatusCodes}
import akka.http.scaladsl.server.Directives._
import testutils._

import scala.collection.immutable

class JsonExceptionHandlerIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with DefaultJwtAdminAuth {

  val illegalRequestExceptionText = "A test IllegalRequestException"
  val exceptionText               = "A test exception"

  override protected def additionalRoutes = immutable.Seq(
    path("testThrowAnExcepton") {
      complete(throw new Exception(exceptionText))
    },
    path("testThrowAnIllegalRequestException") {
      complete(
        throw new IllegalRequestException(new ErrorInfo(illegalRequestExceptionText),
                                          StatusCodes.custom(400, "test").asInstanceOf[ClientError]))
    }
  )

  "return a valid JSON exception on an IllegalRequestException" in {
    val response = GET("testThrowAnIllegalRequestException", jwtCookie = None)

    response.mustHaveStatus(StatusCodes.BadRequest)
    response.entity.contentType must === (ContentTypes.`application/json`)
    response.error must startWith(illegalRequestExceptionText)
  }

  "return a valid JSON exception on an other exception" in {
    val response = GET("testThrowAnExcepton", jwtCookie = None)

    response.mustHaveStatus(StatusCodes.InternalServerError)
    response.entity.contentType must === (ContentTypes.`application/json`)
    response.error must startWith(exceptionText)
  }
}
