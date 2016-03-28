import React, { PropTypes } from 'react';
import { actions } from '../../modules/customers/list';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import _ from 'lodash';

import CustomerRow from './customer-row';
import { SelectableSearchList } from '../list-page';

const getState = state => ({ list: state.customers.list });

const mapDispatchToProps = dispatch => {
  return { actions: bindActionCreators(actions, dispatch) };
};

const Customers = props => {

  const renderRow = (row, index, columns, params) => {
    const key = `customer-${row.id}`;
    return (
      <CustomerRow key={key}
                   customer={row}
                   columns={columns}
                   params={params} />
    );
  };

  const tableColumns = [
    { field: 'name', text: 'Name' },
    { field: 'email', text: 'Email' },
    { field: 'id', text: 'Customer ID' },
    { field: 'shipRegion', text: 'Ship To Region' },
    { field: 'billRegion', text: 'Bill To Region' },
    { field: 'rank', text: 'Rank' },
    { field: 'joinedAt', text: 'Date/Time Joined', type: 'datetime' }
  ];

  return (
    <SelectableSearchList
      emptyMessage="No customers found."
      list={props.list}
      renderRow={renderRow}
      tableColumns={tableColumns}
      searchActions={props.actions} />
  );
};

Customers.propTypes = {
  list: PropTypes.object.isRequired,
  actions: PropTypes.object.isRequired,
};

export default connect(getState, mapDispatchToProps)(Customers);
