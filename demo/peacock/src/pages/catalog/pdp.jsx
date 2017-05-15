/* @flow */

// libs
import _ from 'lodash';
import { assoc } from 'sprout-data';
import React, { Component, Element } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import * as tracking from 'lib/analytics';

// i18n
import localized from 'lib/i18n';
import type { Localized } from 'lib/i18n';

// modules
import { searchGiftCards } from 'modules/products';
import { fetch, getNextId, getPreviousId, resetProduct } from 'modules/product-details';
import { addLineItem, toggleCart } from 'modules/cart';

// styles
import styles from './pdp.css';

// components
// import { SecondaryButton } from 'ui/buttons';
import AddToCartBtn from 'ui/add-to-cart-btn';
import Currency from 'ui/currency';
import Gallery from 'ui/gallery/gallery';
import Loader from 'ui/loader';
import ErrorAlerts from 'ui/alerts/error-alerts';
import ProductVariants from 'components/product-variants/product-variants';
import GiftCardForm from 'components/gift-card-form';
import ImagePlaceholder from 'components/products-item/image-placeholder';

// types
import type { ProductResponse, Sku } from 'modules/product-details';
import type { RoutesParams } from 'types';
import type { TProductView } from './types';

type Params = {
  productSlug: string,
};

type Actions = {
  fetch: (id: number) => any,
  getNextId: Function,
  getPreviousId: Function,
  resetProduct: Function,
  addLineItem: Function,
  toggleCart: Function,
};

type Props = Localized & RoutesParams & {
  actions: Actions,
  params: Params,
  product: ?ProductResponse,
  isLoading: boolean,
  isCartLoading: boolean,
  notFound: boolean,
};

type State = {
  error?: any,
  currentSku: ?Sku,
  attributes?: Object,
};

const mapStateToProps = (state) => {
  const product = state.productDetails.product;

  return {
    product,
    fetchError: _.get(state.asyncActions, 'pdp.err', null),
    notFound: !product && _.get(state.asyncActions, 'pdp.err.response.status') == 404,
    isLoading: _.get(state.asyncActions, ['pdp', 'inProgress'], true),
    isCartLoading: _.get(state.asyncActions, ['cartChange', 'inProgress'], false),
  };
};

const mapDispatchToProps = dispatch => ({
  actions: bindActionCreators({
    fetch,
    getNextId,
    getPreviousId,
    resetProduct,
    addLineItem,
    toggleCart,
  }, dispatch),
});

class Pdp extends Component {
  props: Props;
  productPromise: Promise<*>;
  _productDetails: ProductVariants;

  state: State = {
    currentSku: null,
    attributes: {},
  };

  componentWillMount() {
    if (_.isEmpty(this.props.product)) {
      this.productPromise = this.fetchProduct();
    } else {
      this.productPromise = Promise.resolve();
    }

    if (typeof document !== 'undefined') {
      // $FlowFixMe: there is product-recommender in node_modules
      const renderProductRecommender = require('product-recommender').default;
      renderProductRecommender(this.props.product.id, 'product-recommender');
    }
  }

  componentDidMount() {
    this.productPromise.then(() => {
      tracking.viewDetails(this.productView);
    });
  }

  componentWillUnmount() {
    this.props.actions.resetProduct();
  }

  componentWillUpdate(nextProps) {
    const nextId = this.getId(nextProps);

    if (this.productId !== nextId) {
      this.setState({ currentSku: null });
      this.props.actions.resetProduct();
      this.fetchProduct(nextProps, nextId);
    }
  }

  safeFetch(id) {
    return this.props.actions.fetch(id)
      .catch(() => {
        const { params } = this.props;
        this.props.actions.fetch(params.productSlug);
      });
  }

  fetchProduct(_props, _productId) {
    const props = _props || this.props;
    const productId = _productId || this.productId;

    if (this.isGiftCardRoute(props)) {
      return searchGiftCards().then(({ result = [] }) => {
        const giftCard = result[0] || {};
        return this.safeFetch(giftCard.productId);
      });
    }
    return this.safeFetch(productId);
  }

  get productId(): string|number {
    return this.getId(this.props);
  }

  get isArchived(): boolean {
    return !!_.get(this.props, ['product', 'archivedAt']);
  }

  @autobind
  getId(props): string|number {
    const slug = props.params.productSlug;

    if (/^\d+$/g.test(slug)) {
      return parseInt(slug, 10);
    }

    return slug;
  }

  get currentSku() {
    return this.state.currentSku || this.sortedSkus[0];
  }

  get sortedSkus() {
    return _.sortBy(
      _.get(this.props, 'product.skus', []),
      'attributes.salePrice.v.value'
    );
  }

  @autobind
  setCurrentSku(currentSku: Sku) {
    this.setState({ currentSku });
  }

  @autobind
  setAttributeFromField({ target: { name, value } }) {
    const namePath = ['attributes', ...name.split('.')];
    const stateValue = name === 'giftCard.message' ? value.split('\n').join('<br>') : value;
    this.setState(assoc(this.state, namePath, stateValue));
  }

  get productView(): TProductView {
    const attributes = _.get(this.props.product, 'attributes', {});
    const price = _.get(this.currentSku, 'attributes.salePrice.v', {});
    let images = _.get(this.currentSku, ['albums', 0, 'images'], []);
    if (_.isEmpty(images)) {
      images = _.get(this.props.product, ['albums', 0, 'images'], []);
    }
    const imageUrls = images.map(image => image.src);

    return {
      title: _.get(attributes, 'title.v', ''),
      description: _.get(attributes, 'description.v', ''),
      images: imageUrls,
      currency: _.get(price, 'currency', 'USD'),
      price: _.get(price, 'value', 0),
      skus: this.sortedSkus,
    };
  }

