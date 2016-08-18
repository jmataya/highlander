package controllers

import (
	"net/http"
	"strings"

	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/api/responses"
	"github.com/FoxComm/middlewarehouse/models"
	"github.com/FoxComm/middlewarehouse/services"

	"github.com/gin-gonic/gin"
)

type shipmentController struct {
	shipmentService         services.IShipmentService
	shipmentLineItemService services.IShipmentLineItemService
}

func NewShipmentController(
	shipmentService services.IShipmentService,
	shipmentLineItemService services.IShipmentLineItemService,
) IController {
	return &shipmentController{shipmentService, shipmentLineItemService}
}

func (controller *shipmentController) SetUp(router gin.IRouter) {
	router.GET(":referenceNumbers", controller.getShipmentsByReferenceNumbers())
	router.POST("", controller.createShipment())
	router.PUT(":id", controller.updateShipment())
	router.POST("from-order", controller.createShipmentFromOrder())
}

func (controller *shipmentController) getShipmentsByReferenceNumbers() gin.HandlerFunc {
	return func(context *gin.Context) {
		referenceNumbers := strings.Split(context.Params.ByName("referenceNumbers"), ",")

		response := []*responses.Shipment{}
		for _, referenceNumber := range referenceNumbers {
			shipments, err := controller.shipmentService.GetShipmentsByReferenceNumber(referenceNumber)
			if err != nil {
				handleServiceError(context, err)
				return
			}

			for _, shipment := range shipments {
				response = append(response, responses.NewShipmentFromModel(shipment))
			}
		}

		context.JSON(http.StatusOK, response)
	}
}

func (controller *shipmentController) createShipment() gin.HandlerFunc {
	return func(context *gin.Context) {
		payload := &payloads.Shipment{}
		if parse(context, payload) != nil {
			return
		}

		shipment, err := controller.shipmentService.CreateShipment(models.NewShipmentFromPayload(payload))
		if err != nil {
			handleServiceError(context, err)
			return
		}

		context.JSON(http.StatusCreated, responses.NewShipmentFromModel(shipment))
	}
}

func (controller *shipmentController) updateShipment() gin.HandlerFunc {
	return func(context *gin.Context) {
		payload := &payloads.Shipment{}
		if parse(context, payload) != nil {
			return
		}

		id, failure := paramUint(context, "id")
		if failure != nil {
			return
		}

		model := models.NewShipmentFromPayload(payload)
		model.ID = id
		for i, _ := range model.ShipmentLineItems {
			model.ShipmentLineItems[i].ShipmentID = model.ID
		}
		shipment, err := controller.shipmentService.UpdateShipment(model)

		if err != nil {
			handleServiceError(context, err)
			return
		}

		context.JSON(http.StatusOK, responses.NewShipmentFromModel(shipment))
	}
}

func (controller *shipmentController) createShipmentFromOrder() gin.HandlerFunc {
	return func(context *gin.Context) {
		// TODO: Add the ability for middlewarehouse to programmatically create
		// shipments based on the order that's passed in. For now, it's super
		// simple and always creates one shipment per order.
		context.JSON(http.StatusOK, gin.H{"message": "I'm here!"})
	}
}
