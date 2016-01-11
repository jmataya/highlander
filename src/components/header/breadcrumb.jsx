
// libs
import _ from 'lodash';
import React, {PropTypes} from 'react';
import { inflect } from 'fleck';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';

// components
import { Link, IndexLink } from '../link/index';

export default class Breadcrumb extends React.Component {

  @autobind
  readableName(route) {
    const parts = route.name.split('-');
    const composed = _.chain(parts)
      .filter(item => item !== 'base')
      .map(_.capitalize)
      .join(' ')
      .value();

    if (route.path != null && route.path[0] === ':') {
      return _.get(this.props, ['params', route.path.slice(1)], composed);
    } else {
      return composed;
    }
  }

  delimeter(idx) {
    return (
      <li className="fc-breadcrumbs__delimeter" key={`${idx}-breadcrumbs-delimeter`}>
        <i className="icon-chevron-right"></i>
      </li>
    );
  }

  get crumbs() {
    return _.compact(this.props.routes.map((route) => {
      if (_.isEmpty(route.path)) {
        return null;
      }

      if (route.path === "/" && _.isEmpty(route.name)) {
        return (
          <li className="fc-breadcrumbs__item" key="home-breadcrumbs-link">
            <Link to="home" params={this.props.params} className="fc-breadcrumbs__link">Home &nbsp;</Link>
          </li>
        );
      }

      if (_.isEmpty(route.indexRoute)) {
        return (
          <li className="fc-breadcrumbs__item" key={`${route.name}-breadcrumbs-link`}>
            <Link to={route.name} params={this.props.params} className="fc-breadcrumbs__link">
              {this.readableName(route)}
            </Link>
          </li>
        );
      }

      return (
        <li className="fc-breadcrumbs__item" key={`${route.name}-breadcrumbs-link`}>
          <IndexLink to={route.indexRoute.name} params={this.props.params} className="fc-breadcrumbs__link">
            {this.readableName(route)}
          </IndexLink>
        </li>
      );
    }))
  }

  render() {
    const fromRoutes = this.crumbs;

    const delimeters = _.range(1, fromRoutes.length).map((idx) => {
      return this.delimeter(idx);
    });

    const withDelimeter = _.zip(fromRoutes, delimeters);

    return (
      <div className="fc-breadcrumbs">
        <ul>
          {withDelimeter}
        </ul>
      </div>
    );
  }
}
