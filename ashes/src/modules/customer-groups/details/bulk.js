// libs
import { get, partial } from 'lodash';

// helpers
import Api from 'lib/api';
import { singularize } from 'fleck';
import createStore from 'lib/store-creator';

// data
import { reducers } from '../../bulk';


const deleteCustomersFromGroup = (actions, groupId, customersIds) => dispatch => {
  dispatch(actions.bulkRequest());

  return Api.post(`/customer-groups/${groupId}/customers`, { toAdd: [], toDelete: customersIds, })
    .then(() => dispatch(actions.bulkDone(customersIds, null)))
    .catch(error => dispatch(actions.bulkError(error)));
};

const { actions, reducer } = createStore({
  path: 'customerGroups.details.bulk',
  actions: {
    deleteCustomersFromGroup,
  },
  reducers,
});

export {
  actions,
  reducer as default
};
