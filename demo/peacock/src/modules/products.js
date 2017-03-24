/* @flow */

import { createReducer } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';
import { addTaxonomyFilter, addTaxonomiesAggregation, addMustNotFilter, defaultSearch, termFilter } from 'lib/elastic';
import _ from 'lodash';
import { api } from 'lib/api';

export type Facet = {
  label: string,
  value: string
}

export type Facets = {
  name: string,
  kind: string,
  values: Array<Facet>,
}

export type Product = {
  id: number;
  context: string,
  title: string;
  description: string,
  images: ?Array<string>,
  currency: string,
  productId: number,
  salePrice: string,
  scope: string,
  skus: Array<string>,
  tags: Array<string>,
  albums: ?Array<Object> | Object,
};

const MAX_RESULTS = 1000;
const context = process.env.FIREBIRD_CONTEXT || 'default';
const GIFT_CARD_TAG = 'GIFT-CARD';

function apiCall(
  categoryName: ?string,
  productType: ?string,
  sorting: { direction: number, field: string },
  { ignoreGiftCards = true } = {}): Promise<*> {
  let payload = defaultSearch(context);

  if (ignoreGiftCards) {
    const giftCardTerm = termFilter('tags', GIFT_CARD_TAG);
    payload = addMustNotFilter(payload, giftCardTerm);
    const order = sorting.direction === -1 ? 'desc' : 'asc';
    // $FlowFixMe
    payload.sort = [{ [sorting.field]: { order } }];
  }

  // Example: adds 'color:Black' taxon. payload = addTaxonomyFilter(payload, 'color', 'Black');
  payload = addTaxonomiesAggregation(payload);

  return this.api.post(`/search/public/products_catalog_view/_search?size=${MAX_RESULTS}`, payload);
}

function searchGiftCards() {
  return apiCall.call({ api }, GIFT_CARD_TAG, null, {direction: 1, field: 'salesPrice'}, { ignoreGiftCards: false });
}

const {fetch, ...actions} = createAsyncActions('products', apiCall);

const initialState = {
  list: [],
};

const reducer = createReducer({
  [actions.succeeded]: (state, payload) => {
    const result = _.isEmpty(payload.result) ? [] : payload.result;
    return {
      ...state,
      list: result,
    };
  },
}, initialState);

export {
  reducer as default,
  fetch,
  searchGiftCards,
};
