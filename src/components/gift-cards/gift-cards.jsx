'use strict';

import _ from 'lodash';
import React, { PropTypes } from 'react';
import TableView from '../tables/tableview';
import { Link } from '../link';
import { connect } from 'react-redux';
import * as giftCardActions from '../../modules/gift-cards/cards';

@connect(({giftCards}) => ({items: giftCards.cards.items}), giftCardActions)
export default class GiftCards extends React.Component {

  static propTypes = {
    tableColumns: PropTypes.array,
    items: PropTypes.array
  };

  static defaultProps = {
    tableColumns: [
      {field: 'code', text: 'Gift Card Number', type: 'link', model: 'giftcard', id: 'code'},
      {field: 'originType', text: 'Type'},
      {field: 'originalBalance', text: 'Original Balance', type: 'currency'},
      {field: 'currentBalance', text: 'Current Balance', type: 'currency'},
      {field: 'availableBalance', text: 'Available Balance', type: 'currency'},
      {field: 'status', text: 'Status'},
      {field: 'date', text: 'Date Issued', type: 'date'}
    ]
  };

  componentDidMount() {
    this.props.fetchGiftCardsIfNeeded();
  }

  render() {
    return (
      <div id="cards">
        <div className="gutter">
          <h2>Gift Cards</h2>
          <Link to='gift-cards-new' className="fc-btn">+ New Gift Card</Link>
          <TableView
              columns={this.props.tableColumns}
              rows={this.props.items}
              model='giftcard'
          />
        </div>
      </div>
    );
  }
}
