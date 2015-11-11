import React from 'react';
import AddressDetails from './address-details';
import { DefaultButton, AddButton } from '../common/buttons';
import classnames from 'classnames';
import { autobind } from 'core-decorators';

export default class AddressSelect extends React.Component {

  constructor(...args) {
    super(...args);
    this.state = {
      value: this.props.value
    };
  }

  onItemSelect(value) {
    this.setState({
      value: value
    }, () => {
      if (this.props.onItemSelect) {
        this.props.onItemSelect(value);
      }
    });
  }

  componentDidUpdate() {
    console.log(this.state);
  }

  @autobind
  renderSelectItem(address) {
    const isSelected = address.id === this.state.value;
    const itemClassName = classnames(
      "fc-address-select-item",
      { "fc-address-select-item-active": isSelected }
    );
    const key = `cutomer-address-${ address.id }`;
    return (
      <div className={ itemClassName }
           key={ key }>
        <div className="fc-address-select-item-info">
          <AddressDetails address={ address }
                          customerId={ this.props.customerId }/>
        </div>
        <div className="fc-address-select-item-controlls">
          <DefaultButton type="button" onClick={ this.onItemSelect.bind(this, address.id) } >Choose</DefaultButton>
        </div>
      </div>
    );
  }

  render() {
    const rootClassName = classnames("fc-address-select", this.props.className);
    return (
      <div className={ rootClassName }>
        <div className="fc-address-select-header">
          <div className="fc-left">
            Address Book
          </div>
          <div className="fc-right">
            <AddButton />
          </div>
        </div>
        <div className="fc-address-select-body">
          <input type="hidden" name={ this.props.name } value={ this.state.value } />
          <div className="gc-address-select-list">
            {(this.props.items && this.props.items.map( this.renderSelectItem ))}
          </div>
        </div>
      </div>
    );
  }

}
