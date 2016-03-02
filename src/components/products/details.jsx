/**
 * @flow
 */

// libs
import React, { Component, Element, PropTypes } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { dispatch } from 'redux';
import _ from 'lodash';

// actions
import * as DetailsActions from '../../modules/products/details';

// components
import ContentBox from '../content-box/content-box';
import InlineField from '../inline-field/inline-field';

// types
import type { DetailsParams } from './types';
import type { Product, ProductDetailsState } from '../../modules/products/details';

type DetailsProps = {
  details: ProductDetailsState,
  params: DetailsParams,
};

type ProductAttributes = { [key:string]: any };

const miscAttributeKeys = ['images'];
const pricingAttributeKeys = ['price', 'retailPrice', 'salePrice'];
const seoAttributeKeys = ['url', 'metaTitle', 'metaDescription'];

export class ProductDetails extends Component<void, DetailsProps, void> {
  static propTypes = {
    details: PropTypes.shape({
      product: PropTypes.object,
    }),
  };

  get generalContentBox(): Element {
    const attributes: ProductAttributes = _.get(this.props, 'details.product.attributes', {});

    const notGeneralKeys = [
      ...miscAttributeKeys,
      ...pricingAttributeKeys,
      ...seoAttributeKeys
    ];

    const generalAttrs: Array<Element> = _(attributes).omit(notGeneralKeys).map((attr, key) => {
      return <InlineField label={key} value={attr} />;
    }).value();

    return (
      <ContentBox title="General" indentContent={false}>
        <div className="fc-product-details__attributes">
          {generalAttrs}
        </div>
      </ContentBox>
    );
  }

  render() {
    return (
      <div className="fc-grid">
        <div className="fc-col-md-2-3">
          {this.generalContentBox}
        </div>
      </div>
    );
  }
}

type DetailsState = { details: ProductDetailsState };
function mapStateToProps(state: Object): DetailsState {
  return { details: state.products.details };
}

export default connect(mapStateToProps, DetailsActions)(ProductDetails);
