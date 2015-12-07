import _ from 'lodash';
import nock from 'nock';

const { default: reducer, ...actions } = importSource('modules/reasons.js', [
  'reasonsRequested',
  'reasonsReceived',
  'reasonsFailed'
]);

describe('reasons module', function() {

  const reasonsPayload = require('../../fixtures/reasons.json');

  context('reducers', function() {

    it('reasonsRequested should return proper state', function() {
      const initialState = {
        isFetching: false,
        reasons: []
      };
      const newState = reducer(initialState, actions.reasonsRequested());
      expect(newState.isFetching).to.be.equal(true);
    });

    it('reasonsReceived should return proper state', function() {
      const initialState = {
        isFetching: true,
        reasons: []
      };
      const skus = [];
      const payload = reasonsPayload;
      const reasonType = 'donkeyResons';

      const newState = reducer(initialState, actions.reasonsReceived(payload, reasonType));
      expect(newState.isFetching).to.be.equal(false);
      expect(newState.reasons[reasonType]).to.deep.equal(payload.result);
    });

    it('reasonsFailed should return proper state', function() {
      const initialState = {
        isFetching: true,
        reasons: []
      };
      const newState = reducer(initialState, actions.reasonsFailed('error'));
      expect(newState.isFetching).to.be.equal(false);
    });

  });

});
