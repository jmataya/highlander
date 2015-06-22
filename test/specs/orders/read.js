'use strict';

describe('Orders #GET', function() {

  it('should find an order', function *() {
    let
      res     = yield this.Api.get('/api/v1/orders/1'),
      status  = res[0].statusCode,
      order   = res[1];
    expect(status).to.equal(200);
    expect(order.id).to.match(/^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i);
    expect(order.orderId).to.be.a('string');
    expect(order.orderStatus).to.be.a('string');
    expect(order.paymentStatus).to.be.a('string');
    expect(order).to.include.key('shippingStatus');
    expect(order.customer).to.be.a('object');
    expect(order.total).to.be.a('number');
    expect(order.createdAt).to.match(/\d{4}-[01]\d-[0-3]\dT[0-2]\d:[0-5]\d:[0-5]\d\.\d+([+-][0-2]\d:[0-5]\d|Z)/);
  });

  it('should get an array of orders', function *() {
    let
      res     = yield this.Api.get('/api/v1/orders'),
      status  = res[0].statusCode,
      orders  = res[1];
    expect(status).to.equal(200);
    expect(orders).to.have.length(50);
  });
});
