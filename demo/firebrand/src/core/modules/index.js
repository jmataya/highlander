
import { combineReducers } from 'redux';
import { routeReducer } from 'react-router-redux';
import cart from './cart';
import checkout from './checkout';
import categories from './categories';
import sidebar from './sidebar';
import products from './products';
import productDetails from './product-details';
import countries from './countries';
import search from './search';
import auth from './auth';
import usermenu from './usermenu';

import { reducer as asyncReducer } from './async-utils';

const reducer = combineReducers({
  routing: routeReducer,
  asyncActions: asyncReducer,
  categories,
  cart,
  checkout,
  sidebar,
  products,
  productDetails,
  countries,
  search,
  auth,
  usermenu,
});

export default reducer;
