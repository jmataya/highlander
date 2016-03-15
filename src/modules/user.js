/** @flow */
import { createAction, createReducer } from 'redux-act';
import fetch from 'isomorphic-fetch';
import Api from '../lib/api';

// types

export type TUser = {name: String, email: String};

export type LoginPayload = {
  email: string,
  password: string,
  kind: string,
};

export type UserState = {
  err: ?String,
  current: ?TUser,
  isFetching: boolean,
};

export const setUser = createAction('USER_SET');
const authStart = createAction('USER_AUTH_START');
const authError = createAction('USER_AUTH_ERROR');


export function authenticate(payload: LoginPayload): ActionDispatch {

  const headers = {'Content-Type': 'application/json;charset=UTF-8'};

  return dispatch => {
    dispatch(authStart());
    return fetch(Api.apiURI('/public/login'), {
      method: 'POST',
      body: JSON.stringify(payload),
      credentials: "same-origin",
      headers,
    }).then(response => {
      if (response.status == 200 && response.headers.get('jwt')) {
        localStorage.setItem('jwt', response.headers.get('jwt'));
        return response.json();
      }
      throw new Error("Server error, try again later. Sorry for inconvenience :(");
    }).then((token: TUser) => {
      if (token.email && token.name) {
        localStorage.setItem('user', JSON.stringify(token));
        return dispatch(setUser(token));
      }
      throw new Error("Server error, try again later. Sorry for inconvenience :(");
    }).catch(reason => {
      dispatch(authError(reason));
      throw new Error(reason);
    });
  };
}

const initialState = {isFetching: false};

const reducer = createReducer({
  [setUser]: (state: UserState, user: TUser) => {
    return {
      ...state,
      current: user,
      isFetching: false,
    };
  },
  [authStart]: (state: UserState) => {
    return {
      ...state,
      err: null,
      isFetching: true,
    };
  },
  [authError]: (state: UserState, error: string) => {
    return {
      ...state,
      err: error,
      isFetching: false,
    };
  },
}, initialState);

export default reducer;
