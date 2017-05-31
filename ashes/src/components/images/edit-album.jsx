/* @flow */

// libs
import _ from 'lodash';
import { autobind } from 'core-decorators';
import React, { Component } from 'react';

// components
import { FormField } from '../forms';
import ContentBox from '../content-box/content-box';
import SaveCancel from 'components/core/save-cancel';
import wrapModal from '../modal/wrapper';
import TextInput from 'components/core/text-input';

// types
import type { NewAlbum } from '../../modules/images';

type Props = {
  isVisible: boolean;
  isNew?: boolean;
  loading: boolean;
  album: NewAlbum;
  onSave: (name: string) => void;
  onCancel: () => void;
};

type State = {
  name: string;
};

class EditAlbum extends Component {

  props: Props;

  state: State = {
    name: this.props.album.name,
  };

  static defaultProps = {
    isNew: false,
  };

  _input: HTMLInputElement;

  get closeAction() {
    return <a onClick={this.props.onCancel}>&times;</a>;
  }

  get saveDisabled(): boolean {
    return _.isEmpty(this.state.name) || this.state.name === this.props.album.name;
  }

  @autobind
  handleUpdateField(value, name) {
    this.setState({ [name]: value });
  }

  @autobind
  handleSave(event: Event) {
    event.preventDefault();
    this.props.onSave(this.state.name);
  }

  @autobind
  handleKeyPress(event) {
    if (!this.saveDisabled && event.keyCode === 13 /*enter*/) {
      event.preventDefault();
      this.props.onSave(this.state.name);
    }
  }

  render() {
    const title = this.props.isNew ? 'Add New Album' : 'Edit Album';

    return (
        <div onKeyDown={this.handleKeyPress}>
          <ContentBox title={title} actionBlock={this.closeAction}>
            <FormField label="Album Name"
                       className="fc-product-details__field"
                       labelClassName="fc-product-details__field-label">
              <TextInput
                     name="name"
                     value={this.state.name}
                     onChange={this.handleUpdateField}
                     autoFocus />
            </FormField>
            <SaveCancel onCancel={this.props.onCancel}
                        onSave={this.handleSave}
                        saveDisabled={this.saveDisabled}
                        isLoading={this.props.loading}
                        saveText="Save and Apply"/>
          </ContentBox>
        </div>
    );
  }
}

const Wrapped: Class<Component<void, Props, State>> = wrapModal(EditAlbum);

export default Wrapped;
