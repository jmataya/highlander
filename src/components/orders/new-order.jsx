import React, { Component, PropTypes } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { transitionTo } from '../../route-helpers';
import _ from 'lodash';

import * as newOrderActions from '../../modules/orders/new-order';

import { Button } from '../common/buttons';
import BigCheckbox from '../checkbox/big-checkbox';
import ChooseCustomer from './choose-customer';
import ChooseCustomerRow from './choose-customer-row';
import ContentBox from '../content-box/content-box';
import Form from '../forms/form';
import FormField from '../forms/formfield';
import PilledInput from '../pilled-search/pilled-input';
import SectionTitle from '../section-title/section-title';
import Typeahead from '../typeahead/typeahead';

function mapStateToProps(state) {
  return {
    newOrder: state.orders.newOrder,
  };
}

@connect(mapStateToProps, { ...newOrderActions})
export default class NewOrder extends Component {
  static propTypes = {
    suggestCustomers: PropTypes.func.isRequired,
  };

  static contextTypes = {
    history: PropTypes.object.isRequired,
  };

  constructor(props, context) {
    super(props, context);

    this.state = {
      customers: [],
      query: '',
    };
  }

  shouldComponentUpdate(nextProps, nextState) {
    const cart = _.get(nextProps, 'newOrder.order.cart.referenceNumber');
    if (cart) {
      transitionTo(this.context.history, 'order', { order: cart });
      return false;
    }

    return true;
  }

  get suggestedCustomers() {
    const empty = { rows: [], from: 0, size: 0, total: 0 };
    return _.get(this.props, 'newOrder.customers.result', empty);
  }

  get isFetching() {
    return _.get(this.props, 'newOrder.customers.isFetching', false);
  }

  get customersList() {
    return (
      <ChooseCustomer items={this.suggestedCustomers} onItemClick={this.selectCustomer} />
    );
  }

  get customers() {
    return this.state.customers.map(c => c.email);
  }

  get chooseCustomerInput() {
    return (
      <PilledInput
        solid={true}
        value={this.state.query}
        onChange={e => this.setState({query: e.target.value})}
        pills={this.customers}
        onPillClose={this.clearCustomer} />
    );
  }

  get nextButton() {
    return (
      <input 
        type="submit" 
        className="fc-order-create__submit fc-btn fc-btn-primary fc-right" 
        value="Next &#xe804;" />
    );
  }  

  @autobind
  clearCustomer() {
    this.setState({
      customers: [],
    });
  }

  @autobind
  selectCustomer(customer) {
    this.setState({
      customers: [customer],
      query: '',
    });
  }

  @autobind
  submitAction() {
    if (!_.isEmpty(this.state.customers)) {
      this.props.createOrder(this.state.customers[0]);
    }
  }

  render() {
    return (
      <div className="fc-order-create">
        <div className="fc-grid">
          <div className="fc-col-md-1-1">
            <SectionTitle className="fc-grid-no-gutter" title="New Order" />
          </div>
          <article className="fc-col-md-1-1">
            <div className="fc-grid fc-order-create__customer-form-panel">
              <div className="fc-order-create__customer-form-subtitle fc-col-md-1-1">
                <h2>Customer</h2>
              </div>
              <div className="fc-order-create__customer-form fc-col-md-1-1">
                <Form 
                  autoComplete="off" 
                  className="fc-grid fc-grid-no-gutter"
                  onSubmit={this.submitAction}>
                  <Typeahead
                    className="fc-order-create__customer-search fc-col-md-5-8"
                    component={ChooseCustomerRow}
                    fetchItems={this.props.suggestCustomers}
                    isFetching={this.isFetching}
                    itemsElement={this.customersList}
                    inputElement={this.chooseCustomerInput}
                    placeholder="Customer name or email..." />
                  <FormField
                    className="fc-order-create__guest-checkout fc-col-md-2-8"
                    label="Checkout as guest"
                    labelAfterInput={true}>
                    <BigCheckbox name="guestCheckout" />
                  </FormField>
                  <FormField className="fc-col-md-1-8">
                    {this.nextButton}
                  </FormField>
                </Form>
              </div>
            </div>
          </article>
        </div>
      </div>
    );
  }
};
