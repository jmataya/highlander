package com.foxcommerce

import com.foxcommerce.common.{Config, Utils}
import com.foxcommerce.endpoints.{CustomerEndpoint, CustomerAddressEndpoint, SearchEndpoint, StoreCreditEndpoint}
import com.foxcommerce.payloads.{CustomerPayload, CustomerAddressPayload, StoreCreditPayload}
import io.gatling.core.Predef._

class TrailSimulation extends Simulation {

  val conf = Config.load()

  before {
    conf.before()
  }

  // Prepare initial payloads
  val storeCredit = StoreCreditPayload(amount = 200, reasonId = 1)
  val address = CustomerAddressPayload(name = Utils.randomString(10), regionId = 1, address1 = "Donkey street, 38",
    address2 = "Donkey street, 39", city = "Donkeyville", zip = "55555")
  val customer = CustomerPayload(name = "Max Power", email = Utils.randomEmail("maxpower"), address = address,
    storeCreditCount = 1, storeCreditTotal = storeCredit.amount)

  // Copy modified payload
  val addressUpdated = address.copy(city = "Seattle", zip = "66666")
  val customerUpdated = customer.copy(name = "Adil Wali", address = addressUpdated, isBlacklisted = true,
    isDisabled = true, storeCreditTotal = 0)

  // Prepare scenario
  val trailScenario = scenario("Trail Scenario")
    .feed(jsonFile("data/admins.json").random)
    // Create customer, address, store credit
    .exec(CustomerEndpoint.create(customer))
    .exec(CustomerAddressEndpoint.create(address))
    .exec(StoreCreditEndpoint.create(storeCredit))
    // Pause and check indexes
    .pause(conf.greenRiverPause)
    .exec(SearchEndpoint.checkCustomer(conf, customer))
    .exec(SearchEndpoint.checkStoreCredit(conf, storeCredit, state = "active"))
    // Update customer + address, cancel store credit
    .exec(CustomerEndpoint.update(customerUpdated))
    .exec(CustomerEndpoint.blacklist(customerUpdated))
    .exec(CustomerEndpoint.disable(customerUpdated))
    .exec(CustomerAddressEndpoint.update(addressUpdated))
    .exec(StoreCreditEndpoint.cancel())
    // Pause and check indexes
    .pause(conf.greenRiverPause)
    .exec(SearchEndpoint.checkCustomer(conf, customerUpdated))
    .exec(SearchEndpoint.checkStoreCredit(conf, storeCredit, state = "canceled"))

  setUp(
    trailScenario.inject(conf.defaultInjectionProfile).protocols(conf.httpConf)
  ).assertions(conf.defaultAssertion)
}
