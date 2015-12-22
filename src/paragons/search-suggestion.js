import _ from 'lodash';
import SearchTerm from './search-term';

export default class SearchSuggestion {
  constructor(suggestion, searchTerm) {
    this._suggestion = suggestion;
    this._searchTerm = searchTerm;
    this._title = `${searchTerm.selectionValue}${suggestion}`;
  }

  get children() {
    return [];
  }

  get displayTerm() {
    return this._title;
  }

  get displayAction() {
    return '';
  }

  get selectionValue() {
    return this.displayTerm;
  }

  get type() {
    return this._searchTerm.type;
  }

  applicableTerms(search) {
    return this.matchesSearchTerm(search) ? [this] : [];
  }

  matchesSearchTerm(search) {
    const nSearch = SearchTerm.normalizeSearchTerm(search).toLowerCase();
    const nTerm = this.displayTerm.toLowerCase();
    return _.startsWith(nTerm, nSearch);
  }

  removeValue(searchTerm) {
    const opIdx = searchTerm.lastIndexOf(':');
    return opIdx > -1
      ? searchTerm.slice(0, opIdx).trim()
      : searchTerm;
  }

  selectTerm(search) {
    return this.matchesSearchTerm(search) && search.trim().length == this.selectionValue.length;
  }

  toFilter(search) {
    return this._searchTerm.toFilter(search);
  }
}
