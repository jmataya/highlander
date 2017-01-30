/* @flow */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';

import { actions } from 'modules/customer-groups/list';

// components
import { SelectableSearchList } from 'components/list-page';
import MultiSelectRow from 'components/table/multi-select-row';
import RoundedPill from 'components/rounded-pill/rounded-pill';

type Props = {
  list: Object;
  actions: Object;
};

const tableColumns = [
  { field: 'name', text: 'Group Name' },
  { field: 'customersCount', text: 'Customers Count' },
  { field: 'groupType', text: 'Type' },
  { field: 'createdAt', type: 'date', text: 'Date/Time Created' },
  { field: 'modifiedAt', type: 'date', text: 'Date/Time Last Modified' },
];

const groupTypes = {
  manual: 'Manual',
  dynamic: 'Dynamic',
};

function setCellContents(group, field) {
  if (field == 'groupType') {
    const type = groupTypes[_.get(group, 'groupType', 'dynamic')];
    return <RoundedPill text={type} />;
  }

  return _.get(group, field);
};

function renderRow(row, index, columns, params) {
  return (
    <MultiSelectRow
      columns={columns}
      linkTo="customer-group"
      linkParams={{ groupId: row.id }}
      row={row}
      params={params}
      key={row.id}
      setCellContents={setCellContents}
    />
  );
}

const GroupsList = ({ list, actions }: Props) => (
  <SelectableSearchList
    entity="customerGroups.list"
    emptyMessage="No groups found."
    list={list}
    renderRow={renderRow}
    tableColumns={tableColumns}
    searchActions={actions}
    searchOptions={{ singleSearch: true }}
  />
);

const mapState = state => ({
  list: state.customerGroups.list,
});

const mapActions = dispatch => ({
  actions: bindActionCreators(actions, dispatch),
});

export default connect(mapState, mapActions)(GroupsList);
