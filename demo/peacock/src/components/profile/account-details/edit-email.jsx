/* @flow */

import React, { Component } from 'react';

// libs
import _ from 'lodash';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

// components
import { TextInput } from 'ui/text-input';
import { FormField } from 'ui/forms';
import CheckoutForm from 'pages/checkout/checkout-form';

import * as actions from 'modules/profile';

// types
import type { AsyncStatus } from 'types/async-actions';
import type { AccountDetailsProps } from 'types/profile';

import styles from '../profile.css';

type Props = AccountDetailsProps & {
  fetchAccount: () => Promise<*>,
  updateAccount: (payload: Object) => Promise<*>,
  updateState: AsyncStatus,
}

type State = {
  email: string,
}

class EditEmail extends Component {
  props: Props;

  state: State = {
    email: this.props.account.email || '',
  };

  componentWillMount() {
    this.props.fetchAccount();
  }

  componentWillUnmount() {
    this.props.clearAccountErrors();
  }

  @autobind
  handleEmailChange(event) {
    this.setState({
      email: event.target.value,
    });
  }

  @autobind
  handleSave() {
    this.props.updateAccount({
      email: this.state.email,
    }).then(() => {
      this.props.toggleEmailModal();
      this.props.clearAccountErrors();
    });
  }

  @autobind
  handleCancel() {
    const email = this.props.account.email;
    this.setState({ email });
    this.props.toggleEmailModal();
    this.props.clearAccountErrors();
  }

  render() {
    const action = {
      title: 'Cancel',
      handler: this.handleCancel,
    };

    return (
      <CheckoutForm
        submit={this.handleSave}
        buttonLabel="Apply"
        title="Edit email"
        action={action}
        error={this.props.updateState.err}
        inProgress={this.props.updateState.inProgress}
      >
        <FormField
          error={!!this.props.updateState.err}
          validator="email"
          styleName="email-field"
        >
          <TextInput
            required
            value={this.state.email}
            onChange={this.handleEmailChange}
            placeholder="Email"
            name="email"
          />
        </FormField>
      </CheckoutForm>
    );
  }
}

const mapStateToProps = (state) => {
  return {
    account: state.profile.account,
    updateState: _.get(state.asyncActions, 'updateAccount', {}),
  };
};

export default connect(mapStateToProps, {
  ...actions,
})(EditEmail);
