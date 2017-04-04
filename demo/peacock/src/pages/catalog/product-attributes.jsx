/* @flow */

// libs
import _ from 'lodash';
import React from 'react';
import { autobind } from 'core-decorators';
import cx from 'classnames';

// styles
import styles from './pdp.css';

type State = {
  currentAdditionalTitle: string,
};

const displayAttribute = (product, attributeName) => {
  const attributeValue = _.get(product, `attributes.${attributeName}.v`);

  if (attributeValue === undefined || _.isEmpty(attributeValue)) {
    return null;
  }

  return (
    <div className="attribute-line" key={attributeName}>
      <div styleName="attribute-title">{attributeName}</div>
      <div styleName="attribute-description">
        {attributeValue}
      </div>
    </div>
  );
};

const renderAttributes = (product, attributeNames = []) => {
  return (
    <div>
      {attributeNames.map((attributeName) => displayAttribute(product, attributeName))}
    </div>
  );
};

const additionalInfoAttributesMap = [
  {
    title: 'Prep',
    attributes: ['Conventional Oven', 'Microwave', 'Pan Fry', 'Steam', 'Grill', 'Defrost'],
  },
  {
    title: 'Ingredients',
    attributes: ['Ingredients', 'Allergy Alerts', 'Allergies'],
  },
  {
    title: 'Nutrition',
    attributes: ['Nutritional Information', 'Nutrition'],
  },
];

export default class ProductAttributes extends React.Component {
  props: {
    product: any,
  };

  state: State = {
    currentAdditionalTitle: 'Prep',
  };

  @autobind
  renderAttributes() {
    const { attributes } =
      _.find(additionalInfoAttributesMap,
        attr => attr.title == this.state.currentAdditionalTitle) || {};

    return renderAttributes(this.props.product, attributes);
  }

  @autobind
  setCurrentAdditionalAttr(currentAdditionalTitle: string) {
    this.setState({ currentAdditionalTitle });
  }

  @autobind
  renderAttributesTitles() {
    return additionalInfoAttributesMap.map(({ title }) => {
      const cls = cx(styles['item-title'], {
        [styles.active]: title === this.state.currentAdditionalTitle,
      });
      const onClick = this.setCurrentAdditionalAttr.bind(this, title);

      return (
        <div className={cls} onClick={onClick} key={title}>
          {title}
        </div>
      );
    });
  }

  render() {
    return (
      <div styleName="additional-info">
        <div>
          <div styleName="items-title-wrap">
            {this.renderAttributesTitles()}
          </div>

          <div styleName="info-block">
            {this.renderAttributes()}
          </div>
        </div>
      </div>
    );
  }
}