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

// AuthStackParamList에 맞춰 'SignIn'으로 설정
type Props = NativeStackScreenProps<AuthStackParamList, 'SignIn'>;

export default function SignInScreen({ navigation }: Props) {
  const [userId, setUserId] = useState('');
  const [password, setPassword] = useState('');
  const { signIn, isAuthLoading } = useContext(AuthContext);

  const handleLogin = async () => {
    // 입력 검증
    if (!userId) {
      return Alert.alert('에러', '아이디를 입력하세요');
    }
    if (userId.length < 4) {
      return Alert.alert('에러', '아이디는 최소 4자 이상이어야 합니다');
    }
    if (!password) {
      return Alert.alert('에러', '비밀번호를 입력하세요');
    }
    if (password.length < 8) {
      return Alert.alert('에러', '비밀번호는 최소 8자 이상이어야 합니다');
    }

    try {
      // Context signIn 호출 (내부에서 API 요청, Keychain 저장 등 처리)
      await signIn(userId, password);
    } catch (error: any) {
      Alert.alert('로그인 실패', error.message || '알 수 없는 오류가 발생했습니다');
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
          placeholder="아이디"
          placeholderTextColor="#999"
          autoCapitalize="none"
          value={userId}
          onChangeText={setUserId}
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
