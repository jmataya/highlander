const { default: formatCurrency, stringToCurrency } = requireSource('lib/language-utils', [
  'codeToName'
]);

describe('language utils', function () {

  if ('codeToName should create proper string', function() {
    const code = 'codeToName';

    expect(codeToName(code)).to.be.equal('Code To Name');
  });

});
