import React from 'react';
import { Route, IndexRoute, IndexRedirect } from 'react-router';
import Site from './components/site/site';
import AuthPages from './components/site/auth-pages';
import Home from './components/home/home';
import Notes from './components/notes/notes';
import ActivityTrailPage from './components/activity-trail/activity-trail-page';
import GiftCards from './components/gift-cards/gift-cards';
import GiftCardsListPage from './components/gift-cards/list-page';
import NewGiftCard from './components/gift-cards/gift-cards-new';
import GiftCard from './components/gift-cards/gift-card';
import GiftCardTransactions from './components/gift-cards/transactions';
import StoreCredits from './components/customers/store-credits/store-credits';
import StoreCreditsTransactions from './components/customers/store-credits/transactions';
import NewStoreCredit from './components/customers/store-credits/new-store-credit';
import InventoryListPage from './components/inventory/list-page';
import InventoryList from './components/inventory/list';
import InventoryItemDetailsBase from './components/inventory/item-details-base';
import InventoryItemDetails from './components/inventory/item-details';
import InventoryItemTransactions from './components/inventory/item-transactions';
import ProductsListPage from './components/products/list-page';
import Products from './components/products/products';
import ProductPage from './components/products/page';
import ProductForm from './components/products/product-form';
import ProductImages from './components/products/images';
import Skus from './components/skus/skus';
import SkusListPage from './components/skus/list-page';
import SkuPage from './components/skus/page';
import SkuDetails from './components/skus/details';
import SkuImages from './components/skus/images';
import PromotionsListPage from './components/promotions/list';
import Promotions from './components/promotions/promotions';
import PromotionPage from './components/promotions/promotion-page';
import PromotionForm from './components/promotions/promotion-form';
import CouponsListPage from './components/coupons/list';
import Coupons from './components/coupons/coupons';
import CouponPage from './components/coupons/page';
import CouponForm from './components/coupons/form';
import CouponCodes from './components/coupons/codes';

import PluginsList from './components/plugins/plugins-list';
import Plugin from './components/plugins/plugin';

import Login from './components/auth/login';
import SetPassword from './components/auth/set-password';

import customerRoutes from './customers/routes';
import orderRoutes from './orders/routes';
import userRoutes from './users/routes';

// no productions pages, make sure these paths are included in `excludedList` in browserify.js
if (process.env.NODE_ENV != 'production') {
  var StyleGuide = require('./components/style-guide/style-guide').default;
  var StyleGuideGrid = require('./components/style-guide/style-guide-grid').default;
  var StyleGuideButtons = require('./components/style-guide/style-guide-buttons').default;
  var StyleGuideContainers = require('./components/style-guide/style-guide-containers').default;

  var AllActivities = require('./components/activity-trail/all').default;
  var AllNotificationItems = require('components/activity-notifications/all').default;
}

