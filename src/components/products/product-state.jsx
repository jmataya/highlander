/**
 * @flow
 */

import React, { Component, Element, PropTypes } from 'react';
import { autobind } from 'core-decorators';
import moment from 'moment';
import _ from 'lodash';

import { illuminateAttributes, setAttribute } from '../../paragons/form-shadow-object';

import { Dropdown, DropdownItem } from '../dropdown';
import DateTimePicker from '../date-time-picker/date-time-picker';
import TextInput from '../forms/text-input';

type Props = {
  form: FormAttributes,
  shadow: ShadowAttributes,
  onChange: (form: FormAttributes, shadow: ShadowAttributes) => void,
};

type State = {
  showActiveFromPicker: bool,
  showActiveToPicker: bool,
};

export default class ProductState extends Component<void, Props, State> {
  static propTypes = {
    form: PropTypes.object.isRequired,
    shadow: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  state: State = { showActiveFromPicker: false, showActiveToPicker: false };

  get illuminatedAttributes(): IlluminatedAttributes {
    return illuminateAttributes(this.props.form, this.props.shadow);
  }

  get activeFrom(): ?string {
    const activeFrom = this.illuminatedAttributes.activeFrom;
    if (activeFrom) {
      return activeFrom.value;
    }
  }

  get activeTo(): ?string {
    const activeTo = this.illuminatedAttributes.activeTo;
    if (activeTo) {
      return activeTo.value;
    }
  }

  get activeFromPicker(): ?Element {
    if (this.state.showActiveFromPicker) {
      return (
        <div className="fc-product-state__picker">
          <div className="fc-product-state__picker-header">
            Product will be active on:
          </div>
          <div className="fc-product-state__picker-label">
            Start
          </div>
          <DateTimePicker
            dateTime={this.activeFrom}
            onSetDate={(v) => console.log(v)}
            onCancel={this.handleCancelFrom} />
        </div>
      );
    }
  }

  get activeToPicker(): ?Element {
    if (this.state.showActiveFromPicker) {
      const picker = this.state.showActiveToPicker
        ? (
          <DateTimePicker
            dateTime={this.activeTo}
            onSetDate={(v) => console.log(v)}
            onCancel={this.handleCancelTo} />
        )
        : <a onClick={this.handleShowActiveTo}><i className="icon-add" /></a>;

      return (
        <div className="fc-product-state__picker _end">
          <div className="fc-product-state__picker-label">
            End
          </div>
          {picker}
        </div>
      );
    }
  }

  get isActive(): bool {
    const now = moment();

    if (!this.activeFrom) {
      return false;
    } else if (now.diff(this.activeFrom) > 0) {
      return false;
    } else if (this.activeTo && now.diff(this.activeTo) < 0) {
      return false;
    }

    return true;
  }

  @autobind
  handleChange(label: string, value: ?string) {
    const { form, shadow } = this.props;
    const [newForm, newShadow] = setAttribute(label, 'datetime', value, form, shadow);
    this.props.onChange(newForm, newShadow);
  }

  @autobind
  handleActiveChange(value: string) {
    const now = moment().utc().format('YYYY-MM-DDTHH:mm:ss.SSS');
    const activeFrom = value == 'active' ? now : null;
    this.handleChange('activeFrom', activeFrom);
  }

  @autobind
  handleCancelFrom() {
    this.setState({
      showActiveFromPicker: false,
      showActiveToPicker: false,
    });
  }

  @autobind
  handleCancelTo() {
    this.setState({
      showActiveToPicker: false,
    });
  }

  @autobind
  handleShowActiveTo() {
    this.setState({
      showActiveFromPicker: true,
      showActiveToPicker: true,
    });
  }


  get activeDropdown(): Element {
    const activeState = this.isActive ? 'active' : 'inactive';
    const isDisabled = this.state.showActiveFromPicker;
    return (
      <Dropdown
        className="fc-product-state__active-state"
        disabled={isDisabled}
        value={activeState}
        onChange={this.handleActiveChange}>
        <DropdownItem value="active">Active</DropdownItem>
        <DropdownItem value="inactive">Inactive</DropdownItem>
      </Dropdown>
    );
  }

  @autobind
  handleClickCalendar() {
    this.setState({
      showActiveFromPicker: true,
      showActiveToPicker: false,
    });
  }

  @autobind
  handleClickCloseFrom() {
    this.setState({
      showActiveFromPicker: false,
      showActiveToPicker: false,
    });
  }

  render(): Element {
    return (
      <div className="fc-product-state">
        <div className="fc-product-state__header">
          <div className="fc-product-state__text">
            State
          </div>
          <div className="fc-product-state__icon">
            <a onClick={this.handleClickCalendar}><i className="icon-calendar" /></a>
          </div>
        </div>
        {this.activeDropdown}
        {this.activeFromPicker}
        {this.activeToPicker}
      </div>
    );
  }
}
