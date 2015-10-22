'use strict';

import React from 'react';
import EditableContentBox from '../content-box/editable-content-box';
import TableView from '../tables/tableview';

const columns = [
  {field: 'name', text: 'Method'},
  {field: 'price', text: 'Price', type: 'currency'}
];

const ShippingMethod = (props) => {
  return (
    <EditableContentBox
      className='fc-shipping-method'
      title='Shipping Method'
      isEditing={props.isEditing}
      editAction={props.editAction}
      doneAction={props.doneAction}
      viewContent={<TableView columns={columns} rows={props.shippingMethods} />}
      editContent={<TableView columns={columns} rows={props.availableShippingMethods} />}
      />
  );
};

export default ShippingMethod;
