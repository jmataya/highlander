package core.db

import com.github.tminglei.slickpg._
import com.github.tminglei.slickpg.array.PgArrayJdbcTypes
import com.github.tminglei.slickpg.utils.SimpleArrayUtils
import org.json4s.JValue
import slick.jdbc.PostgresProfile

trait ExPostgresDriver
    extends PostgresProfile
    with PgArraySupport
    with PgDateSupport
    with PgRangeSupport
    with PgHStoreSupport
    with PgJson4sSupport
    with PgArrayJdbcTypes
    with PgSearchSupport
    // with PgPostGISSupport
    with PgNetSupport
    with PgLTreeSupport {

  type Json = JValue

  override val pgjson = "jsonb"
  type DOCType = Json
  override val jsonMethods = org.json4s.jackson.JsonMethods

  override val api = MyAPI

  val plainAPI = new API with Json4sJsonPlainImplicits

  object MyAPI
      extends API
      with ArrayImplicits
      with DateTimeImplicits
      with JsonImplicits
      with NetImplicits
      with LTreeImplicits
      with RangeImplicits
      with HStoreImplicits
      with SearchImplicits
      with SearchAssistants {
    implicit val strListTypeMapper: DriverJdbcType[List[String]] =
      new SimpleArrayJdbcType[String]("text").to(_.toList)

    implicit val json4sJsonArrayTypeMapper: DriverJdbcType[List[Json]] =
      new AdvancedArrayJdbcType[Json](pgjson,
                                      (s) ⇒ SimpleArrayUtils.fromString[Json](jsonMethods.parse(_))(s).orNull,
                                      (v) ⇒ SimpleArrayUtils.mkString[Json](_.toString())(v)).to(_.toList)
  }
}

object ExPostgresDriver extends ExPostgresDriver
