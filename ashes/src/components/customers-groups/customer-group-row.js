// @flow
import React, { PropTypes } from 'react';
import styles from './customer-group-row.css';

type Props = {
	model: Object
};

const CustomerGroupRow = (props: Props) => {
  const { name } = props.model;

  return (
    <div styleName="item">
      <div styleName="item-name">
        {name}
      </div>
    </div>
  );
};

export default CustomerGroupRow;
