package mocks

import (
	"github.com/FoxComm/middlewarehouse/models"
	"github.com/FoxComm/middlewarehouse/services"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/mock"
)

type SummaryServiceMock struct {
	mock.Mock
}

// implement service interface to pass mock as service (another solution?)
func (m *SummaryServiceMock) GetSummary() ([]*models.StockItemSummary, error) {
	args := m.Called()

	if model, ok := args.Get(0).([]*models.StockItemSummary); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (m *SummaryServiceMock) GetSummaryBySKU(code string) (*models.StockItemSummary, error) {
	args := m.Called(code)

	if model, ok := args.Get(0).(*models.StockItemSummary); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (m *SummaryServiceMock) CreateStockItemSummary(stockItemId uint, dbContext *gorm.DB) error {
	args := m.Called(stockItemId, dbContext)

	return args.Error(0)
}

func (m *SummaryServiceMock) UpdateStockItemSummary(stockItemId, typeId uint, qty int, status services.StatusChange, dbContext *gorm.DB) error {
	args := m.Called(stockItemId, typeId, qty, status, dbContext)

	return args.Error(0)
}
