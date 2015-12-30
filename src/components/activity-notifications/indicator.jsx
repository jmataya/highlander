
// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import classNames from 'classnames';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

// components
import { Button } from '../common/buttons';

export default class NotificationIndicator extends React.Component {

  static propTypes = {
    count: PropTypes.number,
    displayed: PropTypes.bool,
    toggleNotifications: PropTypes.func.isRequired,
    markAsReadAndClose: PropTypes.func.isRequired
  };

  static defaultProps = {
    displayed: false
  };

  get indicator() {
    if (_.isNumber(this.props.count) && this.props.count > 0) {
      return (
        <div className="fc-activity-notifications__indicator">
          <span>{this.props.count}</span>
        </div>
      );
    }
  }

  @autobind
  toggleNotifications() {
    if (this.props.displayed) {
      this.props.markAsReadAndClose();
    } else {
      this.props.toggleNotifications();
    }
  }

  render() {
    const classes = classNames('fc-activity-notifications__toggle', {
      '_active': this.props.displayed
    });
    return (
      <div className="fc-activity-notifications">
        <Button icon="bell"
                className={ classes }
                onClick={ this.toggleNotifications }>
          { this.indicator }
        </Button>
      </div>
    );
  }
}
