/* @flow */

import React, { Component, Element, PropTypes } from 'react';
import cssModules from 'react-css-modules';
import styles from './auth.css';
import { autobind } from 'core-decorators';
import {reduxForm} from 'redux-form';

import { TextInput } from '../../common/inputs';
import { FormField } from '../../forms';
import Button from '../../common/buttons';
import { Link } from 'react-router';

type FormData = {
  email: string;
};

type RestoreState = {
  emailSent: boolean;
};

/* ::`*/
@reduxForm({
  form: 'restore-password',
  fields: ['email'],
})
@cssModules(styles)
/* ::`*/
export default class RestorePassword extends Component {

  static propTypes = {
    fields: PropTypes.object.isRequired,
    handleSubmit: PropTypes.func.isRequired,
    resetForm: PropTypes.func.isRequired,
    submitting: PropTypes.bool.isRequired,
    error: PropTypes.string,
  };

  state: RestoreState = {
    emailSent: false,
  };

  @autobind
  handleSubmit(data: FormData): Promise {
    if (data.email.endsWith('.com')) {
      this.setState({
        emailSent: true,
      });
      return Promise.resolve({error: null});
    }

    return Promise.reject({
      email: 'A user with this email does not exist.',
      _error: `Oops! We don’t have a user with that email. Please check your entry and try again.`,
    });
  }

  get topMessage(): Element {
    const { emailSent } = this.state;
    const { fields: {email}, error } = this.props;

    if (error) {
      return (
        <div styleName="top-message-error">
          {error}
        </div>
      );
    }

    if (emailSent) {
      return (
        <div styleName="top-message-success">
          An email was successfully sent to <strong>{email.value}</strong> with reset instructions!
        </div>
      );
    }

    return (
      <div styleName="top-message">
        No worries! We’ll email you instructions on how to reset your password.
      </div>
    );
  }

  get emailField(): ?Element {
    const { emailSent } = this.state;
    const { fields: {email}} = this.props;

    if (emailSent) return null;

    return (
      <FormField key="email" styleName="form-field" {...email}>
        <TextInput placeholder="EMAIL" required type="email" {...email} />
      </FormField>
    );
  }

  render(): Element {
    const {handleSubmit} = this.props;

    return (
      <div>
        <div styleName="title">FORGOT PASSWORD</div>
        {this.topMessage}
        <form onSubmit={handleSubmit(this.handleSubmit)}>
          {this.emailField}
          <Button styleName="primary-button">SUBMIT</Button>
        </form>
        <div styleName="switch-stage">
          <Link to="/login">BACK TO LOG IN</Link>
        </div>
      </div>
    );
  }
}
