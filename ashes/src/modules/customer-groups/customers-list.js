import makeLiveSearch from '../live-search';

const searchTerms = [
  {
    title: 'Customer',
    type: 'object',
    options: [
      {
        title: 'Name',
        type: 'string',
        term: 'name'
      }, {
        title: 'Email',
        type: 'string',
        term: 'email'
      }
    ]
  }
];

const { reducer, actions } = makeLiveSearch(
  'customerGroups.details.customers',
  searchTerms,
  'customers_search_view/_search',
  'customersScope',
  { skipInitialFetch: true }
);

export {
  reducer as default,
  actions
};
