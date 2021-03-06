package gatling

import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.core.structure.StructureBuilder
import io.gatling.jdbc.Predef._
import gatling.seeds.Conf._
import io.gatling.core.feeder.RecordSeqFeederBuilder

package object seeds {

  def dbFeeder(sql: String): RecordSeqFeederBuilder[Any] =
    jdbcFeeder(dbUrl, dbUser, dbPassword, sql)

  implicit class StopOnFailure[B <: StructureBuilder[B]](val builder: B) extends AnyVal {
    def stopOnFailure: B =
      builder.exec {
        doIf(session ⇒ session.isFailed)(exec { session ⇒
          Console.err.println("[ERROR] Seeds failed, exiting.")
          session.onExit(session)
          System.exit(1)
          session
        })
      }
  }

  implicit class DefaultPause[B <: StructureBuilder[B]](val builder: B) extends AnyVal {
    def doPause: B = builder.pause(100.milliseconds, 1.second)
  }
}
