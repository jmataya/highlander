# Firebird

[![Build status](https://badge.buildkite.com/1238dff6913c220ef0612d9a9f4b0c5198a8dd270d260f8ff2.svg)](https://buildkite.com/foxcommerce/firebird)

### Prerequisites

* node

5.1.0 or above is required version for Firebird.
To install this or anothers versions of node you can use [n](https://github.com/tj/n) or [nvm](https://github.com/creationix/nvm) node version manager.

If using n, run the following command to install latest version of node:

### Install Flow

We're using [Flow](https://flowtype.org) to perform type checks and `babel-plugin-typecheck` for same thing at runtime.
Install Flow per the instructions on the website. Checkout required version in .flowconfig file.

```
[sudo] n latest
```

### Install npm modules

```
npm install
```

### Run the dev server
```
npm run dev
```

### Push hooks

By default, gulpfile installs pre-push hooks for you.
And, usually, it's ok having pre-push hooks, even if you needed to push broken branch
you can push with `--no-verify` option.
Also, you can disable auto installing hooks by creating `.gulprc` in project root with following content:

```
exports.autoInstallHooks = false;
```

## Base infrastructure

For achieve right isomorphism [redux-wait](https://www.npmjs.com/package/redux-wait) is used.
It utilizes multiple rendering calls for get all async dependencies for project.
Read about code organization limitations in redux-wait's README.

For **grids** [lost](https://www.npmjs.com/package/lost) postcss plugin is used. It's really good.
For different margins which depends on viewport size use `--grid-margin` css variable: `margin: 0 var(--grid-margin)`.

For **static type checking** [flowtype](http://flowtype.org/) is used. You can run check manually by `npm run flow` command.

For **icons** svg icons is used. Just place svg icon to `src/images/svg` folder and gulp sprites task builds sprite for you
automatically. Name for each icon in a sprite will be `fc-<file-name-lowecased>` Usage:

```jsx
import Icon from 'ui/icon';

const icon = <Icon name="fc-google" />;

```

## Expand diffs for src/node_modules files in GitHub

GitHub [doesn't allow](https://github.com/github/linguist/issues/2206#issuecomment-103383178) overriding config
for vendor files in terms of diff suppression.
So, for you convenience, you can install [userscript](./unsuppressor.user.js) that expands diffs for you automatically.
You can install this userscript via [tampermonkey](http://tampermonkey.net) for chrome or
via [greasemonkey](https://addons.mozilla.org/en-US/firefox/addon/greasemonkey/) for firefox.

![Firebird and Phoenix](http://i.imgur.com/7Cyj5q8.jpg "Firebird and Phoenix")
