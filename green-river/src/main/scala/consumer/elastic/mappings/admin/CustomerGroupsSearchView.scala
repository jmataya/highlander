package consumer.elastic.mappings.admin

import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping, _}
import com.sksamuel.elastic4s.mappings.FieldType._
import consumer.aliases._
import consumer.elastic.AvroTransformer
import consumer.elastic.mappings._

case class CustomerGroupsSearchView()(implicit ec: EC) extends AvroTransformer {
  def topic() = "customer_groups_search_view"
  def mapping() = esMapping(topic()).fields(
    field("id", LongType),
    field("name", StringType).analyzer("autocomplete"),
    field("groupType", StringType).index("not_analyzed"),
    field("customersCount", IntegerType),
    field("scope", StringType).index("not_analyzed"),
    field("createdAt", DateType).format(dateFormat),
    field("updatedAt", DateType).format(dateFormat),
    field("deletedAt", DateType).format(dateFormat)
  )
}
