'use strict';

import React from 'react';
import IncrementButton from '../common/increment-button';
import DecrementButton from '../common/decrement-button';

const defaultProps = {
  value: 1,
  stepAmount: 1,
  minValue: 1,
  maxValue: 100
};

const noop = () => {
  return;
};

const Counter = (props) => {
  let handleChange = props.onChange || noop;
  let handleIncreaseTotal = props.increaseTotal || noop;
  let handleDecreaseTotal = props.decreaseTotal || noop;
  let value = props.value || defaultProps.value;
  let minValue = props.minValue || defaultProps.minValue;
  let maxValue = props.maxValue || defaultProps.maxValue;
  let stepAmount = props.stepAmount || defaultProps.stepAmount;

  return (
    <div className="fc-input-group fc-counter">
      <div className="fc-input-prepend">
        <DecrementButton onClick={handleDecreaseTotal} />
      </div>
      <input
        type="number"
        id={props.inputName}
        name={props.inputName}
        value={value}
        min={minValue}
        max={maxValue}
        step={stepAmount}
        onChange={handleChange} />
      <div className="fc-input-append">
        <IncrementButton onClick={handleIncreaseTotal} />
      </div>
    </div>
  );
};

export default Counter;