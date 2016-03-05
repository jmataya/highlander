
import React, { PropTypes } from 'react';
import LiveSearch from './live-search';

const LiveSearchAdapter = props => {
  const {searchActions, ...rest} = props;

  return (
    <LiveSearch
      {...rest}
      fetchSearches={searchActions.fetchSearches}
      saveSearch={searchActions.saveSearch}
      selectSavedSearch={searchActions.selectSearch}
      submitFilters={searchActions.addSearchFilters}
      submitPhrase={searchActions.addSearchPhrase}
      deleteSearch={searchActions.deleteSearch}
      updateSearch={searchActions.updateSearch}
    >
      {props.children}
    </LiveSearch>
  );
};

LiveSearchAdapter.propTypes = {
  searchActions: PropTypes.shape({
    fetchSearches: PropTypes.func,
    saveSearch: PropTypes.func,
    selectSearch: PropTypes.func,
    addSearchFilters: PropTypes.func,
    addSearchPhrase: PropTypes.func,
    deleteSearch: PropTypes.func,
    updateSearch: PropTypes.func
  }),
  singleSearch: PropTypes.bool,
  searches: PropTypes.object,
};

export default LiveSearchAdapter;
