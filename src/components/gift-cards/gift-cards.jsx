import _ from 'lodash';
import React, { PropTypes } from 'react';
import SectionTitle from '../section-title/section-title';
import LocalNav from '../local-nav/local-nav';
import TableView from '../table/tableview';
import TableRow from '../table/row';
import TableCell from '../table/cell';
import { Link, IndexLink } from '../link';
import { Date } from '../common/datetime';
import { TabListView, TabView } from '../tabs';
import { connect } from 'react-redux';
import * as giftCardActions from '../../modules/gift-cards/cards';
import GiftCardCode from './gift-card-code';
import Currency from '../common/currency';

@connect(state => ({giftCards: state.giftCards.cards}), giftCardActions)
export default class GiftCards extends React.Component {

  static propTypes = {
    giftCards: PropTypes.object,
    tableColumns: PropTypes.array,
    fetch: PropTypes.func,
  };

  static defaultProps = {
    tableColumns: [
      {field: 'code', text: 'Gift Card Number', type: 'link', model: 'giftcard', id: 'code'},
      {field: 'originType', text: 'Type'},
      {field: 'originalBalance', text: 'Original Balance', type: 'currency'},
      {field: 'currentBalance', text: 'Current Balance', type: 'currency'},
      {field: 'availableBalance', text: 'Available Balance', type: 'currency'},
      {field: 'status', text: 'State'},
      {field: 'createdAt', text: 'Date/Time Issued', type: 'date'}
    ]
  };

  componentDidMount() {
    this.props.fetch(this.props.giftCards);
  }

  render() {
    const renderRow = (row, index) => (
      <TableRow key={`${index}`}>
        <TableCell>
          <Link to="giftcard" params={{giftCard: row.code}}>
            <GiftCardCode value={row.code} />
          </Link>
        </TableCell>
        <TableCell>{row.originType}</TableCell>
        <TableCell><Currency value={row.originalBalance}/></TableCell>
        <TableCell><Currency value={row.currentBalance}/></TableCell>
        <TableCell><Currency value={row.availableBalance}/></TableCell>
        <TableCell>{row.status}</TableCell>
        <TableCell>
          <Date value={row.createdAt}/>
        </TableCell>
      </TableRow>
    );

    return (
      <div className="fc-list-page">
        <div className="fc-list-page-header">
          <SectionTitle title="Gift Cards" subtitle={this.props.giftCards.total}>
            <Link to="gift-cards-new" className="fc-btn fc-btn-primary">
              <i className="icon-add"></i> Gift Card
            </Link>
          </SectionTitle>
          <LocalNav>
            <IndexLink to="gift-cards">Lists</IndexLink>
            <a href="">Insights</a>
            <a href="">Activity Trail</a>
          </LocalNav>
          <TabListView>
            <TabView>All</TabView>
            <TabView>Active</TabView>
          </TabListView>
        </div>
        <div className="fc-grid fc-list-page-content">
          <div className="fc-col-md-1-1">
            <TableView
              columns={this.props.tableColumns}
              data={this.props.giftCards}
              renderRow={renderRow}
              setState={this.props.fetch}
              />
          </div>
        </div>
      </div>
    );
  }
}
