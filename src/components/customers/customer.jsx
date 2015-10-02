'use strict';

import React, { PropTypes } from 'react';

export default class Customer extends React.Component {

  static propTypes = {
    children: PropTypes.array
  };

  render() {
    return (
      <div id="user">
        {this.props.children}
      </div>
    );
  }
}
