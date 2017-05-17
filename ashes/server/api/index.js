const proxy = require('koa-proxy');

module.exports = function(app) {
  const config = app.config.api;
  const matchUriRegexp = new RegExp(`^/api/`);

  app.use(function *apiHandler(next) {
    if (this.request.url.match(matchUriRegexp)) {
      this.request.headers['Accept'] = 'application/json';

      yield app.jsonError.call(this, next);
    } else {
      yield next;
    }
  });

  app.use(proxy({
    host: config.host,
    match: matchUriRegexp,
  }));

};
