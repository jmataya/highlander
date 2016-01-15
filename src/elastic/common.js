import _ from 'lodash';
import elasticsearch from 'elasticsearch';
import ejs from 'elastic.js';
import moment from 'moment';

/**
 * Converts search terms into a query to ElasticSearch.
 * @param {array} filters An array of the Ashes version of a search terms.
 *                A filter is in the following format:
 *  {
 *    selectedTerm: 'someTerm',
 *    selectedOperator: 'eq',
 *    value: {
 *      type: 'bool',
 *      value: true
 *    }
 *  }
 * @returns The ElasticSearch query.
 */
export function toQuery(filters) {
  const esFilters = _.map(filters, filter => {
    return filter.selectedTerm.lastIndexOf('.') != -1
      ? createNestedFilter(filter)
      : createFilter(filter, ejs.TermsFilter, rangeToFilter);
  });

  return ejs
    .Request()
    .query(ejs.MatchAllQuery())
    .filter(ejs.AndFilter(esFilters));
}

function createFilter(filter, boolFn, rangeFn) {
  const { selectedTerm, selectedOperator, value: { type, value } } = filter;
  switch(type) {
    case 'bool':
      return boolFn(selectedTerm, selectedOperator, value);
    case 'currency':
    case 'enum':
    case 'number':
      return rangeFn(selectedTerm, selectedOperator, value);
    case 'string':
      return rangeFn(selectedTerm, selectedOperator, value.toLowerCase());
    case 'date':
      return dateRangeFilter(selectedTerm, selectedOperator, value, rangeFn);
  }
}

function createNestedFilter(filter) {
  const term = filter.selectedTerm;
  const path = term.slice(0, term.lastIndexOf('.'));
  const query = createFilter(
    filter,
    (term, operator, value) => ejs.MatchQuery(term, value),
    rangeToQuery
  );

  return ejs
    .NestedFilter(path)
    .query(ejs.BoolQuery().must(query));
}

function _newClient(opts = {}) {
  opts = _.merge({
    host: 'localhost:9200',
    apiVersion: '1.7',
  }, opts);
  return new elasticsearch.Client(opts);
}

export const newClient = _.memoize(_newClient);

export const DEFAULT_INDEX = 'phoenix';

function dateRangeFilter(field, operator, value, rangeFn) {
  const formattedDate = moment(value, 'MM/DD/YYYY').format('YYYY-MM-DD HH:mm:ss');
  const esDate = `${formattedDate}||/d`;

  switch(operator) {
    case 'eq':
      const dates = [esDate, `${esDate}+1d`];
      return rangeFn(field, 'gte__lte', dates);
    case 'neq':
      return ejs.OrFilter([
        rangeFn(field, 'lt', esDate),
        rangeFn(field, 'gte', `${esDate}+1d`)
      ]);
    case 'lt':
    case 'gte':
      return rangeFn(field, operator, esDate);
    case 'lte':
    case 'gt':
      return rangeFn(field, operator, `${esDate}+1d`);
  }
}

function _rangeTo(field, operator, value, eqFn, rangeFn) {
  const filter = rangeFn(field);
  switch(operator) {
    case 'eq':
      return eqFn(field, value);
    case 'gt':
    case 'gte':
    case 'lte':
    case 'from':
    case 'to':
    case 'lt':
      filter[operator](value);
      break;
    default:
      if (_.contains(operator, '__') && _.isArray(value)) {
        const [op1,op2] = operator.split('__');
        filter[op1](value[0]);
        filter[op2](value[1]);
        break;
      }
      console.error('operator', operator, 'isn\'t suitable for value', value);
      break;

  }
  return filter;
}

export function rangeToFilter(field, operator, value) {
  return _rangeTo(field, operator, value, ejs.TermsFilter, ejs.RangeFilter);
}

export function rangeToQuery(field, operator, value) {
  return _rangeTo(field, operator, value, ejs.MatchQuery, ejs.RangeQuery);
}
