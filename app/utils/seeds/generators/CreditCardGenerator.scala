
package utils.seeds.generators

import java.time.{Instant, ZoneId}

import scala.concurrent.ExecutionContext.Implicits.global
import models.payment.creditcard.CreditCard
import utils.DbResultT._
import utils.DbResultT.implicits._
import GeneratorUtils.randomString

import faker._;

trait CreditCardGenerator extends AddressGenerator {

  val today = Instant.now().atZone(ZoneId.of("UTC"))

  def generateCreditCard(customerId: Int) = {
    val base = new Base{}
    val address = generateAddress(customerId, false)
    val gateway = randomString(10)
    val gatewayCardId = randomString(10)
    CreditCard(customerId = customerId, gatewayCustomerId = gateway, gatewayCardId = gatewayCardId, holderName = Name.name,
      lastFour = base.numerify("####"), expMonth = today.getMonthValue, expYear = today.getYear + 2, isDefault = true,
      regionId = 4129, addressName = address.name, address1 = address.address1, address2 = address.address2,
      city = address.city, zip = address.zip, brand = "Visa")
  }

  def generateCreditCards(customerIds: Seq[Int]) : Seq[CreditCard] =
    customerIds map generateCreditCard
}
