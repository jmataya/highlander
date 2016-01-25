
import _ from 'lodash';
import { assoc } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';
import makePagination from '../pagination/structured-store';
import Api from '../../lib/api';

const dataPath = storeCreditId => [storeCreditId];

const { makeActions, makeReducer } = makePagination('STORECREDIT_TRANSACTIONS', dataPath);

function storeCreditTransactionsUrl(customerId) {
  return `/customers/${customerId}/payment-methods/store-credit/transactions`;
}

const { fetch } = makeActions(storeCreditTransactionsUrl);
const reducer = makeReducer();

const fetchStoreCreditTransactions = (customerId, params = {}) => fetch(customerId, params);

export {
  reducer as default,
  fetchStoreCreditTransactions
};
