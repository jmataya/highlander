/* @flow */

import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import { browserHistory, Link } from 'react-router';
import { connect } from 'react-redux';

import styles from './auth.css';

import { TextInput, TextInputWithLabel } from 'ui/inputs';
import { FormField } from 'ui/forms';
import Button from 'ui/buttons';
import WrapToLines from 'ui/wrap-to-lines';

import * as actions from 'modules/auth';
import { authBlockTypes } from 'paragons/auth';
import { fetch as fetchCart } from 'modules/cart';

import type { HTMLElement } from 'types';

import localized from 'lib/i18n';
import type { Localized } from 'lib/i18n';

type AuthState = {
  email: string,
  password: string,
  error: ?string,
};

type Props = Localized & {
  changePath: Function,
  isLoading: boolean,
  authenticate: Function,
  fetchCart: Function,
};

const mapState = state => ({
  isLoading: _.get(state.asyncActions, ['auth-login', 'inProgress'], false),
});

class Login extends Component {
  props: Props;

  state: AuthState = {
    email: '',
    password: '',
    error: null,
  };

  @autobind
  onChangeEmail({target}: any) {
    this.setState({
      email: target.value,
      error: null,
    });
  }

  @autobind
  onChangePassword({target}: any) {
    this.setState({
      password: target.value,
      error: null,
    });
  }

  @autobind
  authenticate(e: any) {
    e.preventDefault();
    e.stopPropagation();
    const { email, password } = this.state;
    const kind = 'customer';
    this.props.authenticate({email, password, kind}).then(() => {
      this.props.fetchCart();
      browserHistory.push(this.props.changePath());
    }).catch(() => {
      this.setState({error: 'Email or password is invalid'});
    });
  }

  @autobind
  googleAuthenticate(e: any) {
    e.preventDefault();
    e.stopPropagation();
    this.props.googleSignin().then(() => {
      this.props.fetchCart();
    });
  }

  render(): HTMLElement {
    const { password, email } = this.state;
    const props = this.props;
    const { t } = props;

    const restoreLink = (
      <Link to={props.changePath(authBlockTypes.RESTORE_PASSWORD)} styleName="restore-link">
        {t('forgot?')}
      </Link>
    );

    const signupLink = (
      <Link to={props.changePath(authBlockTypes.SIGNUP)} styleName="link">
        {t('Sign Up')}
      </Link>
    );

    return (
      <div>
        <div styleName="title">{t('LOG IN')}</div>
        <form>
          <Button icon="fc-google" onClick={this.googleAuthenticate} type="button" styleName="google-login">
            {t('LOG IN WITH GOOGLE')}
          </Button>
        </form>
        <WrapToLines styleName="divider">{t('or')}</WrapToLines>
        <form>
          <FormField key="email" styleName="form-field" error={this.state.error}>
            <TextInput placeholder={t('EMAIL')} value={email} type="email" onChange={this.onChangeEmail} />
          </FormField>
          <FormField key="passwd" styleName="form-field" error={!!this.state.error}>
            <TextInputWithLabel
              placeholder="PASSWORD"
              label={!password && restoreLink}
              value={password} onChange={this.onChangePassword} type="password"
            />
          </FormField>
          <Button
            styleName="primary-button"
            isLoading={props.isLoading}
            onClick={this.authenticate}
          >
            {t('LOG IN')}
          </Button>
        </form>
        <div styleName="switch-stage">
          {t('Don’t have an account?')} {signupLink}
        </div>
      </div>
    );
  }
}

export default connect(mapState, {
  ...actions,
  fetchCart,
})(localized(Login));
