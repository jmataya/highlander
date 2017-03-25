/* @flow */

// libs
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import * as actions from 'modules/products';
import { categoryNameFromUrl } from 'paragons/categories';
import _ from 'lodash';

// components
import ProductsList, { LoadingBehaviors } from 'components/products-list/products-list';
import Facets from 'components/facets/facets';
import Breadcrumbs from 'components/breadcrumbs/breadcrumbs';

// styles
import styles from './products.css';

// types
import { Element, Route } from 'types';

type Params = {
  categoryName: ?string,
  subCategory: ?string,
  leafCategory: ?string,
};

type Category = {
  name: string,
  id: number,
  description: string,
};

type Props = {
  params: Params,
  list: Array<Object>,
  categories: ?Array<Category>,
  isLoading: boolean,
  fetch: Function,
  location: any,
  routes: Array<Route>,
  routerParams: Object,
};

type State = {
  sorting: {
    direction: number,
    field: string,
  },
  selectedFacets: {},
};

// redux
const mapStateToProps = (state) => {
  const async = state.asyncActions.products;

  return {
    ...state.products,
    isLoading: async ? async.inProgress : true,
    categories: state.categories.list,
  };
};

class Products extends Component {
  props: Props;
  state: State = {
    sorting: {
      direction: 1,
      field: 'salePrice',
    },
    selectedFacets: {},
  };

  componentWillMount() {
    const { categoryName, subCategory, leafCategory } = this.props.params;
    const { sorting, selectedFacets } = this.state;
    this.props.fetch([categoryName, subCategory, leafCategory], sorting, selectedFacets);
  }

  componentWillReceiveProps(nextProps: Props) {
    const { categoryName, subCategory, leafCategory } = this.props.params;
    const {
      categoryName: nextCategoryName,
      subCategory: nextSubCategory,
      leafCategory: nextLeafCategory,
    } = nextProps.params;

    if ((categoryName !== nextCategoryName) ||
        (subCategory !== nextSubCategory) ||
        (leafCategory !== nextLeafCategory)) {
      this.props.fetch([nextCategoryName, nextSubCategory, nextLeafCategory], this.state.sorting, this.state.selectedFacets);
    }
  }


  @autobind
  changeSorting(field: string) {
    const { sorting, selectedFacets } = this.state;
    const direction = sorting.field === field
      ? sorting.direction * (-1)
      : sorting.direction;

    const newState = {
      field,
      direction,
    };

    this.setState({selectedFacets: selectedFacets, sorting: newState}, () => {
      const { categoryName, subCategory, leafCategory } = this.props.params;
      this.props.fetch([categoryName, subCategory, leafCategory], newState, selectedFacets);
    });
  }

  @autobind
  categoryName(categoryName: string) {
    return categoryNameFromUrl(categoryName);
  }

  @autobind
  onSelectFacet(facet, value, selected) {
    let newSelection = this.state.selectedFacets;
    if(selected) {
      if (facet in newSelection) {
        newSelection[facet].push(value);
      } else {
        newSelection[facet] = [value];
      }
    } else if (facet in newSelection) {
      let values = newSelection[facet];
      let newValues = _.filter(values, (v) => {
        return v != value;
      });
      newSelection[facet] = newValues;
    }

    this.setState({selectedFacets: newSelection, sorting: this.state.sorting}, () => {
      const { categoryName, subCategory, leafCategory } = this.props.params;
      this.props.fetch([categoryName, subCategory, leafCategory], this.state.sorting, newSelection);
    });
  }

  renderHeader() {
    const { params } = this.props;
    const { categoryName, subCategory, leafCategory } = params;

    let realCategoryName = '';
    if (leafCategory) {
      realCategoryName = this.categoryName(leafCategory);
    } else if (subCategory) {
      realCategoryName = this.categoryName(subCategory);
    } else if (categoryName) {
      realCategoryName = this.categoryName(categoryName);
    }

    return (
      <header styleName="header">
        <div styleName="crumbs">
          <Breadcrumbs
            routes={this.props.routes}
            params={this.props.routerParams}
          />
        </div>
        <div>
          <h1 styleName="title">{realCategoryName}</h1>
        </div>
      </header>
    );
  }

  render(): Element<*> {
    return (
      <section styleName="catalog">
        {this.renderHeader()}
        <div styleName="dropDown">
          {this.navBar}
        </div>
        <div styleName="facetted-container">
          <div styleName="sidebar">
            <Facets facets={this.props.facets} onSelect={this.onSelectFacet} />
          </div>
          <div styleName="content">
            <ProductsList
              sorting={this.state.sorting}
              changeSorting={this.changeSorting}
              list={this.props.list}
              isLoading={this.props.isLoading}
              loadingBehavior={LoadingBehaviors.ShowWrapper}
            />
          </div>
        </div>
      </section>
    );
  }
}

export default connect(mapStateToProps, actions)(Products);
