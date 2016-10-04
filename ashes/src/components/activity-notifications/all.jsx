
import _ from 'lodash';
import React from 'react';
import NotificationItem from './item';
import types from '../activity-trail/activities/base/types';
import moment from 'moment';
import activities from '../activity-trail/all';

export default class AllNotificationItems extends React.Component {

  render() {
    return (
      <div style={{margin: '20px'}}>
        {activities.map(activity => {
          return (
            <NotificationItem item={activity} key={`${activity.id}-notification`}/>
          );
        })}
      </div>
    );
  }
}
