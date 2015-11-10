'use strict';

import React, { PropTypes } from 'react';
import _ from 'lodash';

export default class Panel extends React.Component {
  static propTypes = {
    children: PropTypes.any,
    title: PropTypes.string,
    content: PropTypes.any,
    featured: PropTypes.bool
  };

  static defaultProps = {
    featured: false
  };

  get contentClasses() {
    return `fc-panel-content ${this.props.featured ? 'fc-panel-content-featured' : null}`;
  }

  render() {
    return (
      <div className={ this.rootClassName }>
        <div className="fc-panel-header">
          {this.props.title}
        </div>
        <div className={this.contentClasses}>
          {this.props.content && this.props.content.props.children}
          {this.props.children}
        </div>
      </div>
    );
  }
}
