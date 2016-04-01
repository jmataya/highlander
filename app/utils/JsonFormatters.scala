package utils

import concepts.discounts._
import concepts.discounts.offers.OfferType
import concepts.discounts.qualifiers.QualifierType
import models.inventory.SkuType
import models.order.Order
import models.order.lineitems.OrderLineItem
import models.payment.PaymentMethod
import models.payment.creditcard.CreditCardCharge
import models.payment.giftcard.{GiftCard, GiftCardAdjustment}
import models.payment.storecredit.{StoreCredit, StoreCreditAdjustment}
import models.rma.{Rma, RmaLineItem, RmaReason}
import models.rules.{Condition, QueryStatement}
import models.sharedsearch.SharedSearch
import models.shipping.Shipment
import models.auth.Identity.IdentityKind
import models.{Assignment, Reason}
import org.json4s.jackson
import responses.CountryWithRegions

object JsonFormatters {
  val serialization = jackson.Serialization

  val DefaultFormats = org.json4s.DefaultFormats + time.JavaTimeJson4sSerializer.jsonFormat + Money.jsonFormat

  val phoenixFormats = DefaultFormats +
    QualifierType.jsonFormat +
    OfferType.jsonFormat +
    Assignment.AssignmentType.jsonFormat +
    Assignment.ReferenceType.jsonFormat +
    Order.State.jsonFormat +
    OrderLineItem.State.jsonFormat +
    Shipment.State.jsonFormat +
    GiftCard.OriginType.jsonFormat +
    GiftCard.State.jsonFormat +
    GiftCardAdjustment.State.jsonFormat +
    StoreCredit.State.jsonFormat +
    StoreCredit.OriginType.jsonFormat +
    StoreCreditAdjustment.State.jsonFormat +
    Reason.ReasonType.jsonFormat +
    Rma.State.jsonFormat +
    Rma.RmaType.jsonFormat +
    RmaLineItem.InventoryDisposition.jsonFormat +
    RmaReason.ReasonType.jsonFormat +
    CreditCardCharge.State.jsonFormat +
    CountryWithRegions.jsonFormat +
    QueryStatement.Comparison.jsonFormat +
    Condition.Operator.jsonFormat +
    PaymentMethod.Type.jsonFormat +
    SkuType.jsonFormat +
    SharedSearch.Scope.jsonFormat +
    IdentityKind.jsonFormat
}

