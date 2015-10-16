'use strict';

import _ from 'lodash';
import React from 'react';
import { listenTo, stopListeningTo, dispatch } from '../../lib/dispatcher';

const changeMethodEvent = 'shippingMethodChange';

export default class ShippingMethodItem extends React.Component {

  render() {
    let model = this.props.model;
    let id = _.uniqueId('shipping-method');

    let editInput = (
      <div className="fc-radio">
        <input id={id} type="radio" defaultChecked={model.isActive} name="shipping-method-active" />
        <label className="fc-strong" htmlFor={id}>{model.storefrontDisplayName}</label>
      </div>
    );

    let readOnlyName = (
      <div className="fc-strong">
        {model.storefrontDisplayName}
      </div>
    );

    return this.props.isEditing ? editInput : readOnlyName;
  }
}

ShippingMethodItem.propTypes = {
  model: React.PropTypes.object
};
