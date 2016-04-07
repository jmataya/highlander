import { routerStateReducer } from 'redux-router';
import { combineReducers } from 'redux';

import giftCards from './gift-cards';
import customers from './customers';
import customerGroups from './customer-groups';
import orders from './orders';
import rmas from './rmas';
import notes from './notes';
import countries from './countries';
import regions from './regions';
import addressForm from './address-form';
import reasons from './reasons';
import activityNotifications from './activity-notifications';
import storeCreditTypes from './store-credit-types';
import activityTrail from './activity-trail';
import siteMenu from './site-menu';
import inventory from './inventory';
import expandableTables from './expandable-tables';
import products from './products';
import skus from './skus';
import user from './user';
import promotions from './promotions';
import { reducer as asyncReducer } from './async-utils';

const rootReducer = combineReducers({
  router: routerStateReducer,
  asyncActions: asyncReducer,
  giftCards,
  customers,
  orders,
  rmas,
  notes,
  user,
  countries,
  regions,
  addressForm,
  reasons,
  storeCreditTypes,
  customerGroups,
  activityNotifications,
  activityTrail,
  siteMenu,
  inventory,
  expandableTables,
  products,
  promotions,
  skus,
});

export default rootReducer;
