/* @flow */

import React, { Component } from 'react';
import { connect } from 'react-redux';

import { PrimaryButton } from 'components/core/button';

import * as CartActions from 'modules/carts/details';

import type { Cart } from 'paragons/order';

type Props = {
  cart: Cart,
  validations: {
    errors: Array<string>,
    warnings: Array<string>,
  },
  checkout: Function,
};

export class Checkout extends Component {
  props: Props;

  render() {
    const { errors, warnings } = this.props.validations;
    const totalCount = errors.length + warnings.length;
    const refNum = this.props.cart.referenceNumber;
    const isCheckingOut = this.props.cart.isCheckingOut;
    const isDisabled = totalCount > 0 || isCheckingOut;

    return (
      <div className="fc-order-checkout fc-col-md-1-1">
        <PrimaryButton id="fct-place-order-btn" onClick={() => this.props.checkout(refNum)} disabled={isDisabled}>
          Place order
        </PrimaryButton>
      </div>
    );
  }
}

export default connect(null, CartActions)(Checkout);
