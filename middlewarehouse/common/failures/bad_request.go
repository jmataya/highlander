package failures

import (
	"net/http"

	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
)

type badRequest struct {
	err error
}

func (failure badRequest) Status() int {
	return http.StatusBadRequest
}

func (failure badRequest) ToJSON() responses.ErrorResponse {
	return toJSON(failure.err)
}

func NewBadRequest(err error) badRequest {
	return badRequest{err: err}
}
