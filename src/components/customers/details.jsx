import React, { PropTypes } from 'react';
import CustomerContacts from './contacts';
import CustomerAccountPassword from './account-password';
import CustomerRoles from './roles';
import CustomerAddressBook from './address-book';
import CustomerCreditCards from './credit-cards';
import CustomerGroups from './groups';
import CustomerNotificationSettings from './notification-settings';
import CustomerAccountStatus from './account-status';
import SectionSubtitle from '../section-title/section-subtitle';
import { connect } from 'react-redux';
import * as CustomerDetailsActions from '../../modules/customers/details';

@connect((state, props) => ({
  ...state.customers.details[props.entity.id]
}), CustomerDetailsActions)
export default class CustomerDetails extends React.Component {

  static propTypes = {
    entity: PropTypes.object,
    addresses: PropTypes.array,
    fetchAddresses: PropTypes.func
  };

  componentDidMount() {
    const customer = this.props.entity.id;
    this.props.fetchAddresses(customer);
  }

  render() {
    const customer = this.props.entity;
    const addresses = this.props.addresses;
    return (
      <div className="fc-customer-details">
        <SectionSubtitle title="Details" />
        <div className="fc-grid fc-grid-gutter">
          <div className="fc-col-md-1-2">
            <CustomerContacts customer={ customer } />
          </div>
          <div className="fc-col-md-1-2">
            <CustomerAccountPassword />
            <CustomerRoles />
          </div>
        </div>
        <div className="fc-grid fc-grid-gutter">
          <div className="fc-col-md-1-1">
            <CustomerAddressBook customerId={ customer.id } addresses={ addresses } />
          </div>
        </div>
        <div className="fc-grid fc-grid-gutter">
          <div className="fc-col-md-1-1">
            <CustomerCreditCards customerId={ customer.id } addresses={ addresses } />
          </div>
        </div>
        <div className="fc-grid fc-grid-gutter">
          <div className="fc-col-md-1-1">
            <CustomerGroups />
          </div>
        </div>
        <div className="fc-grid fc-grid-gutter">
          <div className="fc-col-md-1-2">
            <CustomerNotificationSettings />
          </div>
          <div className="fc-col-md-1-2">
            <CustomerAccountStatus customer={ customer }/>
          </div>
        </div>
      </div>
    );
  }
};
