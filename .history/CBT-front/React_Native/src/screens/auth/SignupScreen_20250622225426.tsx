// src/screens/auth/SignupScreen.tsx
import React, { useState, useRef } from 'react';
import {
  View,
  Text,
  TextInput,
  StyleSheet,
  TouchableOpacity,
  Alert,
  ScrollView,
  Image,
} from 'react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { AuthStackParamList } from '../../navigation/AuthStack';
import { BASIC_URL } from '../../constants/api';
import debounce from 'lodash.debounce';

type Props = NativeStackScreenProps<AuthStackParamList, 'SignUp'>;

interface Res {
  status: string;
  message: string;
  data: boolean;
  timestamp: string;
}

export default function SignupScreen({ navigation }: Props) {
  const [loginId, setLoginId] = useState('');
  const [emailCode, setEmailCode] = useState('');
  const [loginIdAvailable, setLoginIdAvailable] = useState<boolean | null>(null);
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [email, setEmail] = useState('');
  const [emailAvailable, setEmailAvailable] = useState<boolean | null>(null);
  const [nickname, setNickname] = useState('');
  const [nicknameAvailable, setNicknameAvailable] = useState<boolean | null>(null);
  const [agreePersonal, setAgreePersonal] = useState(false);
  const [agreeTerms, setAgreeTerms] = useState(false);
  const [isEmailVerified, setIsEmailVerified] = useState(false);

  const [loginIdError, setLoginIdError] = useState('');
  const [emailError, setEmailError] = useState('');
  const [passwordError, setPasswordError] = useState('');
  const [nicknameError, setNicknameError] = useState('');

  const isLoginIdTooShort = (loginId: string): boolean =>
    loginId.length > 0 && loginId.length < 4;

  const isValidEmailFormat = (email: string): boolean =>
    /^\S+@\S+\.\S+$/.test(email);

  const isValidPassword = (
    password: string,
    loginId: string // 비밀번호에 ID 포함 검사용
  ): boolean => {
    if (password.length < 8 || password.length > 50) return false;
    const patterns = [/[A-Z]/, /[a-z]/, /\d/, /[^A-Za-z0-9]/];
    if (patterns.filter(rx => rx.test(password)).length < 3) return false;
    if (/(.)\1\1/.test(password)) return false;
    if (loginId && password.includes(loginId)) return false;
    return true;
  };

  // ✅ CONSISTENT duplication check function
  const checkDuplication = async (
    endpoint: string,
    payload: object,
    fieldName: string
  ): Promise<{ isDuplicate: boolean; isAvailable: boolean; message: string }> => {
    const res = await fetch(`${BASIC_URL}/api/public/check/${endpoint}/IsDuplicate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    const res_json: Res = await res.json();

    // ✅ CONSISTENT LOGIC: API returns data: true for duplicate, data: false for available
    const isDuplicate = res_json.data === true;
    const isAvailable = !isDuplicate;

    console.log(`🔍 ${fieldName} duplication check:`, {
      endpoint,
      payload,
      response: res_json,
      isDuplicate,
      isAvailable
    });

    return {
      isDuplicate,
      isAvailable,
      message: res_json.message || ''
    };
  };

  // Debounced API checks - ALL using consistent logic
  const debouncedCheckLoginId = useRef(
    debounce(async (value: string) => {
      try {
        const { isDuplicate, isAvailable, message } = await checkDuplication(
          'loginId',
          { loginId: value },
          'LoginId'
        );

        setLoginIdAvailable(isAvailable);

        if (isDuplicate) {
          setLoginIdError(message || '이미 사용 중인 아이디입니다.');
        } else {
          setLoginIdError('');
        }
      } catch (e: any) {
        setLoginIdAvailable(null);
        setLoginIdError(e.message || '알 수 없는 오류가 발생했습니다.');
      }
    }, 300)
  ).current;

  const debouncedCheckEmail = useRef(
    debounce(async (value: string) => {
      try {
        const { isDuplicate, isAvailable, message } = await checkDuplication(
          'email',
          { email: value },
          'Email'
        );

        setEmailAvailable(isAvailable);

        if (isDuplicate) {
          setEmailError(message || '이미 사용 중인 이메일입니다.');
        } else {
          setEmailError('');
        }
      } catch (e: any) {
        setEmailAvailable(null);
        setEmailError(e.message || '알 수 없는 오류가 발생했습니다.');
      }
    }, 300)
  ).current;

  const debouncedCheckNickname = useRef(
    debounce(async (value: string) => {
      if (!value) return;
      try {
        const { isDuplicate, isAvailable, message } = await checkDuplication(
          'nickname',
          { nickname: value },
          'Nickname'
        );

        setNicknameAvailable(isAvailable);

        if (isDuplicate) {
          setNicknameError(message || '이미 사용 중인 닉네임입니다.');
        } else {
          setNicknameError('');
        }
      } catch (e: any) {
        setNicknameAvailable(null);
        setNicknameError(e.message || '알 수 없는 오류가 발생했습니다.');
      }
    }, 300)
  ).current;

  const handleLoginIdChange = (text: string) => {
    setLoginId(text);
    setLoginIdAvailable(null);

    if (!text) {
      setLoginIdError('');
      return;
    }

    if (isLoginIdTooShort(text)) {
      setLoginIdError('아이디는 4자 이상이어야 합니다.');
    } else {
      setLoginIdError('');
      debouncedCheckLoginId(text);
    }
  };

  const handleEmailChange = (text: string) => {
    console.log('📧 Email change:', text);
    setEmail(text);
    setEmailAvailable(null);
    setIsEmailVerified(false);
    console.log('📧 Reset emailAvailable to null and emailVerified to false');

    if (text && !isValidEmailFormat(text)) {
      setEmailError('올바른 이메일 형식을 입력해주세요.');
      console.log('📧 Invalid email format');
    } else if (text.length > 0) {
      setEmailError('');
      console.log('📧 Valid email format, calling debouncedCheckEmail');
      debouncedCheckEmail(text);
    } else {
      setEmailError('');
    }
  };

  const handlePasswordChange = (text: string) => {
    setPassword(text);
    // 패스워드 유효성 검사
    if (text && !isValidPassword(text, loginId)) {
      setPasswordError('비밀번호는 8~50자, 영문/숫자/특수문자 중 3종류 이상이어야 합니다.');
    } else if (confirmPassword && text !== confirmPassword) {
      setPasswordError('비밀번호가 일치하지 않습니다.');
    } else {
      setPasswordError('');
    }
  };

  const handleConfirmPasswordChange = (text: string) => {
    setConfirmPassword(text);
    // 확인용 패스워드 일치 여부만 검사
    if (password && text !== password) {
      setPasswordError('비밀번호가 일치하지 않습니다.');
    } else {
      setPasswordError('');
    }
  };

  const handleNicknameChange = (text: string) => {
    setNickname(text);
    setNicknameAvailable(null);

    if (!text) {
      setNicknameError('');
      return;
    }

    setNicknameError('');
    debouncedCheckNickname(text);
  };

  const handleSendEmailCode = async () => {
    console.log('🔴 handleSendEmailCode called');
    console.log('🔴 Current email:', email);
    console.log('🔴 isValidEmailFormat:', isValidEmailFormat(email));
    console.log('🔴 emailAvailable:', emailAvailable);

    // 이메일 형식 체크
    if (!email || !isValidEmailFormat(email)) {
      console.log('🔴 Invalid email or format');
      Alert.alert('오류', '먼저 올바른 이메일을 입력해주세요.');
      return;
    }

    // 이메일 중복 체크가 아직 진행 중이면 수행
    if (emailAvailable === null) {
      console.log('🔴 emailAvailable is null, performing check');
      try {
        const { isDuplicate, isAvailable, message } = await checkDuplication(
          'email',
          { email },
          'Email (Manual)'
        );

        setEmailAvailable(isAvailable);

        if (isDuplicate) {
          setEmailError(message || '이미 사용 중인 이메일입니다.');
          console.log('🔴 ❌ Email duplicate - showing alert');
          Alert.alert('오류', message || '이미 사용 중인 이메일입니다.');
          return;
        } else {
          setEmailError('');
          console.log('🔴 ✅ Email available - continuing');
        }
      } catch (e: any) {
        console.log('🔴 Manual email check error:', e);
        setEmailAvailable(null);
        setEmailError(e.message || '알 수 없는 오류가 발생했습니다.');
        Alert.alert('오류', '이메일 중복 확인 중 오류가 발생했습니다.');
        return;
      }
    } else if (emailAvailable === false) {
      console.log('🔴 Email not available (duplicate)');
      Alert.alert('오류', '이미 사용 중인 이메일입니다.');
      return;
    }

    console.log('🔴 Sending email verification code');
    try {
      // 인증번호 요청 API 호출
      const res = await fetch(`${BASIC_URL}/api/public/emailCode`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email }),
      });
      const json = await res.json() as {
        status: string;
        message: string;
        data: any;
      };
      console.log('🔴 Email code response:', json);
      Alert.alert(
        json.status === 'success' ? '확인' : '오류',
        json.message
      );
    } catch (e: any) {
      console.log('🔴 Email code send error:', e);
      Alert.alert('오류', '인증번호 발송에 실패했습니다.');
    }
  };

  const handleConfirmEmailCode = async () => {
    try {
      const res = await fetch(`${BASIC_URL}/api/public/emailCheck`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, code: emailCode }),
      });
      const json = await res.json() as {
        status: string;
        message: string;
        data: any;
      };
      Alert.alert(
        json.status === 'success' ? '확인' : '오류',
        json.message
      );
      setIsEmailVerified(json.status === 'success');
    } catch {
      Alert.alert('오류', '인증 확인 중 오류가 발생했습니다.');
    }
  };

  const handleSignup = async () => {
    try {
      const res = await fetch(
        `${BASIC_URL}/api/public/join`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ loginId, userPw: password, email, nickname }),
        }
      );
      const json = await res.json() as { status: string; message: string };
      if (json.status === 'success') {
        Alert.alert(
          '성공',
          json.message,
          [{ text: '확인', onPress: () => navigation.replace('SignIn') }],
          { cancelable: false }
        );
      } else {
        Alert.alert('실패', json.message || '회원가입에 실패했습니다');
      }
    } catch {
      Alert.alert('오류', '네트워크 오류가 발생했습니다');
    }
  };

  // ✅ FINAL CHECK: All validation logic
  const allFieldsValid =
    !!loginId &&
    loginIdAvailable === true &&
    !!password &&
    isValidPassword(password, loginId) &&
    password === confirmPassword &&
    !!email &&
    isValidEmailFormat(email) &&
    emailAvailable === true &&
    isEmailVerified &&
    !!nickname &&
    nicknameAvailable === true &&
    agreePersonal &&
    agreeTerms;

  // Debug log for allFieldsValid
  console.log('🔍 allFieldsValid calculation:', {
    loginId: !!loginId,
    loginIdAvailable: loginIdAvailable === true,
    password: !!password,
    isValidPassword: isValidPassword(password, loginId),
    passwordMatch: password === confirmPassword,
    email: !!email,
    isValidEmailFormat: isValidEmailFormat(email),
    emailAvailable: emailAvailable === true,
    isEmailVerified,
    nickname: !!nickname,
    nicknameAvailable: nicknameAvailable === true,
    agreePersonal,
    agreeTerms,
    finalResult: allFieldsValid,
  });

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <View style={styles.logoContainer}>
        <Image
          source={require('../../../assets/images/logo.png')}
          style={styles.logo}
        />
      </View>

      <View style={styles.formContainer}>
        <Text style={styles.title}>회원가입</Text>

        {/* 아이디 입력 */}
        <View style={styles.inputGroup}>
          <Text style={styles.label}>아이디</Text>
          <TextInput
            style={[styles.input, loginIdError ? styles.inputError : null]}
            value={loginId}
            onChangeText={handleLoginIdChange}
            placeholder="아이디를 입력하세요 (4자 이상)"
            autoCapitalize="none"
          />
          {loginIdError ? <Text style={styles.errorText}>{loginIdError}</Text> : null}
          {loginIdAvailable === true && !loginIdError ? (
            <Text style={styles.successText}>사용 가능한 아이디입니다</Text>
          ) : null}
        </View>

        {/* 비밀번호 입력 */}
        <View style={styles.inputGroup}>
          <Text style={styles.label}>비밀번호</Text>
          <TextInput
            style={[styles.input, passwordError ? styles.inputError : null]}
            value={password}
            onChangeText={handlePasswordChange}
            placeholder="비밀번호를 입력하세요"
            secureTextEntry
          />
          {passwordError ? <Text style={styles.errorText}>{passwordError}</Text> : null}
        </View>

        {/* 비밀번호 확인 */}
        <View style={styles.inputGroup}>
          <Text style={styles.label}>비밀번호 확인</Text>
          <TextInput
            style={[styles.input, passwordError && confirmPassword ? styles.inputError : null]}
            value={confirmPassword}
            onChangeText={handleConfirmPasswordChange}
            placeholder="비밀번호를 다시 입력하세요"
            secureTextEntry
          />
        </View>

        {/* 이메일 입력 */}
        <View style={styles.inputGroup}>
          <Text style={styles.label}>이메일</Text>
          <View style={styles.emailContainer}>
            <TextInput
              style={[styles.emailInput, emailError ? styles.inputError : null]}
              value={email}
              onChangeText={handleEmailChange}
              placeholder="이메일을 입력하세요"
              keyboardType="email-address"
              autoCapitalize="none"
            />
            <TouchableOpacity
              style={[
                styles.verifyButton,
                (!email || !isValidEmailFormat(email))
                  ? styles.verifyButtonDisabled
                  : null
              ]}
              onPress={handleSendEmailCode}
              disabled={!email || !isValidEmailFormat(email)}
            >
              <Text style={[styles.verifyButtonText, (!email || !isValidEmailFormat(email) || emailAvailable === false) ? styles.verifyButtonTextDisabled : null]}>인증번호 발송</Text>
            </TouchableOpacity>
          </View>
          {emailError ? <Text style={styles.errorText}>{emailError}</Text> : null}
          {emailAvailable === true && !emailError ? (
            <Text style={styles.successText}>사용 가능한 이메일입니다</Text>
          ) : null}
        </View>

        {/* 이메일 인증번호 입력 */}
        <View style={styles.inputGroup}>
          <Text style={styles.label}>이메일 인증번호</Text>
          <View style={styles.emailContainer}>
            <TextInput
              style={styles.emailInput}
              value={emailCode}
              onChangeText={setEmailCode}
              placeholder="인증번호를 입력하세요"
              keyboardType="number-pad"
            />
            <TouchableOpacity
              activeOpacity={0.8}
              style={[styles.verifyButton, !emailCode ? styles.verifyButtonDisabled : null]}
              onPress={handleConfirmEmailCode}
              disabled={!emailCode}
            >
              <Text style={styles.verifyButtonText}>인증 확인</Text>
            </TouchableOpacity>
          </View>
          {isEmailVerified ? (
            <Text style={styles.successText}>이메일 인증이 완료되었습니다</Text>
          ) : null}
        </View>

        {/* 닉네임 입력 */}
        <View style={styles.inputGroup}>
          <Text style={styles.label}>닉네임</Text>
          <TextInput
            style={[styles.input, nicknameError ? styles.inputError : null]}
            value={nickname}
            onChangeText={handleNicknameChange}
            placeholder="닉네임을 입력하세요"
          />
          {nicknameError ? <Text style={styles.errorText}>{nicknameError}</Text> : null}
          {nicknameAvailable === true && !nicknameError ? (
            <Text style={styles.successText}>사용 가능한 닉네임입니다</Text>
          ) : null}
        </View>

        {/* 이용약관 동의 */}
        <View style={styles.agreementContainer}>
          <TouchableOpacity
            style={styles.agreementRow}
            onPress={() => setAgreePersonal(!agreePersonal)}
          >
            <View style={[styles.checkbox, agreePersonal ? styles.checkboxChecked : null]}>
              {agreePersonal ? <Text style={styles.checkmark}>✓</Text> : null}
            </View>
            <Text style={styles.agreementText}>개인정보 처리방침에 동의합니다</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={styles.agreementRow}
            onPress={() => setAgreeTerms(!agreeTerms)}
          >
            <View style={[styles.checkbox, agreeTerms ? styles.checkboxChecked : null]}>
              {agreeTerms ? <Text style={styles.checkmark}>✓</Text> : null}
            </View>
            <Text style={styles.agreementText}>이용약관에 동의합니다</Text>
          </TouchableOpacity>
        </View>

        {/* 회원가입 버튼 */}
        <TouchableOpacity
          style={[styles.signupButton, allFieldsValid ? null : styles.signupButtonDisabled]}
          onPress={handleSignup}
          disabled={!allFieldsValid}
        >
          <Text style={styles.signupButtonText}>회원가입</Text>
        </TouchableOpacity>

        {/* 로그인 페이지로 이동 */}
        <TouchableOpacity
          style={styles.loginLinkContainer}
          onPress={() => navigation.navigate('SignIn')}
        >
          <Text style={styles.loginLinkText}>이미 계정이 있으신가요? 로그인</Text>
        </TouchableOpacity>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flexGrow: 1,
    backgroundColor: '#f5f5f5',
    paddingVertical: 20,
  },
  logoContainer: {
    alignItems: 'center',
    marginBottom: 30,
  },
  logo: {
    width: 120,
    height: 120,
    resizeMode: 'contain',
  },
  formContainer: {
    paddingHorizontal: 30,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    textAlign: 'center',
    marginBottom: 30,
    color: '#333',
  },
  inputGroup: {
    marginBottom: 20,
  },
  label: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 8,
    color: '#333',
  },
  input: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    paddingHorizontal: 15,
    paddingVertical: 12,
    fontSize: 16,
    backgroundColor: '#fff',
  },
  inputError: {
    borderColor: '#ff4757',
  },
  emailContainer: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  emailInput: {
    flex: 1,
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    paddingHorizontal: 15,
    paddingVertical: 12,
    fontSize: 16,
    backgroundColor: '#fff',
    marginRight: 10,
  },
  verifyButton: {
    backgroundColor: '#007bff',
    paddingHorizontal: 15,
    paddingVertical: 12,
    borderRadius: 8,
  },
  verifyButtonDisabled: {
    backgroundColor: '#ccc',
    opacity: 1, // keep visible even when disabled
  },
  verifyButtonText: {
    color: '#fff',
    fontSize: 14,
    fontWeight: '600',
  },
  verifyButtonTextDisabled: {
    color: '#666',
  },
  errorText: {
    fontSize: 14,
    color: '#ff4757',
    marginTop: 5,
  },
  successText: {
    fontSize: 14,
    color: '#2ed573',
    marginTop: 5,
  },
  agreementContainer: {
    marginVertical: 20,
  },
  agreementRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 15,
  },
  checkbox: {
    width: 20,
    height: 20,
    borderWidth: 2,
    borderColor: '#ddd',
    borderRadius: 4,
    marginRight: 10,
    alignItems: 'center',
    justifyContent: 'center',
  },
  checkboxChecked: {
    backgroundColor: '#007bff',
    borderColor: '#007bff',
  },
  checkmark: {
    color: '#fff',
    fontSize: 12,
    fontWeight: 'bold',
  },
  agreementText: {
    fontSize: 14,
    color: '#666',
  },
  signupButton: {
    backgroundColor: '#007bff',
    paddingVertical: 15,
    borderRadius: 8,
    marginVertical: 20,
  },
  signupButtonDisabled: {
    backgroundColor: '#ccc',
  },
  signupButtonText: {
    color: '#fff',
    fontSize: 18,
    fontWeight: 'bold',
    textAlign: 'center',
  },
  loginLinkContainer: {
    alignItems: 'center',
    marginTop: 10,
  },
  loginLinkText: {
    fontSize: 16,
    color: '#007bff',
    textDecorationLine: 'underline',
  },
});
