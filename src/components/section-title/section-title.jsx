'use strict';

import React from 'react';
import Title from './title';

export default class SectionTitle extends React.Component {
  static propTypes = {
    title: React.PropTypes.node,
    count: React.PropTypes.number,
    buttonClickHandler: React.PropTypes.func
  };

  render() {
    let buttonMarkup = null;
    if (this.props.buttonClickHandler !== undefined) {
      buttonMarkup = (
        <div className="fc-col-md-2-6 fc-push-md-2-6 fc-actions">
          <button className="fc-btn fc-btn-primary"
                  onClick={ this.props.buttonClickHandler.bind(this) }>
            <i className="icon-add"></i> { this.props.title }
          </button>
        </div>
      );
    }
    return (
      <div className="fc-grid gutter">
        <div className="fc-col-md-2-6">
          <Title title={ this.props.title } subtitle={ this.props.count } />
        </div>
        { buttonMarkup }
      </div>
    );
  }
}
