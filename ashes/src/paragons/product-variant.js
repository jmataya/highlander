/* @flow */

import _, { cloneDeep } from 'lodash';
import { getJWT } from 'lib/claims';
import * as t from 'paragons/object-types';

import type { JWT } from 'lib/claims';
import type { ProductVariant, NewProductVariant } from 'modules/product-variants/details';
export function generateSkuCode(): string {
  return Math.random().toString(36).substring(7).toUpperCase();
}

// should contain all known attributes
export const productVariantEmptyAttributes = {
  code: t.string(''),
  title: t.string(''),
  salePrice: t.price({ currency: 'USD', value: 0 }),
  retailPrice: t.price({ currency: 'USD', value: 0 }),
};

export function configureProductVariant(productVariant: ProductVariant): ProductVariant {
  _.defaults(productVariant.attributes, productVariantEmptyAttributes);

  return productVariant;
}

// HACK
function isMerchant(): boolean {
  const jwt = getJWT();
  if (jwt != null && jwt.scope == '1') {
    return false;
  }

  return true;
}

export function createEmptyProductVariant(): NewProductVariant {
  let merchantAttributes = {};

  if (isMerchant()) {
    merchantAttributes = {
      externalId: t.string(''),
      mpn: t.string(''),
      gtin: t.string(''),
      depth: t.string(''),
    };
  }

  return {
    id: null,
    attributes: { ...cloneDeep(productVariantEmptyAttributes), ...merchantAttributes },
    context: {
      name: 'default',
    }
  };
}