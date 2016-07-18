/**
 * @flow weak
 */

// libs
import _ from 'lodash';
import React, {PropTypes} from 'react';
import {autobind} from 'core-decorators';
import { DragDropContext } from 'react-dnd';
import HTML5Backend from 'react-dnd-html5-backend';
import localStorage from 'localStorage';

// components
import { PrimaryButton } from '../common/buttons';
import Overlay from '../overlay/overlay';
import SelectorItem from './column-selector-item';

//styles
import styles from './column-selector.css';

type Props = {
  columns: Array<Object>,
  onChange: Function,
  setColumns: Function,
  identifier: string,
  toggleColumnSelector: Function,
}

type State = {
  selectedColumns: Array<Object>,
  isSelectorVisible: boolean,
  hasDraggingItem: boolean,
}

/*::`*/ @DragDropContext(HTML5Backend) /*::`;*/
export default class ColumnSelector extends React.Component {
  props: Props;

  state: State = {
    selectedColumns: this.getSelectedColumns(),
    isSelectorVisible: false,
    hasDraggingItem: false,
  };

  @autobind
  moveItem(dragIndex: number, hoverIndex: number) {
    let { selectedColumns } = this.state;
    const dragItem = selectedColumns[dragIndex];

    selectedColumns.splice(dragIndex, 1);
    selectedColumns.splice(hoverIndex, 0, dragItem);

    this.setState({
      selectedColumns: selectedColumns,
      hasDraggingItem: true
    });
  }

  @autobind
  dropItem() {
    this.setState({
      hasDraggingItem: false
    });
  }

  getSelectedColumns() {
    let columns = localStorage.getItem('columns');

    if (columns) {
      columns = JSON.parse(columns);
      if (columns[this.props.identifier]) return columns[this.props.identifier];
    }

    return this.props.columns.map((column, i) => {
      return _.assign(column, {
        isVisible: true,
        id: i,
      });
    });
  }

  toggleColumnSelection(id: any) {
    let selectedColumns = this.state.selectedColumns;

    selectedColumns[id].isVisible = !selectedColumns[id].isVisible;

    this.setState({
      selectedColumns: selectedColumns,
    });
  }

  @autobind
  saveColumns() {
    const tableName = this.props.identifier;
    const columnState = this.state.selectedColumns;

    // update table data
    const filteredColumns = _.filter(columnState, {isVisible: true});
    this.props.setColumns(filteredColumns);

    // save to storage
    let columns = localStorage.getItem('columns') ? JSON.parse(localStorage.getItem('columns')) : {};
    columns[tableName] = columnState;
    localStorage.setItem('columns', JSON.stringify(columns));

    // close dropdown
    this.toggleColumnSelector();
  }

  @autobind
  toggleColumnSelector() {
    this.setState({
      isSelectorVisible: !this.state.isSelectorVisible,
    });
  }

  renderSelectorItems() {
    return this.state.selectedColumns.map((item, id) => {
      const checked = item.isVisible;

      return (
        <SelectorItem key={item.id}
            index={id}
            id={item.id}
            text={item.text}
            moveItem={this.moveItem}
            dropItem={this.dropItem}
            checked={checked}
            onChange={e => this.toggleColumnSelection(id)} />
      );
    });
  }

  renderDropDown() {
    const listClassName = this.state.hasDraggingItem ? '_hasDraggingItem' : '';

    return (
      <div styleName="dropdown">
        <ul styleName="list" className={listClassName}>
          {this.renderSelectorItems()}
        </ul>
        <div styleName="actions">
          <PrimaryButton onClick={this.saveColumns}>
            Save
          </PrimaryButton>
        </div>
      </div>
    );
  }

  render() {
    const overlay = <Overlay shown={true} onClick={this.toggleColumnSelector}/>;

    return (
      <div styleName="column-selector">
        <i className="icon-settings-col" onClick={this.toggleColumnSelector}/>
        {this.state.isSelectorVisible && overlay }
        {this.state.isSelectorVisible && this.renderDropDown() }
      </div>
    );
  }
}
