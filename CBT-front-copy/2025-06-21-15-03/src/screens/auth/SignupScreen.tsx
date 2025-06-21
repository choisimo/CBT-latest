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

export default function SignupScreen({ navigation }: Props) {
  const [userId, setUserId] = useState('');
  const [userIdError, setUserIdError] = useState('');
  const [userIdAvailable, setUserIdAvailable] = useState<boolean | null>(null);
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [email, setEmail] = useState('');
  const [emailAvailable, setEmailAvailable] = useState<boolean | null>(null);
  const [nickname, setNickname] = useState('');
  const [nicknameAvailable, setNicknameAvailable] = useState<boolean | null>(null);
  const [agreePersonal, setAgreePersonal] = useState(false);
  const [agreeTerms, setAgreeTerms] = useState(false);

  const [emailError, setEmailError] = useState('');
  const [passwordError, setPasswordError] = useState('');

  // Debounced API checks
  const debouncedCheckUserId = useRef(
    debounce(async (value: string) => {
      if (value.length < 4) {
        setUserIdAvailable(null);
        return;
      }
      try {
        const res = await fetch(`${BASIC_URL}/api/public/check/userId/IsDuplicate`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ userId: value }),
        });
        const { isDuplicate } = await res.json();
        setUserIdAvailable(!isDuplicate);
      } catch {
        // ignore
      }
    }, 300)
  ).current;

  const debouncedCheckEmail = useRef(
    debounce(async (value: string) => {
      if (!value) return;
      try {
        const res = await fetch(`${BASIC_URL}/api/public/check/email/IsDuplicate`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ email: value }),
        });
        const { isDuplicate } = await res.json();
        setEmailAvailable(!isDuplicate);
      } catch {
        // ignore
      }
    }, 300)
  ).current;

  const debouncedCheckNickname = useRef(
    debounce(async (value: string) => {
      if (!value) return;
      try {
        const res = await fetch(`${BASIC_URL}/api/public/check/nickname/IsDuplicate`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ nickname: value }),
        });
        const { isDuplicate } = await res.json();
        setNicknameAvailable(!isDuplicate);
      } catch {
        // ignore
      }
    }, 300)
  ).current;

  const isValidEmailFormat = (e: string) => /^\S+@\S+\.\S+$/.test(e);
  const isValidPassword = (pw: string) => {
    if (pw.length < 8 || pw.length > 50) return false;
    const types = [/[A-Z]/, /[a-z]/, /\d/, /[^A-Za-z0-9]/];
    if (types.filter(rx => rx.test(pw)).length < 3) return false;
    if (/(.)\1\1/.test(pw)) return false;
    if (userId && pw.includes(userId)) return false;
    return true;
  };

  const handleUserIdChange = (text: string) => {
    setUserId(text);
    setUserIdError(
      text.length > 0 && text.length < 4
        ? '아이디는 4자 이상이어야 합니다.'
        : ''
    );
    setUserIdAvailable(null);
    debouncedCheckUserId(text);
  };

  const handleEmailChange = (text: string) => {
    setEmail(text);
    setEmailAvailable(null);
    if (text && !isValidEmailFormat(text)) {
      setEmailError('올바른 이메일 형식을 입력해주세요.');
    } else {
      setEmailError('');
      debouncedCheckEmail(text);
    }
  };

  const handlePasswordChange = (text: string) => {
    setPassword(text);
    // 패스워드 유효성 검사
    if (text && !isValidPassword(text)) {
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
    debouncedCheckNickname(text);
  };

  const handleSignup = async () => {
    if (userIdAvailable !== true) {
      Alert.alert('아이디 오류', '아이디 중복 확인을 해주세요.');
      return;
    }
    if (emailAvailable !== true) {
      Alert.alert('이메일 오류', '이메일 중복 확인을 해주세요.');
      return;
    }
    if (nicknameAvailable !== true) {
      Alert.alert('닉네임 오류', '닉네임 중복 확인을 해주세요.');
      return;
    }

    try {
      const res = await fetch(
        `${BASIC_URL}/api/public/join`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ userId, userPw: password, email, nickname }),
        }
      );
      if (res.ok) {
        Alert.alert('성공', '회원가입이 완료되었습니다');
        navigation.replace('SignIn');
      } else {
        const { message } = await res.json();
        Alert.alert('실패', message || '회원가입에 실패했습니다');
      }
    } catch {
      Alert.alert('오류', '네트워크 오류가 발생했습니다');
    }
  };

  const allFieldsValid =
    !!userId &&
    userIdAvailable === true &&
    !!email &&
    emailAvailable === true &&
    isValidPassword(password) &&
    password === confirmPassword &&
    !!nickname &&
    nicknameAvailable === true &&
    agreePersonal &&
    agreeTerms;

  return (
    <View style={styles.container}>
      <ScrollView contentContainerStyle={styles.centerBox} keyboardShouldPersistTaps="handled">
        <Image
          source={require('../../../assets/images/logo.png')}
          style={styles.logo}
          resizeMode="cover"
        />
        <TextInput
          style={[styles.input, { marginBottom: 16 }]} 
          placeholder="아이디"
          placeholderTextColor="#999"
          value={userId}
          onChangeText={handleUserIdChange}
        />
        {userIdError ? <Text style={styles.errorText}>{userIdError}</Text> : null}
        <TextInput
          style={[styles.input, { marginBottom: 16 }]} 
          placeholder="이메일"
          placeholderTextColor="#999"
          keyboardType="email-address"
          autoCapitalize="none"
          value={email}
          onChangeText={handleEmailChange}
        />
        {emailError ? <Text style={styles.errorText}>{emailError}</Text> : null}
        <TextInput
          style={[styles.input, { marginBottom: 16 }]} 
          placeholder="닉네임"
          placeholderTextColor="#999"
          value={nickname}
          onChangeText={handleNicknameChange}
        />
                <TextInput
          style={styles.input}
          placeholder="비밀번호"
          placeholderTextColor="#999"
          secureTextEntry
          value={password}
          onChangeText={handlePasswordChange}
        />
        <TextInput
          style={styles.input}
          placeholder="비밀번호 확인"
          placeholderTextColor="#999"
          secureTextEntry
          value={confirmPassword}
          onChangeText={handleConfirmPasswordChange}
        />
        {passwordError ? <Text style={styles.errorText}>{passwordError}</Text> : null}
      </ScrollView>

      <View style={styles.agreeContainer}>
        <TouchableOpacity style={styles.agreeRow} onPress={() => setAgreePersonal(!agreePersonal)}>
          <Text style={styles.checkbox}>{agreePersonal ? '✅' : '⬜'}</Text>
          <Text style={styles.agreeText}>[필수] 개인정보 수집 및 이용</Text>
        </TouchableOpacity>
        <TouchableOpacity 
        style={[styles.agreeRow, {marginBottom: 4}]}
         onPress={() => setAgreeTerms(!agreeTerms)}>
          <Text style={styles.checkbox}>{agreeTerms ? '✅' : '⬜'}</Text>
          <Text style={styles.agreeText}>[필수] 서비스 이용 약관</Text>
        </TouchableOpacity>
      </View>

      <TouchableOpacity
        style={[styles.button, { opacity: allFieldsValid ? 1 : 0.5 }]}
        onPress={handleSignup}
        disabled={!allFieldsValid}
      >
        <Text style={styles.buttonText}>회원가입</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },

  centerBox: {
    flexGrow: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 20,
  },

  logo: {
    width: 180,
    height: 90,
    alignSelf: 'center',
    marginBottom: 20,
  },

  input: {
    width: '90%',
    borderWidth: 1,
    borderColor: '#ccc',
    padding: 12,
    borderRadius: 8,
    marginBottom: 8,
    color: '#000',
  },

  errorText: {
    width: '90%',
    color: 'red',
    marginBottom: 8,
    marginLeft: '5%',
  },

  agreeContainer: {
    width: '90%',
    paddingHorizontal: '5%',
    marginBottom: 20,
  },

  agreeRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 12,
  },

  checkbox: {
    fontSize: 20,
    marginRight: 8,
  },

  agreeText: {
    fontSize: 16,
    color: '#333',
  },

  button: {
    width: '90%',
    alignSelf: 'center',
    backgroundColor: '#C4B5FD',
    paddingVertical: 10,
    borderRadius: 8,
    alignItems: 'center',
    marginBottom: 52,
  },

  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
});
