package payloads

import (
	"encoding/json"
	"fmt"
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type ShipmentPayloadTestSuite struct {
	suite.Suite
}

func TestShipmentMethodPayloadSuite(t *testing.T) {
	suite.Run(t, new(ShipmentPayloadTestSuite))
}

func (suite *ShipmentPayloadTestSuite) Test_ShipmentMethodDecoding_RunsNormally() {
	//arrange
	carrierID, name := uint(1), "UPS 2 days ground"
	raw := fmt.Sprintf(`{
		"carrierId": %v,
		"name": "%v"
	}`, carrierID, name)
	decoder := json.NewDecoder(strings.NewReader(raw))

	//act
	var payload ShipmentMethod
	err := decoder.Decode(&payload)

	//assert
	assert.Nil(suite.T(), err)
	assert.Equal(suite.T(), carrierID, payload.CarrierID)
	assert.Equal(suite.T(), name, payload.Name)
}
