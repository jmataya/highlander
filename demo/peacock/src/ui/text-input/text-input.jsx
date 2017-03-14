// @flow

import React, { Element, Component, PropTypes } from 'react';
import s from './text-input.css';
import classNames from 'classnames';

type Props = {
  className?: string,
  pos?: string, // could be any combination of characters l, r, b, t or one of "middle-v", "middle-h"
  error?: boolean|string,
  type?: string,
  placeholder?: string,
  label?: string|Element,
}

class TextInput extends Component {
  props: Props;

  static contextTypes = {
    error: PropTypes.string,
  };

  get contextError(): ?string {
    return this.context.error;
  }

  render() {
    const { props } = this;

    let positions = [];
    if (props.pos === 'middle-v') {
      positions = ['t', 'b'];
    } else if (props.pos === 'middle-h') {
      positions = ['r', 'l'];
    } else if (props.pos) {
      positions = props.pos.split('');
    }
    const posClassNames = positions.map(side => s[`pos-${side}`]);

    const {className, type = 'text', ...rest} = props;
    const error = props.error || this.contextError;

    const showSmallPlaceholder = !!props.value && props.placeholder;
    const showErrorText = error && typeof error == 'string';

    const inputClass = classNames(s.textInput, className, posClassNames, {
      [s.error]: !!error,
    });
    const blockClass = classNames(s.block, posClassNames, {
      [s.error]: !error,
      [s.hasTopMessages]: showSmallPlaceholder || showErrorText,
    });

    const content = props.children || <input className={inputClass} type={type} {...rest} />;

    return (
      <div className={blockClass}>
        {showSmallPlaceholder && <span className={s.placeholder}>{props.placeholder}</span>}
        {showErrorText && <span className={s.errorMessage}>{error}</span>}
        {content}
        {props.label && <span className={s.label}>{props.label}</span>}
      </div>
    );
  }
}

export default TextInput;
