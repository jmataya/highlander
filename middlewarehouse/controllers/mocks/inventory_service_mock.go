package mocks

import (
	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/services"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/mock"
)

type InventoryServiceMock struct {
	mock.Mock
}

func (m *InventoryServiceMock) WithTransaction(txn *gorm.DB) services.IInventoryService {
	return m
}

func (m *InventoryServiceMock) GetStockItems() ([]*models.StockItem, error) {
	args := m.Called()

	if model, ok := args.Get(0).([]*models.StockItem); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (m *InventoryServiceMock) GetStockItemById(id uint) (*models.StockItem, error) {
	args := m.Called(id)

	if model, ok := args.Get(0).(*models.StockItem); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (m *InventoryServiceMock) GetAFSByID(id uint, unitType models.UnitType) (*models.AFS, error) {
	args := m.Called(id, unitType)

	if model, ok := args.Get(0).(*models.AFS); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (m *InventoryServiceMock) GetAFSBySKU(sku string, unitType models.UnitType) (*models.AFS, error) {
	args := m.Called(sku, unitType)

	if model, ok := args.Get(0).(*models.AFS); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (m *InventoryServiceMock) CreateStockItem(stockItem *models.StockItem) (*models.StockItem, error) {
	args := m.Called(stockItem)

	if model, ok := args.Get(0).(*models.StockItem); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (m *InventoryServiceMock) IncrementStockItemUnits(id uint, unitType models.UnitType, units []*models.StockItemUnit) error {
	args := m.Called(id, unitType, units)

	return args.Error(0)
}

func (m *InventoryServiceMock) DecrementStockItemUnits(id uint, unitType models.UnitType, qty int) error {
	args := m.Called(id, unitType, qty)

	return args.Error(0)
}

func (m *InventoryServiceMock) HoldItems(res *payloads.Reservation) error {
	args := m.Called(res)

	return args.Error(0)
}

func (m *InventoryServiceMock) ReserveItems(refNum string) error {
	args := m.Called(refNum)

	return args.Error(0)
}

func (m *InventoryServiceMock) ReleaseItems(refNum string) error {
	args := m.Called(refNum)

	return args.Error(0)
}

func (m *InventoryServiceMock) ShipItems(refNum string) error {
	args := m.Called(refNum)

	return args.Error(0)
}

func (m *InventoryServiceMock) DeleteItems(refNum string) error {
	args := m.Called(refNum)

	return args.Error(0)
}
