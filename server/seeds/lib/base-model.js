'use strict';

const
  _       = require('underscore'),
  Chance  = require('chance'),
  errors  = require('../../errors');

const
  chance = new Chance();

class BaseModel {

  static nextId() {
    let
      ids     = this.data.map(function(i) { return i.id; }),
      nextId  = ids.length ? Math.max.apply(null, ids) + 1 : 1;
    return nextId;
  }

  static generate() {
    let model = {
      id: this.nextId(),
      createdAt: chance.date({year: 2014})
    };

    for (let key of this.seed) {
      let method = key.method || key.field;
      model[key.field] = chance[method](key.opts);
    }
    this.data.push(model);
    return model;
  }

  static generateList(limit) {
    limit = limit || 50;
    let models = [];
    while(limit--) { models.push(this.generate()); }
    return models;
  }

  static findOne(id) {
    id = +id;
    let results = this.data.filter(function(item) {
      return item.id === id;
    });
    if (!results.length) { throw new errors.NotFound(`Cannot find ${this.name}`); }
    return new this(results[0]);
  }

  static paginate(limit, page) {
    limit = +limit || 50;
    page = page ? +page - 1 : 0;
    return this.data.slice(limit * page, (limit * page) + limit);
  }

  constructor(model) {
    this.model = model || {};
    if (!this.id) {
      let newModel = this.constructor.generate();
      this.model.id = newModel.id;
      this.model.createdAt = newModel.createdAt;
      this.amend(model);
    }
  }

  get id() { return this.model.id; }
  get createdAt() { return this.model.createdAt.toISOString(); }

  update(values) {
    let proto = Object.getPrototypeOf(this);
    for (let key in values) {
      let desc = Object.getOwnPropertyDescriptor(proto, key);
      if (desc && desc.set) this[key] = values[key];
    }
    return this;
  }

  amend(values) {
    if (this.whitelist) {
      values = _(values).pick(this.whitelist);
    }
    if (this.blacklist) {
      values = _(values).omit(this.blacklist);
    }
    return this.update(values);
  }

  toJSON() {
    let
      proto = Object.getPrototypeOf(this),
      names = Object.getOwnPropertyNames(proto);

    let json = {
      id: this.id,
      createdAt: this.createdAt
    };

    for (let key of names) {
      let desc = Object.getOwnPropertyDescriptor(proto, key);
      if (desc && desc.get) json[key] = this[key];
    }
    return json;
  }
}

module.exports = BaseModel;
