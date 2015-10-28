'use strict';

import React from 'react';
import { CustomerInfo, RmaSummary } from './helpers';
import LineItems from '../line-items/line-items';
import Payment from '../payment/payment';
import RmaStore from '../../stores/rmas';

export default class RmaDetails extends React.Component {
  constructor(props, context) {
    super(props, context);
    this.state = {};
  }

  render() {
    let rma = this.props.rma;
    let isEditing = this.state.isEditing;
    let lineColumns = [
      {field: 'imagePath', text: 'Image', type: 'image'},
      {field: 'name', text: 'Name'},
      {field: 'sku', text: 'SKU'},
      {field: 'quantity', text: 'Quantity'},
      {field: 'inventoryDisposition', text: 'Inventory Disposition'},
      {field: 'refund', text: 'Refund', type: 'currency'},
      {field: 'reason', text: 'Reason'}
    ];

    /*return (
      <div className="fc-rma-details fc-grid fc-grid-match">
        <div className="fc-col-md-3-10">
          <RmaSummary rma={rma} isEditing={isEditing}/>
        </div>
        <div className="fc-col-md-7-10">
          <CustomerInfo rma={rma} isEditing={isEditing}/>
        </div>
        <div className="fc-col-md-1-1">
          <LineItems
            entity={rma}
            isEditing={isEditing}
            tableColumns={lineColumns}
            model={'rma'}
            />
        </div>
        <div className="fc-col-md-1-1">
        </div>
        <div className="fc-col-md-1-1">
          <Payment rma={rma} isEditing={isEditing}/>
        </div>
      </div>
    );*/
    return null;
  }
}

RmaDetails.propTypes = {
  rma: React.PropTypes.object
};
