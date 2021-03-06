package main

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"io/ioutil"
	"log"
	"net/http"

	"github.com/FoxComm/highlander/middlewarehouse/models/activities"
	"github.com/FoxComm/highlander/middlewarehouse/shared"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix"
	"github.com/FoxComm/metamorphosis"
)

const (
	activityOrderStateChanged     = "order_state_changed"
	activityOrderBulkStateChanged = "order_bulk_state_changed"
	orderStateFulfillmentStarted  = "fulfillmentStarted"
)

type OrderConsumer struct {
	phoenixClient phoenix.PhoenixClient
	mwhURL        string
}

func NewOrderConsumer(phoenixClient phoenix.PhoenixClient, mwhURL string) (*OrderConsumer, error) {
	if mwhURL == "" {
		return nil, errors.New("middlewarehouse URL must be set")
	}

	return &OrderConsumer{phoenixClient, mwhURL}, nil
}

// Handler accepts an Avro encoded message from Kafka and takes
// based on the activities topic and looks for orders that were just placed in
// fulfillment started. If it finds one, it sends to middlewarehouse to create
// a shipment. Returning an error will cause a panic.
func (o OrderConsumer) Handler(message metamorphosis.AvroMessage) error {
	activity, err := activities.NewActivityFromAvro(message)
	if err != nil {
		return fmt.Errorf("Unable to decode Avro message with error %s", err.Error())
	}

	switch activity.Type() {
	case activityOrderStateChanged:
		fullOrder, err := shared.NewFullOrderFromActivity(activity)
		if err != nil {
			return fmt.Errorf("Unable to decode order from activity with error %s", err.Error())
		}

		return o.handlerInner(fullOrder)
	case activityOrderBulkStateChanged:
		bulkStateChange, err := shared.NewOrderBulkStateChangeFromActivity(activity)
		if err != nil {
			return fmt.Errorf("Unable to decode bulk state change activity with error %s", err.Error())
		}

		if bulkStateChange.NewState != orderStateFulfillmentStarted {
			return nil
		}

		if len(bulkStateChange.CordRefNums) == 0 {
			return nil
		}

		// Get orders from Phoenix
		orders, err := bulkStateChange.GetRelatedOrders(o.phoenixClient)
		if err != nil {
			return err
		}

		// Handle each order
		for _, fullOrder := range orders {
			err := o.handlerInner(fullOrder)
			if err != nil {
				return err
			}
		}

		return nil
	default:
		return nil
	}
}

// Handle activity for single order
func (o OrderConsumer) handlerInner(fullOrder *shared.FullOrder) error {
	order := fullOrder.Order
	if order.OrderState != orderStateFulfillmentStarted {
		return nil
	}

	log.Printf(
		"Found order %s in fulfillmentStarted. Add to middlewarehouse!",
		order.ReferenceNumber,
	)

	b, err := json.Marshal(&order)
	if err != nil {
		return err
	}

	url := fmt.Sprintf("%s/v1/public/shipments/from-order", o.mwhURL)
	req, err := http.NewRequest("POST", url, bytes.NewBuffer(b))
	if err != nil {
		return err
	}

	if err := o.phoenixClient.EnsureAuthentication(); err != nil {
		log.Panicf("Error auth in phoenix with error: %s", err.Error())
	}

	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("JWT", o.phoenixClient.GetJwt())

	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		log.Printf("Error creating shipment with error: %s", err.Error())
	}

	defer resp.Body.Close()
	if resp.StatusCode < 200 || resp.StatusCode > 299 {
		errResp, err := ioutil.ReadAll(resp.Body)
		if err != nil {
			return fmt.Errorf(
				"Failed to create shipment. Unable to read response with error %s",
				err.Error(),
			)
		}

		return fmt.Errorf(
			"Failed to create shipment with error %s",
			string(errResp),
		)
	}

	log.Printf("Created shipment(s) for order %s", order.ReferenceNumber)
	return nil
}
