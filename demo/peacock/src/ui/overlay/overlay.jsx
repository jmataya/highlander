/* @flow */

import React, { Component } from 'react';
import classNames from 'classnames';

import styles from './overlay.css';

type Props = {
  shown: boolean,
  onClick?: Function,
};

class Overlay extends Component {
  props: Props;

  componentWillReceiveProps(nextProps: Props) {
    if (nextProps.shown) {
      document.getElementById('app').className = styles['no-scroll'];
    } else {
      document.getElementById('app').className = '';
    }
  }

  render() {
    const style = classNames({
      overlay: !this.props.shown,
      'overlay-shown': this.props.shown,
    });
    return <div styleName={style} onClick={this.props.onClick}></div>;
  }
}

export default Overlay;