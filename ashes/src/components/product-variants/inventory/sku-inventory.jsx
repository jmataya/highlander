// @flow weak

import _ from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

// components
import ExpandableTable from 'components/table/expandable-table';
import InventoryWarehouseRow from './sku-inventory-row';
import WarehouseDrawer from './sku-inventory-drawer';

// redux
import * as WarehousesActions from 'modules/inventory/warehouses';
import type { WarehouseInventorySummary, WarehouseInventoryMap } from 'modules/inventory/warehouses';

const mapStateToProps = (state, props) => ({
  inventoryDetails: _.get(state, ['inventory', 'warehouses', 'details', props.skuId], {}),
  fetchState: _.get(state, 'asyncActions.inventory-summary', {}),
  inventoryUpdated: _.get(state, 'asyncActions.inventory-increment.finished', false),
});

type Props = {
  skuId: number;
  // connected
  inventoryDetails: WarehouseInventoryMap,
  fetchSummary: (id: number) => Promise,
  fetchState: {
    inProgress?: boolean,
    err?: any,
  }
}

function array2tableData(rows) {
  return {
    rows,
    total: rows.length,
    from: 0,
    size: rows.length,
  };
}

class InventoryItemDetails extends Component {
  props: Props;

  componentDidMount() {
    this.props.fetchSummary(this.props.skuId);
  }

  componentWillReceiveProps(nextProps: Props) {
    if (!this.props.inventoryUpdated && nextProps.inventoryUpdated) {
      this.props.fetchSummary(this.props.skuId);
    }
  }

  get tableColumns() {
    return [
      { field: 'stockLocation.name', text: 'Warehouse' },
      { field: 'onHand', text: 'On Hand' },
      { field: 'onHold', text: 'Hold' },
      { field: 'reserved', text: 'Reserved' },
      { field: 'afs', text: 'AFS' },
      { field: 'afsCost', text: 'AFS Cost Value', type: 'currency' },
    ];
  }

  get drawerColumns() {
    return [
      { field: 'type', text: 'Type' },
      { field: 'onHand', text: 'On Hand' },
      { field: 'onHold', text: 'Hold' },
      { field: 'reserved', text: 'Reserved' },
      { field: 'afs', text: 'AFS' },
      { field: 'afsCost', text: 'AFS Cost Value', type: 'currency' },
    ];
  }

  @autobind
  renderDrawer(row: WarehouseInventorySummary, index, params) {
    return (
      <WarehouseDrawer
        data={this.drawerData(row)}
        columns={this.drawerColumns}
        isLoading={_.get(this.props, ['fetchState', 'inProgress'], true)}
        failed={!!_.get(this.props, ['fetchState', 'err'])}
        params={params}
      />
    );
  }

  @autobind
  renderRow(row: WarehouseInventorySummary, index, columns, params) {
    const key = `inventory-warehouse-${row.stockLocation.id}`;
    return (
      <InventoryWarehouseRow
        key={key}
        warehouse={row}
        columns={columns}
        params={params}
      />
    );
  }

  get summaryData() {
    const { inventoryDetails } = this.props;

    return array2tableData(_.map(inventoryDetails, _.identity));
  }

  drawerData(warehouseSummary: WarehouseInventorySummary) {
    const inventoryDetails: WarehouseInventoryMap = this.props.inventoryDetails;

    const stockItems = _.get(inventoryDetails, [warehouseSummary.stockLocation.id, 'stockItems'], []);
    return array2tableData(stockItems);
  }

  render() {
    const isFetching = this.props.fetchState.inProgress !== false;
    const failed = !!this.props.fetchState.err;

    return (
      <ExpandableTable
        columns={this.tableColumns}
        data={this.summaryData}
        renderRow={this.renderRow}
        renderDrawer={this.renderDrawer}
        idField="stockLocation.id"
        isLoading={isFetching}
        failed={failed}
        emptyMessage="No warehouse data found."
        className="fc-inventory-item-details__warehouses-table"
      />
    );
  }
}

export default connect(mapStateToProps, WarehousesActions)(InventoryItemDetails);
