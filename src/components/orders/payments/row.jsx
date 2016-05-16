/* @flow */

import _ from 'lodash';
import React, { Component, PropTypes, Element } from 'react';

import styles from './payments.css';

import Currency from '../../common/currency';
import TableRow from '../../table/row';
import TableCell from '../../table/cell';
import { DateTime } from '../../common/datetime';
import { EditButton, DeleteButton } from '../../common/buttons';
import PaymentMethod from '../../payment/payment-method';

type Props = {
  editMode: boolean;
  title: string|Element;
  subtitle?: string|Element;
  amount: number;
  details: Element;
  showDetails: boolean;
  toggleDetails: Function;
  editAction: Function;
  deleteAction: Function;
  paymentMethod: Object;
}

export default class PaymentRow extends Component {
  props: Props;

  get editAction() {
    if (this.props.editMode) {
      return (
        <TableCell>
          <EditButton onClick={this.props.editAction} />
          <DeleteButton onClick={this.props.deleteAction} />
        </TableCell>
      );
    }
  }

  get amount() {
    const { amount } = this.props;
    return _.isNumber(amount) ? <Currency value={amount} /> : null;
  }

  get detailsRow() {
    if (this.props.showDetails) {
      return (
        <TableRow styleName="details-row">
          <TableCell colspan={5}>
            {this.props.details}
          </TableCell>
        </TableRow>
      );
    }
  }

  get summaryRow() {
    const { props } = this;
    const nextDetailAction = props.showDetails ? 'up' : 'down';

    return (
      <TableRow styleName="payment-row">
        <TableCell>
          <i styleName="row-toggle" className={`icon-chevron-${nextDetailAction}`} onClick={props.toggleDetails} />
          <PaymentMethod paymentMethod={props.paymentMethod} />
        </TableCell>
        <TableCell>
          {this.amount}
        </TableCell>
        <TableCell></TableCell>
        <TableCell>
          <DateTime value={props.paymentMethod.createdAt}></DateTime>
        </TableCell>
        {this.editAction}
      </TableRow>
    );
  }

  render() {
    return (
      <tbody>
        {this.summaryRow}
        {this.detailsRow}
      </tbody>
    );
  }
};

export default PaymentRow;
