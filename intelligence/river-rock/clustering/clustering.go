package clustering

import (
	"bytes"
	"encoding/json"
	"io/ioutil"
	"log"
	"net/http"
	"strconv"
	"strings"
	//"net/http"
	_ "github.com/lib/pq"
)

func languageTrait(src string) string {
	switch src {
	case "en-us":
		return "en"
	}
	return src
}

func modalityTrait(src string) string {
	if strings.Contains(src, "Mobi") {
		return "mobile"
	}
	return "desktop"
}

func defaultTraits() map[string]interface{} {
	return map[string]interface{}{
		"lang":     "eng",
		"channel":  1,
		"modality": "desktop",
	}
}

func extractTraits(req *http.Request) map[string]interface{} {

	traits := defaultTraits()

	for k, v := range req.Header {
		switch k {
		case "Accept-Language":
			traits["lang"] = languageTrait(v[0])
		case "X-Channel":
			channel, _ := strconv.Atoi(v[0])
			traits["channel"] = channel
		case "User-Agent":
			traits["modality"] = modalityTrait(v[0])
		}
	}

	return traits
}

type BernardoPayload struct {
	Group  string                 `json:"group"`
	Scope  string                 `json:"scope"`
	Traits map[string]interface{} `json:"traits"`
}

func MapRequestToCluster(req *http.Request, bernardoUrl string) (int, error) {
	traits := extractTraits(req)
	jsonTraits, _ := json.Marshal(traits)
	log.Print("Traits: " + string(jsonTraits))

	payload := &BernardoPayload{
		Group:  "client",
		Scope:  "1.2",
		Traits: traits,
	}

	data, err := json.Marshal(payload)
	if err != nil {
		log.Printf("Error creating bernardo payload: %s", err)
		return 0, err
	}

	breq, err := http.NewRequest("GET", bernardoUrl, bytes.NewBuffer(data))
	if err != nil {
		log.Printf("Error creating bernardo req: %s", err)
		return 0, err
	}

	client := &http.Client{}
	res, err := client.Do(breq)
	if err != nil {
		log.Printf("Error querying bernardo : %s", err)
		return 0, err
	}

	defer res.Body.Close()
	clusterStr, err := ioutil.ReadAll(res.Body)
	if err != nil {
		log.Printf("Error reading bernardo response: %s", err)
		return 0, err
	}

	cluster, err := strconv.Atoi(string(clusterStr))
	if err != nil {
		log.Printf("Error converting bernardo response to a cluster id: %s, %s", res, err)
		return 0, err
	}

	log.Printf("cluster %v", cluster)

	return cluster, nil
}
