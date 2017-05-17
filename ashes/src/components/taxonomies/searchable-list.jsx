// @flow

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';

// data
import { actions, rawSorts } from 'modules/taxonomies/list';

// components
import SelectableSearchList from 'components/list-page/selectable-search-list';
import TaxonomyRow from './taxonomy-row';

// helpers
import { filterArchived } from 'elastic/archive';

type Props = {
  actions: Object,
  list: Object,
};

const tableColumns = [
  { field: 'taxonomyId', text: 'ID' },
  { field: 'name', text: 'Name' },
  { field: 'type', text: 'Type' },
  { field: 'valuesCount', text: 'Values' },
  { field: 'createdAt', type: 'datetime', text: 'Date/Time Created' },
  { field: 'updatedAt', type: 'datetime', text: 'Date/Time Updated' },
  { field: 'state', text: 'State' },
];

export class SearchableList extends Component {
  props: Props;

  @autobind
  addSearchFilters(filters: Array<SearchFilter>, initial: boolean = false) {
    return this.props.actions.addSearchFilters(filterArchived(filters), initial);
  }

  renderRow(row: TaxonomyResult, index: number, columns: Columns, params: Object) {
    const key = `taxonomies-${row.id}`;
    return <TaxonomyRow key={key} taxonomy={row} columns={columns} params={params} />;
  }

  render() {
    const { list, actions } = this.props;

    const searchActions = {
      ...actions,
      addSearchFilters: this.addSearchFilters,
    };

    return (
      <div>
        <SelectableSearchList
          rawSorts={rawSorts}
          entity="taxonomies.list"
          emptyMessage="No taxonomies found."
          list={list}
          renderRow={this.renderRow}
          tableColumns={tableColumns}
          searchActions={searchActions}
          predicate={({id}) => id} />
      </div>
    );
  }
}

function mapStateToProps({ taxonomies: { list } }) {
  return { list };
}

function mapDispatchToProps(dispatch) {
  return {
    actions: bindActionCreators(actions, dispatch),
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(SearchableList);
