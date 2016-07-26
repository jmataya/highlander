package controllers

import (
	"bytes"
	"encoding/json"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"

	"github.com/gin-gonic/gin"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type GeneralControllerTestSuite struct {
	suite.Suite
	assert *assert.Assertions
	router *gin.Engine
}

func (suite *GeneralControllerTestSuite) Get(url string, args ...interface{}) *httptest.ResponseRecorder {
	request, _ := http.NewRequest("GET", url, nil)

	return suite.query(request, args...)
}

func (suite *GeneralControllerTestSuite) Post(url string, args ...interface{}) *httptest.ResponseRecorder {
	request, _ := http.NewRequest("POST", url, prepareBody(args[0]))

	return suite.query(request, args[1:]...)
}

func (suite *GeneralControllerTestSuite) Put(url string, args ...interface{}) *httptest.ResponseRecorder {
	request, _ := http.NewRequest("PUT", url, prepareBody(args[0]))

	return suite.query(request, args[1:]...)
}

func (suite *GeneralControllerTestSuite) Delete(url string, args ...interface{}) *httptest.ResponseRecorder {
	request, _ := http.NewRequest("DELETE", url, nil)

	return suite.query(request, args...)
}

func (suite *GeneralControllerTestSuite) query(request *http.Request, target ...interface{}) *httptest.ResponseRecorder {
	//record response
	response := httptest.NewRecorder()

	//serve request with router, writing to response
	suite.router.ServeHTTP(response, request)

	//return raw response or parse, if needed
	switch len(target) {
	case 0:
		return response
	case 1:
		return parseBody(response, target[0])
	default:
		panic("Unexpected number of arguments")
	}
}

func prepareBody(raw interface{}) io.Reader {
	switch raw.(type) {
	case string:
		return strings.NewReader(raw.(string))
	default:
		buffer := new(bytes.Buffer)
		json.NewEncoder(buffer).Encode(raw)

		return buffer
	}
}

func parseBody(response *httptest.ResponseRecorder, target interface{}) *httptest.ResponseRecorder {
	//decode if any data
	if response.Body.Len() != 0 {
		if err := json.NewDecoder(response.Body).Decode(target); err != nil {
			panic(err)
		}
	}

	return response
}
