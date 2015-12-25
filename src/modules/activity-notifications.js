
import Api from '../lib/api';
import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';
import { assoc, deepMerge } from 'sprout-data';

const notificationReceived = createAction('NOTIFICATION_RECEIVED');
export const toggleNotifications = createAction('NOTIFICATIONS_TOGGLE');
export const markAsRead = createAction('NOTIFICATIONS_MARK_AS_READ');

export function startFetchingNotifications() {
  console.log('starting fetching');
  return (dispatch) => {
    const eventSource = new EventSource('/sse/v1/notifications/1', {withCredentials: true});

    eventSource.onmessage = function(e) {
      console.log(e);
      if (_.isEmpty(e.data)) {
        console.log('heartbeat');
      } else {
        console.log('Received data');
        console.log(e.data);
        const data = JSON.parse(e.data);
        dispatch(notificationReceived(data));
      }
    };

    eventSource.onopen = function(e) {
      console.log('Connection was opened.');
    };

    eventSource.onerror = function(e) {
      console.log('Connection was closed.');
    };
  };
}

export function markAsReadAndClose() {
  console.log('mark all as read');
  return dispatch => {
    dispatch(markAsRead());
    dispatch(toggleNotifications());
  }
}

const initialState = {
  displayed: false
};

const reducer = createReducer({
  [notificationReceived]: (state, data) => {
    const notificationList = _.get(state, 'notifications', []);
    const notReadData = assoc(data, 'isRead', false);
    const updatedNotifications = notificationList.concat([notReadData]);
    const newCount = updatedNotifications.reduce((acc, item) => {
      if (!item.isRead) {
        acc++;
      }
      return acc;
    }, 0);

    return {
      ...state,
      notifications: updatedNotifications,
      count: newCount
    };
  },
  [markAsRead]: state => {
    const notificationList = _.get(state, 'notifications', []);
    const readNotifications = notificationList.map((item) => {
      const copy = item;
      copy.isRead = true;
      return copy;
    });

    return {
      ...state,
      notifications: readNotifications,
      count: 0
    };
  },
  [toggleNotifications]: state => {
    const displayed = state.displayed;
    return {
      ...state,
      displayed: !displayed
    };
  }
}, initialState);

export default reducer;
