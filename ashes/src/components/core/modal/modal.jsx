/* @flow */

// libs
import noop from 'lodash/noop';
import React, { Element } from 'react';

// components
import { ModalContainer } from 'components/core/modal-container';

// styles
import s from './modal.css';

type Props = {
  isVisible: boolean,
  onCancel: () => any,
  title: string | Element<any>,
  children?: Element<any>,
}

export default ({ isVisible, onCancel = noop, title, children }: Props) => (
  <ModalContainer className={s.modal} isVisible={isVisible} onCancel={onCancel}>
    <header className={s.header}>
      <div className={s.title}>{title}</div>
      <a className={s.close} onClick={onCancel}>&times;</a>
    </header>
    <div className={s.body}>
      {children}
    </div>
  </ModalContainer>
);
