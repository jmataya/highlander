import cats.implicits._
import io.circe.Json
import models.customer.CustomerGroup._
import models.customer.{CustomerGroupTemplate, CustomerGroupTemplates}
import org.scalatest.mockito.MockitoSugar
import payloads.CustomerGroupPayloads.CustomerGroupPayload
import responses.GroupResponses.GroupResponse.Root
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import utils.seeds.Factories

class CustomerGroupTemplateIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with DefaultJwtAdminAuth
    with MockitoSugar
    with BakedFixtures {

  "sends full list of templates when templates are not used" in new Fixture {
    customerGroupTemplateApi.get().as[Seq[CustomerGroupTemplate]] must === (groupTemplates)
  }

  "sends unused templates only when templates are not used" in new Fixture {
    val scopeN = "1"

    val payload = CustomerGroupPayload(name = "Group number one",
                                       clientState = Json.obj(),
                                       elasticRequest = Json.obj(),
                                       customersCount = 1,
                                       templateId = groupTemplates.head.id.some,
                                       scope = scopeN.some,
                                       groupType = Template)

    val root = customerGroupsApi.create(payload).as[Root]

    val expected = groupTemplates.tail
    customerGroupTemplateApi.get().as[Seq[CustomerGroupTemplate]] must === (expected)
  }

  trait Fixture extends StoreAdmin_Seed {

    val groupTemplates = Factories.templates.map(CustomerGroupTemplates.create(_).gimme)

  }

}
