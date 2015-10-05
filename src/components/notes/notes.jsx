'use strict';

import React from 'react';
import Api from '../../lib/api';
import Wrapper from '../wrapper/wrapper';
import Panel from '../panel/panel';
import TableView from '../table/tableview';
import TableRow from '../table/row';
import TableCell from '../table/cell';
import Controls from './controls';
import Form from './form';
import NoteStore from './store';
import UserInitials from '../users/initials';
import { pluralize } from 'fleck';

export default class Notes extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      notes: [],
      creating: false,
      editing: false,
      editingNote: null
    };
  }

  componentDidMount() {
    let model = this.props.modelName;
    if (model === 'order') {
      NoteStore.uriRoot = `${pluralize(model)}/${this.props[model].referenceNumber}`;
    } else if (model === 'gift-card') {
      NoteStore.uriRoot = `${pluralize(model)}/${this.props[model].code}`;
    } else {
      NoteStore.uriRoot = `${pluralize(model)}/${this.props[model].id}`;
    }

    NoteStore.listenToEvent('change', this);
    NoteStore.fetch();
  }

  componentWillUnmount() {
    NoteStore.stopListeningToEvent('change', this);
  }

  onChangeNoteStore(notes) {
    this.setState({notes: notes});
  }

  handleEdit(item) {
    this.setState({
      creating: false,
      editing: true,
      editingNote: item
    });
  }

  handleDelete(item) {
    NoteStore.delete(item.id);
  }

  handleResetForm() {
    this.setState({
      creating: false,
      editing: false,
      editingNote: null
    });
  }

  handleSubmit(event) {
    event.preventDefault();
    Api.submitForm(event.target)
      .then((note) => {
        note.isNew = true;
        let notes = this.state.notes.slice(0);
        notes.unshift(note);
        this.setState({notes: notes});
        this.toggleCreating();
        this.removeNew();
      })
      .catch((err) => {
        console.error(err);
      });
  }

  removeNew() {
    setTimeout(() => {
      let row = document.querySelector('tr.new');
      row.classList.remove('new');
    }, 5000);
  }

  toggleCreating() {
    this.setState({
      creating: !this.state.creating,
      editing: this.state.editing && this.state.creating
    });
  }

  render() {
    let renderRow = (row) => {
      return (
        <TableRow>
          <TableCell>Today</TableCell>
          <TableCell>{row.body}</TableCell>
          <TableCell>
            <Controls
              model={row}
              onEditClick={this.handleEdit.bind(this)}
              onDeleteClick={this.handleDelete.bind(this)}
              />
          </TableCell>
          {this.state.editing && (this.state.editingNote.id === row.id) && (
            <Form
              uri={NoteStore.baseUri}
              text={this.state.editingNote && this.state.editingNote.body}
              onReset={this.handleResetForm.bind(this)}
              onSubmit={this.handleSubmit.bind(this)}
              />
          )}
        </TableRow>
      );
    };

    let controls = (
      <Wrapper>
        <button onClick={this.toggleCreating.bind(this)} disabled={!!this.state.creating}>
          <i className="icon-add"></i>
        </button>
      </Wrapper>
    );

    let test = 'It is a long established fact that a reader will be distracted by the readable content of a page when looking at its layout. The point of using Lorem Ipsum is that it has a more-or-less normal distribution of letters, as opposed to using Content here, content here, making it look like readable English. Many desktop publishing packages and web page editors now use Lorem Ipsum as their default model text, and a search for lorem ipsum will uncover many web sites still in their infancy. Various versions have evolved over the years, sometimes by accident, sometimes on purpose (injected humour and the like).';

    return (
      <Panel title={test} controls={controls}>
        {this.state.creating && (
          <Form
            uri={NoteStore.baseUri}
            onReset={this.handleResetForm.bind(this)}
            onSubmit={this.handleSubmit.bind(this)}
            />
        )}
        {this.state.notes.length && (
          <TableView store={NoteStore} renderRow={renderRow}/>
        )}
        {!this.state.notes.length && (
          <div className="empty">No notes yet.</div>
        )}
      </Panel>
    );
  }
}

Notes.propTypes = {
  tableColumns: React.PropTypes.array,
  order: React.PropTypes.object,
  modelName: React.PropTypes.string
};
