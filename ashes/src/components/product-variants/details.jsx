// @flow

// libs
import _ from 'lodash';
import React, { Element } from 'react';
import connectVariants from './connect-variants';

// components
import { Link } from '../link';
import ObjectDetails from '../object-page/object-details';
import { renderFormField } from '../object-form/object-form-inner';
import AssociatedVariants from './associated-items/associated-variants';
import AssociatedProduct from './associated-items/associated-product';

const layout = require('./layout.json');

import type { DetailsProps } from '../object-page/object-details';
import type { ProductVariant } from 'modules/product-variants/details';
import type { ProductVariant as ESProdactVaraint } from 'modules/product-variants/list';

type Props = DetailsProps & {
  object: ProductVariant,
  // connected via connectVariants
  productVariants: Array<ESProdactVaraint>,
  productVariantsState: AsyncState,
}

class ProductVariantDetails extends ObjectDetails {
  props: Props;
  layout = layout;

  get title(): string {
    const productTitle = _.get(this.props.object, 'attributes.title.v');

    if (!_.isEmpty(this.props.object.options)) {
      const optionsString = _.map(this.props.object.options, option => option.value.name).join(', ');

      return `${productTitle} — ${optionsString}`;
    }

    return productTitle;
  }

  get titleField(): Element {
    return renderFormField('title', <span>{this.title}</span>, {label: 'title'});
  }

  get skuField(): Element {
    const skuCode = _.get(this.props.object, 'attributes.code.v');
    const skuId = this.props.object.skuId;

    const field = (
      <Link to="sku-details" params={{skuId: skuId}}>{skuCode}</Link>
    );

    return renderFormField('SKU', field, {label: 'SKU'});
  }

  get options(): Array<Element> {
    return _.map(this.props.object.options, option => {
      const name = _.get(option.attributes, 'name.v');
      const value = option.value.name;

      return renderFormField(name, <div>{value}</div>, {label: name});
    });
  }

  renderAssociatedProduct() {
    const { object, productVariants, productVariantsState } = this.props;

    return (
      <AssociatedProduct
        productVariants={productVariants}
        productVariantsState={productVariantsState}
        context={_.get(object, 'context.name', 'default')}
        product={object.product}
      />
    );
  }

  renderAssociatedVariants() {
    const { productVariants, productVariantsState } = this.props;
    return (
      <AssociatedVariants
        productVariants={productVariants}
        productVariantsState={productVariantsState}
      />
    );
  }

  renderGeneralSection() {
    return (
      <div>
        {this.titleField}
        {this.options}
        {this.skuField}
      </div>
    );
  }
}

export default connectVariants(
  props => props.object.product.id
)(ProductVariantDetails);
