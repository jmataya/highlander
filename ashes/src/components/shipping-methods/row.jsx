/* @flow */

//libs
import React, { Component, Element } from 'react';
import _ from 'lodash';

// components
import RoundedPill from '../rounded-pill/rounded-pill';
import MultiSelectRow from '../table/multi-select-row';

// types
import type { ShippingMethod } from 'paragons/shipping-method';

type Props = {
  application: ShippingMethod,
  columns?: Array<Object>,
  params: Object,
};

function setCellContents(method, field) {
  return _.get(method, field);
}

export default class ShippingMethodRow extends Component {
  props: Props;

  render(): Element {
    const { shippingMethod, columns, params } = this.props;
    const commonParams = {
      columns,
      row: shippingMethod,
      setCellContents,
      params,
    };

    console.log('Rendering a row ' + shippingMethod.id);

    return (
      <MultiSelectRow
        { ...commonParams }
        linkTo="shippingMethod-details"
        linkParams={{shippingMethodId: shippingMethod.id}} />
    );

  }
}
