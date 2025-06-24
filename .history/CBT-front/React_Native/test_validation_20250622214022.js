// test_validation.js - Simple test script to verify our validation logic
const { isValidIdentifier, createLoginPayload, getLoginErrorMessage } = require('./src/utils/validation');

console.log('=== Testing Frontend Validation ===\n');

// Test invalid identifiers
const invalidIdentifiers = [
  '',
  ' ',
  'NONE_PROVIDED',
  'null',
  'undefined',
  'none',
  'empty',
  'N/A'
];

console.log('Testing invalid identifiers:');
invalidIdentifiers.forEach(identifier => {
  console.log(`"${identifier}": ${isValidIdentifier(identifier)}`);
});

console.log('\nTesting createLoginPayload with invalid data:');

// Test with empty identifier
try {
  const payload = createLoginPayload('', 'password123');
  console.log('ERROR: Empty identifier should have thrown an error');
} catch (e) {
  console.log(`✓ Empty identifier rejected: ${e.message}`);
}

// Test with NONE_PROVIDED
try {
  const payload = createLoginPayload('NONE_PROVIDED', 'password123');
  console.log('ERROR: NONE_PROVIDED should have thrown an error');
} catch (e) {
  console.log(`✓ NONE_PROVIDED rejected: ${e.message}`);
}

// Test with valid email
try {
  const payload = createLoginPayload('test@example.com', 'password123');
  console.log(`✓ Valid email payload: ${JSON.stringify(payload)}`);
} catch (e) {
  console.log(`ERROR: Valid email failed: ${e.message}`);
}

// Test with valid loginId
try {
  const payload = createLoginPayload('testuser', 'password123');
  console.log(`✓ Valid loginId payload: ${JSON.stringify(payload)}`);
} catch (e) {
  console.log(`ERROR: Valid loginId failed: ${e.message}`);
}

console.log('\n=== Test Complete ===');
