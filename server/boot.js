
const path = require('path');

require('babel-register')({
  ignore: path.join(process.cwd(), 'node_modules'),
});
require('../src/postcss').installHook();

const App = require('./app').default;

const app = new App();
app.start();
