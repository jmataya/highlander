import Api from '../lib/api';
import AshesDispatcher from '../lib/dispatcher';
import LineItemConstants from '../constants/line-items';
import { List } from 'immutable';
import { pluralize } from 'fleck';

class LineItemActions {
  orderLineItemSuccess(order) {
    AshesDispatcher.handleAction({
      actionType: LineItemConstants.ORDER_LINE_ITEM_SUCCESS,
      order: order
    });
  }

  rmaLineItemSuccess(rma) {
    AshesDispatcher.handleAction({
      actionType: LineItemConstants.RETURN_LINE_ITEM_SUCCESS,
      rma: rma
    });
  }

  failedLineItems(errorMessage) {
    AshesDispatcher.handleAction({
      actionType: LineItemConstants.FAILED_LINE_ITEMS,
      errorMessage: errorMessage
    });
  }

  editLineItems(model, refNum, lineItems) {
    let cb;
    if (model === 'order') {
      cb = this.orderLineItemSuccess;
    } else {
      cb = this.rmaLineItemSuccess;
    }
    return Api.post(`/${pluralize(model)}/${refNum}/line-items`, lineItems)
      .then((entity) => {
        cb(entity);
      })
      .catch((err) => {
        this.failedLineItems(err);
      });
  }
}

export default new LineItemActions();
