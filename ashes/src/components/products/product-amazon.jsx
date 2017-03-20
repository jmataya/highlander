/**
 * @flow weak
 */

// libs
import React, { Component, Element, PropTypes } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import classNames from 'classnames';
import _ from 'lodash';
import * as productActions from 'modules/products/details';
import * as amazonActions from 'modules/channels/amazon';
import * as schemaActions from 'modules/object-schema';
import s from './product-amazon.css';
import { Suggester } from 'components/suggester/suggester';
import WaitAnimation from '../common/wait-animation';
import Progressbar from '../common/progressbar';
import { getSuggest } from './selector';
import ObjectFormInner from '../object-form/object-form-inner';
import ProductAmazonForm from './product-amazon-form';
import ContentBox from '../content-box/content-box';

function mapDispatchToProps(dispatch) {
  return {
    actions: bindActionCreators({
      ...schemaActions,
      updateProduct: productActions.updateProduct,
      fetchProduct: productActions.fetchProduct,
      clearAmazonErrors: amazonActions.clearErrors,
      resetAmazonState: amazonActions.reset,
      fetchAmazonSchema: amazonActions.fetchAmazonSchema,
      fetchSuggest: amazonActions.fetchSuggest,
      pushProduct: amazonActions.pushToAmazon,
      fetchProductStatus: amazonActions.fetchAmazonProductStatus,
    }, dispatch),
  };
}

function mapStateToProps(state) {
  const product = state.products.details.product;
  const { suggest, schema, productStatus } = state.channels.amazon;

  return {
    title: product && product.attributes && product.attributes.title.v,
    product,
    productStatus,
    fetchingProduct: _.get(state.asyncActions, 'fetchProduct.inProgress'),
    fetchingSuggest: _.get(state.asyncActions, 'fetchSuggest.inProgress'),
    fetchingSchema: _.get(state.asyncActions, 'fetchAmazonSchema.inProgress'),
    pushingProduct: _.get(state.asyncActions, 'pushToAmazon.inProgress'),
    suggest: getSuggest(suggest),
    schema,
  };
}

type State = {
  categoryId: string,
  categoryPath: string,
  stepNum: number,
};

// @todo maybe move to another component?
const steps = [{
  text: 'Choose Category'
}, {
  text: 'Fill all fields'
}, {
  text: 'Submit'
}];

class ProductAmazon extends Component {
  state: State = {
    categoryId: '',
    categoryPath: '',
    stepNum: 0,
  };

  componentDidMount() {
    const { productId } = this.props.params;
    const { product } = this.props;
    const { clearAmazonErrors, fetchProduct, fetchProductStatus, fetchSchema, resetAmazonState } = this.props.actions;

    if (!product) {
      clearAmazonErrors();
      resetAmazonState();
      fetchSchema('product');
      fetchProduct(productId);
    }

    // setInterval(() => {
    //   fetchProductStatus(productId);
    // }, 10 * 1000);
  }

  componentWillUpdate(nextProps) {
    const nodeId = _.get(this.props.product, ['attributes', 'nodeId', 'v'], null);
    const nextNodeId = _.get(nextProps.product, ['attributes', 'nodeId', 'v'], null);
    const nextNodePath = _.get(nextProps.product, ['attributes', 'nodePath', 'v'], null);

    if (nextNodeId && nextNodeId != nodeId && nextNodePath) {
      this._setCat(nextNodeId, nextNodePath);
    }
  }

  // @todo move to the new component
  renderForm() {
    const { schema, product, fetchingSchema } = this.props;
    const { categoryId, categoryPath } = this.state;

    if (!schema || !product) {
      if (fetchingSchema) {
        return <WaitAnimation />;
      }

      return null;
    }

    return (
      <ProductAmazonForm
        schema={schema}
        product={product}
        categoryId={categoryId}
        categoryPath={categoryPath}
        onSubmit={(p) => this._handleSubmit(p)}
      />
    );
  }

  render() {
    const { title, suggest, product, productStatus, fetchingProduct, fetchingSuggest } = this.props;
    const { stepNum } = this.state;
    const progressSteps = steps.map((step, i) => ({
      text: `${i+1}. ${step.text}`,
      current: i == stepNum,
      incompleted: i > stepNum,
    }));

    if (!product || fetchingProduct) {
      return <div className={s.root}><WaitAnimation /></div>;
    }

    // @todo productStatus

    return (
      <div className={s.root}>
        <Progressbar steps={progressSteps} className={s.progressbar} />
        <h1>{title} for Amazon</h1>
        <ContentBox title="Amazon Category">
          <div className={s.suggesterWrapper}>
            <Suggester
              className={s.suggester}
              onChange={(text) => this._onTextChange(text)}
              onPick={this._onCatPick.bind(this)}
              data={suggest}
              inProgress={fetchingSuggest}
            />
          </div>
        </ContentBox>
        {this.renderForm()}
        <button onClick={this._handlePush.bind(this)}>Push</button>
      </div>
    );
  }

  _getNodeId() {
    const { product } = this.props;

    return _.get(product, ['attributes', 'nodeId', 'v'], null);
  }

  _onTextChange(text) {
    const { title } = this.props;

    this.props.actions.fetchSuggest(title, text);
  }

  _onCatPick(item) {
    const { id, path } = item;

    this._setCat(id, path);
  }

  _setCat(id, path) {
    const { fetchAmazonSchema } = this.props.actions;

    this.setState({ categoryId: id, categoryPath: path, stepNum: 1 });

    fetchAmazonSchema(id);
  }

  _handleSubmit(nextProduct) {
    const { actions: { updateProduct } } = this.props;

    updateProduct(nextProduct);
  }

  _handlePush() {
    const { product, actions: { pushProduct } } = this.props;

    pushProduct(product.id);
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(ProductAmazon);
