/* @flow */
import _ from 'lodash';
import Api from 'lib/api';
import { createReducer } from 'redux-act';
import { transitionTo } from 'browserHistory';
import OrderParagon from 'paragons/order';

import createAsyncActions from 'modules/async-utils';

////////////////////////////////////////////////////////////////////////////////
// Cart Manipulation Actions

const _fetchCart = createAsyncActions(
  'fetchCart',
  (refNum: string) => Api.get(`/carts/${refNum}`)
);

const _fetchCustomerCart = createAsyncActions(
  'fetchCustomerCart',
  (customerId: number) => Api.get(`/customers/${customerId}/cart`)
);

export const fetchCart = _fetchCart.perform;
export const fetchCustomerCart = _fetchCustomerCart.perform;
export const clearFetchCartErrors = _fetchCart.clearErrors;

////////////////////////////////////////////////////////////////////////////////
// Line Item Actions

const _updateLineItemCount = createAsyncActions(
  'updateLineItemCount',
  (refNum: string, payload: Object) => Api.post(`/orders/${refNum}/line-items`, payload)
);

export function updateLineItemCount(refNum: string, sku: string, quantity: number): Function {
  const payload = [{ sku, quantity }];
  return _updateLineItemCount.perform(refNum, payload);
}

export function deleteLineItem(refNum: string, sku: string): Function {
  const payload = [{ sku, quantity: 0 }];
  return _updateLineItemCount.perform(refNum, payload);
}

////////////////////////////////////////////////////////////////////////////////
// Shipping Address Actions

const _chooseShippingAddress = createAsyncActions(
  'chooseShippingAddress',
  (refNum: string, addressId: number) => Api.patch(`/orders/${refNum}/shipping-address/${addressId}`)
);

const _createShippingAddress = createAsyncActions(
  'createShippingAddress',
  (refNum: string, payload: Object) => Api.post(`/orders/${refNum}/shipping-address`, payload)
);

const _updateShippingAddress = createAsyncActions(
  'updateShippingAddress',
  (refNum: string, payload: Object) => Api.patch(`/orders/${refNum}/shipping-address`, payload)
);

const _deleteShippingAddress = createAsyncActions(
  'deleteShippingAddress',
  (refNum: string) => Api.delete(`/orders/${refNum}/shipping-address`)
);

export const chooseShippingAddress = _chooseShippingAddress.perform;
export const createShippingAddress = _createShippingAddress.perform;
export const updateShippingAddress = _updateShippingAddress.perform;
export const deleteShippingAddress = _deleteShippingAddress.perform;

////////////////////////////////////////////////////////////////////////////////
// Shipping Method Actions

const _updateShippingMethod = createAsyncActions(
  'updateShippingMethod',
  (refNum: string, shippingMethodId: number) => {
    const payload = { shippingMethodId };
    return Api.patch(`/orders/${refNum}/shipping-method`, payload);
  }
);

export const updateShippingMethod = _updateShippingMethod.perform;

////////////////////////////////////////////////////////////////////////////////
// Payment Method Actions

const _selectCreditCard = createAsyncActions(
  'selectCreditCard',
  (refNum: string, creditCardId: number) => {
    const payload = { creditCardId };
    return Api.post(`${paymentsBasePath(refNum)}/credit-cards`, payload);
  }
);

const _setStoreCreditPayment = createAsyncActions(
  'setStoreCreditPayment',
  (refNum: string, amount: number) => {
    const payload = { amount };
    return Api.post(`${paymentsBasePath(refNum)}/store-credit`, payload);
  }
);

const _addGiftCardPayment = createAsyncActions(
  'addGiftCardPayment',
  (refNum: string, code: string, amount: number) => {
    const payload = { code, amount };
    return Api.post(`${paymentsBasePath(refNum)}/gift-cards`, payload);
  }
);

const _editGiftCardPayment = createAsyncActions(
  'editGiftCardPayment',
  (refNum: string, code: string, amount: number) => {
    const payload = { code, amount };
    return Api.patch(`${paymentsBasePath(refNum)}/gift-cards`, payload);
  }
);

const _deleteCreditCardPayment = createAsyncActions(
  'deleteCreditCardPayment',
  (refNum: string) => Api.delete(`${paymentsBasePath(refNum)}/credit-cards`)
);

