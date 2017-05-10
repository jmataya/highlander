package server

import akka.actor.{ActorSystem, Props}
import akka.agent.Agent
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RouteResult._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import com.stripe.Stripe
import com.typesafe.scalalogging.LazyLogging
import models.account.{AccountAccessMethod, Scope, Scopes}
import org.json4s._
import org.json4s.jackson._
import services.Authenticator
import services.Authenticator.{UserAuthenticator, requireAdminAuth}
import services.account.AccountCreateContext
import services.actors._
import slick.jdbc.PostgresProfile.api._
import utils.FoxConfig.config
import utils.apis._
import utils.db._
import utils.http.CustomHandlers
import utils.http.HttpLogger.logFailedRequests
import utils.{ElasticsearchApi, Environment, FoxConfig}

import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}

object Main extends App with LazyLogging {
  logger.info("Starting phoenix server")

  try {
    val service = new Service()
    service.performSelfCheck()
    service.bind()
    service.setupRemorseTimers()

    logger.info("Startup process complete")
  } catch {
    case e: Throwable ⇒
      val cause = Option(e.getCause).fold("") { c ⇒
        s"\nCaused by $c"
      }
      logger.error(s"$e$cause\nExiting now!")
      Thread.sleep(1000)
      System.exit(1)
  }
}

object Setup extends LazyLogging {

  implicit val executionContext: ExecutionContextExecutor =
    ExecutionContext.fromExecutor(java.util.concurrent.Executors.newCachedThreadPool())

  lazy val defaultApis: Apis =
    Apis(setupStripe(), new AmazonS3, setupMiddlewarehouse(), setupElasticSearch())

  def setupStripe(): FoxStripe = {
    logger.info("Loading Stripe API key")
    Stripe.apiKey = config.apis.stripe.key
    logger.info("Successfully set Stripe key")
    new FoxStripe(new StripeWrapper())
  }

  def setupMiddlewarehouse(): Middlewarehouse = {
    logger.info("Setting up MWH...")
    new Middlewarehouse(config.apis.middlewarehouse.url)
  }

  def setupElasticSearch(): ElasticsearchApi = {
    logger.info("Setting up Elastic Search")
    ElasticsearchApi.fromConfig(FoxConfig.config)
  }
}

