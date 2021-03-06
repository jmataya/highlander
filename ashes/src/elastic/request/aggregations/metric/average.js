/* @flow */

import MetricAggregation from './metric';


export default class AverageAggregation extends MetricAggregation {

  constructor(name: string, field: string) {
    super(name, field);
  }

  toRequest(): Object {
    return this.wrap({
      avg: {
        field: this.field,
      },
    });
  }

}
