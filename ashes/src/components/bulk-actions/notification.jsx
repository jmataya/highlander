// libs
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import classNames from 'classnames';

// helpers
import { numberize } from '../../lib/text-utils';


export default class Notification extends React.Component {

  static propTypes = {
    resultType: PropTypes.oneOf([
      'success',
      'error',
    ]),
    entity: PropTypes.string.isRequired,
    overviewMessage: PropTypes.string.isRequired,
    children: PropTypes.node.isRequired,
    onHide: PropTypes.func.isRequired,
  };

  state = {
    expanded: false,
  };

  @autobind
  toggleExpanded() {
    this.setState({expanded: !this.state.expanded});
  }

  render() {
    const {resultType, entity, overviewMessage, children, onHide} = this.props;
    const {expanded} = this.state;
    const count = React.Children.count(children);
    const message = `${count} ${numberize(entity, count)} ${overviewMessage}.`;

    return (
      <div className={classNames('fc-bulk-notification', `_${resultType}`)}>
        <div>
          <div className="fc-bulk-notification__preview">
            <i className={`fc-bulk-notification__icon icon-${resultType}`} />
            <span className="fc-bulk-notification__message">
              {message}
            </span>
            <a className="fc-bulk-notification__details-link" onClick={this.toggleExpanded}>
              {expanded ? 'View Less...' : 'View Details...'}
            </a>
            <span className="fc-bulk-notification__flex-separator"></span>
            <i onClick={onHide} className="fc-bulk-notification__close fc-btn-close icon-close" title="Close" />
          </div>
          <div className={classNames('fc-bulk-notification__details', {'_open': expanded})}>
            {children}
          </div>
        </div>
      </div>
    );
  }
}
