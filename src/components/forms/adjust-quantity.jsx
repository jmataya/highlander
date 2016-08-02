// @flow

import _ from 'lodash';
import React, { Component } from 'react';
import classNames from 'classnames';
import styles from './css/adjust-quantity.css';
import { autobind } from 'core-decorators';

import Counter from './counter';
import Overlay from '../overlay/overlay';

type State = {
  diff: number,
  value: number,
  popupOpen: boolean,
}

type Props = {
  value: number,
  onChange: (quantity: number) => void,
}

export default class AdjustQuantity extends Component {
  props: Props;
  state: State = {
    diff: 0,
    value: this.props.value,
    popupOpen: false,
  };

  adjustValue(newValue: number) {
    this.setState({
      value: newValue,
      diff: newValue - this.props.value,
    }, () => {
      this.props.onChange(newValue);
    });
  }

  @autobind
  handleChange({target}) {
    const quantity = Number(target.value);
    if (!_.isNaN(quantity)) {
      this.adjustValue(quantity);
    }
  }

  @autobind
  handleInputFocus() {
    this.setState({
      popupOpen: true,
    });
  }

  hide() {
    this.setState({
      popupOpen: false,
    });
  }

  render() {
    const popupState = classNames({
      '_open': this.state.popupOpen,
    });

    return (
      <div styleName="block">
        <Overlay shown={this.state.popupOpen} onClick={() => this.hide()} />
        <input
          className="fc-text-input _no-counters"
          styleName="input"
          type="number"
          value={this.state.value}
          onChange={this.handleChange}
          onFocus={this.handleInputFocus}
          min={0}
        />
        <div styleName="popup" className={popupState}>
          <div styleName="title">Adjust Quantity</div>
          <Counter
            value={this.state.diff}
            increaseAction={() => this.adjustValue(this.state.value + 1)}
            decreaseAction={() => this.adjustValue(this.state.value - 1)}
            min={null}
            onBlur={evt => evt.stopPropagation()}
          />
        </div>
      </div>
    );
  }
}
