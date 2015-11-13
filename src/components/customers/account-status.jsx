import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import ContentBox from '../content-box/content-box';
import { SliderCheckbox } from '../checkbox/checkbox';
import { connect } from 'react-redux';
import * as CustomersActions from '../../modules/customers/details';
import ConfirmationDialog from '../modal/confirmation-dialog';


@connect((state, props) => ({
  ...state.customers.details
}), CustomersActions)
export default class CustomerAccountStatus extends React.Component {

  static propTypes = {
    customer: PropTypes.object.isRequired,
    toggleDisableStatus: PropTypes.func.isRequired,
    startDisablingCustomer: PropTypes.func.isRequired,
    stopDisablingCustomer: PropTypes.func.isRequired
  };

  static customerInfo(customer) {
    return (<ul className="fc-customer-disable-confirm-customer">

    <li><strong>{customer.name}</strong></li>
    <li>{customer.email}</li>
    <li>{customer.phoneNumber}</li>
    </ul>);
  }

  get disableOptions() {
    const customer = this.props.customer;
    if (customer.disabled) {
      return {
        header: 'Activate Customer Account',
        body: (<div className="fc-customer-disable-confirm">
          <div>Are you sure you want to active the account for the following customer?</div>
        {CustomerAccountStatus.customerInfo(customer)}
          <div>You can deactivate this account at anytime.</div>
        </div>),
        confirm: 'Yes, Activate Account',
        cancel: 'Cancel'
      };
    } else {
      return {
        header: 'Deactivate Customer Account',
        body: (<div className="fc-customer-disable-confirm">
          <div>Are you sure you want to deactivate the account for the following customer?</div>
        {CustomerAccountStatus.customerInfo(customer)}
        <div>You can reactivate this account at anytime.</div></div>),
        confirm: 'Yes, Deactivate Account',
        cancel: 'Cancel'
      };
    }
  }

  render() {
    let customer = this.props.customer;
    return (
    <div>
      <ContentBox title="Account Status" className="fc-customer-account-status">
        <div className="fc-grid fc-customer-status-row">
            <div className="fc-col-md-2-3">
              <strong>Active Account</strong>
            </div>
            <div className="fc-col-md-1-3">
              <SliderCheckbox className="fc-right" onChange={this.props.startDisablingCustomer}
                              id="customerDisabled" checked={ !customer.disabled } />
            </div>
        </div>
        <div className="fc-grid fc-customer-status-row">
            <div className="fc-col-md-2-3">
              <strong>Blacklist Customer</strong>
            </div>
            <div className="fc-col-md-1-3">
              <SliderCheckbox className="fc-right" id="customerBlacklisted" defaultChecked={ customer.blacklisted } />
            </div>
        </div>
        <ConfirmationDialog
          {...this.disableOptions}
          isVisible={this.props.isDisablingStarted}
          confirmAction={() => {
            const customer = this.props.customer;
            this.props.toggleDisableStatus(customer.id, !customer.disabled);
          }}
          cancelAction={this.props.stopDisablingCustomer}/>
      </ContentBox>
    </div>
    );
  }
}
