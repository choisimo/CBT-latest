// src/screens/auth/EmailVerificationScreen.tsx
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
import { BASIC_URL } from '../../constants/api';

// AuthStackParamList에 맞춰 'VerifyEmail'으로 설정
type Props = NativeStackScreenProps<AuthStackParamList, 'VerifyEmail'>;

export default function EmailVerificationScreen({ navigation }: Props) {
  const [code, setCode] = useState('');
  const { refreshUser, fetchWithAuth } = useContext(AuthContext);
  const [loading, setLoading] = useState(false);

  const handleVerify = async () => {
    if (!code.trim()) {
      return Alert.alert('에러', '인증 코드를 입력해주세요.');
    }

    setLoading(true);
    try {
      const res = await fetchWithAuth(
        `https://${BASIC_URL}/api/auth/verify-email`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ code: code.trim() }),
        }
      );

      if (!res.ok) {
        const { message } = await res.json();
        throw new Error(message || '인증 실패');
      }

      // 이메일 인증 성공!
      await refreshUser(); 
      Alert.alert('인증 성공', '이메일 인증이 완료되었습니다.');
    } catch (err: any) {
      Alert.alert('오류', err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>이메일 인증</Text>
      <Text style={styles.subtitle}>
        이메일로 발송된 인증 코드를 입력해주세요.
      </Text>
      <TextInput
        style={styles.input}
        placeholder="인증 코드"
        placeholderTextColor="#999"
        value={code}
        onChangeText={setCode}
        keyboardType="number-pad"
      />
      <TouchableOpacity
        style={styles.button}
        onPress={handleVerify}
        disabled={loading}
      >
        {loading ? (
          <ActivityIndicator color="#fff" />
        ) : (
          <Text style={styles.buttonText}>확인</Text>
        )}
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
    padding: 20,
    justifyContent: 'center',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    textAlign: 'center',
    marginBottom: 12,
  },
  subtitle: {
    fontSize: 16,
    textAlign: 'center',
    marginBottom: 20,
    color: '#555',
  },
  input: {
    borderWidth: 1,
    borderColor: '#ccc',
    borderRadius: 8,
    padding: 12,
    marginBottom: 16,
    color: '#000',
  },
  button: {
    backgroundColor: '#4A90E2',
    paddingVertical: 15,
    borderRadius: 8,
    alignItems: 'center',
  },
  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
});
