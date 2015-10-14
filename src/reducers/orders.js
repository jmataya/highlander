'use strict';

import * as actionTypes from '../actions/orders';

const initialState = {
  isFetching: false,
  didInvalidate: true,
  items: []
};

export function orders(state = initialState, action) {
  switch (action.type) {
    case actionTypes.ORDERS_REQUEST:
      return {
        ...state,
        isFetching: true,
        didInvalidate: false
      };
    case actionTypes.ORDERS_SUCCESS:
      return {
        ...state,
        isFetching: false,
        didInvalidate: false,
        items: action.items
      };
    case actionTypes.ORDERS_FAILED:
      return {
        ...state,
        isFetching: false,
        didInvalidate: false
      };
    default:
      return state;
  }
}