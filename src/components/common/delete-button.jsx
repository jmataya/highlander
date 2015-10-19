'use strict';

import React from 'react';

const DeleteButton = (props) => {
  return (
    <button className='fc-btn' onClick={props.onClick}>
      <i className='icon-trash'></i>
    </button>
  );
};

export default DeleteButton;