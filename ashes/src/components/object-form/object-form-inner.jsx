/**
 * @flow
 */

// libs
import React, { Component } from 'react';
import _ from 'lodash';
import { autobind } from 'core-decorators';
import classNames from 'classnames';
import invariant from 'invariant';
import { stripTags } from 'lib/text-utils';
import { isDefined } from 'lib/utils';

import renderers from './renderers';

import type { AttrSchema } from 'paragons/object';

type Props = {
  canAddProperty?: boolean,
  fieldsToRender?: Array<string>,
  fieldsOptions?: Object,
  attributes: Attributes,
  onChange: (attributes: Attributes) => void,
  schema?: Object,
  className?: string,
  processAttr?: Function,
};

type State = {
  errors: { [id: string]: any }
};

function formatLabel(label: string): string {
  return _.snakeCase(label).split('_').reduce((res, val) => {
    return `${res} ${_.capitalize(val)}`;
  });
}

export default class ObjectFormInner extends Component {
  props: Props;
  state: State = {
    errors: {},
  };

  @autobind
  handleChange(name: string, type: string, value: any) {
    const { attributes } = this.props;
    const newAttributes = {
      ...attributes,
      [name]: {
        t: type,
        v: value,
      }
    };

    if (['options', 'richText'].indexOf(type) >= 0) {
      const attrOptions = this.getAttrOptions(name);
      if (attrOptions.required) {
        const error = attrOptions.isDefined(value) ? null : `${attrOptions.label} is a required field`;
        const { errors } = this.state;
        errors[name] = error;
        this.setState({ errors });
      }
    }
    this.props.onChange(newAttributes);
  }

  shouldComponentUpdate(nextProps: Props, nextState: State): boolean {
    const attributesChanged = !_.eq(this.props.attributes, nextProps.attributes);
    const stateChanged = !_.eq(this.state, nextState);

    return attributesChanged || stateChanged;
  }

  isRequired(name: string): boolean {
    const { schema } = this.props;
    return schema ? _.includes(schema.required, name) : false;
  }

  getTypeFromSchema(schema: AttrSchema): string {
    if (schema.widget) {
      return schema.widget;
    }

    return _.isArray(schema.type) ? _.first(schema.type) : schema.type;
  }

  getType(schema: ?AttrSchema, attribute: ?Attribute): ?string {
    let name = null;

    if (attribute) {
      name = attribute.t;
    }
    if (name === null && schema) {
      name = this.getTypeFromSchema(schema);
    }
    if (name === 'integer') {
      name = 'number';
    }

    return name;
  }

  guessRenderName(schema: ?AttrSchema, attribute: ?Attribute): string {
    let type = this.getType(schema, attribute);

    return `render${_.upperFirst(type)}`;
  }

  getAttrOptions(name: string,
                 schema: ?AttrSchema = this.props.schema && this.props.schema.properties[name]): Object {
    const options = {
      required: this.isRequired(name),
      label: schema && schema.title || formatLabel(name),
      isDefined: isDefined,
      disabled: schema && schema.disabled,
    };
    if (schema && schema.widget === 'richText') {
      options.isDefined = value => isDefined(stripTags(value));
    }

    return options;
  }

  render() {
    const { props } = this;
    const { attributes, schema, className } = props;
    const fieldsToRender = _.isEmpty(props.fieldsToRender) ? Object.keys(attributes) : props.fieldsToRender;

    const renderedAttributes: Array<Element<*>> = _.map(fieldsToRender, name => {
      const attribute: Attribute = attributes[name];
      const attrSchema: ?AttrSchema = schema ? schema.properties[name] : null;

      const renderName = this.guessRenderName(attrSchema, attribute);
      const attrOptions = this.getAttrOptions(name, attrSchema);

      const renderFn = renderers[renderName];
      const type = this.getType(schema, attribute);

      invariant(type, `There is no type defined for field "${name}".`);
      invariant(renderFn, `There is no method for render "${type}" type.`);

      const renderer = renderFn(this.state.errors, this.handleChange);

      const content = React.cloneElement(renderer(name, attribute && attribute.v, attrOptions), { key: name });

      if (this.props.processAttr) {
        return this.props.processAttr(content, name, attribute && attribute.t, attribute && attribute.v);
      }

      return content;
    });

    return (
      <div className={classNames('fc-object-form', className)}>
        {renderedAttributes}
      </div>
    );
  }
}
