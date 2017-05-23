package testutils

import java.sql.PreparedStatement
import java.util.Locale
import javax.sql.DataSource

import models.objects.ObjectContexts
import org.scalatest._
import phoenix.models.product.SimpleContext
import phoenix.utils.aliases.EC
import phoenix.utils.db.flyway.{newFlyway, subprojectSqlLocation}
import phoenix.utils.seeds.Factories
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.hikaricp.HikariCPJdbcDataSource
import utils.db._

import scala.annotation.tailrec

trait DbTestSupport extends SuiteMixin with BeforeAndAfterAll with GimmeSupport {
  this: TestSuite ⇒

  import DbTestSupport._

  implicit lazy val db = database

  implicit val ec: EC

  val api = slick.jdbc.PostgresProfile.api

  /* tables which should *not* be truncated b/c they're static and seeded by migration */
  val doNotTruncate = Set("states",
                          "countries",
                          "regions",
                          "schema_version",
                          "systems",
                          "resources",
                          "scopes",
                          "organizations",
                          "scope_domains",
                          "roles",
                          "permissions",
                          "role_permissions")

  override protected def beforeAll(): Unit = {
    if (!migrated) {
      Locale.setDefault(Locale.US)
      val flyway = newFlyway(dataSource, subprojectSqlLocation)

      flyway.clean()
      flyway.migrate()

      truncateTablesStmt = {
        val allTables =
          persistConn.getMetaData.getTables(persistConn.getCatalog, "public", "%", Array("TABLE"))

        @tailrec
        def iterate(in: Seq[String]): Seq[String] =
          if (allTables.next()) iterate(in :+ allTables.getString(3)) else in

        tables = iterate(Seq()).filterNot { t ⇒
          t.startsWith("pg_") || t.startsWith("sql_") || doNotTruncate.contains(t)
        }
        val sqlTables = tables.mkString("{", ",", "}")
        persistConn.prepareStatement(s"select truncate_nonempty_tables('$sqlTables'::text[])")
      }

      Factories.createSingleMerchantSystem.gimme

      migrated = true
    }
  }

  override abstract protected def withFixture(test: NoArgTest): Outcome = {
    truncateTablesStmt.executeQuery()

    // TODO: Use Seeds.createBase after promo tests are fixed?
    createBaseTestSeeds()

    test()
  }

  private def createBaseTestSeeds() = {
    // Base test data
    (for {
      _ ← * <~ ObjectContexts.create(SimpleContext.create())
      // Can't create all schemas right now because promo tests are fucky
      // FIXME @anna @michalrus
      _ ← * <~ Factories.FIXME_createAllButPromoSchemas
    } yield {}).gimme
  }
}

object DbTestSupport {

  @volatile var migrated                              = false
  @volatile var tables: Seq[String]                   = Seq()
  @volatile var truncateTablesStmt: PreparedStatement = _

  lazy val database    = Database.forConfig("db", TestBase.bareConfig)
  lazy val dataSource  = jdbcDataSourceFromSlickDB(database)
  lazy val persistConn = dataSource.getConnection
  val api              = slick.jdbc.PostgresProfile.api

  def jdbcDataSourceFromSlickDB(db: api.Database): DataSource =
    db.source match {
      case source: HikariCPJdbcDataSource ⇒ source.ds
    }
}