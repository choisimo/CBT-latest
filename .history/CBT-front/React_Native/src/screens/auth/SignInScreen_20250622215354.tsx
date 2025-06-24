// src/screens/auth/SignInScreen.tsx
import React, { useState, useContext } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  Alert,
  ActivityIndicator,
  Image,
} from 'react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { AuthStackParamList } from '../../navigation/AuthStack';
import { AuthContext } from '../../context/AuthContext';
import { isValidEmail, isValidLoginId, isValidPassword, getLoginErrorMessage } from '../../utils/validation';

// AuthStackParamList에 맞춰 'SignIn'으로 설정
type Props = NativeStackScreenProps<AuthStackParamList, 'SignIn'>;

export default function SignInScreen({ navigation }: Props) {
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const { signIn, isAuthLoading } = useContext(AuthContext);

  const handleLogin = async () => {
    // 매우 엄격한 입력 검증
    console.log('handleLogin 시작 - identifier:', JSON.stringify(identifier), 'password length:', password?.length);
    
    if (!identifier || typeof identifier !== 'string' || identifier.trim() === '') {
      console.error('handleLogin: 식별자가 비어있음');
      return Alert.alert('에러', '이메일 또는 아이디를 입력하세요');
    }
    
    if (!password || typeof password !== 'string' || password.trim() === '') {
      console.error('handleLogin: 비밀번호가 비어있음');
      return Alert.alert('에러', '비밀번호를 입력하세요');
    }
    
    // 이메일 형식 검증 (indexOf를 사용하여 ES5 호환성 확보)
    const isEmail = identifier.indexOf('@') !== -1;
    if (isEmail) {
      if (!isValidEmail(identifier)) {
        console.error('handleLogin: 유효하지 않은 이메일:', identifier);
        return Alert.alert('에러', '올바른 이메일 형식을 입력하세요');
      }
    } else {
      if (!isValidLoginId(identifier)) {
        console.error('handleLogin: 유효하지 않은 로그인ID:', identifier);
        return Alert.alert('에러', '아이디는 최소 4자 이상이어야 합니다');
      }
    }
    
    if (!isValidPassword(password)) {
      console.error('handleLogin: 유효하지 않은 비밀번호');
      return Alert.alert('에러', '비밀번호는 최소 8자 이상이어야 합니다');
    }

    console.log('handleLogin: 모든 검증 통과, signIn 호출');

    try {
      // Context signIn 호출 (내부에서 API 요청, Keychain 저장 등 처리)
      await signIn(identifier, password);
    } catch (error: any) {
      const errorMessage = getLoginErrorMessage(error);
      console.error('handleLogin 에러:', error);
      Alert.alert('로그인 실패', errorMessage);
    }
  };

  const handleSignup = () => {
    navigation.navigate('SignUp');
  };

  return (
    <View style={styles.container}>
      {/* 중앙 콘텐츠 */}
      <View style={styles.centerContainer}>
        <Image
          source={require('../../../assets/images/logo.png')}
          style={styles.logo}
          resizeMode="cover"
        />

        <TextInput
          style={styles.input}
          placeholder="이메일 또는 아이디"
          placeholderTextColor="#999"
          autoCapitalize="none"
          keyboardType={identifier.indexOf('@') !== -1 ? 'email-address' : 'default'}
          value={identifier}
          onChangeText={setIdentifier}
        />

        <TextInput
          style={styles.input}
          placeholder="비밀번호"
          placeholderTextColor="#999"
          secureTextEntry
          value={password}
          onChangeText={setPassword}
        />

        <TouchableOpacity
          style={styles.button}
          onPress={handleLogin}
          disabled={isAuthLoading}
        >
          {isAuthLoading ? (
            <ActivityIndicator color="#fff" />
          ) : (
            <Text style={styles.buttonText}>로그인</Text>
          )}
        </TouchableOpacity>
      </View>

      <TouchableOpacity
        style={[styles.button, styles.signupButton]}
        onPress={handleSignup}
        disabled={isAuthLoading}
      >
        <Text style={styles.buttonText}>회원가입</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    paddingVertical: 40,
    backgroundColor: '#fff',
  },

  centerContainer: {
    flexGrow: 1,
    justifyContent: 'center',
  },

  logo: {
    width: 180,
    height: 90,
    alignSelf: 'center',
    marginBottom: 20,
  },

  input: {
    borderWidth: 1,
    borderColor: '#ccc',
    padding: 12,
    borderRadius: 8,
    marginBottom: 16,
    color: '#000',
  },

  button: {
    backgroundColor: '#00B8B0',
    paddingVertical: 10,
    borderRadius: 8,
    alignItems: 'center',
    marginBottom: 12,
  },

  signupButton: {
    backgroundColor: '#C4B5FD',
  },

  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
});
