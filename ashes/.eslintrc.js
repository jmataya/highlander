module.exports = {
  "root": true,
  "ecmaFeatures": {
    "defaultParams": true,
    "classes": true,
    "modules": true,
    "experimentalObjectRestSpread": true,
    "blockBindings": true,
    "forOf": true,
    "jsx": true
  },
  "env": {
    "node": true,
    "browser": true,
    "es6": true,
    "mocha": true
  },
  "globals": {
    "expect": false,
    "JWTString": true
  },
  "rules": {
    "semi": 2,
    "react/prop-types": 0,
    "strict": 0,
    "max-len": [
      2,
      120,
      2,
      {
        "ignoreComments": true,
        "ignoreUrls": true,
        "ignorePattern": "^.+=\\s*require\\s*\\("
      }
    ],
    "quotes": [
      2,
      "single",
      { "allowTemplateLiterals": true }
    ],
    "curly": [
      2,
      "multi-line"
    ],
    "no-process-exit": 0,
    "no-underscore-dangle": 0,
    "no-multi-spaces": [
      1,
      {
        "exceptions": {
          "VariableDeclarator": true
        }
      }
    ],

    "no-trailing-spaces": 2,
    "eol-last": ["error", "always"],

    "no-cond-assign": 2,
    "no-console": ["error", { allow: ["warn", "error", "info", "dir"] }],
    "no-constant-condition": 2,
    "no-alert": 2,
    "no-debugger": 2,
    "no-unused-vars": ["error", { "vars": "local", "args": "none", varsIgnorePattern: "styles|Element|_|key", ignoreRestSiblings: true }],

    "no-dupe-args": 2,
    "no-dupe-keys": 2,
    "no-duplicate-case": 2,
    "no-empty": 2,
    "no-empty-character-class": 2,
    "no-ex-assign": 2,
    "no-extra-boolean-cast": 2,
    "no-extra-parens": 0,
    "no-extra-semi": 2,
    "no-func-assign": 2,
    "no-inner-declarations": 2,
    "no-invalid-regexp": 2,
    "no-irregular-whitespace": 2,
    "no-obj-calls": 2,
    "no-prototype-builtins": 2,
    "no-regex-spaces": 2,
    "no-sparse-arrays": 2,
    "no-template-curly-in-string": 2,
    "no-unexpected-multiline": 2,
    "no-unreachable": 2,
    "no-unsafe-finally": 2,
    "no-unsafe-negation": 2,
    "use-isnan": 2,
    "valid-jsdoc": 0,
    "valid-typeof": 2,

    "no-case-declarations": 2,
    "no-empty-pattern": 2,
    "no-fallthrough": 2,
    "no-global-assign": 2,
    "no-octal": 2,
    "no-redeclare": 2,
    "no-self-assign": 2,
    "no-unused-labels": 2,

    "no-delete-var": 2,

    "indent": ["error", 2, { SwitchCase: 1 }],
    "no-mixed-spaces-and-tabs": 2,

    "react/display-name": 0,
    "react/jsx-uses-react": 1,
    "react/jsx-uses-vars": 1,
    "react/jsx-tag-spacing": [2, {
      "closingSlash": "never",
      "beforeSelfClosing": "always",
      "afterOpening": "never",
    }],
    "react/no-did-update-set-state": 1,
    "react/no-multi-comp": [
      0,
      {
        "ignoreStateless": true
      }
    ],
    "react/react-in-jsx-scope": 1,
    "react/self-closing-comp": 0,
    "react/jsx-wrap-multilines": 1,
    "react/no-did-mount-set-state": 1,
    "lodash-fp/use-fp": 0,
    "lodash-fp/no-extraneous-function-wrapping": 0,
    "lodash-fp/prefer-constant": 0,
    "lodash-fp/no-extraneous-iteratee-args": 2
  },
  "plugins": [
    "react", "lodash-fp"
  ],
  "parser": "babel-eslint"
};
