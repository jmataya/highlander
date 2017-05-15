const path = require('path');

module.exports = {
  module: {
    rules: [
      {
        test: /\.js(x)?$/,
        include: [
          path.resolve(__dirname, '../src'),
        ],
        use: [ 'babel-loader?cacheDirectory=true' ],
      },
      {
        test: /\.css$/,
        use: [ 'style-loader', 'css-loader', 'postcss-loader' ]
      },
      {
        test: /\.less$/,
        use: [ 'style-loader', 'css-loader', 'less-loader' ]
      },
    ]
  },

  devtool: 'eval-source-map',
};
