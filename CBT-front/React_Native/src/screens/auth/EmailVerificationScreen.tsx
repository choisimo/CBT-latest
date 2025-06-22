// src/screens/auth/EmailVerificationScreen.tsx
import React, { useState, useContext, useEffect } from 'react';
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
import { BASIC_URL } from '../../constants/api';

type Props = NativeStackScreenProps<AuthStackParamList, 'VerifyEmail'>;

export default function EmailVerificationScreen({ navigation }: Props) {
  const { refreshUser, fetchWithAuth, user } = useContext(AuthContext);
  const [code, setCode] = useState('');
  const [loading, setLoading] = useState(false);
  const [sending, setSending] = useState(false);
  const [resendTimer, setResendTimer] = useState(0);
  const [infoMessage, setInfoMessage] = useState('');

  // Countdown timer for resend
  useEffect(() => {
    let timer: ReturnType<typeof setTimeout>;
    if (resendTimer > 0) {
      timer = setTimeout(() => setResendTimer(resendTimer - 1), 1000);
    }
    return () => clearTimeout(timer);
  }, [resendTimer]);

  const handleSendCode = async () => {
    if (!user?.email) {
      return Alert.alert('에러', '이메일 정보가 없습니다.');
    }

    setSending(true);
    setInfoMessage('메일을 발송 중입니다…');
    try {
      const res = await fetch(`${BASIC_URL}/api/public/emailSend`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: user.email }),
      });
      const json = await res.json() as {
        status: 'success' | 'error';
        message: string;
        data: { message?: string; title?: string; code?: string } | null;
      };
      if (json.status === 'success' && json.data?.message) {
        // 성공: data.message 로 UI 업데이트
        setInfoMessage(json.data.message);
        setResendTimer(60);
        Alert.alert('알림', json.data.message);
      } else {
        // 실패: error 메시지 (data.title 우선, 없으면 message)
        const errMsg = json.data?.title || json.message || '이메일 발송에 실패했습니다.';
        setInfoMessage('');
        Alert.alert('실패', errMsg);
      }
    } catch (err: any) {
      // 네트워크/예외
      setInfoMessage('');
      Alert.alert('오류', err.message || '네트워크 오류가 발생했습니다.');
    } finally {
      setSending(false);
    }
  };

  const handleVerify = async () => {
    if (!code.trim()) {
      return Alert.alert('에러', '인증 코드를 입력해주세요.');
    }

    setLoading(true);
    try {
      const res = await fetchWithAuth(
        `${BASIC_URL}/api/public/emailCheck`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ email: user?.email, code: code.trim() }),
        }
      );
      const json = (await res.json()) as {
        status: 'success' | 'error';
        message: string;
        data: {
          message?: string;
          title?: string;
        } | null;
      };
      if (!res.ok) {
        const { message } = await res.json();
        throw new Error(message || '인증 실패');
      }

      await refreshUser();
      Alert.alert('인증 성공', '이메일 인증이 완료되었습니다.');
      navigation.navigate('SignIn');
    } catch (err: any) {
      Alert.alert('오류', err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <Image
        source={require('../../../assets/images/logo.png')}
        style={styles.logo}
        resizeMode="cover"
      />
      <Text style={styles.title}>이메일 인증</Text>
      <Text style={styles.subtitle}>이메일로 발송된 인증 코드를 입력해주세요.</Text>

      <TouchableOpacity
        style={[
          styles.sendCodeButton,
          resendTimer > 0 && styles.sendDisabled,
        ]}
        onPress={handleSendCode}
        disabled={sending || resendTimer > 0}
      >
        {sending ? (
          <ActivityIndicator color="#fff" />
        ) : (
          <Text style={styles.buttonText}>
            {resendTimer > 0 ? `재전송 (${resendTimer}s)` : '인증 코드 발송'}
          </Text>
        )}
      </TouchableOpacity>
      {infoMessage ? <Text style={styles.infoText}>{infoMessage}</Text> : null}

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
  logo: {
    width: 180,
    height: 90,
    alignSelf: 'center',
    marginBottom: 20,
  },
  input: {
    borderWidth: 1,
    borderColor: '#ccc',
    borderRadius: 8,
    padding: 12,
    marginBottom: 16,
    color: '#000',
  },
  sendCodeButton: {
    backgroundColor: '#00B8B0',
    paddingVertical: 12,
    borderRadius: 8,
    alignItems: 'center',
    marginBottom: 20,
  },
  sendDisabled: {
    backgroundColor: '#6C757D',
  },
  infoText: {
    textAlign: 'center',
    color: '#555',
    marginVertical: 10,
    fontSize: 14,
  },
  button: {
    backgroundColor: '#6C757D',
    paddingVertical: 12,
    borderRadius: 8,
    alignItems: 'center',
  },
  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
});