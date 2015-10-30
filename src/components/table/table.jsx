'use strict';

import React, { PropTypes } from 'react';
import TableHead from './head';
import TableBody from './body';

const Table = (props) => {
  return (
    <table className='fc-table'>
      <TableHead columns={props.columns}/>
      <TableBody columns={props.columns} rows={props.data.rows} renderRow={props.renderRow}/>
    </table>
  );
};

Table.propTypes = {
  data: PropTypes.object.isRequired,
  renderRow: PropTypes.func
};

export default Table;
