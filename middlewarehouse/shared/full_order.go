package shared

import (
	"encoding/json"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/models/activities"
)

type FullOrder struct {
	Order payloads.Order `json:"Order" binding:"required"`
}

func NewFullOrderFromActivity(activity activities.ISiteActivity) (*FullOrder, error) {
	bt := []byte(activity.Data())
	fo := new(FullOrder)
	err := json.Unmarshal(bt, fo)
	return fo, err
}

func NewFullOrderFromPayload(payload *payloads.Order) *FullOrder {
	fo := new(FullOrder)
	fo.Order = *payload
	return fo
}
