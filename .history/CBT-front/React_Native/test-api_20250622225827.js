// Simple test script to validate our API health utilities
const https = require('https');

const BASIC_URL = 'https://auth.nodove.com';

// Simplified fetch for Node.js
const simpleFetch = (url, options = {}) => {
  return new Promise((resolve, reject) => {
    const urlObj = new URL(url);
    const postData = options.body || '';
    
    const req = https.request({
      hostname: urlObj.hostname,
      port: urlObj.port || 443,
      path: urlObj.pathname,
      method: options.method || 'GET',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(postData),
        ...options.headers
      }
    }, (res) => {
      let data = '';
      res.on('data', (chunk) => data += chunk);
      res.on('end', () => {
        try {
          const json = JSON.parse(data);
          resolve({
            ok: res.statusCode >= 200 && res.statusCode < 300,
            status: res.statusCode,
            headers: {
              get: (name) => res.headers[name.toLowerCase()]
            },
            json: () => Promise.resolve(json),
            text: () => Promise.resolve(data)
          });
        } catch (e) {
          resolve({
            ok: res.statusCode >= 200 && res.statusCode < 300,
            status: res.statusCode,
            headers: {
              get: (name) => res.headers[name.toLowerCase()]
            },
            json: () => Promise.reject(new Error('Invalid JSON')),
            text: () => Promise.resolve(data)
          });
        }
      });
    });
    
    req.on('error', reject);
    req.setTimeout(5000, () => {
      req.destroy();
      reject(new Error('Request timeout'));
    });
    
    if (postData) {
      req.write(postData);
    }
    req.end();
  });
};

// Copy our safeApiCall logic for testing
const checkServerHealth = async () => {
  try {
    const response = await simpleFetch(`${BASIC_URL}/api/public/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: 'health_check', password: 'health_check' }),
    });
    
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
