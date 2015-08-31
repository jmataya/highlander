'use strict';

const parse = require('co-body');
const Chance = require('chance');

const chance = new Chance();

module.exports = function(app, router) {
  const
    Return = app.seeds.models.Return,
    Note = app.seeds.models.Note,
    Notification = app.seeds.models.Notification,
    Activity = app.seeds.models.Activity;

  router
    .param('return', function *(id, next) {
      this.return = Return.findByIdOrRef(id);
      yield next;
    })
    .param('notification', function *(id, next) {
      this.notification = Notification.findOne(id);
      yield next;
    })
    .get('/returns', function *() {
      let query = this.request.query;
      this.body = Return.paginate(query.limit, query.page);
    })
    .post('/returns', function *() {
      let
        body = yield parse.json(this),
        retrn = new Return(body);
      this.status = 201;
      this.body = retrn;
    })
    .get('/returns/:return', function *() {
      this.body = this.return;
    })
    .patch('/returns/:return', function *() {
      let
        body = yield parse.json(this);
      this.return.amend(body);
      this.status = 200;
      this.body = this.return;
    })
    .post('/returns/:return/edit', function *() {
      this.status = chance.weighted([202, 423], [50, 1]);
      if (this.status === 423) return this.status;
      this.body = this.return;
    })
    .get('/returns/:return/viewers', function *() {
      this.body = this.return.viewers();
    })
    .get('/returns/:return/notes', function *() {
      this.body = Note.findByReturn(this.return.id);
    })
    .post('/returns/:return/notes', function *() {
      let
        body = yield parse.json(this),
        note = new Note(body);
      note.amend({
        returnId: this.return.id,
        orderId: this.return.orderId,
        customerId: 1
      });
      this.status = 201;
      this.body = note;
    })
    .get('/returns/:return/activity-trail', function *() {
      this.body = Activity.findByReturn(this.return.id);
    })
    .get('/returns/:return/notifications', function *() {
      this.body = Notification.findByReturn(this.return.id);
    })
    .post('/returns/:return/notifications/:notification', function *() {
      this.body = this.notification;
    });
};
