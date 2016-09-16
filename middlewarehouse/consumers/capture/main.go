package main

import (
	"log"

	"github.com/FoxComm/metamorphosis"
	"github.com/FoxComm/highlander/middlewarehouse/consumers"
	"github.com/FoxComm/highlander/middlewarehouse/consumers/capture/lib"

	_ "github.com/jpfuentes2/go-env/autoload"
)

const (
	clientID = "capture-01"
	groupID  = "mwh-capture-consumers"
)

func main() {
	config, err := consumers.MakeConsumerConfig()
	if err != nil {
		log.Fatalf("Unable to initialize consumer with error: %s", err.Error())
	}

	capConf, err := MakeCaptureConsumerConfig()
	if err != nil {
		log.Fatalf("Unable to initialize consumer with error: %s", err.Error())
	}

	consumer, err := metamorphosis.NewConsumer(config.ZookeeperURL, config.SchemaRepositoryURL)
	if err != nil {
		log.Fatalf("Unable to connect to Kafka with error %s", err.Error())
	}

	consumer.SetGroupID(groupID)
	consumer.SetClientID(clientID)

	client := lib.NewPhoenixClient(capConf.PhoenixURL, capConf.PhoenixUser, capConf.PhoenixPassword)
	if err := client.Authenticate(); err != nil {
		log.Fatalf("Unable to authenticate with Phoenix with error %s", err.Error())
	}

	oh, err := NewShipmentHandler(config.MiddlewarehouseURL, client)
	if err != nil {
		log.Fatalf("Can't create handler for orders with error %s", err.Error())
	}

	consumer.RunTopic(config.Topic, config.Partition, oh.Handler)
}
