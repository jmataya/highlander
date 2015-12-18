import React, { PropTypes } from 'react';
import TableView from '../table/tableview';
import TableRow from '../table/row';
import TableCell from '../table/cell';
import Link from '../link/link';
import { DateTime } from '../common/datetime';
import TabListView from '../tabs/tabs';
import TabView from '../tabs/tab';
import SectionTitle from '../section-title/section-title';
import LocalNav from '../local-nav/local-nav';
import Currency from '../common/currency';
import Status from '../common/status';
import CustomersBase from '../customers/base';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { transitionTo } from '../../route-helpers';
import PilledInput from '../pilled-search/pilled-input';
import { PrimaryButton } from '../common/buttons';
import * as groupsActions from '../../modules/groups/list';


@connect(state => ({groups: state.groups.list}), groupsActions)
export default class Groups extends React.Component {

  static propTypes = {
    fetch: PropTypes.func.isRequired,
    setFetchParams: PropTypes.func.isRequired,
    tableColumns: PropTypes.array,
    groups: PropTypes.shape({
      rows: PropTypes.array.isRequired,
      total: PropTypes.number
    }),
  };

  static defaultProps = {
    tableColumns: [
      {field: 'name', text: 'Group Name', model: 'group'},
      {field: 'type', text: 'Type', model: 'group'},
      {field: 'customersCount', text: 'Number in Group', model: 'group'},
      {field: 'createdAt', text: 'Date/Time Created', type: 'date'},
      {field: 'modifiedAt', text: 'Date/Time Last Modified', type: 'date'},
    ]
  };

  static contextTypes = {
    history: PropTypes.object.isRequired
  };

  componentDidMount() {
    this.props.fetch(this.props.groups);
  }

  @autobind
  onAddGroup() {
    transitionTo(this.context.history, 'groups-new-dynamic');
  }

  render() {
    const renderRow = (row, index) => (
      <TableRow key={`${index}`}>
        <TableCell>
          <Link to={'group'} params={{groupId: row.id}}>
            {row.name}
          </Link>
        </TableCell>
        <TableCell>{row.type}</TableCell>
        <TableCell>{row.customersCount}</TableCell>
        <TableCell><DateTime value={row.createdAt}/></TableCell>
        <TableCell><DateTime value={row.modifiedAt}/></TableCell>
      </TableRow>
    );

    return (
      <CustomersBase>
        <div className="fc-grid fc-groups-components">
          <div className="fc-col-md-1-1 _group-header _group-component">
            <h2 className="_group-title">Customers Groups</h2>
            <PrimaryButton icon="add" onClick={this.onAddGroup}></PrimaryButton>
          </div>
          <div className="fc-col-md-1-1 _group-component">
            <PilledInput placeholder="Add filter or keyword search"/>
          </div>
          <div className="fc-col-md-1-1 _group-component">
            <TableView
                columns={this.props.tableColumns}
                data={this.props.groups}
                renderRow={renderRow}
                setState={this.props.fetch}
              />
          </div>
        </div>
      </CustomersBase>
    );
  }
}
