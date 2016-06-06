
import _ from 'lodash';
import { denormalize, setAttribute, addAttribute } from './form-shadow-object';

const storefrontNameValidotor = (value: string): ?string => {
  console.log(value);
  if (value == null || value === '' || value === '<p><br/></p>') {
    return 'Storefront Name is required field';
  }

  return null;
};

export const options = {
  name: { required: true },
  storefrontName: { validator: storefrontNameValidotor },
};

export function createEmptyCoupon() {
  const usageRules = {
    isExclusive: false,
    isUnlimitedPerCode: false,
    usesPerCode: 1,
    isUnlimitedPerCustomer: false,
    usesPerCustomer: 1,
  };

  const coupon = {
    form: {
      id: null,
      createdAt: null,
      attributes: {
        usageRules
      },
    },
    shadow: {
      id: null,
      createdAt: null,
      attributes: {},
    },
    promotion: null
  };

  return configureCoupon(coupon);
}

export function configureCoupon(coupon) {
  const { form, shadow } = coupon;

  const defaultAttrs = {
    name: 'string',
    storefrontName: 'richText',
    description: 'text',
    details: 'richText',
    usageRules: 'usageRules',
  };

  denormalize(coupon);

  _.each(defaultAttrs, (type, label) => {
    const formAttribute = _.get(coupon, ['form', 'attributes', label]);
    if (formAttribute === void 0) {
      [form.attributes, shadow.attributes] = setAttribute(label, type, '', form.attributes, shadow.attributes);
    }
  });

  return coupon;
}
