package testutils

import com.typesafe.config.{Config, ConfigFactory}
import java.sql.{Connection, PreparedStatement}
import java.util.Locale
import javax.sql.DataSource

import scala.concurrent.Future

import objectframework.models.ObjectContexts
import org.scalatest._
import phoenix.models.product.SimpleContext
import phoenix.utils.aliases.{EC, SF, SL}
import phoenix.utils.db.flyway.{newFlyway, rootProjectSqlLocation, subprojectSqlLocation}
import phoenix.utils.seeds.Factories
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.hikaricp.HikariCPJdbcDataSource
import core.db._
import scala.annotation.tailrec

trait DbTestSupport extends SuiteMixin with BeforeAndAfterAll with GimmeSupport { self: TestSuite ⇒

  def dbOverride(): Option[DB] = None

  private def time[R](what: String)(block: ⇒ R): R = {
    System.out.print(s"$what....")
    System.out.flush()
    val t0     = System.nanoTime()
    val result = block
    val ms     = (System.nanoTime() - t0) / 10e5.toInt
    System.out.println(s"... $what completed. Time elapsed: ${ms}ms")
    System.out.flush()
    result
  }

  def dbConfig: Config =
    ConfigFactory.parseString(s"""db.name = "$dbName"
                                 |db.url = "jdbc:postgresql://localhost/$dbName?user=phoenix&prepareThreshold=0"
       """.stripMargin).withFallback(TestBase.bareConfig)

  def fullSuiteName: String = this.getClass.getName.replace('.', '_')

  val dbName: String = fullSuiteName.flatMap {
    case c if c.isUpper ⇒ s"_${c.toLower}"
    case c              ⇒ s"$c"
  }.stripPrefix("_")

  private def acquireDb(): DB = {
    Locale.setDefault(Locale.US)
    if (!DbTestSupport.migrated) {
      DbTestSupport.synchronized {
        if (!DbTestSupport.migrated) {
          migrate()
        }
      }
    }
    System.out.println(s"ACQUIRE DB for $dbName")
    System.out.flush()
    require(DbTestSupport.migrated, "Create test db before migration has completed")
    time(s"createDb $dbName") {
      DbTestSupport.createDB(dbName)
    }
    Database.forConfig("db", dbConfig)
  }

  implicit lazy val db: DB = {
    dbOverride().getOrElse {
      acquireDb()
    }
  }

  implicit val ec: EC

  val api: PostgresProfile.API = DbTestSupport.api

  private[this] lazy val truncateTables: PreparedStatement = {
    val conn      = db.source.createConnection()
    val allTables = conn.getMetaData.getTables(conn.getCatalog, "public", "%", Array("TABLE"))

    @tailrec
    def iterate(in: Seq[String]): Seq[String] =
      if (allTables.next()) iterate(in :+ allTables.getString(3)) else in

    val sqlTables = iterate(Seq()).filterNot { t ⇒
      t.startsWith("pg_") || t.startsWith("sql_") || DbTestSupport.DoNotTruncate.contains(t)
    }.mkString("{", ",", "}")

    conn.prepareStatement(s"select truncate_nonempty_tables('$sqlTables'::text[])")
  }

  private def migrate(): Unit = {
    if (DbTestSupport.migrated) {
      ()
    } else {
      val tplName = DbTestSupport.DB_TEMPLATE
      val conn    = DbTestSupport.db.source.createConnection()
      val stmt1   = conn.createStatement()
      try {
        stmt1.execute(s"drop database if exists $tplName")
        stmt1.execute(s"create database $tplName owner phoenix")
      } finally stmt1.close()

      val tplCfg = ConfigFactory.parseString(s"""db.name = "$tplName"
                 |db.url = "jdbc:postgresql://localhost/$tplName?user=phoenix&prepareThreshold=0"
         """.stripMargin).withFallback(TestBase.bareConfig)
      val tplDb  = Database.forConfig("db", tplCfg)
      DbTestSupport.setSearchPath(tplName, conn, List("\"$user\"", "public", "exts"))

      val originDs = DbTestSupport.jdbcDataSourceFromSlickDB(api)(tplDb)
      DbTestSupport.migrateDB(originDs)
      Factories.createSingleMerchantSystem
        .gimme(ec = ec, db = tplDb, line = implicitly[SL], file = implicitly[SF])
      tplDb.close()
      val stmt = conn.createStatement()
      try stmt.execute(s"""select pg_terminate_backend(pid)
                          |from pg_stat_activity
                          |where datname = '$tplName'""".stripMargin)
      finally stmt.close()

      conn.close()
      DbTestSupport.migrated = true
    }

  }

  override protected def beforeAll(): Unit = {
    Locale.setDefault(Locale.US)
    if (!DbTestSupport.migrated) {
      DbTestSupport.synchronized {
        if (!DbTestSupport.migrated) {
          migrate()
        }
      }
    }
    truncateTables // init
  }

  override protected def afterAll(): Unit = {
    truncateTables.close()
    truncateTables.getConnection.close()
    db.close()
    DbTestSupport.dropDB(dbName)
  }

  override abstract protected def withFixture(test: NoArgTest): Outcome = {
    truncateTables.executeQuery()

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
  val db: DB = Database.forConfig("db", TestBase.bareConfig)
  val conn   = db.source.createConnection()

  def api: PostgresProfile.API = slick.jdbc.PostgresProfile.api

  @volatile var migrated               = false
  @volatile var sqlTables: Seq[String] = _

  val DB_TEMPLATE: String = "phoenix_test_tpl"

  /* tables which should *not* be truncated b/c they're static and seeded by migration */
  val DoNotTruncate: Set[String] = Set("states",
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

  private def createDB(name: String, conn: Connection): Unit = {
    val stmt = conn.createStatement()

    try {
      stmt.execute(s"create database $name with template $DB_TEMPLATE owner phoenix")
    } finally stmt.close()
  }

  private def dropDB(name: String, conn: Connection): Unit = {
    val stmt = conn.createStatement()

    try {
      stmt.execute(s"update pg_database set datallowconn = 'false' where datname = '$name'")
      stmt.execute(s"""select pg_terminate_backend(pid)
                      |from pg_stat_activity
                      |where datname = '$name'""".stripMargin)
      stmt.execute(s"drop database if exists $name")
    } finally stmt.close()
  }

  def createDB(name: String)(implicit ec: EC): Unit = {
    dropDB(name, conn)
    createDB(name, conn)
    // must be set before creation of hikari pool for IT db
    setSearchPath(name, conn, List("\"$user\"", "public", "exts"))
  }

  def dropDB(name: String)(implicit ec: EC): Unit = {
    dropDB(name, conn)
  }

  def migrateDB(dataSource: DataSource): Unit = {
    val flyway = newFlyway(dataSource, rootProjectSqlLocation)

    flyway.clean()
    flyway.migrate()
  }

  def setSearchPath(name: String, conn: Connection, path: List[String]) = {
    val stmt = conn.createStatement()

    try stmt.execute(s"alter database $name set search_path to ${path.mkString(",")}")
    finally stmt.close()
  }

  def jdbcDataSourceFromSlickDB(api: PostgresProfile.API)(implicit db: api.Database): DataSource =
    db.source match {
      case source: HikariCPJdbcDataSource ⇒ source.ds
    }
}
