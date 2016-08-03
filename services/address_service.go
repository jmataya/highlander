package services

import (
	"github.com/FoxComm/middlewarehouse/models"
	"github.com/FoxComm/middlewarehouse/repositories"
)

type addressService struct {
	repository repositories.IAddressRepository
}

type IAddressService interface {
	GetAddressByID(id uint) (*models.Address, error)
	CreateAddress(address *models.Address) (*models.Address, error)
}

func NewAddressService(repository repositories.IAddressRepository) IAddressService {
	return &addressService{repository}
}

func (service *addressService) GetAddressByID(id uint) (*models.Address, error) {
	return service.repository.GetAddressByID(id)
}

func (service *addressService) CreateAddress(address *models.Address) (*models.Address, error) {
	return service.repository.CreateAddress(address)
}