class Service(
    systemOverride: Option[ActorSystem] = None,
    dbOverride: Option[Database] = None,
    apisOverride: Option[Apis] = None,
    esOverride: Option[ElasticsearchApi] = None,
    addRoutes: immutable.Seq[Route] = immutable.Seq.empty)(implicit val env: Environment) {

  import FoxConfig.config
  import utils.JsonFormatters._

  implicit val serialization: Serialization.type = jackson.Serialization
  implicit val formats: Formats                  = phoenixFormats

  implicit val system: ActorSystem = systemOverride.getOrElse {
    ActorSystem.create("Orders", FoxConfig.unsafe)
  }

  implicit val executionContext: ExecutionContextExecutor = Setup.executionContext

  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val logger: LoggingAdapter = Logging(system, getClass)

  implicit val db: Database = dbOverride.getOrElse(Database.forConfig("db", FoxConfig.unsafe))
  implicit val apis: Apis   = apisOverride.getOrElse(Setup.defaultApis)

  private val roleName: String = config.users.customer.role
  private val orgName: String  = config.users.customer.org
  private val scopeId: Int     = config.users.customer.scopeId

  private val scope: Scope = Await
    .result(Scopes.findOneById(scopeId).run(), Duration.Inf)
    .getOrElse(throw new RuntimeException(s"Unable to find a scope with id $scopeId"))

  private val customerCreateContext        = AccountCreateContext(List(roleName), orgName, scopeId)
  implicit val userAuth: UserAuthenticator = Authenticator.forUser(customerCreateContext)

  val defaultRoutes: Route = {
    pathPrefix("v1") {
      routes.AuthRoutes.routes(scope.ltree) ~
      routes.Public.routes(customerCreateContext, scope.ltree) ~
      routes.Customer.routes ~
      requireAdminAuth(userAuth) { implicit auth ⇒
        routes.admin.AdminRoutes.routes ~
        routes.admin.NotificationRoutes.routes ~
        routes.admin.AssignmentsRoutes.routes ~
        routes.admin.OrderRoutes.routes ~
        routes.admin.CartRoutes.routes ~
        routes.admin.CustomerRoutes.routes ~
        routes.admin.CustomerGroupsRoutes.routes ~
        routes.admin.GiftCardRoutes.routes ~
        routes.admin.ReturnRoutes.routes ~
        routes.admin.ProductRoutes.routes ~
        routes.admin.SkuRoutes.routes ~
        routes.admin.VariantRoutes.routes ~
        routes.admin.DiscountRoutes.routes ~
        routes.admin.PromotionRoutes.routes ~
        routes.admin.ImageRoutes.routes ~
        routes.admin.CouponRoutes.routes ~
        routes.admin.CategoryRoutes.routes ~
        routes.admin.GenericTreeRoutes.routes ~
        routes.admin.StoreAdminRoutes.routes ~
        routes.admin.ObjectRoutes.routes ~
        routes.admin.PluginRoutes.routes ~
        routes.admin.TaxonomyRoutes.routes ~
        routes.admin.ProductReviewRoutes.routes ~
        routes.admin.ShippingMethodRoutes.routes ~
        routes.service.MigrationRoutes.routes(customerCreateContext, scope.ltree) ~
        pathPrefix("service") {
          routes.service.PaymentRoutes.routes ~ //Migrate this to auth with service tokens once we have them
          routes.service.CustomerGroupRoutes.routes
        }
      }
    }
  }

  lazy val devRoutes: Route = {
    pathPrefix("v1") {
      requireAdminAuth(userAuth) { implicit auth ⇒
        routes.admin.DevRoutes.routes
      }
    }
  }

  val allRoutes: Route = {
    val routes = if (!env.isProd) {
      logger.info("Activating dev routes")
      addRoutes.foldLeft(defaultRoutes ~ devRoutes)(_ ~ _)
    } else
      addRoutes.foldLeft(defaultRoutes)(_ ~ _)
    logFailedRequests(routes, logger)
  }

  implicit def rejectionHandler: RejectionHandler = CustomHandlers.jsonRejectionHandler

  implicit def exceptionHandler: ExceptionHandler = CustomHandlers.jsonExceptionHandler

  private final val serverBinding = Agent[Option[ServerBinding]](None)

  def bind(config: FoxConfig = config): Future[ServerBinding] = {
    val host = config.http.interface
    val port = config.http.port
    val bind = Http().bindAndHandle(allRoutes, host, port).flatMap { binding ⇒
      serverBinding.alter(Some(binding)).map(_ ⇒ binding)
    }
    logger.info(s"Bound to $host:$port")
    bind
  }

  def close(): Future[Unit] =
    serverBinding.future.flatMap {
      case Some(b) ⇒ b.unbind()
      case None    ⇒ Future.successful(())
    }

  def setupRemorseTimers(): Unit = {
    logger.info("Scheduling remorse timer")
    val remorseTimer      = system.actorOf(Props(new RemorseTimer()), "remorse-timer")
    val remorseTimerBuddy = system.actorOf(Props(new RemorseTimerMate()), "remorse-timer-mate")
    system.scheduler
      .schedule(Duration.Zero, 1.minute, remorseTimer, Tick)(executionContext, remorseTimerBuddy)
  }

  def performSelfCheck(): Unit = {
    logger.info("Performing self check")
    if (config.auth.method == FoxConfig.AuthMethod.Jwt) {
      import models.auth.Keys
      assert(Keys.loadPrivateKey.isSuccess, "Can't load private key")
      assert(Keys.loadPublicKey.isSuccess, "Can't load public key")
    }
    logger.info(s"Using password hash algorithm: ${AccountAccessMethod.passwordsHashAlgorithm}")
    logger.info("Self check complete")
  }
}