  get productShortDescription(): ?Element<*> {
    const shortDescription = _.get(this.props.product, 'attributes.shortDescription.v');

    if (!shortDescription) return null;

    return (
      <h2 styleName="short-description">{shortDescription}</h2>
    );
  }

  isGiftCardRoute(props = this.props) {
    return props.route.name === 'gift-cards';
  }

  isGiftCard(props = this.props): boolean {
    const tags = _.get(props.product, 'attributes.tags.v', []);
    return tags.indexOf('GIFT-CARD') !== -1;
  }

  @autobind
  addToCart(): void {
    const { actions } = this.props;
    const unselectedFacets = this._productDetails.getUnselectedFacets();
    if (unselectedFacets.length) {
      this._productDetails.flashUnselectedFacets(unselectedFacets);
      return;
    }
    const skuCode = _.get(this.currentSku, 'attributes.code.v', '');
    tracking.addToCart(this.productView, 1);
    actions.addLineItem(skuCode, 1, this.state.attributes)
      .then(() => {
        actions.toggleCart();
        this.setState({
          attributes: {},
          currentSku: null,
        });
      })
      .catch((ex) => {
        this.setState({
          error: ex,
        });
      });
  }

  renderGallery() {
    const { images } = this.productView;

    return !_.isEmpty(images)
      ? <Gallery images={images} />
      : <ImagePlaceholder largeScreenOnly />;
  }

  get productDetails(): Element<*> {
    const description = _.get(this.props.product, 'attributes.description.v', '');
    const descriptionList = _.get(this.props.product, 'attributes.description_list.v', '');
    return (
      <div styleName="body">
        <div
          styleName="description"
          dangerouslySetInnerHTML={{__html: description}}
        />
        <ul
          styleName="description-list"
          dangerouslySetInnerHTML={{__html: descriptionList}}
        />
      </div>
    );
  }

  @autobind
  handleSkuChange(sku: ?Sku) {
    if (sku) {
      this.setCurrentSku(sku);
    }
  }

  @autobind
  getTaxonValue(name: string): ?string {
    const taxons = _.get(this.props.product, 'taxons', []);
    const taxonomy = _.find(taxons, (taxonomyEntity) => {
      const taxonomyName = _.get(taxonomyEntity, 'attributes.name.v');
      return name === taxonomyName;
    });

    return _.get(taxonomy, ['taxons', 0, 'attributes', 'name', 'v']);
  }

  get productCategory(): ?Element<any> {
    let gender = this.getTaxonValue('gender');
    const type = this.getTaxonValue('type');

    if (gender && type) {
      if (gender.toLowerCase() === 'men') {
        gender = 'men\'s';
      } else if (gender.toLowerCase() === 'women') {
        gender = 'women\'s';
      }

      return (
        <div>{`${gender} ${type}`}</div>
      );
    }
  }

  get productForm(): Element<any> {
    if (this.isGiftCard()) {
      return (
        <GiftCardForm
          productView={this.productView}
          onSkuChange={this.setCurrentSku}
          selectedSku={this.currentSku}
          attributes={this.state.attributes}
          onAttributeChange={this.setAttributeFromField}
        />
      );
    }
    return (
      <ProductVariants
        ref={(_ref) => { this._productDetails = _ref; }}
        product={this.props.product}
        productView={this.productView}
        selectedSku={this.currentSku}
        onSkuChange={this.handleSkuChange}
      />
    );
  }

  get productPrice(): ?Element<any> {
    if (this.isGiftCard()) return null;
    const {
      currency,
      price,
      skus,
    } = this.productView;

    const salePrice = _.get(skus[0], 'attributes.salePrice.v.value', 0);
    const retailPrice = _.get(skus[0], 'attributes.retailPrice.v.value', 0);

    if (retailPrice > salePrice) {
      return (
        <div styleName="price">
          <Currency
            styleName="retail-price"
            value={retailPrice}
            currency={currency}
          />
          <Currency
            styleName="on-sale-price"
            value={salePrice}
            currency={currency}
          />
        </div>
      );
    }

    return (
      <div styleName="price">
        <Currency value={price} currency={currency} />
      </div>
    );
  }

  render(): Element<any> {
    const {
      t,
      isLoading,
      notFound,
      fetchError,
    } = this.props;

    if (isLoading) {
      return <Loader />;
    }

    if (notFound || this.isArchived) {
      return <p styleName="not-found">{t('Product not found')}</p>;
    }

    if (fetchError) {
      return <ErrorAlerts error={fetchError} />;
    }
    const title = this.isGiftCard() ? t('Gift Card') : this.productView.title;

    return (
      <div styleName="container">
        <div styleName="body">
          <div styleName="sixty">
            {this.renderGallery()}
          </div>
          <div styleName="forty">
            <div styleName="category">{this.productCategory}</div>
            <h1 styleName="title">{title}</h1>
            <ErrorAlerts error={this.state.error} />
            {this.productPrice}
            {this.productForm}
            <div styleName="cart-actions">
              <AddToCartBtn
                onClick={this.addToCart}
              />
              {/* <SecondaryButton styleName="one-click-checkout">1-click checkout</SecondaryButton> */}
            </div>
          </div>
        </div>
        <div styleName="title-block">
          <h1 styleName="title-secondary">{title}</h1>
          {this.productShortDescription}
        </div>
        {this.productDetails}
        <div styleName="share-block">
          <div styleName="share-title">
            Share How You Wear It
          </div>
          <p styleName="share-description">
            For your change to be featured in our photo gallery<br />
            tag your favorite Pure photo using #3stripestyle.
          </p>
          <img styleName="share-image" src="/images/pdp/style.jpg" />
        </div>
        <div id="product-recommender">test</div>
      </div>
    );
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(localized(Pdp));
