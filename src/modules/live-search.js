import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';
import { assoc, get } from 'sprout-data';
import { post } from '../lib/search';
import Api from '../lib/api';
import { toQuery } from '../elastic/common';
import SearchTerm from '../paragons/search-term';

const emptyState = {
  isDirty: false,
  isEditingName: false,
  // isFetching = null, - fetching wasn't started yet
  // isFetching = true, - fetching was started
  // isFetching = false, - fetching was finished
  isFetching: null,
  isNew: false,
  options: [],
  results: {
    from: 0,
    size: 0,
    total: 0,
    rows: []
  },
  searches: [],
  searchValue: '',
  selectedIndex: -1
};

function _createAction(namespace, description, ...args) {
  const name = `${namespace}_${description}`.toUpperCase();
  return createAction(name, ...args);
}

export default function makeLiveSearch(namespace, searchTerms, scope) {
  const internalNS = namespace.toUpperCase();
  const cloneSearch = _createAction(internalNS, 'CLONE_SEARCH');
  const editSearchNameStart = _createAction(internalNS, 'EDIT_SEARCH_NAME_START');
  const editSearchNameCancel = _createAction(internalNS, 'EDIT_SEARCH_NAME_CANCEL');
  const editSearchNameComplete = _createAction(internalNS, 'EDIT_SEARCH_NAME_COMPLETE');
  const saveSearch = _createAction(internalNS, 'SAVE_SEARCH');
  const searchStart = _createAction(internalNS, 'SEARCH_START');
  const searchSuccess = _createAction(internalNS, 'SEARCH_SUCCESS');
  const searchFailure = _createAction(internalNS, 'SEARCH_FAILURE');
  const selectSavedSearch = _createAction(internalNS, 'SELECT_SAVED_SEARCH');
  const submitFilters = _createAction(internalNS, 'SUBMIT_FILTER');
  const fetchSearchesStart = _createAction(internalNS, 'FETCH_SEARCHES_START');
  const fetchSearchesSuccess = _createAction(internalNS, 'FETCH_SEARCHES_SUCCESS');
  const fetchSearchesFailure = _createAction(internalNS, 'FETCH_SEARCHES_FAILURE');

  const addSearchFilter = (url, filters) => {
    return dispatch => {
      dispatch(submitFilters(filters));
      const esQuery = toQuery(filters);
      dispatch(fetch(url, esQuery.toJSON()));
    };
  };

  const getSelectedSearch = (state) => {
    const selectedSearch = _.get(state, [namespace, 'list', 'selectedSearch']);
    return _.get(state, [namespace, 'list', 'savedSearches', selectedSearch]);
  };

  const fetch = (url, ...args) => {
    let fetchPromise;

    return (dispatch, getState) => {
      const { isFetching } = getSelectedSearch(getState());

      if (!isFetching) {
        dispatch(searchStart());
        fetchPromise = post(url, ...args)
          .then(
            res => dispatch(searchSuccess(res)),
            err => dispatch(searchFailure(err, fetch))
          );
      }

      return fetchPromise;
    };
  };

  const fetchSearches = () => {
    return dispatch => {
      dispatch(fetchSearchesStart());
      return Api.get(`/shared-search?=${scope}`)
        .then(
          searches => dispatch(fetchSearchesSuccess(searches)),
          err => dispatch(fetchSearchesFailure(err, fetchSearches))
        );
      };
  };

  const selectSearch = (url, idx) => {
    return (dispatch, getState) => {
      dispatch(selectSavedSearch(idx));

      const searchTerms = _.get(getSelectedSearch(getState()), 'searches', []);

      const esQuery = toQuery(searchTerms);
      dispatch(fetch(url, esQuery.toJSON()));
    };
  };

  const terms = searchTerms.map(st => new SearchTerm(st));
  const initialState = {
    fetchingSearches: false,
    potentialOptions: terms,
    selectedSearch: 0,
    savedSearches: [
      {
        ...emptyState,
        name: 'All',
        currentOptions: terms
      }
    ]
  };

  const reducer = createReducer({
    [cloneSearch]: (state) => _cloneSearch(state),
    [editSearchNameStart]: (state, idx) => _editSearchNameStart(state, idx),
    [editSearchNameCancel]: (state) => _editSearchNameCancel(state),
    [editSearchNameComplete]: (state, newName) => _editSearchNameComplete(state, newName),
    [saveSearch]: (state) => _saveSearch(state),
    [searchStart]: (state) => _searchStart(state),
    [searchSuccess]: (state, res) => _searchSuccess(state, res),
    [searchFailure]: (state, [err, source]) => _searchFailure(state, [err, source]),
    [selectSavedSearch]: (state, idx) => _selectSavedSearch(state, idx),
    [submitFilters]: (state, filters) => _submitFilters(state, filters),
    [fetchSearchesStart]: (state) => _fetchSearchesStart(state),
    [fetchSearchesSuccess]: (state, searches) => _fetchSearchesSuccess(state, searches),
    [fetchSearchesFailure]: (state, [err, source]) => _fetchSearchesFailure(state, [err, source])
  }, initialState);

  return {
    reducer: reducer,
    actions: {
      addSearchFilter,
      cloneSearch,
      editSearchNameStart,
      editSearchNameCancel,
      editSearchNameComplete,
      fetch,
      fetchSearches,
      saveSearch,
      searchStart,
      searchSuccess,
      searchFailure,
      selectSearch,
      selectSavedSearch,
      submitFilters,
      fetchSearchesStart,
      fetchSearchesSuccess,
      fetchSearchesFailure
    }
  };
}

