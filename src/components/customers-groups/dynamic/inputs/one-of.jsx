//libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import classNames from 'classnames';

//helpers
import { prefix } from '../../../../lib/text-utils';

//components
import { Button } from '../../../common/buttons';


const propTypes = {
  criterion: PropTypes.shape({
    field: PropTypes.string.isRequired,
  }).isRequired,
  prefixed: PropTypes.func.isRequired,
  value: PropTypes.arrayOf(PropTypes.any),
  changeValue: PropTypes.func,
};

const Input = ({Input: Widget}) => ({criterion, value, prefixed, changeValue}) => {
  const values = value || [null];
  const widgetPrefixed = prefixed;
  prefixed = prefix(prefixed('vertical-select'));

  return (
    <div className={classNames('fc-grid', prefixed())}>
      {values.map((item, index) => renderInputItem({
        Widget,
        criterion,
        values,
        index,
        widgetPrefixed,
        prefixed,
        changeValue,
      }))}
    </div>
  );
};
Input.propTypes = propTypes;

const renderInputItem = ({Widget, criterion, values, index, widgetPrefixed, prefixed, changeValue}) => {
  const value = values[index];

  const add = () => {
    changeValue([
      ...values,
      null,
    ]);
  };
  const change = (value) => {
    changeValue([
      ...values.slice(0, index),
      value,
      ...values.slice(index + 1),
    ]);
  };
  const remove = () => {
    changeValue([
      ...values.slice(0, index),
      ...values.slice(index + 1),
    ]);
  };

  return (
    <div className={prefixed('container')} key={index}>
      <div className={prefixed('item')}>
        {React.createElement(Widget, ({
          criterion,
          value: values[index],
          prefixed: widgetPrefixed,
          changeValue: change
        }))}
      </div>
      {renderNodeOrAdd(prefixed, index < values.length - 1, add)}
      <i className={classNames(prefixed('remove'), 'icon-close')} onClick={remove} />
    </div>
  );
};

const renderNodeOrAdd = (prefixed, isNode, add) => {
  if (isNode) {
    return <div className={prefixed('or')}>or</div>;
  }

  return <Button className={classNames(prefixed('add'), 'icon-add')} onClick={add} />;
};

const Label = ({Label: Widget}) => ({criterion, value, prefixed, changeValue}) => {
  const values = value || [null];
  const widgetPrefixed = prefixed;
  prefixed = prefix(prefixed('vertical-select'));

  return (
    <div className={classNames('fc-grid', prefixed())}>
      {values.map((item, index) => renderLabelItem({
        Widget,
        criterion,
        values,
        index,
        prefixed,
        widgetPrefixed,
        changeValue,
      }))}
    </div>
  );
};
Label.propTypes = propTypes;

const renderLabelItem = ({Widget, criterion, values, index, widgetPrefixed, prefixed}) => {
  return (
    <div className={prefixed('container')} key={index}>
      <div className={prefixed('item')}>
        {React.createElement(Widget, ({
          criterion,
          value: values[index],
          prefixed: widgetPrefixed
        }))}
      </div>
      {index < values.length - 1 ? <div className={prefixed('or')}>or</div> : null}
    </div>
  );
};

export default function(Widget) {
  return {
    Input: Input(Widget),
    Label: Label(Widget)
  };
}
