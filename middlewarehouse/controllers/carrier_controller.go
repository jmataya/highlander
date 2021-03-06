package controllers

import (
	"net/http"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
	"github.com/FoxComm/highlander/middlewarehouse/services"

	"github.com/gin-gonic/gin"
)

type carrierController struct {
	service services.ICarrierService
}

func NewCarrierController(service services.ICarrierService) IController {
	return &carrierController{service}
}

func (controller *carrierController) SetUp(router gin.IRouter) {
	router.Use(FetchJWT)
	router.GET("", controller.getCarriers())
	router.GET(":id", controller.getCarrierByID())
	router.POST("", controller.createCarrier())
	router.PUT(":id", controller.updateCarrier())
	router.DELETE(":id", controller.deleteCarrier())
}

func (controller *carrierController) getCarriers() gin.HandlerFunc {
	return func(context *gin.Context) {
		carriers, err := controller.service.GetCarriers()
		//ensure fetched successfully
		if err != nil {
			context.AbortWithError(http.StatusInternalServerError, err)
			return
		}

		//convert to responses slice
		response := make([]*responses.Carrier, len(carriers))
		for i := range carriers {
			response[i] = responses.NewCarrierFromModel(carriers[i])
		}
		context.JSON(http.StatusOK, response)
	}
}

func (controller *carrierController) getCarrierByID() gin.HandlerFunc {
	return func(context *gin.Context) {
		//get id from context
		id, failure := paramUint(context, "id")
		if failure != nil {
			return
		}

		//get carrier by id
		carrier, err := controller.service.GetCarrierByID(id)
		if err != nil {
			handleServiceError(context, err)
			return
		}
		context.JSON(http.StatusOK, responses.NewCarrierFromModel(carrier))
	}
}

func (controller *carrierController) createCarrier() gin.HandlerFunc {
	return func(context *gin.Context) {
		//try parse payload
		payload := &payloads.Carrier{}
		if parse(context, payload) != nil {
			return
		}

		if !setScope(context, payload) {
			return
		}

		//try create
		carrier, err := controller.service.CreateCarrier(payload.Model())
		if err != nil {
			handleServiceError(context, err)
			return
		}
		context.JSON(http.StatusCreated, responses.NewCarrierFromModel(carrier))
	}
}

func (controller *carrierController) updateCarrier() gin.HandlerFunc {
	return func(context *gin.Context) {
		//try parse payload
		payload := &payloads.Carrier{}
		if parse(context, payload) != nil {
			return
		}

		//get id from context
		id, failure := paramUint(context, "id")
		if failure != nil {
			return
		}

		//try update
		model := payload.Model()
		model.ID = id
		carrier, err := controller.service.UpdateCarrier(model)

		if err != nil {
			handleServiceError(context, err)
			return
		}
		context.JSON(http.StatusOK, responses.NewCarrierFromModel(carrier))
	}
}

func (controller *carrierController) deleteCarrier() gin.HandlerFunc {
	return func(context *gin.Context) {
		id, failure := paramUint(context, "id")
		if failure != nil {
			return
		}

		if err := controller.service.DeleteCarrier(id); err != nil {
			handleServiceError(context, err)
			return
		}
		context.Status(http.StatusNoContent)
	}
}
