package models

import (
	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/common/gormfox"
)

type ShipmentLineItem struct {
	gormfox.Base
	ShipmentID      uint
	StockItemUnitID uint
	SKU             string
	Name            string
	Price           uint
	ImagePath       string
}

func NewShipmentLineItemFromPayload(payload *payloads.ShipmentLineItem) *ShipmentLineItem {
	return &ShipmentLineItem{
		Base: gormfox.Base{
			ID: payload.ID,
		},
		SKU:       payload.SKU,
		Name:      payload.Name,
		Price:     payload.Price,
		ImagePath: payload.ImagePath,
	}
}
