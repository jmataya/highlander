const crypto = require('crypto');
const path = require('path');
const identity = require('lodash/identity');

// '../../src/components/product/page' + 'wrapper' -> 'product/page__wrapper'
function generateLongName(exportedName, filepath) {
  const sanitisedPath = path.relative(process.cwd(), filepath)
    .replace('src/components', '')
    .replace('lib/components', '')
    .replace('src/css', '')
    .replace(/\.[^\.\/\\]+$/, '')
    .replace(/^[\.\/\\]+/, '')
    .replace(/\//g, '⁄'); // http://www.fileformat.info/info/unicode/char/2044/browsertest.htm

  const sanitisedName = exportedName.replace(/^_+|_+$/g, '');

  return `${sanitisedPath}__${sanitisedName}`;
}

function generateShortName(name, filename, css) {
  const i = css.indexOf(`.${name}`);
  const numLines = css.substr(0, i).split(/[\r\n]/).length;

  const hash = crypto.createHash('md5').update(css).digest('hex').substr(0, 5);
  return `_${name}_${hash}_${numLines}`;
}

function generateScopedNameFn() {
  switch (process.env.NODE_ENV) {
    case 'test':
      return identity;
    case 'production':
      return generateShortName;
    default:
      return generateLongName;
  }
}

console.info(process.env.NODE_ENV);

const plugins = [
  require('postcss-import')({
    path: ['src/css', 'node_modules'],
  }),
  require('postcss-assets')({
    loadPaths: ['src/images/']
  }),
  require('postcss-css-variables'),
  require('postcss-cssnext')({
    features: {
      // Instead of it we are using `postcss-css-variables` above
      // https://github.com/MadLittleMods/postcss-css-variables#differences-from-postcss-custom-properties
      customProperties: false,
    },
  }),
  require('lost')({
    flexbox: 'flex',
    gutter: '1.85%',
  }),
  require('postcss-mixins'),
  require('postcss-nested'),
  require('postcss-modules-local-by-default'),
  require('postcss-modules-scope')({
    generateScopedName: generateScopedNameFn(),
  }),
];

exports.plugins = plugins;
