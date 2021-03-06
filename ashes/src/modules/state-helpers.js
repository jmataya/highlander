import _ from 'lodash';

export function entityId(entity, type=entity.entityType) {
  switch (type) {
    case 'rma':
    case 'order':
      return entity.referenceNumber;
    case 'gift-card':
    case 'inventory-item':
      return entity.code;
    default:
      return entity.id;
  }
}

export function makeEntityId(type) {
  return entity => entityId(entity, type);
}

export function updateItems(items, newItems, iteratee='id') {
  return _.values({
    ..._.keyBy(items, iteratee),
    ..._.keyBy(newItems, iteratee)
  });
}

export function haveType(entity, entityType) {
  return {
    entityId: entityId(entity, entityType),
    entityType,
    ...entity
  };
}
