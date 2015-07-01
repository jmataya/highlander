'use strict';

const
  fs    = require('fs'),
  path  = require('path'),
  t     = require('thunkify-wrap'),
  fleck = require('fleck');

module.exports = function *() {
  let
    models  = yield t(fs.readdir)(`${__dirname}/models`),
    db = { models: {} };

  for (let file of models) {
    let
      name      = path.basename(file, '.js'),
      modelName = fleck.camelize(name, true);

    let model = require(`${__dirname}/models/${file}`);
    db.models[modelName] = model;
    model.data = [];
    model.generateList(1000);
  }

  for (let modelName in db.models) {
    let model = db.models[modelName];
    if (!model.relationships) continue;
    for (let item of model.data) {
      for (let relation of model.relationships) {
        item[`${relation}Id`] = ((item.id - 1) / 7 | 0) + 1;
      }
    }
  }

  return db;
};
