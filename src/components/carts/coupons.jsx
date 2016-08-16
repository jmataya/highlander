/* @flow */

import React, { Component, Element } from 'react';
import _ from 'lodash';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { trackEvent } from 'lib/analytics';

import CouponsPanel from 'components/coupons-panel/coupons-panel';
import EditableContentBox from 'components/content-box/editable-content-box';
import PanelHeader from 'components/panel-header/panel-header';
import CouponRow from 'components/coupons-panel/coupon-row';
import TableView from '../table/tableview';
import { Button, EditButton } from '../common/buttons';
import AppendInput from '../forms/append-input';

import * as CouponActions from 'modules/carts/coupons';

import styles from 'components/coupons-panel/coupons-panel.css';

import type { CouponModuleProps, CouponModuleActions } from 'modules/carts/coupons';

type Target = {
  name: string,
  value: string|number|boolean,
};

type Props = CouponModuleActions & {
  readOnly: bool,
  coupons: CouponModuleProps,
  cart: {
    coupon: Object,
    referenceNumber: string,
  }
};

const viewColumns = [
  {field: 'name', text: 'Name'},
  {field: 'storefrontName', text: 'Storefront Name'},
  {field: 'code', text: 'Code'},
];

const editColumns = viewColumns.concat([
  {field: 'edit', text: ''},
]);

const bindStateToProps = (state) => ({
  coupons: state.carts.coupons,
});

class CartCoupons extends Component {
  props: Props;

  static defaultProps = {
    readOnly: false,
  };

  get isEditing(): bool {
    return this.props.coupons.isEditing;
  }

  get orderReferenceNumber(): string {
    return this.props.cart.referenceNumber;
  }

  get title(): Element {
    return (
      <PanelHeader isOptional={true} text="Coupons" />
    );
  }

  get coupons(): Array<Object> {
    const coupon = _.get(this.props, 'cart.coupon');

    if (!coupon) return [];

    return [coupon];
  }

  get editFooter(): Element {
    const plate = (
      <Button styleName="add-coupon-button" onClick={this.onAddClick}>Apply</Button>
    );
    const errorMessage = this.props.coupons.error && (
      <div className="fc-form-field-error">{this.fancyErrorMessage}</div>
    );
    return (
      <div styleName="add-coupon-block">
        <div styleName="add-coupon-label">
          <strong>Add Coupon</strong>
        </div>
        <AppendInput
          styleName="add-coupon-input-container"
          inputName="couponCode"
          value={this.props.coupons.code}
          inputClass={styles['add-coupon-input']}
          plate={plate}
          placeholder="Enter coupon code"
          onChange={this.orderCouponCodeChange}
        />
        {errorMessage}
      </div>
    );
  }

  get fancyErrorMessage(): ?string {
    const errorMessage = _.get(this.props, 'coupons.error');
    if (errorMessage && errorMessage.indexOf('not found') >= 0) {
      return 'This coupon code does not exist.';
    }
    if (errorMessage && errorMessage.indexOf('rejected') >= 0) {
      return 'Your order does not qualify for this coupon.';
    }
    if (errorMessage && errorMessage.indexOf('inactive') >= 0) {
      return 'This coupon code is inactive.';
    }
    return errorMessage;
  }

  @autobind
  onAddClick(): void {
    this.props.addCoupon(this.orderReferenceNumber);
  }

  @autobind
  orderCouponCodeChange(event: {target: Target}): void {
    this.props.orderCouponCodeChange(event.target.value);
  }

  renderRow(isEditing: bool): Function {
    const columns = isEditing ? editColumns : viewColumns;
    const renderFn = (row: Object, index: number, isNew: boolean) => {
      return (
        <CouponRow
          key={`order-coupon-row-${row.id}`}
          item={row}
          columns={columns}
          onDelete={() => this.props.removeCoupon(this.orderReferenceNumber)}
        />
      );
    };
    return renderFn;
  }

  @autobind
  handleEditAction() {
    trackEvent('Orders', 'edit_order_coupons');
    this.props.orderCouponsStartEdit();
  }

  @autobind
  handleDoneAction() {
    trackEvent('Orders', 'edit_order_coupons_done');
    this.props.orderCouponsStopEdit();
  }

  render(): Element {
    const content = <CouponsPanel coupons={this.coupons} />;

    return (
      <EditableContentBox
        title={this.title}
        indentContent={false}
        viewContent={content}
        editContent={content}
        editFooter={this.editFooter}
        isEditing={this.isEditing}
        editAction={this.handleEditAction}
        doneAction={this.handleDoneAction}
      />
    );
  }
}

export default connect(bindStateToProps, CouponActions)(CartCoupons);
