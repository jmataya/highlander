// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';

// data
import { stateTitles } from '../../paragons/gift-card';
import { actions } from '../../modules/gift-cards/list';
import { actions as bulkActions } from '../../modules/gift-cards/bulk';

// components
import BulkActions from '../bulk-actions/bulk-actions';
import BulkMessages from '../bulk-actions/bulk-messages';
import GiftCardRow from './gift-card-row';
import { ChangeStateModal, CancelModal } from '../bulk-actions/modal';
import { SelectableSearchList } from '../list-page';
import { Link } from '../link';


const mapStateToProps = state => ({list: state.giftCards.list});

const mapDispatchToProps = dispatch => {
  return {
    actions: bindActionCreators(actions, dispatch),
    bulkActions: bindActionCreators(bulkActions, dispatch),
  };
};

@connect(mapStateToProps, mapDispatchToProps)
export default class GiftCards extends React.Component {
  static propTypes = {
    list: PropTypes.object.isRequired,
    actions: PropTypes.objectOf(PropTypes.func).isRequired,
    bulkActions: PropTypes.objectOf(PropTypes.func).isRequired,
  };

  static tableColumns = [
    {field: 'code', text: 'Gift Card Number', model: 'giftcard'},
    {field: 'originType', text: 'Type', model: 'giftCard'},
    {field: 'originalBalance', text: 'Original Balance', type: 'currency'},
    {field: 'currentBalance', text: 'Current Balance', type: 'currency'},
    {field: 'availableBalance', text: 'Available Balance', type: 'currency'},
    {field: 'state', text: 'State', type: 'state', model: 'giftCard'},
    {field: 'createdAt', text: 'Date/Time Issued', type: 'date'}
  ];

  @autobind
  cancelGiftCards(allChecked, toggledIds) {
    const {cancelGiftCards} = this.props.bulkActions;

    return (
      <CancelModal
        count={toggledIds.length}
        onConfirm={(reasonId) => cancelGiftCards(toggledIds, reasonId)} />
    );
  }

  getChangeGiftCardsState(state) {
    const stateTitle = stateTitles[state];

    return (allChecked, toggledIds) => {
      const {changeGiftCardsState} = this.props.bulkActions;

      return (
        <ChangeStateModal
          count={toggledIds.length}
          stateTitle={stateTitle}
          onConfirm={() => changeGiftCardsState(toggledIds, state)} />
      );
    };
  }

  getChangeGiftCardsStateAction(state) {
    const stateTitle = stateTitles[state];

    return [
      `Change Gift Cards state to ${stateTitle}`,
      this.getChangeGiftCardsState(state),
      `successfully changed state to ${stateTitle}`,
      `could not change state to ${stateTitle}`,
    ];
  }

  get bulkActions() {
    return [
      ['Cancel Gift Cards', this.cancelGiftCards, 'successfully canceled', 'could not be canceled'],
      this.getChangeGiftCardsStateAction('active'),
      this.getChangeGiftCardsStateAction('onHold'),
    ];
  }

  renderDetail(messages, code) {
    return (
      <span key={code}>
        Gift card <Link to="giftcard" params={{giftCard: code}}>{code}</Link>
      </span>
    );
  }

  renderRow(row, index, columns, params) {
    const key = `gift-card-${row.code}`;
    return (
      <GiftCardRow key={key}
                   giftCard={row}
                   columns={columns}
                   params={params} />
    );
  }

  render() {
    const {list, actions} = this.props;

    return (
      <div>
        <BulkMessages
          storePath="giftCards.bulk"
          module="giftCards"
          entity="gift card"
          renderDetail={this.renderDetail} />
        <BulkActions
          module="giftCards"
          entity="gift card"
          actions={this.bulkActions}>
          <SelectableSearchList
            emptyMessage="No gift cards found."
            list={list}
            renderRow={this.renderRow}
            tableColumns={GiftCards.tableColumns}
            searchActions={actions}
            predicate={({code}) => code} />
        </BulkActions>
      </div>
    );
  }
}
