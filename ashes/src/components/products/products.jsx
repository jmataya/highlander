/**
 * @flow
 */

// libs
import get from 'lodash/get';
import React, { Component, Element } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

// data
import { actions } from 'modules/products/list';

// components
import SelectableSearchList from '../list-page/selectable-search-list';
import ProductRow from './product-row';

// helpers
import { filterArchived } from 'elastic/archive';

type Props = {
  actions: Object,
  list: Object,
};

const tableColumns: Columns = [
  { field: 'productId', text: 'Product ID' },
  { field: 'image', text: 'Image', type: 'image' },
  { field: 'title', text: 'Name' },
  { field: 'state', text: 'State' },
];

export class Products extends Component {
  props: Props;

  @autobind
  addSearchFilters(filters: Array<SearchFilter>, initial: boolean = false) {
    return this.props.actions.addSearchFilters(filterArchived(filters), initial);
  }

  renderRow(row: Product, index: number, columns: Columns, params: Object) {
    const key = `products-${get(row, 'id', index)}`;
    return <ProductRow key={key} product={row} columns={columns} params={params} />;
  }

  render() {
    const { list, actions } = this.props;

    const searchActions = {
      ...actions,
      addSearchFilters: this.addSearchFilters,
    };

    return (
      <div className="fc-products-list">
        <SelectableSearchList
          entity="products.list"
          emptyMessage="No products found."
          list={list}
          renderRow={this.renderRow}
          tableColumns={tableColumns}
          searchActions={searchActions}
          predicate={({id}) => id} />
      </div>
    );
  }
}

function mapStateToProps({ products: { list } }) {
  return { list };
}

function mapDispatchToProps(dispatch) {
  return {
    actions: bindActionCreators(actions, dispatch),
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(Products);
