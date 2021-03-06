/* @flow */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import classNames from 'classnames';
import { connect } from 'react-redux';
import { browserHistory } from 'lib/history';
import { autobind } from 'core-decorators';
import * as tracking from 'lib/analytics';

// localization
import localized from 'lib/i18n';

// components
import Currency from 'ui/currency';
import LineItem from './line-item';
import Button from 'ui/buttons';
import ErrorAlerts from '@foxcommerce/wings/lib/ui/alerts/error-alerts';
import { skuIdentity } from '@foxcommerce/wings/lib/paragons/sku';
import { parseError } from '@foxcomm/api-js';
import Overlay from 'ui/overlay/overlay';
import ActionLink from 'ui/action-link/action-link';

// styles
import styles from './cart.css';

// types
import type { Totals } from 'modules/cart';

// actions
import * as actions from 'modules/cart';

type Props = {
  fetch: Function,
  deleteLineItem: Function,
  updateLineItemQuantity: Function,
  toggleCart: Function,
  hideCart: Function,
  skus: Array<any>,
  coupon: ?Object,
  promotion: ?Object,
  totals: Totals,
  user?: ?Object,
  isVisible: boolean,
  t: any,
};

type State = {
  errors?: Array<any>,
};

class Cart extends Component {
  props: Props;

  state: State = {

  };

  componentDidMount() {
    if (this.props.user) {
      this.props.fetch(this.props.user);
    } else {
      this.props.fetch();
    }
  }

  @autobind
  deleteLineItem(sku) {
    tracking.removeFromCart(sku, sku.quantity);
    this.props.deleteLineItem(sku).catch((ex) => {
      this.setState({
        errors: parseError(ex),
      });
    });
  }

  @autobind
  updateLineItemQuantity(sku, quantity) {
    const diff = quantity - sku.quantity;
    if (diff > 0) {
      tracking.addToCart(sku, diff);
    } else if (diff < 0) {
      tracking.removeFromCart(sku, -diff);
    }
    this.props.updateLineItemQuantity(sku, quantity).catch((ex) => {
      this.setState({
        errors: parseError(ex),
      });
    });
  }

  get lineItems() {
    if (_.isEmpty(this.props.skus)) {
      return (
        <div styleName="empty-cart">
          <p styleName="empty-text">Your cart is empty</p>
        </div>
      );
    }

    return _.map(this.props.skus, (sku) => {
      return (
        <LineItem
          {...sku}
          deleteLineItem={() => this.deleteLineItem(sku)}
          updateLineItemQuantity={(id, quantity) => this.updateLineItemQuantity(sku, quantity)}
          key={skuIdentity(sku)}
        />
      );
    });
  }

  @autobind
  closeError(error, index) {
    const { errors } = this.state;

    if (!errors || _.isEmpty(this.state.errors)) return;

    errors.splice(index, 1);

    this.setState({
      errors,
    });
  }

  get errorsLine() {
    if (this.state.errors && !_.isEmpty(this.state.errors)) {
      return <ErrorAlerts errors={this.state.errors} closeAction={this.closeError} />;
    }
  }

  @autobind
  onCheckout() {
    Promise.resolve(this.props.hideCart())
      .then(() => {
        browserHistory.push('/checkout');
      })
    ;
  }

  render() {
    const {
      t,
      totals,
      toggleCart,
      skus,
      isVisible,
    } = this.props;

    const cartClass = classNames({
      'cart-hidden': !isVisible,
      'cart-shown': isVisible,
    });

    const checkoutDisabled = _.size(skus) < 1;

    return (
      <div styleName={cartClass}>
        <Overlay onClick={toggleCart} shown={isVisible} />
        <div styleName="cart-box">
          <div styleName="cart-header">
            <span styleName="my-cart">My Cart</span>
            <ActionLink
              action={toggleCart}
              title="Close"
              styleName="action-link-cart-close"
            />
          </div>

          <div styleName="cart-content">
            <div styleName="line-items">
              {this.lineItems}
            </div>
            {this.errorsLine}
          </div>

          <div styleName="cart-footer">
            <Button onClick={this.onCheckout} disabled={checkoutDisabled} styleName="checkout-button">
              <span>{t('Checkout')}</span>
              <span styleName="subtotal-price">
                <Currency value={totals.subTotal} />
              </span>
            </Button>
          </div>
        </div>
      </div>
    );
  }
}

const mapStateToProps = state => ({ ...state.cart, ...state.auth });

export default connect(mapStateToProps, {
  ...actions,
})(localized(Cart));
