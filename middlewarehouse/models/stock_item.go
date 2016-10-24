package models

import (
	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/common/gormfox"
)

type StockItem struct {
	gormfox.Base
	SkuID           uint
	SkuCode         string
	StockLocation   StockLocation
	StockLocationID uint
	DefaultUnitCost int
}

func (si StockItem) Identifier() uint {
	return si.ID
}

func NewStockItemFromPayload(payload *payloads.StockItem) *StockItem {
	item := &StockItem{
		StockLocationID: payload.StockLocationID,
		SkuID:           payload.SkuID,
		SkuCode:         payload.SkuCode,
		DefaultUnitCost: payload.DefaultUnitCost,
	}

	return item
}
