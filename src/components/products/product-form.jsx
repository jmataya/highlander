/**
 * @flow
 */

// libs
import React, { Component, Element, PropTypes } from 'react';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';
import _ from 'lodash';

// components
import ContentBox from '../content-box/content-box';
import ObjectForm from '../object-form/object-form';
import ObjectScheduler from '../object-scheduler/object-scheduler';
import SkuList from './sku-list';
import Tags from '../tags/tags';
import VariantList from './variant-list';

// types
import type { Attributes } from 'paragons/object';
import type { Product } from 'paragons/product';

type Props = {
  product: Product,
  onUpdateProduct: (product: Product) => void,
  onSetSkuProperty: (code: string, field: string, value: any) => void,
  onSetSkuProperties: (code: string, toUpdate: Array<Array<any>>) => void,
};

type State = {
  isAddingProperty: bool,
};

const omitKeys = {
  general: ['skus', 'variants', 'activeFrom', 'activeTo', 'tags'],
};

const defaultKeys = {
  general: ['title', 'description'],
  misc: ['images'],
  seo: ['url', 'metaTitle', 'metaDescription'],
};

/**
 * ProductForm is dumb component that implements the logic needed for creating
 * or updating a product.
 */
export default class ProductForm extends Component {
  props: Props;

  state: State = {
    isAddingProperty: false,
  };

  get generalAttrs(): Array<string> {
    const toOmit = [
      ...defaultKeys.general,
      ...defaultKeys.misc,
      ...defaultKeys.seo,
      ..._.flatten(_.valuesIn(omitKeys)),
    ];
    const attributes = _.get(this.props, 'product.attributes', {});
    return [
      ...defaultKeys.general,
      ...(_(attributes).omit(toOmit).keys().value())
    ];
  }

  get skusContentBox(): Element {
    return (
      <ContentBox title="SKUs">
        <SkuList
          fullProduct={this.props.product}
          updateField={this.props.onSetSkuProperty}
          updateFields={this.props.onSetSkuProperties}
        />
      </ContentBox>
    );
  }

  get variantContentBox(): Element {
    return <VariantList variants={{}} />;
  }

  @autobind
  handleProductChange(attributes: Attributes) {
    const newProduct = assoc(this.props.product, ['attributes'], attributes);
    this.props.onUpdateProduct(newProduct);
  }

  @autobind
  handleAddProperty() {
    this.setState({ isAddingProperty: true });
  }

  get productState(): Element {
    const { attributes } = this.props.product;

    return (
      <ObjectScheduler
        attributes={attributes}
        onChange={this.handleProductChange}
        title="Product" />
    );
  }

  render(): Element {
    const attributes = _.get(this.props, 'product.attributes', {});

    return (
      <div className="fc-grid fc-grid-no-gutter">
        <div className="fc-col-md-3-5">
          <ObjectForm
            canAddProperty={true}
            onChange={this.handleProductChange}
            fieldsToRender={this.generalAttrs}
            attributes={attributes}
            title="General" />
          {this.skusContentBox}
          <ObjectForm
            onChange={this.handleProductChange}
            fieldsToRender={defaultKeys.seo}
            attributes={attributes}
            title="SEO" />
        </div>
        <div className="fc-col-md-2-5">
          <Tags
            attributes={attributes}
            onChange={this.handleProductChange} />
          {this.productState}
        </div>
      </div>
    );
  }
}
