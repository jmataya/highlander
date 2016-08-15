
// @flow

import React, { Element, Component } from 'react';

import styles from './auth-pages.css';

type Props = {
  children: Element,
};

type State = {
  isMounted: boolean,
}

export default class AuthPages extends Component {
  props: Props;

  state: State = {
    isMounted: false,
  };

  componentDidMount() {
    this.setState({
      isMounted: true,
    });
  }

  get body(): Element {
    return React.cloneElement(
      this.props.children, {
        isMounted: this.state.isMounted,
      }
    );
  }

  render() {
    return (
      <div styleName="body">
        <img styleName="logo" src="/images/fc-logo-v.svg"/>
        {this.body}
        <div styleName="copyright">
          © 2016 FoxCommerce. All rights reserved. Privacy Policy. Terms of Use.
        </div>
      </div>
    );
  }
}

export default AuthPages;
