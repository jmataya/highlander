package models

import cats.data.NonEmptyList
import org.scalatest.prop.TableDrivenPropertyChecks._
import util.TestBase

class AddressTest extends TestBase {

  "Address" - {
    ".validateNew" - {
      val valid = Address(id = 0, customerId = 1, regionId = 1, name = "Yax Home",
        street1 = "555 E Lake Union St.", street2 = None, city = "Seattle", zip = "12345", phoneNumber = None)

      "returns errors when zip is invalid" in {
        val badZip = valid.copy(zip = "AB+123")
        val wrongLengthZip = valid.copy(zip = "1")

        val addresses = Table(
          ("address", "errors"),
          (badZip, NonEmptyList("zip must fully match regular expression '%s'".format(Address.zipPattern))),
          (wrongLengthZip, NonEmptyList("zip must fully match regular expression '%s'".format(Address.zipPattern)))
        )

        forAll(addresses) { (address: Address, errors: NonEmptyList[String]) =>
          invalidValue(address.validateNew) must === (errors)
        }
      }

      "returns errors when name or street1 is empty" in {
        val result = valid.copy(name = "", street1 = "").validateNew
        invalidValue(result) must === (NonEmptyList("name must not be empty", "street1 must not be empty"))
      }

      "returns errors if US address and Some(phoneNumber) < 10 digits" in {
        val result = valid.copy(regionId = Country.usRegions.head, phoneNumber = Some("5551234")).validateNew
        invalidValue(result).head must (include("phoneNumber") and include("'[0-9]{10}'"))
      }

      "returns errors if non-US address and Some(phoneNumber) > 15 digits" in {
        val result = valid.copy(regionId = 1, phoneNumber = Some("1" * 16)).validateNew
        invalidValue(result).head must (include("phoneNumber") and include("'[0-9]{0,15}'"))
      }
    }
  }
}
