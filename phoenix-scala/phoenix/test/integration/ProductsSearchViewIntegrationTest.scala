import java.time.Instant

import com.github.tminglei.slickpg.LTree
import phoenix.utils.aliases.Json
import testutils._
import testutils.apis.PhoenixAdminApi
import utils.MockedApis

object ProductsSearchViewIntegrationTest {

  case class ProductSearchViewResult(
      id: Int,
      productId: Int,
      context: String,
      title: String,
      description: String,
      activeFrom: Json,
      activeTo: Json,
      tags: String,
      skus: Json,
      albums: Json,
      archivedAt: Option[Instant],
      externalId: String,
      scope: LTree,
      slug: String,
      taxonomies: Json
  )

}

class ProductsSearchViewIntegrationTest
    extends SearchViewTestBase
    with TestBase
    with GimmeSupport
    with JwtTestAuth
    with MockedApis
    with PhoenixAdminApi
    with DefaultJwtAdminAuth {

  import ProductsSearchViewIntegrationTest._

  type SearchViewResult = ProductSearchViewResult
  val searchViewName: String = "products_search_view"
  val searchKeyName: String  = "id"

}
