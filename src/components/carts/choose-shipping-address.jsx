import React, { Component, PropTypes } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import _ from 'lodash';

import * as AddressActions from 'modules/customers/addresses';
import * as CartActions from 'modules/carts/details';

import AddressBox from 'components/addresses/address-box';
import AddressForm from 'components/addresses/address-form/modal';
import ConfirmationDialog from 'components/modal/confirmation-dialog';
import TileSelector from 'components/tile-selector/tile-selector';

function mapStateToProps(state, props) {
  const customerId = _.get(props, 'cart.customer.id');
  return {
    addressState: state.customers.addresses[customerId],
    // shippingAddressState: state.carts.shippingAddresses,
  };
}

function mapDispatchToProps(dispatch, props) {
  const customerId = _.get(props, 'cart.customer.id');
  const refNum = _.get(props, 'cart.referenceNumber');

  const addressActions = _.transform(AddressActions, (res, action, key) => {
    res[key] = (...args) => dispatch(action(customerId, ...args));
  });

  return {
    actions: {
      ...addressActions,
      ...CartActions,
    },
  };
}

@connect(mapStateToProps, mapDispatchToProps)
export default class ChooseShippingAddress extends Component {
  static propTypes = {
    selectedAddress: PropTypes.object,

    actions: PropTypes.shape({
      chooseAddress: PropTypes.func.isRequired,
      createShippingAddress: PropTypes.func.isRequired,
      deleteAddress: PropTypes.func.isRequired,
      deleteShippingAddress: PropTypes.func.isRequired,
      fetchAddresses: PropTypes.func.isRequired,
      patchShippingAddress: PropTypes.func.isRequired,
      setAddressDefault: PropTypes.func.isRequired,
      patchAddress: PropTypes.func.isRequired,
    }).isRequired,

    addressState: PropTypes.shape({
      addresses: PropTypes.array,
      isFetching: PropTypes.bool,
    }).isRequired,
  };

  static defaultProps = {
    addressState: {},
  };

  constructor(...args) {
    super(...args);

    this.state = {
      address: null,
      isDeleteDialogVisible: false,
      isFormVisible: false,
      isSelectedShippingAddress: false,
    };
  }

  componentDidMount() {
    this.props.actions.fetchAddresses();
  }

  componentWillReceiveProps(nextProps) {
    // if (this.props.shippingAddressState.updateNum != nextProps.shippingAddressState.updateNum ||
    //     !Object.is(this.props.addressState.addresses, nextProps.addressState.addresses)) {
    //   this.setState({
    //     address: null,
    //     isDeleteDialogVisible: false,
    //     isFormVisible: false,
    //     isShippingAddress: false,
    //   });
    // }
  }

  get addresses() {
    return _.get(this.props, 'addressState.addresses', []);
  }

  get customerId() {
    return _.get(this.props, 'cart.customer.id');
  }

  get isFetching() {
    return _.get(this.props, 'addressState.isFetching', false);
  }

  get renderedAddressBoxes() {
    const selectedAddressId = _.get(this.props, 'selectedAddress.id');
    return this.addresses.map(a => {
      return (
        <AddressBox
          address={a}
          chooseAction={() => this.handleChooseAddress(a)}
          editAction={() => this.handleStartEditAddress(a)}
          deleteAction={() => this.handleStartDeleteAddress(a)}
          toggleDefaultAction={() => this.handleSetAddressDefault(a)} />
      );
    });
  }

  get renderAddressForm() {
    const saveTitle = _.isNull(this.state.address) ? 'Save and Choose' : 'Save';

    return (
      <AddressForm
        isVisible={this.state.isFormVisible}
        address={this.state.address}
        submitAction={this.handleFormSubmit}
        closeAction={this.handleCloseAddressForm}
        customerId={this.customerId}
        saveTitle={saveTitle} />
    );
  }

  get renderSelectedAddress() {
    if (this.props.selectedAddress) {
      return (
        <div>
          <h3 className="fc-shipping-address-sub-title">
            Chosen Address
          </h3>
          <ul className="fc-addresses-list">
            <AddressBox
              address={this.props.selectedAddress}
              chosen={true}
              checkboxLabel={null}
              editAction={this.handleStartEditShippingAddress}
              deleteAction={this.handleStartDeleteShippingAddress}
              actionBlock={null} />
          </ul>
        </div>
      );
    }
  }

  get renderDeleteDialog() {
    const text = this.state.isShippingAddress
      ? 'Are you sure you want to remove shipping address from order?'
      : 'Are you sure you want to delete address?';

    const deleteAction = this.state.isShippingAddress
      ? () => this.props.actions.deleteShippingAddress()
      : () => this.props.actions.deleteAddress(this.state.address.id);

    return (
      <ConfirmationDialog
        isVisible={this.state.isDeleteDialogVisible}
        header='Confirm'
        body={text}
        cancel='Cancel'
        confirm='Yes, Delete'
        cancelAction={this.handleStopDeletingAddress}
        confirmAction={deleteAction} />
    );
  }

  @autobind
  handleChooseAddress(address) {
    const refNum = this.props.cart.referenceNumber;
    this.props.actions.chooseAddress(refNum, address.id);
  }

  @autobind
  handleAddNewAddress() {
    this.setState({
      address: {},
      isFormVisible: true,
    });
  }

  @autobind
  handleCloseAddressForm() {
    this.setState({
      address: {},
      isFormVisible: false,
    });
  }

  @autobind
  handleStartDeleteAddress(address) {
    this.setState({
      address: address,
      isDeleteDialogVisible: true,
      isFormVisible: false,
      isShippingAddress: false,
    });
  }

  @autobind
  handleStartDeleteShippingAddress() {
    this.setState({
      isDeleteDialogVisible: true,
      isFormVisible: false,
      isShippingAddress: true,
    });
  }

  @autobind
  handleStopDeletingAddress() {
    this.setState({
      address: {},
      isDeleteDialogVisible: false,
    });
  }

  @autobind
  handleStartEditAddress(address) {
    this.setState({
      address: address,
      isFormVisible: true,
    });
  }

  @autobind
  handleStartEditShippingAddress() {
    this.setState({
      address: this.props.selectedAddress,
      isFormVisible: true,
      isShippingAddress: true,
    });
  }

  @autobind
  handleFormSubmit(address) {
    const isEdit = address.id;
    const { referenceNumber } = this.props.cart;

    if (!isEdit) {
      this.props.actions.createShippingAddress(address)
        .then(this.props.actions.fetchAddresses);
    } else if (this.state.isShippingAddress) {
      this.props.actions.patchShippingAddress(referenceNumber, address);
    } else {
      this.props.actions.patchAddress(referenceNumber, address.id, address);
    }
  }

  @autobind
  handleSetAddressDefault(address) {
    this.props.actions.setAddressDefault(address.id, !address.isDefault);
  }

  render() {
    return (
      <div>
        {this.renderSelectedAddress}
        <TileSelector
          onAddClick={this.handleAddNewAddress}
          emptyMessage="Customer's address book is empty."
          isFetching={this.isFetching}
          items={this.renderedAddressBoxes}
          title="Address Book" />
        {this.renderAddressForm}
        {this.renderDeleteDialog}
      </div>
    );
  }
}
