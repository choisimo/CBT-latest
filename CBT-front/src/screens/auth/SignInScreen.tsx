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
} from 'react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { AuthStackParamList } from '../../navigation/AuthStack';
import { AuthContext } from '../../context/AuthContext';

// AuthStackParamList에 맞춰 'SignIn'으로 설정
type Props = NativeStackScreenProps<AuthStackParamList, 'SignIn'>;

export default function SignInScreen({ navigation }: Props) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const { signIn, isAuthLoading } = useContext(AuthContext);

  const handleLogin = async () => {
    // 입력 검증
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!email) {
      return Alert.alert('에러', '이메일을 입력하세요');
    }
    if (!emailRegex.test(email)) {
      return Alert.alert('에러', '유효한 이메일을 입력하세요');
    }
    if (!password) {
      return Alert.alert('에러', '비밀번호를 입력하세요');
    }
    if (password.length < 6) {
      return Alert.alert('에러', '비밀번호는 최소 6자 이상이어야 합니다');
    }

    try {
      // Context signIn 호출 (내부에서 API 요청, Keychain 저장 등 처리)
      await signIn(email, password);
      // RootNavigator가 자동으로 AppStack으로 전환합니다.
    } catch (error: any) {
      Alert.alert('로그인 실패', error.message || '알 수 없는 오류가 발생했습니다');
    }
  };

  const handleSignup = () => {
    navigation.navigate('SignUp');
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>로그인</Text>

      <TextInput
        style={styles.input}
        placeholder="이메일"
        placeholderTextColor="#999"
        keyboardType="email-address"
        autoCapitalize="none"
        value={email}
        onChangeText={setEmail}
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
    padding: 20,
    justifyContent: 'center',
    backgroundColor: '#fff',
  },
  title: {
    fontSize: 28,
    fontWeight: 'bold',
    marginBottom: 32,
    textAlign: 'center',
    color: '#000',
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
    backgroundColor: '#4A90E2',
    paddingVertical: 15,
    borderRadius: 8,
    marginBottom: 12,
    alignItems: 'center',
  },
  signupButton: {
    backgroundColor: '#888',
  },
  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
});
