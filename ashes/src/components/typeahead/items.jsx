// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';

// styles
import s from './typeahead.css';

const TypeaheadItems = props => {
  let innerContent = null;

  if (_.isEmpty(props.items)) {
    innerContent = <li className={s['not-found']}>No results found.</li>;
  } else {
    innerContent = props.items.map(item => (
      <li
        className={s.item}
        onMouseDown={() => props.onItemSelected(item)}
        key={`item-${item.key || item.id}`}
      >
        {React.createElement(props.component, { model: item, query: props.query })}
      </li>
    ));
  }

  return (
    <ul className={s.items}>
      {innerContent}
    </ul>
  );
};

TypeaheadItems.propTypes = {
  component: PropTypes.func.isRequired,
  updating: PropTypes.bool,
  onItemSelected: PropTypes.func,
  items: PropTypes.array,
};

TypeaheadItems.defaultProps = {
  items: [],
};

export default TypeaheadItems;
