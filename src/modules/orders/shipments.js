/* @flow */
// libs
import _ from 'lodash';
import Api, { request } from '../../lib/api';
import shipments from './shipments.json';

// helpers
import type { Store} from '../../lib/store-creator';
import createStore from '../../lib/store-creator';

const initialState = {
  shipments: [],
  unshippedItems: [],
};

const reducers = {
  setData: function (state: Object, data: Object): Object {
    return {
      ...state,
      ...data,
    };
  },
};

function load(actions: Object, state: Object, referenceNumber: string): Function {
  return dispatch => Promise.resolve(shipments)
    .then(
      data => {
        dispatch(actions.setData(data));
        return data;
      }
    );
}

const { actions, reducer } = createStore({
  path: 'orders.shipments',
  asyncActions: {
    load
  },
  reducers,
  initialState,
});

export {
  actions,
  reducer as default
};
