
import _ from 'lodash';
import makePagination from './base';
import reduceReducers from 'reduce-reducers';
import { apiStaticUrl, apiDynamicUrl } from './fetchers';

export function addPaginationParams(url, searchState) {
  let uriGetParams = [];

  if (searchState.from || searchState.size) {
    if (searchState.from) {
      uriGetParams = [`from=${searchState.from}`];
    }
    if (searchState.size) {
      uriGetParams = [...uriGetParams, `size=${searchState.size}`];
    }
  }

  if (uriGetParams.length) {
    const joinWith = url.indexOf('?') != -1 ? '&' : '?';
    return url + joinWith + uriGetParams.join('&');
  }

  return url;
}

export default function(url, namespace, moduleReducer) {
  const fetcher = _.isString(url) ? apiStaticUrl(url) : apiDynamicUrl(url);

  const {reducer, ...rest} = makePagination(namespace, fetcher);

  return {
    reducer: moduleReducer ? reduceReducers(reducer, moduleReducer) : reducer,
    ...rest
  };
}
