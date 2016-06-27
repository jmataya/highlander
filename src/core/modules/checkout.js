
/* @flow weak */

import _ from 'lodash';
import {createAction, createReducer} from 'redux-act';
import { assoc } from 'sprout-data';
import createAsyncActions from './async-utils';
import { fetchCountry } from 'modules/countries';
import { updateCart } from 'modules/cart';

export const AddressKind = {
  SHIPPING: 0,
  BILLING: 1,
};

export type AddressKindType = number;

export const EditStages = {
  SHIPPING: 0,
  DELIVERY: 1,
  BILLING: 2,
  FINISHED: 3,
};

export type EditStage = number;

export type ShippingAddress = {
  city?: string;
}

export type CheckoutState = {
  editStage: EditStage;
  shippingAddress: ShippingAddress;
  billingAddress: ShippingAddress;
};

export type BillingData = {
  holderName?: string;
  cardNumber?: string;
  expMonth?: string;
  expYear?: string;
}

export const setEditStage = createAction('CHECKOUT_SET_EDIT_STAGE');
export const setBillingData = createAction('CHECKOUT_SET_BILLING_DATA', (key, value) => [key, value]);

export const setAddressData = createAction('CHECKOUT_SET_ADDRESS_DATA', (kind, key, value) => [kind, key, value]);
export const extendAddressData = createAction('CHECKOUT_EXTEND_ADDRESS_DATA', (kind, props) => [kind, props]);
export const resetCheckout = createAction('CHECKOUT_RESET');

/* eslint-disable quotes, quote-props */

function _fetchShippingMethods() {
  return this.api.get('/v1/my/cart/shipping-methods');
}

/* eslint-enable quotes, quote-props */

const shippingMethodsActions = createAsyncActions('shipping-methods', _fetchShippingMethods);

export const fetchShippingMethods = shippingMethodsActions.fetch;
export const toggleSeparateBillingAddress = createAction('CHECKOUT_TOGGLE_BILLING_ADDRESS');

export function initAddressData(kind: AddressKindType): Function {
  return (dispatch, getState) => {
    const state = getState();

    const shippingAddress = getState().cart.shippingAddress;

    const countries = state.countries.list;

    const usaCountry = _.find(countries, {alpha3: 'USA'});
    const countryDetails = state.countries.details[usaCountry && usaCountry.id] || {
      regions: [],
    };

    let uiAddressData = {
      country: usaCountry,
      state: countryDetails.regions[0],
    };

    if (kind == AddressKind.SHIPPING && shippingAddress && shippingAddress.region) {
      dispatch(fetchCountry(shippingAddress.region.countryId)).then(() => {
        const countryInfo = getState().countries.details[shippingAddress.region.countryId];

        uiAddressData = _.pick(shippingAddress, ['name', 'address1', 'address2', 'city', 'zip', 'phoneNumber']);
        uiAddressData.country = countryInfo;
        uiAddressData.state = _.find(countryInfo.regions, {id: shippingAddress.region.id});

        dispatch(extendAddressData(kind, uiAddressData));
      });
    } else {
      dispatch(extendAddressData(kind, uiAddressData));
    }
  };
}

function addressToPayload(address) {
  const payload = _.pick(address, ['name', 'address1', 'address2', 'city', 'zip', 'phoneNumber']);
  payload.regionId = address.state.id;
  payload.phoneNumber = String(payload.phoneNumber);

  return payload;
}

export function saveShippingAddress(): Function {
  return (dispatch, getState, api) => {
    const shippingAddress = getState().checkout.shippingAddress;
    const payload = addressToPayload(shippingAddress);

    return api.post('/v1/my/cart/shipping-address', payload)
      .then(res => {
        dispatch(updateCart(res.result));
      });
  };
}

export function saveShippingMethod(): Function {
  return (dispatch, getState, api) => {
    const payload = {
      shippingMethodId: getState().cart.shippingMethod.id,
    };

    return api.patch('/v1/my/cart/shipping-method', payload)
      .then(res => {
        dispatch(updateCart(res.result));
      });
  };
}

export function saveGiftCard(code: string): Function {
  return (dispatch, getState, api) => {
    const payload = { code: code.trim() };

    return api.post('/v1/my/cart/payment-methods/gift-cards', payload)
      .then(res => {
        dispatch(updateCart(res.result));
      });
  };
}

export function saveCouponCode(code: string): Function {
  return (dispatch, getState, api) => {
    return api.post(`/v1/my/cart/coupon/${code.trim()}`, {})
      .then(res => {
        dispatch(updateCart(res));
      });
  };
}

export function addCreditCard(): Function {
  return (dispatch, getState, api) => {
    const billingData = getState().checkout.billingData;
    let payload = _.pick(billingData, ['holderName', 'cvv']);

    payload = {
      ...payload,
      cardNumber: billingData.cardNumber,
      expMonth: Number(billingData.expMonth),
      expYear: Number(billingData.expYear),
      isShipping: true,
    };

    if (getState().checkout.billingAddressIsSame) {
      payload.addressId = getState().cart.shippingAddress.id;
    } else {
      payload.address = addressToPayload(getState().checkout.billingAddress);
    }

    return api.post('/v1/my/payment-methods/credit-cards', payload)
      .then(creditCard => {
        console.info('added credit card', creditCard);

        const addCreditCardPayload = {
          creditCardId: creditCard.id,
        };

        return api.post('/v1/my/cart/payment-methods/credit-cards', addCreditCardPayload);
      })
      .then(res => {
        dispatch(updateCart(res.result));
      });
  };
}

// Place order from cart.
export function checkout(): Function {
  return (dispatch, getState, api) => {
    return api.post('/v1/my/cart/checkout').then(res => {
      return dispatch(updateCart(res));
    });
  };
}

const initialState: CheckoutState = {
  editStage: EditStages.SHIPPING,
  shippingAddress: {},
  billingAddress: {},
  billingData: {},
  billingAddressIsSame: true,
  shippingMethods: [],
};

const reducer = createReducer({
  [setEditStage]: (state, editStage: EditStage) => {
    return {
      ...state,
      editStage,
    };
  },
  [setAddressData]: (state, [kind, key, value]) => {
    const ns = kind == AddressKind.SHIPPING ? 'shippingAddress' : 'billingAddress';
    return assoc(state,
      [ns, key], value
    );
  },
  [extendAddressData]: (state, [kind, props]) => {
    const ns = kind == AddressKind.SHIPPING ? 'shippingAddress' : 'billingAddress';
    return assoc(state,
      [ns], props
    );
  },
  [setBillingData]: (state, [key, value]) => {
    return assoc(state,
      ['billingData', key], value
    );
  },
  [shippingMethodsActions.succeeded]: (state, list) => {
    return {
      ...state,
      shippingMethods: list,
    };
  },
  [toggleSeparateBillingAddress]: state => {
    return assoc(state,
      ['billingAddressIsSame'], !state.billingAddressIsSame
    );
  },
  [resetCheckout]: () => {
    return initialState;
  },
}, initialState);

export default reducer;
