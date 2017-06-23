/* @flow */

// libs
import _ from 'lodash';
import React, { Element, Component } from 'react';
import ReactDOM from 'react-dom';
import { autobind, debounce } from 'core-decorators';
import classNames from 'classnames';

import Icon from 'components/core/icon';
import { SmartList } from 'components/core/dropdown';
import TextInput from 'components/core/text-input';
import Spinner from 'components/core/spinner';

// styles
import s from './search-dropdown.css';

type InternalItem = {
  value: string,
  displayText?: string,
};

type Item = [string, string];

type Props = {
  /** An array of initial values which will be in a list */
  // $FlowFixMe
  items: Array<Item | InternalItem | string>,
  /** Input name which will be used by form */
  name: string, // input name
  /** Input value */
  value: string | number | null, // input value
  /** Initial display text to be shown (if no renderItem defined) */
  displayText: string,
  /** Text which is visible when no value */
  placeholder: string,
  /** Placeholder for search input */
  searchbarPlaceholder: string,
  /** Additional root className */
  className?: string,
  /** If true, you cant open dropdown or change its value from UI */
  disabled: boolean,
  /** If true, the component can change its value only via props */
  stateless: boolean,
  /** Callback which fires when the value has been changes */
  onChange: Function,
  /** Callback for token change (fetching new results for the list) */
  fetch: (token: string) => Promise<any>,
  /** For custom layout of each item */
  renderItem?: Function,
};

type State = {
  open: boolean, // show or hide the menu
  selectedValue: string, // current selected value of menu
  displayText: string,
  items: Array<InternalItem>,
  isLoading: boolean,
};

/**
 * Text Dropdown component.
 * This component is about to render simple text list and current value.
 * This component is not responsible for different skins, TextInputs, infinite lists, or any other complex stuff.
 * If you need any functionality which is not exists here, try different Dropdown or build new one.
 */
export default class SearchDropdown extends Component {
  props: Props;

  static defaultProps = {
    name: '',
    value: '',
    displayText: '',
    placeholder: '- Select -',
    searchbarPlaceholder: 'Start to type...',
    disabled: false,
    onChange: () => {},
    stateless: false,
    items: [],
  };

  state: State = {
    open: false,
    selectedValue: this.getValue(this.props.value),
    displayText: this.props.displayText,
    items: this.unifyItems(this.props.items),
    isLoading: false,
  };

  _pivot: HTMLElement;
  _input: Element;

  componentDidUpdate(prevProps, prevState) {
    const input = ReactDOM.findDOMNode(this._input);

    if (this.state.open && !prevState.open && input) {
      input.focus();
    }
  }

  getValue(value: any) {
    return value ? String(value) : '';
  }

  @autobind
  handleToggleClick(event: any) {
    event.preventDefault();

    if (this.props.disabled) {
      return;
    }

    this.toggleMenu();
  }

  handleItemClick(item: InternalItem) {
    const nextState = {
      open: false,
      selectedValue: item.value,
      displayText: item.displayText || item.value,
    };

    if (item.value !== this.state.selectedValue) {
      this.props.onChange(item.value);
      this.setState(nextState);
    }
  }

  toggleMenu(nextOpen: ?boolean) {
    const open = nextOpen != null ? nextOpen : !this.state.open;

    this.setState({ open });
  }

  unifyItems(dirtyItems): Array<InternalItem> {
    if (Array.isArray(dirtyItems[0])) {
      return dirtyItems.map(([value, displayText]) => ({ value, displayText }));
    } else if (typeof dirtyItems[0] === 'string') {
      return dirtyItems.map((value: string) => ({ value, displayText: value }));
    } else if (!dirtyItems) {
      return [];
    }

    return dirtyItems;
  }

  @debounce(400)
  fetch(token: string) {
    this.props
      .fetch(token)
      .then(data => {
        if (data.token === token) {
          this.setState({ items: this.unifyItems(data.items), isLoading: false });
        }
      })
      .catch(() => this.setState({ isLoading: false }));
  }

  onTokenChange(token: string) {
    this.setState({ token });

    if (!token) {
      this.setState({ items: [], isLoading: false });
    } else {
      this.setState({ isLoading: true });
      this.fetch(token);
    }
  }

  renderSearchBar() {
    return (
      <div className={s.searchBar}>
        <Icon name="search" className={s.loupeIcon} />
        <TextInput
          ref={i => (this._input = i)}
          placeholder={this.props.searchbarPlaceholder}
          className={s.searchBarInput}
          value={this.state.token}
          onChange={value => this.onTokenChange(value)}
        />
      </div>
    );
  }

  renderItems() {
    let content = this.state.items.map(item => {
      let itemContent = item.displayText || item.value;

      if (this.props.renderItem) {
        itemContent = this.props.renderItem(item.value);
      }

      return (
        <div key={item.value} className={s.item} onClick={() => this.handleItemClick(item)}>
          {itemContent}
        </div>
      );
    });

    if (!content.length && this.state.isLoading) {
      content = <Spinner className={s.spinner} />;
    }

    return (
      <SmartList
        className={s.menu}
        onEsc={() => this.toggleMenu(false)}
        pivot={this._pivot}
        before={this.renderSearchBar()}
      >
        {content}
      </SmartList>
    );
  }

  get menu(): ?Element<any> {
    if (!this.state.open) {
      return;
    }

    return this.renderItems();
  }

  render() {
    const { disabled, name, placeholder, className, renderItem } = this.props;
    const { items, selectedValue, open } = this.state;
    const cls = classNames(s.block, className, {
      [s.disabled]: disabled,
      [s.open]: open,
      [s.empty]: !items.length,
    });
    const arrow = this.state.open ? 'chevron-up' : 'chevron-down';
    let displayText = this.state.displayText;

    if (renderItem && this.state.selectedValue) {
      displayText = renderItem(this.state.selectedValue);
    }

    return (
      <div className={cls}>
        <div className={s.pivot} ref={p => (this._pivot = p)} onClick={this.handleToggleClick}>
          <div className={s.displayText}>{displayText || this.state.selectedValue || placeholder}</div>
          <Icon name={arrow} />
          <input type="hidden" name={name} value={this.state.selectedValue} />
        </div>
        {this.menu}
      </div>
    );
  }
}
