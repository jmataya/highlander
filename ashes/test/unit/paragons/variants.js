
import _ from 'lodash';

const Variants = requireSource('paragons/variants.js');
const Products = requireSource('paragons/product.js');

function makeSkus(count) {
  const result = [];
  let i = 0;
  while (count--) {
    const sku = Products.createEmptySku();
    sku.feCode = `sku${i++}`;
    result.push(sku);
  }
  return result;
}

describe('Variants', function () {
  context('#autoAssignOptions', () => {
    it('grow1', () => {
      const variants = makeSkus(2);

      const options = [
        {
          values: [
            {
              name: 'L',
              skuCodes: [variants[0].feCode]
            },
            {
              name: 'S',
              skuCodes: [variants[1].feCode]
            }
          ]
        }, {
          values: [
            {
              name: 'green',
              skuCodes: []
            }
          ]
        }
      ];

      const newOptions = Variants.autoAssignOptions({variants}, options).options;
      expect(newOptions).to.deep.equal(
        [
          {
            values: [
              {
                name: 'L',
                skuCodes: [variants[0].feCode]
              },
              {
                name: 'S',
                skuCodes: [variants[1].feCode]
              }
            ]
          }, {
            values: [
              {
                name: 'green',
                skuCodes: [variants[0].feCode, variants[1].feCode]
              }
            ]
          }
        ]
      );
    });

    it('grow2', () => {
      const variants = makeSkus(2);

      const options = [
        {
          values: [
            {
              name: 'S',
              skuCodes: [variants[1].feCode]
            },
            {
              name: 'L',
              skuCodes: [variants[0].feCode]
            },
          ]
        }, {
          values: [
            {
              name: 'green',
              skuCodes: [variants[0].feCode, variants[1].feCode]
            }
          ]
        }, {
          values: [
            {
              name: 'male',
              skuCodes: []
            }, {
              name: 'female',
              skuCodes: []
            }
          ]
        }
      ];

      const newOptions = Variants.autoAssignOptions({variants}, options).options;
      expect(newOptions).to.have.length(3);
      expect(newOptions[0].values[0].skuCodes).to.have.length(2);
      expect(newOptions[0].values[1].skuCodes).to.have.length(2);
      expect(newOptions[1].values[0].skuCodes).to.have.length(4);
      expect(newOptions[2].values[0].skuCodes).to.have.length(2);
      expect(newOptions[2].values[0].skuCodes).to.have.members([variants[0].feCode, variants[1].feCode]);
    });

    it('decrease1', () => {
      const variants = makeSkus(5);
      const options = [
        {
          values: [
            {
              name: 'L',
              skuCodes: [variants[0].feCode, variants[2].feCode, variants[4].feCode]
            },
            {
              name: 'S',
              skuCodes: [variants[1].feCode, variants[3].feCode]
            }
          ]
        }, {
          values: [
            {
              name: 'green',
              skuCodes: [variants[0].feCode, variants[1].feCode, variants[2].feCode, variants[3].feCode]
            }
          ]
        }
      ];

      const result = Variants.autoAssignOptions({variants}, options);
      //console.log(JSON.stringify(result.variants, null, 2));
      expect(result.options).to.have.length(2);
      expect(result.options[0].values[0].skuCodes).to.have.members([variants[0].feCode]);
      expect(result.options[0].values[1].skuCodes).to.have.members([variants[1].feCode]);
      expect(result.options[1].values[0].skuCodes).to.have.members([variants[0].feCode, variants[1].feCode]);
    });

    it('keep one sku if there is no variants', () => {
      const variants = makeSkus(2);
      const product = {variants};

      const newProduct = Variants.autoAssignOptions(product, []);
      expect(newProduct.variants).to.have.length(1);
    });
  });

  const redValue = {'name':'Red','swatch':'FF0000','skuCodes':[]};
  const greenValue = {'name':'Green','swatch':'00FF00','skuCodes':[]};
  const smallValue = {'name':'S','skuCodes':[]};
  const mediumValue = {'name':'M','skuCodes':[]};
  const largeValue = {'name':'L','skuCodes':[]};

  context('#allOptionsValues', function () {
    it('should return array of items when one varinat is defined', function () {
      const options = [
        {
          'values':[
            redValue,
            greenValue,
          ],
          'attributes':{'name':{'t':'string','v':'Color'}}
        }
      ];
      expect(Variants.allOptionsValues(options)).to.be.eql(
        [[redValue], [greenValue]]
      );
    });

    it('should return array of all combinations when multiple varinats are defined', function () {
      const options = [
        {
          'values': [
            redValue,
            greenValue,
          ],
          'attributes': {'name':{'t':'string','v':'Color'}}
        },
        {
          'values': [
            smallValue, mediumValue, largeValue,
          ],
          'attributes': {'name':{'t':'string','v':'Size'}}
        }
      ];
      expect(Variants.allOptionsValues(options)).to.be.eql(
        [
          [redValue, smallValue],
          [redValue, mediumValue],
          [redValue, largeValue],
          [greenValue, smallValue],
          [greenValue, mediumValue],
          [greenValue, largeValue],
        ]
      );
    });

    it('should return array of all combinations when multiple varinats are defined with one option', function () {
      const options = [
        {
          'values': [
            greenValue,
          ],
          'attributes': {'name':{'t':'string','v':'Color'}}
        },
        {
          'values': [
            smallValue, mediumValue, largeValue,
          ],
          'attributes': {'name':{'t':'string','v':'Size'}}
        }
      ];
      expect(Variants.allOptionsValues(options)).to.be.eql(
        [
          [greenValue, smallValue],
          [greenValue, mediumValue],
          [greenValue, largeValue],
        ]
      );
    });
  });

  context('optionsWithMultipleValues', function () {
    it('should replace variants with empty value list from array', function () {
      const options = [
        {
          'values': [],
          'attributes': {'name':{'t':'string','v':'Color'}}
        }
      ];
      expect(Variants.optionsWithMultipleValues(options)).to.be.eql([]);
    });
  });
});
