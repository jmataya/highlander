import React, { PropTypes } from 'react';
import _ from 'lodash';

import WaitAnimation from '../common/wait-animation';

const TileItems = props => {
  const { emptyMessage, isFetching, items } = props;
  let content = null;

  if (isFetching) {
    content = <WaitAnimation />;
  } else if (_.isEmpty(items)) {
    content = <div className="fc-tile-selector__empty-message">{emptyMessage}</div>;
  } else {
    content = items.map(i => <div className="fc-tile-selector__item">{i}</div>);
  }
  
  return (
    <div className="fc-tile-selector__items">
      {content}
    </div>
  );
};

TileItems.propTypes = {
  emptyMessage: PropTypes.string,
  isFetching: PropTypes.bool,
  items: PropTypes.array,
};

TileItems.defaultProps = {
  emptyMessage: 'No items found.',
  isFetching: false,
  items: [],
};

export default TileItems;
