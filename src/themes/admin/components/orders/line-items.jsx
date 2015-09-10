'use strict';

import React from 'react';
import TableHead from '../tables/head';
import TableBody from '../tables/body';
import SkuStore from './sku-store';
import SkuResult from './sku-result';
import Typeahead from '../typeahead/typeahead';
import Counter from '../forms/counter';

const defaultColumns = [
  {field: 'image', text: 'Image', type: 'image'},
  {field: 'name', text: 'Name'},
  {field: 'skuId', text: 'SKU'},
  {field: 'price', text: 'Price', type: 'currency'},
  {field: 'quantity', text: 'Quantity'},
  {field: 'total', text: 'Total', type: 'currency'}
];

const editColumns = [
  {field: 'image', text: 'Image', type: 'image'},
  {field: 'name', text: 'Name'},
  {field: 'skuId', text: 'SKU'},
  {field: 'price', text: 'Price', type: 'currency'},
  {field: 'lineItem', text: 'Quantity', component: 'Counter'},
  {field: 'total', text: 'Total', type: 'currency'}
  //{field: 'delete', text: 'Delete', component: 'DeleteLineItem'}
];

export default class OrderLineItems extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isEditing: false
    };
  }

  toggleEdit() {
    this.setState({
      isEditing: !this.state.isEditing
    });
  }

  render() {
    let order = this.props.order;
    let editing = this.state.isEditing;
    let typeahead = null;
    let body = null;
    let columns = null;

    if (editing) {
      columns = editColumns;
      typeahead = <Typeahead component={SkuResult} store={SkuStore} selectEvent="addLineItem" />
      body = (
        <TableBody columns={columns} rows={order.lineItems} model='order'>
          <Counter defaultValue='quantity' stepAmount="1" minValue="0" maxValue="1000000" />
        </TableBody>
      );
    } else {
      columns = defaultColumns;
      body = <TableBody columns={columns} rows={order.lineItems} model='order'/>;
    }
    return (
      <section id="order-line-items">
        <header>
          <div className='fc-grid'>
            <div className="fc-col-2-3">Items</div>
            <div className="fc-col-1-3">
              <button className="fc-button" onClick={this.toggleEdit.bind(this)}>
                <i className="fa fa-pencil"></i>
              </button>
            </div>
          </div>
        </header>
        <table className="fc-table">
          <TableHead columns={columns}/>
          {body}
        </table>
      </section>
    );
  }
}

OrderLineItems.propTypes = {
  order: React.PropTypes.object
};
