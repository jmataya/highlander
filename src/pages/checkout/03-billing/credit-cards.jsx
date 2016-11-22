/* @flow weak */

// libs
import _ from 'lodash';
import { connect } from 'react-redux';
import React, { Component } from 'react';
import localized from 'lib/i18n';

// styles
import styles from '../checkout.css';

// components
import Loader from 'ui/loader';
import CreditCard from './credit-card';

// actions
import { fetchCreditCards } from 'modules/checkout';
import type { CreditCardType } from '../types';

type Props = {
  fetchCreditCards: Function,
  creditCards: Array<CreditCardType>,
  selectedCreditCard: Object,
  selectCreditCard: Function,
  editCard: Function,
  deleteCard: Function,
  isLoading: boolean,
  cardAdded: boolean,
  cart: Object,
};

class CreditCards extends Component {
  props: Props;

  componentWillMount() {
    this.props.fetchCreditCards()
      .then(() => {
        const { creditCards, selectCreditCard, cardAdded, cart } = this.props;
        const paymentMethods = _.get(cart, 'paymentMethods', []);
        const chosenCreditCard = _.find(paymentMethods, {type: 'creditCard'});

        if (creditCards.length === 1 || cardAdded) {
          selectCreditCard(creditCards[0]);
        } else if (chosenCreditCard) {
          selectCreditCard(chosenCreditCard);
        } else {
          const defaultCards = _.filter(creditCards, { isDefault: true });
          if (defaultCards.length) {
            selectCreditCard(defaultCards[0]);
          }
        }
      });
  }

  get creditCards() {
    const { creditCards, selectedCreditCard, selectCreditCard, editCard, deleteCard } = this.props;

    return creditCards.map(creditCard => {
      const selected = !!selectedCreditCard && selectedCreditCard.id === creditCard.id;

      return (
        <CreditCard
          creditCard={creditCard}
          selected={selected}
          onSelect={selectCreditCard}
          key={creditCard.id}
          editCard={editCard}
          deleteCard={deleteCard}
        />
      );
    });
  }

  render() {
    const { isLoading, creditCards } = this.props;

    if (isLoading) return <Loader size="m" />;

    if (_.isEmpty(creditCards)) return null;

    return (
      <div>
        {this.creditCards}
      </div>
    );
  }
}

function mapStateToProps(state) {
  return {
    isLoading: _.get(state.asyncActions, ['creditCards', 'inProgress'], true),
    creditCards: state.checkout.creditCards,
    selectedCreditCard: _.get(state.cart, 'creditCard', {}),
    cart: state.cart,
  };
}

export default connect(mapStateToProps, { fetchCreditCards })(localized(CreditCards));
