import testutils._
import testutils.fixtures.api.ApiFixtures

import org.json4s._
import org.json4s.jackson.JsonMethods._
import objectframework.content._
import objectframework.failures._
import objectframework.payloads.ContentPayloads._
import objectframework.services._
import phoenix.models.product.SimpleContext
import phoenix.utils.JsonFormatters

// Ideally this code should live under the objectframework module. However,
// it currently lives here to take advantage of the test infrastructure and DB
// migration logic that lives in the main integration tests. At some point,
// moving it back to that project will make sense once we have a better idea
// of how modules will do things like manage the database.
// -- Jeff

class ContentManagerTest extends IntegrationTestBase with TestObjectContext with ApiFixtures {

  "ContentManager.create" - {
    implicit val formats: Formats = JsonFormatters.phoenixFormats

    "successfully with no relations" in new Fixture {
      val payload =
        CreateContentPayload(kind = "product", attributes = attributes, relations = relations)

      val content = ContentManager.create(SimpleContext.id, payload).gimme
      content.attributes("title").v must === (JString("a test product"))
    }

    "successfully with a relation" in new SkuFixture {
      val productRelations = Map("sku" → Seq(sku.commitId))
      val payload = CreateContentPayload(kind = "product",
                                         attributes = attributes,
                                         relations = productRelations)

      val content = ContentManager.create(SimpleContext.id, payload).gimme
      content.attributes("title").v must === (JString("a test product"))
      content.relations("sku") must === (Seq(sku.commitId))
    }

    "fails if the relation does not exist" in new Fixture {
      val productRelations = Map("sku" → Seq(1))
      val payload = CreateContentPayload(kind = "product",
                                         attributes = attributes,
                                         relations = productRelations)

      val failures = ContentManager.create(SimpleContext.id, payload).gimmeFailures
      failures.head must === (RelatedContentDoesNotExist("sku", 1))
    }

    "fails if the relation has the wrong commit id" in new SkuFixture {
      val productRelations = Map("sku" → Seq(2))
      val payload = CreateContentPayload(kind = "product",
                                         attributes = attributes,
                                         relations = productRelations)

      val failures = ContentManager.create(SimpleContext.id, payload).gimmeFailures
      failures.head must === (RelatedContentDoesNotExist("sku", 2))
    }

    "fails if the relation has the wrong kind" in new SkuFixture {
      val productRelations = Map("variant" → Seq(1))
      val payload = CreateContentPayload(kind = "product",
                                         attributes = attributes,
                                         relations = productRelations)

      val failures = ContentManager.create(SimpleContext.id, payload).gimmeFailures
      failures.head must === (RelatedContentDoesNotExist("variant", 1))
    }
  }

  trait Fixture {
    val attributes = Map(
        "title"       → ContentAttribute(t = "string", v = JString("a test product")),
        "description" → ContentAttribute(t = "richText", v = JString("<p>A test description</p>"))
    )

    val relations = Map.empty[String, Seq[Commit#Id]]
  }

  trait SkuFixture extends Fixture {
    implicit val formats: Formats = JsonFormatters.phoenixFormats

    val skuAttributes = Map("code" → ContentAttribute(t = "string", v = JString("TEST-SKU")))
    val skuRelations  = Map.empty[String, Seq[Commit#Id]]

    val skuPayload =
      CreateContentPayload(kind = "sku", attributes = attributes, relations = relations)

    val sku = ContentManager.create(SimpleContext.id, skuPayload).gimme
  }
}
