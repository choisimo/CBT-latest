// Simple test script to validate our API health utilities
const fetch = require('node-fetch');

const BASIC_URL = 'https://auth.nodove.com';

// Copy our safeApiCall logic for testing
const checkServerHealth = async () => {
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 5000);
    
    const response = await fetch(`${BASIC_URL}/api/public/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: 'health_check', password: 'health_check' }),
      signal: controller.signal,
    });
    
    clearTimeout(timeoutId);
    console.log('✅ Server health check passed - server is responding');
    return true;
  } catch (error) {
    console.warn('❌ Server health check failed:', error.message);
    return false;
  }
};

const safeApiCall = async (url, options = {}) => {
  try {
    const isServerHealthy = await checkServerHealth();
    if (!isServerHealthy) {
      return {
        success: false,
        error: '서버에 연결할 수 없습니다. 서버가 실행 중인지 확인해주세요.'
      };
    }

    const response = await fetch(url, options);
    const contentType = response.headers.get('content-type');
    const isJson = contentType && contentType.indexOf('application/json') !== -1;
    
    if (!isJson) {
      const responseText = await response.text();
      console.error('Non-JSON response received:', {
        url,
        status: response.status,
        contentType,
        responseText: responseText.substring(0, 500)
      });
      
      return {
        success: false,
        error: '서버 응답 형식이 올바르지 않습니다. 서버 상태를 확인해주세요.'
      };
    }

    const data = await response.json();
    
    if (!response.ok) {
      return {
        success: false,
        error: data.message || `서버 오류 (${response.status})`
      };
    }

    return {
      success: true,
      data
    };
  } catch (error) {
    console.error('API call failed:', error);
    return {
      success: false,
      error: error.message || 'API 호출 중 오류가 발생했습니다'
    };
  }
};

// Test functions
async function testSignup() {
  console.log('\n🧪 Testing Signup API...');
  
  const result = await safeApiCall(
    `${BASIC_URL}/api/public/join`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ 
        loginId: 'test_user_' + Date.now(), 
        userPw: 'TestPassword123!', 
        email: 'test' + Date.now() + '@example.com', 
        nickname: 'TestUser_' + Date.now()
      }),
    }
  );

  console.log('Signup result:', result);
}

async function testDuplicationCheck() {
  console.log('\n🧪 Testing Duplication Check API...');
  
  const result = await safeApiCall(
    `${BASIC_URL}/api/public/check/loginId/IsDuplicate`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ loginId: 'admin' }),
    }
  );

  console.log('Duplication check result:', result);
}

async function testLogin() {
  console.log('\n🧪 Testing Login API...');
  
  const result = await safeApiCall(
    `${BASIC_URL}/api/public/login`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ loginId: 'test', userPw: 'test' }),
    }
  );

  console.log('Login result:', result);
}

// Run tests
async function runTests() {
  console.log('🚀 Starting API tests with remote server...');
  
  await testLogin();
  await testDuplicationCheck();
  // Don't actually test signup to avoid creating dummy accounts
  console.log('\n✅ API health tests completed!');
}

runTests().catch(console.error);
