'use strict';

import React from 'react/addons';

export default class TypeaheadResults extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      results: props.store.getState()
    };
  }

  componentDidMount() {
    let
      store = this.props.store,
      storeName = store.storeName;

    this[`onChange${storeName}`] = this.onStoreChange;
    store.listenToEvent('change', this);
  }

  componentWillUnmount() {
    this.props.store.stopListeningToEvent('change', this);
  }

  createComponent(props) {
    return React.createElement(this.props.component, props);
  }

  onStoreChange() {
    this.setState({results: this.props.store.getState()});
  }

  render() {
    let innerContent = null;

    if (this.state.results.length > 0) {
      innerContent = this.state.results.map((result) => {
        return <li key={result.id}>{this.createComponent({result: result})}</li>;
      });
    } else {
      innerContent = <li>No results found.</li>;
    }

    return (
      <ul className={`typeahead-results ${this.props.showResults ? 'show' : ''}`}>
        {innerContent}
      </ul>
    );
  }
}

TypeaheadResults.propTypes = {
  store: React.PropTypes.object,
  component: React.PropTypes.func,
  showResults: React.PropTypes.bool
};
