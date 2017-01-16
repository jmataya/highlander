package main

import (
	"services"

	"net/http"
	"os"

	"github.com/labstack/echo"
)

func main() {
	PORT := ":" + os.Getenv("PORT")
	e := echo.New()
	e.GET("/ping", func(c echo.Context) error {
		return c.String(http.StatusOK, "pong")
	})
	e.GET("/productFunnel/:id", services.GetProductFunnel)
	e.GET("/productSum/list/:id", services.GetProductSum("list"))
	e.GET("/productSum/pdp/:id", services.GetProductSum("pdp"))
	e.GET("/productSum/cart/:id", services.GetProductSum("cart"))
	e.GET("/productSum/checkout/:id", services.GetProductSum("checkout"))
	e.Logger.Fatal(e.Start(PORT))
}
