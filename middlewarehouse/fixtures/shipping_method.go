package fixtures

import (
	"database/sql/driver"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/models"
)

func GetShippingMethod(id uint, carrierID uint, carrier *models.Carrier) *models.ShippingMethod {
	return &models.ShippingMethod{
		ID:        id,
		CarrierID: carrierID,
		Carrier:   *carrier,
		Name:      "UPS 2 day ground",
	}
}

func ToShippingMethodPayload(shippingMethod *models.ShippingMethod) *payloads.ShippingMethod {
	return &payloads.ShippingMethod{
		CarrierID: shippingMethod.CarrierID,
		Name:      shippingMethod.Name,
	}
}

func GetShippingMethodColumns() []string {
	return []string{"id", "carrier_id", "name"}
}

func GetShippingMethodRow(shippingMethod *models.ShippingMethod) []driver.Value {
	return []driver.Value{shippingMethod.ID, shippingMethod.CarrierID, shippingMethod.Name}
}
