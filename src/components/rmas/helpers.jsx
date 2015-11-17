import React, { PropTypes } from 'react';
import _ from 'lodash';
import ContentBox from '../content-box/content-box';
import TableRow from '../table/row';
import TableCell from '../table/cell';
import { DateTime } from '../common/datetime';
import Currency from '../common/currency';
import { Link } from '../link';

const CustomerInfo = props => {
  return (
    <div className="fc-rma-summary fc-content-box">
      <header className="fc-content-box-header">Message for Customer</header>
      <article>
        {props.rma.customerMessage}
      </article>
    </div>
  );
};

CustomerInfo.propTypes = {
  rma: PropTypes.object
};

const PaymentMethod = props => {
  const model = props.model;

  return (
    <div className="fc-payment-method">
      <i className={`fc-icon-lg icon-${model.cardType}`}></i>
      <div>
        <div className="fc-strong">{model.cardNumber}</div>
        <div>{model.cardExp}</div>
      </div>
    </div>
  );
};

const renderRow = (row, index) => {
  return (
    <TableRow key={`${index}`}>
      <TableCell><Link to="rma" params={{rma: row.referenceNumber}}>{row.referenceNumber}</Link></TableCell>
      <TableCell><DateTime value={row.createdAt} /></TableCell>
      <TableCell><Link to="order" params={{order: row.orderRefNum}}>{row.orderRefNum}</Link></TableCell>
      <TableCell>{row.customer.email}</TableCell>
      <TableCell>{row.status}</TableCell>
      <TableCell><Currency value={row.total} /></TableCell>
    </TableRow>
  );
};

export {
  CustomerInfo,
  PaymentMethod,
  renderRow
};
