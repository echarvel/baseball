import {validateId, validateCountryCode} from '../utils/index';

test('basic utility functions tests', () => {
    expect(validateId('aardsda01')).toBe(true);
    expect(validateCountryCode('USA')).toBe(true);
    // need some more tests here
});

