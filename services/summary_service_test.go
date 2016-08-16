package services

import (
	"testing"

	"github.com/FoxComm/middlewarehouse/common/db/config"
	"github.com/FoxComm/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/middlewarehouse/fixtures"
	"github.com/FoxComm/middlewarehouse/models"
	"github.com/FoxComm/middlewarehouse/repositories"

	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type summaryServiceTestSuite struct {
	suite.Suite
	service          ISummaryService
	inventoryService IInventoryService
	si               *models.StockItem
	unitCost         int
	onHand           int
	db               *gorm.DB
	assert           *assert.Assertions
}

func TestSummaryServiceSuite(t *testing.T) {
	suite.Run(t, new(summaryServiceTestSuite))
}

func (suite *summaryServiceTestSuite) SetupSuite() {
	tasks.TruncateTables([]string{
		"stock_items",
		"stock_locations",
		"inventory_search_view",
	})

	suite.db, _ = config.DefaultConnection()

	summaryRepository := repositories.NewSummaryRepository(suite.db)
	stockItemRepository := repositories.NewStockItemRepository(suite.db)
	stockLocationRepository := repositories.NewStockLocationRepository(suite.db)
	stockItemUnitRepository := repositories.NewStockItemUnitRepository(suite.db)

	suite.service = NewSummaryService(summaryRepository, stockItemRepository)

	inventoryService := NewInventoryService(stockItemRepository, stockItemUnitRepository, suite.service)
	stockLocationService := NewStockLocationService(stockLocationRepository)

	sl, _ := stockLocationService.CreateLocation(fixtures.GetStockLocation())
	si, _ := inventoryService.CreateStockItem(fixtures.GetStockItem(sl.ID))

	suite.si = si
	suite.onHand = 10
	suite.unitCost = 5000
}

func (suite *summaryServiceTestSuite) SetupTest() {
	tasks.TruncateTables([]string{
		"stock_item_summaries",
		"stock_item_transactions",
		"inventory_search_view",
	})

	// setup initial summary for all tests
	suite.service.CreateStockItemSummary(suite.si.ID)
	suite.service.UpdateStockItemSummary(suite.si.ID, models.Sellable, suite.onHand, models.StatusChange{To: models.StatusOnHand})
}

func (suite *summaryServiceTestSuite) Test_Increment_OnHand() {
	err := suite.service.UpdateStockItemSummary(suite.si.ID, models.Sellable, 5, models.StatusChange{To: models.StatusOnHand})
	suite.Nil(err)

	summary := models.StockItemSummary{}
	err = suite.db.First(&summary, suite.si.ID).Error
	suite.Nil(err)
	suite.Equal(suite.onHand+5, summary.OnHand)
	suite.Equal(suite.onHand+5, summary.AFS)
	suite.Equal((suite.onHand+5)*suite.unitCost, summary.AFSCost)
}

func (suite *summaryServiceTestSuite) Test_Increment_OnHold() {
	err := suite.service.UpdateStockItemSummary(suite.si.ID, models.Sellable, 5, models.StatusChange{To: models.StatusOnHold})
	suite.Nil(err)

	summary := models.StockItemSummary{}
	err = suite.db.First(&summary, suite.si.ID).Error
	suite.Nil(err)
	suite.Equal(suite.onHand, summary.OnHand)
	suite.Equal(5, summary.OnHold)
}

func (suite *summaryServiceTestSuite) Test_Increment_Reserved() {
	err := suite.service.UpdateStockItemSummary(suite.si.ID, models.Sellable, 5, models.StatusChange{To: models.StatusReserved})
	suite.Nil(err)

	summary := models.StockItemSummary{}
	err = suite.db.First(&summary, suite.si.ID).Error
	suite.Nil(err)
	suite.Equal(suite.onHand, summary.OnHand)
	suite.Equal(5, summary.Reserved)
}

func (suite *summaryServiceTestSuite) Test_Increment_Chain() {
	suite.service.UpdateStockItemSummary(suite.si.ID, models.Sellable, 5, models.StatusChange{From: models.StatusOnHand, To: models.StatusOnHold})

	summary := models.StockItemSummary{}
	suite.db.First(&summary, suite.si.ID)
	suite.Equal(suite.onHand, summary.OnHand)
	suite.Equal(5, summary.OnHold)
	suite.Equal(0, summary.Reserved)
	suite.Equal(5, summary.AFS)
	suite.Equal(5*suite.unitCost, summary.AFSCost)

	suite.service.UpdateStockItemSummary(suite.si.ID, models.Sellable, 2, models.StatusChange{From: models.StatusOnHold, To: models.StatusReserved})

	suite.db.First(&summary, suite.si.ID)
	suite.Equal(suite.onHand, summary.OnHand)
	suite.Equal(3, summary.OnHold)
	suite.Equal(2, summary.Reserved)
	suite.Equal(5, summary.AFS)
	suite.Equal(5*suite.unitCost, summary.AFSCost)

	suite.service.UpdateStockItemSummary(suite.si.ID, models.Sellable, 1, models.StatusChange{From: models.StatusReserved, To: models.StatusOnHand})

	suite.db.First(&summary, suite.si.ID)
	suite.Equal(suite.onHand, summary.OnHand)
	suite.Equal(3, summary.OnHold)
	suite.Equal(1, summary.Reserved)
	suite.Equal(6, summary.AFS)
	suite.Equal(6*suite.unitCost, summary.AFSCost)
}

func (suite *summaryServiceTestSuite) Test_GetSummary() {
	suite.service.UpdateStockItemSummary(suite.si.ID, models.Sellable, 5, models.StatusChange{To: models.StatusOnHand})

	summary, err := suite.service.GetSummary()
	suite.Nil(err)

	suite.NotNil(summary)
	suite.Equal(4, len(summary))
	suite.Equal(suite.si.StockLocationID, summary[0].StockItem.StockLocation.ID)
}

func (suite *summaryServiceTestSuite) Test_GetSummaryBySKU() {
	summary, err := suite.service.GetSummaryBySKU(suite.si.SKU)
	suite.Nil(err)

	suite.NotNil(summary)
	suite.Equal(suite.onHand, summary[0].OnHand)
}

func (suite *summaryServiceTestSuite) Test_GetSummaryBySKU_NotFoundSKU() {
	_, err := suite.service.GetSummaryBySKU("NO-SKU")
	suite.NotNil(err, "There should be an error as entity should not be found")
}

func (suite *summaryServiceTestSuite) Test_GetSummaryBySKU_NonZero() {
	err := suite.service.UpdateStockItemSummary(suite.si.ID, models.Sellable, 5, models.StatusChange{To: models.StatusOnHand})
	suite.Nil(err)

	summary, err := suite.service.GetSummaryBySKU(suite.si.SKU)
	suite.Equal(suite.onHand+5, summary[0].OnHand)
}
