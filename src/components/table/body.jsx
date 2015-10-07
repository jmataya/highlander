'use strict';

import React from 'react';
import Wrapper from '../wrapper/wrapper';
import TableStore from '../../lib/table-store';
import TableRow from './row';
import TableCell from './cell';

export default class TableBody extends React.Component {
  static propTypes = {
    store: React.PropTypes.instanceOf(TableStore),
    renderRow: React.PropTypes.func
  };

  static defaultProps = {
    renderRow: function(row) {
      return (
        <Wrapper>
          <TableRow>
            {this.props.store.columns.map((column) => {
              return <TableCell>{row[column.field]}</TableCell>;
            })}
          </TableRow>
        </Wrapper>
      );
    }
  };

  render() {
    let renderRow = this.props.renderRow.bind(this);
    return (
      <tbody className="fc-table-tbody">
      {this.props.store.rows.map((row, index) => {
        return renderRow(row, index).props.children;
      })}
      </tbody>
    );
  }
}


