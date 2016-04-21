
/* @flow weak */

//libs
import React, { PropTypes } from 'react';

// components
import { SectionTitle } from '../section-title';
import TabListView from '../tabs/tabs';
import TabView from '../tabs/tab';
import { Link, IndexLink } from '../link';

const InventoryItemDetailsBase = (props, context) => {
  const transactionsTabActive = context.router.isActive({name: 'inventory-item-transactions', params: props.params});

  return (
    <div className="fc-inventory-item-details">
      <div className="fc-inventory-item-details__summary">
        <div className="fc-grid">
          <div className="fc-col-md-1-1">
            <SectionTitle title="Inventory"/>
          </div>
        </div>
        <TabListView>
          <TabView draggable={false} selected={!transactionsTabActive}>
            <IndexLink to="inventory-item-details"
                       params={props.params}
                       className="fc-inventory-item-details__tab-link">
              Inventory
            </IndexLink>
          </TabView>
          <TabView draggable={false} selected={transactionsTabActive}>
            <Link to="inventory-item-transactions"
                  params={props.params}
                  className="fc-inventory-item-details__tab-link">
              Transactions
            </Link>
          </TabView>
        </TabListView>
      </div>

      {props.children}

    </div>
  );
};

InventoryItemDetailsBase.propTypes = {
  params: PropTypes.object,
  children: PropTypes.node,
};

InventoryItemDetailsBase.contextTypes = {
  router: PropTypes.object.isRequired,
};

export default InventoryItemDetailsBase;
