import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';
import { assoc, get } from 'sprout-data';
import { post } from '../lib/search';
import { toQuery } from '../elastic/common';
import SearchTerm from '../paragons/search-term';
import util from 'util';

const emptyState = {
  isDirty: false,
  isEditingName: false,
  isFetching: false,
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

export default function makeLiveSearch(namespace, searchTerms) {
  const cloneSearch = _createAction(namespace, 'CLONE_SEARCH');
  const deleteSearchFilter = _createAction(namespace, 'DELETE_SEARCH_FILTER');
  const editSearchNameStart = _createAction(namespace, 'EDIT_SEARCH_NAME_START');
  const editSearchNameCancel = _createAction(namespace, 'EDIT_SEARCH_NAME_CANCEL');
  const editSearchNameComplete = _createAction(namespace, 'EDIT_SEARCH_NAME_COMPLETE');
  const saveSearch = _createAction(namespace, 'SAVE_SEARCH');
  const searchStart = _createAction(namespace, 'SEARCH_START');
  const searchSuccess = _createAction(namespace, 'SEARCH_SUCCESS');
  const searchFailure = _createAction(namespace, 'SEARCH_FAILURE');
  const selectSavedSearch = _createAction(namespace, 'SELECT_SAVED_SEARCH');
  const submitFilter = _createAction(namespace, 'SUBMIT_FILTER');

  const addSearchFilter = (url, searchTerm) => {
    return dispatch => {
      dispatch(submitFilter(searchTerm));

      const filters = [searchTerm];
      console.log(filters);

      const esQuery = toQuery(filters);
      console.log(esQuery);

      dispatch(fetch(url, esQuery.toJSON()));
    };
  };

  const fetch = (url, ...args) => {
    return dispatch => {
      dispatch(searchStart());
      return post(url, ...args)
        .then(
          res => dispatch(searchSuccess(res)),
          err => dispatch(searchFailure(err, fetch))
        );
    };
  };

  const terms = searchTerms.map(st => new SearchTerm(st));
  const initialState = {
    potentialOptions: terms,
    selectedSearch: 0,
    savedSearches: [
      {
        ...emptyState,
        name: 'All',
        currentOptions: terms
      }, {
        ...emptyState,
        name: 'Remorse Hold',
        currentOptions: terms,
        searches: ['Order : State : Remorse Hold']
      }, {
        ...emptyState,
        name: 'Manual Hold',
        currentOptions: terms,
        searches: ['Order : State : Manual Hold']
      }, {
        ...emptyState,
        name: 'Fraud Hold',
        currentOptions: terms,
        searches: ['Order : State : Fraud Hold']
      }
    ]
  };

  const reducer = createReducer({
    [cloneSearch]: (state) => _cloneSearch(state),
    [deleteSearchFilter]: (state, idx) => _deleteSearchFilter(state, idx),
    [editSearchNameStart]: (state, idx) => _editSearchNameStart(state, idx),
    [editSearchNameCancel]: (state) => _editSearchNameCancel(state),
    [editSearchNameComplete]: (state, newName) => _editSearchNameComplete(state, newName),
    [saveSearch]: (state) => _saveSearch(state),
    [searchStart]: (state) => _searchStart(state),
    [searchSuccess]: (state, res) => _searchSuccess(state, res),
    [searchFailure]: (state, [err, source]) => _searchFailure(state, [err, source]),
    [selectSavedSearch]: (state, idx) => _selectSavedSearch(state, idx),
    [submitFilter]: (state, searchTerm) => _submitFilter(state, searchTerm)
  }, initialState);

  return {
    reducer: reducer,
    actions: {
      addSearchFilter,
      cloneSearch,
      deleteSearchFilter,
      editSearchNameStart,
      editSearchNameCancel,
      editSearchNameComplete,
      fetch,
      saveSearch,
      searchStart,
      searchSuccess,
      searchFailure,
      selectSavedSearch,
      submitFilter
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

function _deleteSearchFilter(state, idx) {
  const curSearches = _.get(state, ['savedSearches', state.selectedSearch, 'searches'], []);
  const curValue = _.get(state, ['savedSearches', state.selectedSearch, 'searchValue'], '');

  if (!_.isEmpty(curSearches) && _.isEmpty(curValue)) {
    const newSearches = _.without(curSearches, curSearches[idx]);
    return assoc(state,
      ['savedSearches', state.selectedSearch, 'isDirty'], true,
      ['savedSearches', state.selectedSearch, 'searches'], newSearches
    );
  }

  return state;
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
  const results = get(res, 'result', []);
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

function _submitFilter(state, searchTerm) {
  const currentSearches = get(state, ['savedSearches', state.selectedSearch, 'searches'], []);
  const newSearches = [...currentSearches, searchTerm];
  return assoc(state, ['savedSearches', state.selectedSearch, 'searches'], newSearches);
}
