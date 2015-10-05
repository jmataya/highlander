'use strict';

import React from 'react';
import { Route, DefaultRoute } from 'react-router';
import Site from './components/site/site';
import Home from './components/home/home';
import Rmas from './components/rmas/rmas';
import Rma from './components/rmas/rma';
import RmaDetails from './components/rmas/details';
import Orders from './components/orders/orders';
import Order from './components/orders/order';
import OrderDetails from './components/orders/details';
import Customers from './components/customers/customers';
import Customer from './components/customers/customer';
import Notes from './components/notes/notes';
import Notifications from './components/notifications/notifications';
import ActivityTrail from './components/activity-trail/activity-trail';
import AddressBook from './components/addresses/addresses';
import GiftCards from './components/gift-cards/gift-cards';
import NewGiftCard from './components/gift-cards/gift-cards-new';
import GiftCard from './components/gift-cards/gift-card';
import GiftCardTransactions from './components/gift-cards/transactions';

const routes = (
  <Route handler={Site}>
    <DefaultRoute name="home" handler={Home}/>
    <Route name='orders' handler={Orders}/>
    <Route name='rmas' path='returns' handler={Rmas}/>
    <Route name='rma' path='/returns/:rma' handler={Rma}>
      <DefaultRoute name='rma-details' handler={RmaDetails}/>
      <Route name='rma-notes' path='notes' handler={Notes}/>
      <Route name='rma-notifications' path='notifications' handler={Notifications}/>
      <Route name='rma-activity-trail' path='activity-trail' handler={ActivityTrail}/>
    </Route>
    <Route name='order' path='/orders/:order' handler={Order}>
      <DefaultRoute name='order-details' handler={OrderDetails}/>
      <Route name='order-notes' path='notes' handler={Notes}/>
      <Route name='order-returns' path='returns' handler={Rmas}/>
      <Route name='order-notifications' path='notifications' handler={Notifications}/>
      <Route name='order-activity-trail' path='activity-trail' handler={ActivityTrail}/>
    </Route>
    <Route name='customers' handler={Customers}/>
    <Route name='customer' path='/customers/:customer' handler={Customer}>
      <Route name='customer-addresses' path='addresses' handler={AddressBook} />
    </Route>
    <Route name='gift-cards' path='/gift-cards' handler={GiftCards} />
    <Route name='gift-cards-new' path='/gift-cards/new' handler={NewGiftCard} />
    <Route name='giftcard' path='/gift-cards/:giftcard' handler={GiftCard}>
      <DefaultRoute name='gift-card-transactions' handler={GiftCardTransactions} />
      <Route name='gift-card-notes' path='notes' handler={Notes} />
      <Route name='gift-card-activity-trail' path='activity-trail' handler={ActivityTrail} />
    </Route>
  </Route>
);

export default routes;
