'use strict';

import React, { PropTypes } from 'react';
import moment from 'moment';
import { Link } from '../link';
import { formatCurrency } from '../../lib/format';

// TODO: We should find a better place for these statuses, but for now just
// getting rid of reference to OrderStore.
const orderStatuses = {
  cart: 'Cart',
  remorseHold: 'Remorse Hold',
  manualHold: 'Manual Hold',
  fraudHold: 'Fraud Hold',
  fulfillmentStarted: 'Fulfillment Started',
  canceled: 'Canceled',
  partiallyShipped: 'Partially Shipped',
  shipped: 'Shipped'
};

export default class TableBody extends React.Component {

  static propTypes = {
    columns: PropTypes.array,
    rows: PropTypes.array.isRequired,
    model: PropTypes.string,
    predicate: PropTypes.func,
    children: PropTypes.node
  };

  constructor(props, context) {
    super(props, context);
    this.state = {
      newRows: {}
    };
  }


  convert(field, column, row) {
    let model = column.model || row.model || this.props.model;
    switch(column.type) {
      case 'id': {
        let params = {};
        params[model] = field;
        return <Link to={model} params={params}>{field}</Link>;
      }
      case 'link': {
        let params = {};
        params[column.model] = row[column.id];
        return <Link to={column.model} params={params}>{field}</Link>;
      }
      case 'image': return <img src={field}/>;
      case 'currency': return formatCurrency(field);
      case 'date': return <time dateTime={field}>{moment(field).format('MM/DD/YYYY HH:mm:ss')}</time>;
      case 'orderStatus': return orderStatuses[field];
      default: return typeof field === 'object' ? this.displayObject(field) : field;
    }
  }

  findComponent(name, row, field) {
    let
      children  = this.props.children,
      model     = row[field] ? row[field] : row;
    children = Array.isArray(children) ? children : [children];
    let element = children.filter((child) => { return child.type.name === name; })[0];
    return React.cloneElement(element, {model: model});
  }

  displayObject(obj) {
    let divs = [];
    for (let field in obj) {
      if (field !== 'createdAt' && typeof obj[field] !== 'object') {
        divs.push(<div key={field}>{obj[field]}</div>);
      }
    }
    return divs;
  }

  componentWillReceiveProps(nextProps) {
    const newRows = {};
    if (this.props.predicate && nextProps.rows && nextProps.rows !== this.props.rows && this.props.rows.length) {

      nextProps.rows.map((row, idx) => {
        if (idx >= this.props.rows.length ||
          this.props.predicate(row) !== this.props.predicate(this.props.rows[idx])) {

          newRows[idx] = true;
        }
      });
    }
    this.setState({ newRows });
  }

  render() {
    let columns = this.props.columns;
    let createRow = (row, idx) => {
      const isNew = idx in this.state.newRows;
      return (
        <tr key={idx} className={`${row.isNew ? 'new' : ''} fc-table-tr`}>
          {columns.map((column) => {
            let data = (
              column.component
                ? this.findComponent(column.component, row, column.field)
                : this.convert(row[column.field], column, row)
            );
            return <td key={`${idx}-${column.field}`} className={`${column.field} fc-table-td`}><div>{data}</div></td>;
          })}
        </tr>
      );
    };

    return <tbody className="fc-table-tbody">{this.props.rows.map(createRow)}</tbody>;
  }
}