const routes = (
  <Route path="/">
    <IndexRedirect to="/orders/"/>

    <Route component={AuthPages}>
      <Route name="login" path="login" component={Login}/>
      <Route name="set-password" path="signup" component={SetPassword}/>
    </Route>
    <Route component={Site}>
      <IndexRoute name="home" component={Home}/>
      {orderRoutes()}
      {customerRoutes()}
      <Route name='products-base' path='products'>
        <Route name='products-list-pages' component={ProductsListPage}>
          <IndexRoute name='products' component={Products} />
        </Route>
        <Route name='product' path=':context/:productId' titleParam=":productId" component={ProductPage}>
          <IndexRoute name='product-details' component={ProductForm} />
          <Route name='new-product' component={ProductForm} />
          <Route name='product-images' title='Images' path='images' component={ProductImages} />
          <Route name='product-notes' title='Notes' path='notes' component={Notes} />
          <Route name='product-activity-trail' title='Activity Trail'
                 path='activity-trail' component={ActivityTrailPage} />
        </Route>
      </Route>
      <Route name='skus-base' title='SKUs' path='skus'>
        <Route name='skus-list-pages' component={SkusListPage}>
          <IndexRoute name='skus' component={Skus} />
        </Route>
        <Route name='sku' path=':skuCode' component={SkuPage}>
          <IndexRoute name='sku-details' component={SkuDetails} />
          <Route name='sku-images' path='images' title='Images' component={SkuImages} />
          <Route name='sku-inventory-details-base' path="inventory" component={InventoryItemDetailsBase}>
            <IndexRoute name='sku-inventory-details' component={InventoryItemDetails} />
            <Route
              title='Transactions'
              name='sku-inventory-transactions'
              path='transactions'
              component={InventoryItemTransactions}
            />
          </Route>
          <Route name='sku-notes' path='notes' title='Notes' component={Notes} />
          <Route name='sku-activity-trail' path='activity-trail' title='Activity Trail' component={ActivityTrailPage} />
        </Route>
      </Route>
      <Route name='gift-cards-base' path='gift-cards'>
        <Route name='gift-cards-list-page' component={GiftCardsListPage}>
          <IndexRoute name='gift-cards' component={GiftCards}/>
          <Route name='gift-cards-activity-trail' path='activity-trail' dimension="gift-card"
                 component={ActivityTrailPage}/>
        </Route>
        <Route name='gift-cards-new' path='new' component={NewGiftCard} />
        <Route name='giftcard' path=':giftCard' component={GiftCard}>
          <IndexRoute name='gift-card-transactions' component={GiftCardTransactions} />
          <Route name='gift-card-notes' path='notes' component={Notes} />
          <Route name='gift-card-activity-trail' path='activity-trail' component={ActivityTrailPage} />
        </Route>
      </Route>
      <Route name='inventory-base' path='inventory'>
        <Route name='inventory-list-page' component={InventoryListPage}>
          <IndexRoute name='inventory' component={InventoryList}/>
          <Route name='inventory-activity-trail'
                 path='activity-trail'
                 dimension="inventory"
                 component={ActivityTrailPage}/>
        </Route>
      </Route>
      <Route name='promotions-base' path='promotions'>
        <Route name='promotions-list-page' component={PromotionsListPage} >
          <IndexRoute name='promotions' component={Promotions} />
          <Route name='promotions-activity-trail' path='activity-trail' dimension="promotions"
                 component={ActivityTrailPage}/>
        </Route>
        <Route name='promotion' path=':promotionId' component={PromotionPage}>
          <IndexRoute name='promotion-details' component={PromotionForm} />
          <Route name='promotion-notes' path='notes' component={Notes} />
          <Route name='promotion-activity-trail' path='activity-trail' component={ActivityTrailPage}/>
        </Route>
      </Route>
      <Route name='coupons-base' path='coupons'>
        <Route name='coupons-list-page' component={CouponsListPage} >
          <IndexRoute name='coupons' component={Coupons} />
          <Route name='coupons-activity-trail' path='activity-trail' dimension="coupons"
                 component={ActivityTrailPage}/>
        </Route>
      </Route>
      <Route name='coupons-base' path='coupons'>
        <Route name='coupons-list-page' component={CouponsListPage} >
          <IndexRoute name='coupons' component={Coupons} />
        </Route>
        <Route name='coupon' path=':couponId' component={CouponPage}>
          <IndexRoute name='coupon-details' component={CouponForm} />
          <Route name='coupon-codes' path='codes' component={CouponCodes} />
          <Route name='coupon-notes' path='notes' component={Notes} />
          <Route name='coupon-activity-trail' path='activity-trail' component={ActivityTrailPage}/>
        </Route>
      </Route>
      {process.env.NODE_ENV != 'production' &&
        <Route name='style-guide' path='style-guide' component={StyleGuide}>
          <IndexRoute name='style-guide-grid' component={StyleGuideGrid}/>
          <Route name='style-guide-buttons' path='buttons' component={StyleGuideButtons}/>
          <Route name='style-guide-containers' path='containers' component={StyleGuideContainers}/>
        </Route>
      }
      {process.env.NODE_ENV != 'production' &&
        <Route name='test' path="_">
          <Route name='test-activities' path='activities' component={AllActivities}/>
          <Route name='test-notifications' path='notifications' component={AllNotificationItems}/>
        </Route>
      }
      {userRoutes()}
      <Route path="plugins" name="plugins-base">
        <IndexRoute name="plugins" component={PluginsList} />
        <Route name="plugin" path=":name" component={Plugin} />
      </Route>
    </Route>
  </Route>
);

export default routes;
