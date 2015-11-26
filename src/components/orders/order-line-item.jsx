import React, { PropTypes } from 'react';
import Counter from '../forms/counter';
import {DeleteButton} from '../common/buttons';
import Currency from '../common/currency';

const OrderLineItem = props => {
  const item = props.item;
  const order = props.order.currentOrder;

  const handleChange = (event) => {
    props.updateLineItemCount(order, item.sku, event.target.value);
  };

  return (
    <tr>
      <td><img src={item.imagePath} /></td>
      <td>{item.name}</td>
      <td>{item.sku}</td>
      <td><Currency value={item.price}/></td>
      <td>
        <Counter
          inputName={`line-item-quantity-${item.sku}`}
          value={item.quantity}
          minValue={0}
          maxValue={1000000}
          stepAmount={1}
          onChange={handleChange}
          decreaseAction={() => props.updateLineItemCount(order, item.sku, item.quantity - 1)}
          increaseAction={() => props.updateLineItemCount(order, item.sku, item.quantity + 1)} />
      </td>
      <td><Currency value={item.totalPrice}/></td>
      <td>
        <DeleteButton onClick={() => props.orderLineItemsStartDelete(item.sku)} />
      </td>
    </tr>
  );
};

OrderLineItem.propTypes = {
  orderLineItemsStartDelete: PropTypes.func,
  order: PropTypes.shape({
    currentOrder: PropTypes.object
  })
};

export default OrderLineItem;
