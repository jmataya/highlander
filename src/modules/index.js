import { routerStateReducer } from 'redux-router';
import { combineReducers } from 'redux';
import giftCards from './gift-cards';
import customers from './customers';
import groups from './groups';
import orders from './orders';
import rmas from './rmas';
import notes from './notes';
import skus from './skus';
import countries from './countries';
import regions from './regions';
import addressForm from './address-form';
import reasons from './reasons';

const rootReducer = combineReducers({
  router: routerStateReducer,
  giftCards,
  customers,
  orders,
  rmas,
  notes,
  skus,
  countries,
  regions,
  addressForm,
  reasons,
  groups,
});

export default rootReducer;
