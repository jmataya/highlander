'use strict';

import React from 'react';
import Table from './table';
import TablePaginator from './paginator';

export default class TableView extends React.Component {
  static propTypes = {
    children: React.PropTypes.any,
    empty: React.PropTypes.any,
    renderRow: React.PropTypes.func,
    paginator: React.PropTypes.bool
  };

  static defaultProps = {
    paginator: true
  };

  onLimitChange(event) {
    event.preventDefault();
    //this.props.store.setLimit(+event.target.value);
  }

  render() {
    let showPaginator = this.props.paginator && this.props.store.models.length > this.props.store.limit;
    let paginator = showPaginator && (
        <TablePaginator store={this.props.store}/>
      );

    return (
      <div className="fc-tableview">
        {showPaginator && (
          <div className="fc-table-header">
            {paginator}
          </div>
        )}
        {this.props.store.models.length && (
          <Table store={this.props.store} renderRow={this.props.renderRow}/>
        )}
        {!this.props.store.models.length && (
          <div>{this.props.empty}</div>
        )}
        {showPaginator && (
          <div className="fc-table-footer">
            <select onChange={this.onLimitChange.bind(this)}>
              <option value="10">Show 10</option>
              <option value="25">Show 25</option>
              <option value="50">Show 50</option>
              <option value="100">Show 100</option>
              <option value="Infinity">Show all</option>
            </select>
            {paginator}
          </div>
        )}
      </div>
    );
  }
}
