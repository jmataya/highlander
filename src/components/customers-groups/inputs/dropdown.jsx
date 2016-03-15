//libs
import React, { PropTypes } from 'react';

//components
import Dropdown from '../../dropdown/dropdown';


const Input = ({criterion, value, prefixed, changeValue}) => {
  return (
    <Dropdown name={criterion.field}
              value={value}
              items={criterion.input.config.choices}
              onChange={changeValue} />
  );
};

Input.propTypes = {
  criterion: PropTypes.shape({
    field: PropTypes.string.isRequired,
  }).isRequired,
  prefixed: PropTypes.func.isRequired,
  value: PropTypes.any,
  changeValue: PropTypes.func.isRequired,
};

export default Input;
