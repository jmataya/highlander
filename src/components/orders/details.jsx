import React, { PropTypes } from 'react';
import _ from 'lodash';
import TotalsSummary from '../common/totals';
import CustomerInfo from './customer-info';
import Messages from './messages';
import OrderLineItems from './order-line-items';
import OrderShippingAddress from './shipping-address';
import OrderShippingMethod from './order-shipping-method';
import Payments from './payments';
import Watchers from '../watchers/watchers';
import { haveType } from '../../modules/state-helpers';

const OrderDetails = props => {
  if (_.isEmpty(props.order.currentOrder)) {
    return <div className="fc-order-details"></div>;
  } else {
    const order = props.order.currentOrder;
    const isCart = _.isEqual(order.orderState, 'cart');

    const {
      shippingAddressStatus,
      shippingMethodStatus,
      paymentMethodStatus
    } = props.order.validations;

    const itemsStatus = props.lineItems.validations.itemsStatus;
    const warnings = props.order.validations.warnings.concat(props.lineItems.validations.warnings);
    const errors = props.order.validations.errors.concat(props.lineItems.validations.errors);

    return (
      <div className="fc-order-details">
        <div className="fc-order-details-body">
          <div className="fc-order-details-main">
            <OrderLineItems isCart={isCart} status={itemsStatus} {...props} />
            <OrderShippingAddress isCart={isCart} status={shippingAddressStatus} order={order} />
            <OrderShippingMethod isCart={isCart} status={shippingMethodStatus} {...props} />
            <Payments isCart={isCart} status={paymentMethodStatus} {...props} />
          </div>
          <div className="fc-order-details-aside">
            <Messages errors={errors} warnings={warnings} />
            <TotalsSummary entity={order} title={order.title} />
            <CustomerInfo order={order} />
            <Watchers entity={haveType(order, 'order')}/>
          </div>
        </div>
      </div>
    );
  }
};

OrderDetails.propTypes = {
  skuSearch: PropTypes.object,
  lineItems: PropTypes.object,
  order: PropTypes.shape({
    currentOrder: PropTypes.object,
    validations: PropTypes.object
  })
};

export default OrderDetails;
