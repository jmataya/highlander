
import React from 'react';

import Activity from './base/activity';
;
import ordersActivities from './orders';
import orderNotesActivities from './order-notes';
import orderShippingAddressActivities from './order-shipping-address';
import orderShippingMethodsActivities from './order-shipping-methods';
import orderPaymentMethodsActivities from './order-payment-methods';
import customersActivities from './customers';
import customerAddressesActivities from './customer-addresses';
import customerCreditCardsActivities from './customer-credit-cards';
import giftCardsActivities from './gift-cards';

const representatives = {
  ...ordersActivities,
  ...orderNotesActivities,
  ...orderShippingAddressActivities,
  ...orderShippingMethodsActivities,
  ...orderPaymentMethodsActivities,
  ...customersActivities,
  ...customerAddressesActivities,
  ...customerCreditCardsActivities,
  ...giftCardsActivities,
};

export function getActivityRepresentative(activity) {
  const desc = representatives[activity.kind];

  if (!desc) return null;

  const args = [activity.data, activity];

  const params = {
    title: desc.title(...args),
    details: desc.details ? desc.details(...args) : null,
  };

  return props => <Activity {...params} {...props} />;
}

export default function(props) {
  const ActivityRepresentative = getActivityRepresentative(props.activity);

  if (!ActivityRepresentative) return null;

  return <ActivityRepresentative {...props} />;
}
