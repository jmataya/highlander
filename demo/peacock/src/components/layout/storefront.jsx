/* @flow */

import React, { Element } from 'react';

import Footer from '../footer/footer';

import styles from './storefront.css';

import type { RoutesParams } from 'types';

type Props = RoutesParams & {
  children: Element<*>,
  location: any,
};

const StoreFront = (props: Props) => {
  const childrenWithRoutes = React.Children.map(props.children,
    child => React.cloneElement(child, {
      routes: props.routes,
      routerParams: props.params,
    })
  );

  return (
    <div styleName="container">
      <div styleName="content-container">
        {childrenWithRoutes}
      </div>
      <Footer />
    </div>
  );
};

export default StoreFront;