const _deleteGiftCardPayment = createAsyncActions(
  'deleteGiftCardPayment',
  (refNum: string, code: string) => {
    return Api.delete(`${paymentsBasePath(refNum)}/gift-cards/${code}`);
  }
);

const _deleteStoreCreditPayment = createAsyncActions(
  'deleteStoreCreditPayment',
  (refNum: string) => Api.delete(`${paymentsBasePath(refNum)}/store-credit`)
);

export const selectCreditCard = _selectCreditCard.perform;
export const setStoreCreditPayment = _setStoreCreditPayment.perform;
export const addGiftCardPayment = _addGiftCardPayment.perform;
export const editGiftCardPayment = _editGiftCardPayment.perform;
export const deleteCreditCardPayment = _deleteCreditCardPayment.perform;
export const deleteGiftCardPayment = _deleteGiftCardPayment.perform;
export const deleteStoreCreditPayment = _deleteStoreCreditPayment.perform;

function paymentsBasePath(refNum: string): string {
  return `/orders/${refNum}/payment-methods`;
}

////////////////////////////////////////////////////////////////////////////////
// Checkout Actions

const _checkout = createAsyncActions(
  'checkout',
  (refNum: string) => Api.post(`/orders/${refNum}/checkout`)
);

export function checkout(refNum: string): Function {
  return dispatch => dispatch(_checkout.perform(refNum)).then(() => {
    transitionTo('order', { order: refNum });
  });
}

function parseMessages(messages, state) {
  return _.reduce(messages, (results, message) => {
    if (message.indexOf('items') != -1) {
      return { ...results, itemsStatus: state };
    } else if (message.indexOf('empty cart') != -1) {
      return { ...results, itemsStatus: state };
    } else if (message.indexOf('shipping address') != -1) {
      return { ...results, shippingAddressStatus: state };
    } else if (message.indexOf('shipping method') != -1) {
      return { ...results, shippingMethodStatus: state };
    } else if (message.indexOf('payment method') != -1) {
      return { ...results, paymentMethodStatus: state };
    } else if (message.indexOf('insufficient funds') != -1) {
      return { ...results, paymentMethodStatus: state };
    }

    return results;
  }, {});
}


const initialState = {
  isCheckingOut: false,
  cart: {},
  validations: {
    errors: [],
    warnings: [],
    itemsStatus: 'success',
    shippingAddressStatus: 'success',
    shippingMethodStatus: 'success',
    paymentMethodStatus: 'success'
  }
};

function receiveCart(state, payload) {
  const order = _.get(payload, 'result', payload);
  const errors = _.get(payload, 'errors', []);
  const warnings = _.get(payload, 'warnings', []);

  // Initial state (assume in good standing)
  const status = {
    itemsStatus: 'success',
    shippingAddressStatus: 'success',
    shippingMethodStatus: 'success',
    paymentMethodStatus: 'success',

    // Find warnings
    ...parseMessages(warnings, 'warning'),

    // Find errors
    ...parseMessages(errors, 'error')
  };

  return {
    ...state,
    cart: new OrderParagon(order),
    validations: {
      errors: errors,
      warnings: warnings,
      ...status
    }
  };
}

function resetCart(state) {
  return {
    ...state,
    ...initialState,
  };
}

const reducer = createReducer({
  [_fetchCart.succeeded]: receiveCart,
  [_fetchCart.failed]: resetCart,
  [_fetchCustomerCart.succeeded]: receiveCart,
  [_updateLineItemCount.succeeded]: receiveCart,
  [_chooseShippingAddress.succeeded]: receiveCart,
  [_createShippingAddress.succeeded]: receiveCart,
  [_updateShippingAddress.succeeded]: receiveCart,
  [_deleteShippingAddress.succeeded]: receiveCart,
  [_updateShippingMethod.succeeded]: receiveCart,
  [_selectCreditCard.succeeded]: receiveCart,
  [_setStoreCreditPayment.succeeded]: receiveCart,
  [_addGiftCardPayment.succeeded]: receiveCart,
  [_editGiftCardPayment.succeeded]: receiveCart,
  [_deleteCreditCardPayment.succeeded]: receiveCart,
  [_deleteGiftCardPayment.succeeded]: receiveCart,
  [_deleteStoreCreditPayment.succeeded]: receiveCart,
  [_checkout.succeeded]: receiveCart,
}, initialState);

export default reducer;
