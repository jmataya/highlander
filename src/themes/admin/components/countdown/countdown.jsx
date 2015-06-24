'use strict';

import React from 'react';
import moment from 'moment';

const zeroTime = '00:00:00';

class Countdown extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      difference: zeroTime,
      endDate: props.endDate
    };
  }

  addTime(number, key) {
    this.setState({
      endDate: moment(this.state.endDate).add(number, key)
    });
    this.startInterval();
  }

  startInterval() {
    this.interval = this.interval || setInterval(this.tick.bind(this), 1000);
  }

  stopInterval() {
    if (this.interval) {
      clearInterval(this.interval);
    }
  }

  tick() {
    let difference = moment(this.state.endDate).diff(moment.utc());
    if (difference <= 0) {
      this.setState({
        difference: zeroTime
      });
      this.stopInterval();
    } else {
      this.setState({
        difference: moment.utc(difference).format('HH:mm:ss')
      });
    }
  }

  componentDidMount() {
    this.startInterval();
  }

  componentWillUnmount() {
    this.stopInterval();
  }

  render() {
    return (
      <div className='countdown'>
        <span>{this.state.difference}</span>
        <button className='btn' onClick={this.addTime.bind(this, 15, 'm')}>+15</button>
      </div>
    );
  }
}

Countdown.propTypes = {
  endDate: React.PropTypes.string
};

Countdown.defaultProps = {
  endDate: moment.utc().add(3, 'h').format()
};

export default Countdown;
