package com.foxcommerce.endpoints

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder

import com.foxcommerce.payloads.CustomerAddressPayload

object CustomerAddressEndpoint {

  def create(address: CustomerAddressPayload): HttpRequestBuilder = http("Create Customer Address")
    .post("/v1/customers/${customerId}/addresses")
    .basicAuth("${email}", "${password}")
    .body(StringBody(createPayload(address)))
    .check(status.is(200))
    .check(jsonPath("$.id").ofType[Long].saveAs("customerAddressId"))
    .check(jsonPath("$.name").ofType[String].is(address.name))
    .check(jsonPath("$.region.id").ofType[Long].is(address.regionId))
    .check(jsonPath("$.address1").ofType[String].is(address.address1))
    .check(jsonPath("$.address2").ofType[String].is(address.address2))
    .check(jsonPath("$.city").ofType[String].is(address.city))
    .check(jsonPath("$.zip").ofType[String].is(address.zip))

  def update(address: CustomerAddressPayload): HttpRequestBuilder = http("Update Customer Address")
    .patch("/v1/customers/${customerId}/addresses/${customerAddressId}")
    .basicAuth("${email}", "${password}")
    .body(StringBody(createPayload(address)))
    .check(status.is(200))
    .check(jsonPath("$.id").ofType[Long].saveAs("customerAddressId"))
    .check(jsonPath("$.name").ofType[String].is(address.name))
    .check(jsonPath("$.region.id").ofType[Long].is(address.regionId))
    .check(jsonPath("$.address1").ofType[String].is(address.address1))
    .check(jsonPath("$.address2").ofType[String].is(address.address2))
    .check(jsonPath("$.city").ofType[String].is(address.city))
    .check(jsonPath("$.zip").ofType[String].is(address.zip))

  private def createPayload(address: CustomerAddressPayload): String =
    """{"name": "%s", "regionId": %d, "address1": "%s", "address2": "%s", "city": "%s", "zip": "%s"}""".
      format(address.name, address.regionId, address.address1, address.address2, address.city, address.zip)
}
