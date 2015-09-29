'use strict';

const
  Router  = require('koa-router'),
  parse   = require('co-body');

module.exports = function(app) {
  let router = new Router();

  // @todo this will be reenabled once auth is available
  // - Tivs
  // router.use(app.requireAdmin);

  router
    .get('/:path*', function *(next) {
      yield next;
    }, app.renderReact);

  app
    .use(router.routes())
    .use(router.allowedMethods());
};