function _cloneSearch(state) {
  const toClone = {
    ...state.savedSearches[state.selectedSearch],
    name: '',
    isEditingName: true,
    isNew: true
  };

  return {
    ...state,
    selectedSearch: state.savedSearches.length,
    savedSearches: [...state.savedSearches, toClone]
  };
}

function _editSearchNameStart(state, idx) {
  const newState = _selectSavedSearch(state, idx);
  return assoc(newState, ['savedSearches', newState.selectedSearch, 'isEditingName'], true);
}

function _editSearchNameCancel(state) {
  const currentState = state.savedSearches[state.selectedSearch];
  if (currentState.isNew) {
    const searches = [
      ...state.savedSearches.slice(0, state.selectedSearch),
      ...state.savedSearches.slice(state.selectedSearch + 1)
    ];
    return {
      ...state,
      savedSearches: searches,
      selectedSearch: 0
    };
  }
  return assoc(state, ['savedSearches', state.selectedSearch, 'isEditingName'], false);
}

function _editSearchNameComplete(state, newName) {
  if (!_.isEmpty(newName)) {
    const newState = assoc(state,
      ['savedSearches', state.selectedSearch, 'name'], newName,
      ['savedSearches', state.selectedSearch, 'isEditingName'], false
    );
    return newState;
  }

  return state;
}

function _saveSearch(state) {
  return assoc(state, ['savedSearches', state.selectedSearch, 'isDirty'], false);
}

function _selectSavedSearch(state, idx) {
  if (idx > -1 && idx < state.savedSearches.length) {
    return assoc(state,
      ['selectedSearch'], idx,
      ['savedSearches', state.selectedSearch, 'isEditingName'], false
    );
  }

  return state;
}

function _searchStart(state) {
  return assoc(state, ['savedSearches', state.selectedSearch, 'isFetching'], true);
}

function _searchSuccess(state, res) {
  // Needed because nginx returns an array when result is found and an object otherwise.
  const results = _.isEmpty(res.result) ? [] : res.result;
  const total = get(res, ['pagination', 'total'], 0);

  return assoc(state,
    ['savedSearches', state.selectedSearch, 'isFetching'], false,
    ['savedSearches', state.selectedSearch, 'results', 'rows'], results,
    ['savedSearches', state.selectedSearch, 'results', 'from'], 0,
    ['savedSearches', state.selectedSearch, 'results', 'size'], total,
    ['savedSearches', state.selectedSearch, 'results', 'total'], total
  );
}

function _searchFailure(state, [err, source]) {
  if (source === fetch) {
    console.error(err);
    return assoc(state, ['savedSearches', state.selectedSearch, 'isFetching'], false);
  }

  return state;
}

function _submitFilters(state, filters) {
  return assoc(state, ['savedSearches', state.selectedSearch, 'searches'], filters);
}

function _fetchSearchesStart(state) {
  return assoc(state, 'fetchingSearches', true);
}

function _fetchSearchesSuccess(state, searches) {
  return assoc(state, 'fetchingSearches', false);
}

function _fetchSearchesFailure(state, [err, source]) {
  if (source == fetchSuccess) {
    console.error(err);
    return assoc(state, 'fetchingSearches', false);
  }
  return assoc(state, 'fetchingSearches', false);
}
