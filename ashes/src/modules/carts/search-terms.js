const searchTerms = [
  {
    title: 'Cart',
    type: 'object',
    options: [
      {
        title: 'Reference Number',
        type: 'identifier',
        term: 'referenceNumber'
      }, {
        title: 'Date Placed',
        type: 'date',
        term: 'placedAt'
      }, {
        title: 'Total',
        type: 'currency',
        term: 'grandTotal'
      }
    ]
  }, {
    title: 'Shipment',
    type: 'object',
    options: [
      {
        title: 'State',
        type: 'enum',
        term: 'shipments.state',
        suggestions: [
          { display: 'Shipped', value: 'shipped' },
          { display: 'Partially Shipped', value: 'partiallyShipped' },
          { display: 'Delivered', value: 'delivered' }
        ]
      }, {
        title: 'Method',
        type: 'string',
        term: 'shipments.admin_display_name',
      }, {
        title: 'Cost',
        type: 'currency',
        term: 'shipments.shipping_price'
      }, {
        title: 'Total Number',
        type: 'number',
        term: 'shippingAddressesCount'
      }
    ]
  }, {
    title: 'Payment',
    type: 'object',
    options: [
      {
        title: 'Credit Card Total',
        type: 'currency',
        term: 'creditCardTotal'
      }, {
        title: 'Credit Card Total Number',
        type: 'number',
        term: 'creditCardCount'
      }, {
        title: 'Gift Card Total',
        type: 'currency',
        term: 'giftCardTotal'
      }, {
        title: 'Gift Card Total Number',
        type: 'number',
        term: 'giftCardCount'
      }, {
        title: 'Store Credit Total',
        type: 'currency',
        term: 'storeCreditTotal'
      }, {
        title: 'Store Credit Total Number',
        type: 'number',
        term: 'storeCreditCount'
      }
    ]
  }, {
    title: 'Customer',
    type: 'object',
    options: [
      {
        title: 'Name',
        type: 'string',
        term: 'customer.name'
      }, {
        title: 'Email',
        type: 'string',
        term: 'customer.email'
      }, {
        title: 'Total Sales',
        type: 'number',
        term: 'customer.revenue'
      }, {
        title: 'Blacklist Status',
        type: 'bool',
        term: 'customer.isBlacklisted'
      }, {
        title: 'Date Joined',
        type: 'date',
        term: 'customer.joinedAt'
      }
    ]
  }, {
    title: 'Shipping',
    type: 'object',
    options: [
      {
        title: 'City',
        type: 'string',
        term: 'shippingAddresses.city'
      }, {
        title: 'State',
        type: 'string',
        term: 'shippingAddresses.region'
      }, {
        title: 'Zip',
        type: 'string',
        term: 'shippingAddresses.zip'
      }
    ]
  }, {
    title: 'Billing',
    type: 'object',
    options: [
      {
        title: 'City',
        type: 'string',
        term: 'billingAddresses.city'
      }, {
        title: 'State',
        type: 'string',
        term: 'billingAddresses.region'
      }, {
        title: 'Zip',
        type: 'string',
        term: 'billingAddresses.zip'
      }
    ]
  }, {
    title: 'Items',
    type: 'object',
    options: [
      {
        title: 'Total Number',
        type: 'number',
        term: 'lineItemCount'
      }, {
        title: 'Product Name',
        type: 'string',
        term: 'lineItems.name'
      }, {
        title: 'Product SKU',
        type: 'identifier',
        term: 'lineItems.sku'
      }
    ]
  }
];

export default searchTerms;
