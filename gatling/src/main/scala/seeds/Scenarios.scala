package seeds

import scala.concurrent.duration._
import scala.util.Random.nextInt

import io.gatling.core.Predef._
import seeds.Addresses._
import seeds.Auth._
import seeds.Cart._
import seeds.Customers._
import seeds.GatlingApp._

object Scenarios {

  val randomCustomerActivity = scenario("Random customer activity")
    .exec(loginAsRandomAdmin).exitHereIfFailed
    .exec(createRandomCustomers).exitHereIfFailed
    .exec(randomAddressLine1("customerAddress")).exitHereIfFailed
    .feed(dbFeeder("""select id as "customerRegionId", name as "customerCity" from regions""").random).exitHereIfFailed
    .randomSwitch(50.0 → randomAddressLine2("customerAddress2")).exitHereIfFailed
    .exec(addCustomerAddress).exitHereIfFailed
    .exec(setDefaultShipping).exitHereIfFailed
    .repeat(_ ⇒ nextInt(3))(placeOrder.exec(ageOrder).exitHereIfFailed)
    .inject(rampUsers(20) over 2.minutes)

  val pacificNwVips = scenario("Pacific Northwest VIPs")
    .exec(loginAsRandomAdmin).exitHereIfFailed
    .exec(createRandomCustomers).exitHereIfFailed
    .exec(randomAddressLine1("customerAddress")).exitHereIfFailed
    .feed(csv("data/scenarios/pacific_northwest_vips/regions_cities.csv").random).exitHereIfFailed
    .randomSwitch(50.0 → randomAddressLine2("customerAddress2")).exitHereIfFailed
    .exec(addCustomerAddress).exitHereIfFailed
    .exec(setDefaultShipping).exitHereIfFailed
    .repeat(_ ⇒ nextInt(10) + 5)(placeOrder.exec(ageOrder).exitHereIfFailed)
    .inject(rampUsers(10) over 1.minute)
}
