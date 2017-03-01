/* @flow */

//libs
import React, { PropTypes } from 'react';
import _ from 'lodash';

// helpers
import { activeStatus, isArchived } from 'paragons/common';

// components
import RoundedPill from '../rounded-pill/rounded-pill';
import MultiSelectRow from '../table/multi-select-row';

// types
import type { Product } from 'paragons/product';

type Props = {
  product: Product,
  columns?: Array<Object>,
  params: Object,
};

function setCellContents(product, field) {
  switch (field) {
    case 'image':
      return _.get(product, ['albums', 0, 'images', 0, 'src']);
    case 'state':
      return <RoundedPill text={activeStatus(product)} />;
    default:
      return _.get(product, field);
  }
}

const ProductRow = (props: Props) => {
  const { product, columns, params } = props;
  const commonParams = {
    columns,
    row: product,
    setCellContents,
    params,
  };

  if (isArchived(product)) {
    return <MultiSelectRow {...commonParams} />;
  }

  return (
    <MultiSelectRow
      { ...commonParams }
      linkTo="product-details"
      linkParams={{productId: product.productId, context: product.context}}
    />
  );
};

export default ProductRow;