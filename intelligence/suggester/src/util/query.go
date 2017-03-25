package util

import (
	"encoding/json"
	"errors"
	"github.com/FoxComm/highlander/intelligence/suggester/src/responses"
	"net/http"
	"os"
)

var antHillSrvHost = os.Getenv("ANTHILL_HOST")

func getHostAndPort(srvName string) (string, string, error) {
	var host string
	var port string
	var portErr error

	host, port, portErr = LookupSrv(srvName)
	if portErr != nil {
		return "", "", portErr
	}

	return host, port, nil
}

func AntHillQuery(customerId string, channel string) (responses.AntHillResponse, error) {
	host, port, err := getHostAndPort(antHillSrvHost)
	if err != nil {
		return responses.AntHillResponse{}, errors.New("Unable to locate AntHill Srv Host")
	}

	action := "/prod-prod/full/" + customerId + "?channel=" + channel
	resp, reqErr := http.Get(host + port + action)
	if reqErr != nil {
		return responses.AntHillResponse{}, reqErr
	}

	var antHillResponse responses.AntHillResponse
	jsonErr := json.NewDecoder(resp.Body).Decode(&antHillResponse)
	if jsonErr != nil {
		return responses.AntHillResponse{}, jsonErr
	}

	return antHillResponse, nil
}
