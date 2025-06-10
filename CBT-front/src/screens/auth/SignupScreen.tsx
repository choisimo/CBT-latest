// src/screens/auth/SignupScreen.tsx
import React, { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  StyleSheet,
  TouchableOpacity,
  Modal,
  Pressable,
  Alert,
  ScrollView,
} from 'react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { AuthStackParamList } from '../../navigation/AuthStack';
import { BASIC_URL } from '../../constants/api';

// AuthStackParamList에 맞춰 'SignUp'으로 설정
type Props = NativeStackScreenProps<AuthStackParamList, 'SignUp'>;

export default function SignupScreen({ navigation }: Props) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [userName, setUserName] = useState('');
  const [nickname, setNickname] = useState('');
  const [modalVisible, setModalVisible] = useState(true);
  const [agreePersonal, setAgreePersonal] = useState(false);
  const [agreeTerms, setAgreeTerms] = useState(false);

  const isValidEmail = (e: string) => /^\S+@\S+\.\S+$/.test(e);
  const isValidPassword = (pw: string) => {
    if (pw.length < 8 || pw.length > 50) return false;
    const typesMatched = [/[A-Z]/, /[a-z]/, /\d/, /[^A-Za-z0-9]/].filter(rx => rx.test(pw));
    if (typesMatched.length < 3) return false;
    if (/(.)\1\1/.test(pw)) return false;
    if (email && pw.includes(email)) return false;
    return true;
  };
  const isValidName = (n: string) => n.length >= 2 && n.length <= 20;
  const isValidNickname = (n: string) => n.length >= 2 && n.length <= 15;

  const handleSignup = async () => {
    if (!isValidEmail(email)) {
      Alert.alert('이메일 형식 오류', '유효한 이메일 주소를 입력해주세요.');
      return;
    }
    if (!isValidPassword(password)) {
      Alert.alert(
        '비밀번호 오류',
        '비밀번호는 8~50자, 영문 대소문자/숫자/특수문자 중 3종류 이상을 포함해야 합니다.'
      );
      return;
    }
    if (!isValidName(userName)) {
      Alert.alert('이름 오류', '이름은 2~20자 사이여야 합니다.');
      return;
    }
    if (!isValidNickname(nickname)) {
      Alert.alert('닉네임 오류', '닉네임은 2~15자 사이여야 합니다.');
      return;
    }
    if (!agreePersonal || !agreeTerms) {
      Alert.alert('약관 동의', '필수 약관에 모두 동의해주세요.');
      return;
    }

    try {
      const res = await fetch(`${BASIC_URL}/api/public/join`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          email,
          userPw: password,
          userName,
          nickname,
        }),
      });
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
    isValidEmail(email) &&
    isValidPassword(password) &&
    isValidName(userName) &&
    isValidNickname(nickname) &&
    agreePersonal &&
    agreeTerms;

  return (
    <View style={styles.container}>
      <ScrollView contentContainerStyle={styles.centerBox} keyboardShouldPersistTaps="handled">
        <Text style={styles.title}>회원가입</Text>

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
        <TextInput
          style={styles.input}
          placeholder="실명"
          placeholderTextColor="#999"
          value={userName}
          onChangeText={setUserName}
        />
        <TextInput
          style={styles.input}
          placeholder="닉네임"
          placeholderTextColor="#999"
          value={nickname}
          onChangeText={setNickname}
        />

        <TouchableOpacity
          style={[styles.button, { opacity: allFieldsValid ? 1 : 0.5 }]}
          onPress={handleSignup}
          disabled={!allFieldsValid}
        >
          <Text style={styles.buttonText}>회원가입</Text>
        </TouchableOpacity>
      </ScrollView>

      <Modal visible={modalVisible} transparent animationType="fade">
        <View style={styles.modalBackdrop}>
          <View style={styles.modalBox}>
            <Text style={styles.modalTitle}>약관 동의</Text>
            <ScrollView style={styles.scroll}>
              <TouchableOpacity
                style={styles.agreeRow}
                onPress={() => setAgreePersonal(!agreePersonal)}
              >
                <Text style={styles.checkbox}>{agreePersonal ? '✅' : '⬜'}</Text>
                <Text style={styles.agreeText}>[필수] 개인정보 수집 및 이용</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={styles.agreeRow}
                onPress={() => setAgreeTerms(!agreeTerms)}
              >
                <Text style={styles.checkbox}>{agreeTerms ? '✅' : '⬜'}</Text>
                <Text style={styles.agreeText}>[필수] 서비스 이용 약관</Text>
              </TouchableOpacity>
            </ScrollView>
            <Pressable
              style={[styles.closeButton, { opacity: agreePersonal && agreeTerms ? 1 : 0.3 }]}
              onPress={() => agreePersonal && agreeTerms && setModalVisible(false)}
              disabled={!(agreePersonal && agreeTerms)}
            >
              <Text style={styles.closeButtonText}>동의하고 닫기</Text>
            </Pressable>
          </View>
        </View>
      </Modal>
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
  },
  title: {
    fontSize: 28,
    fontWeight: 'bold',
    marginBottom: 24,
    textAlign: 'center',
  },
  input: {
    width: '90%',
    borderWidth: 1,
    borderColor: '#ccc',
    padding: 12,
    borderRadius: 8,
    marginBottom: 12,
    color: '#000'
  },
  button: {
    width: '90%',
    backgroundColor: '#4A90E2',
    paddingVertical: 15,
    borderRadius: 8,
    alignItems: 'center',
    marginTop: 8,
  },
  buttonText: { color: '#fff', fontSize: 16, fontWeight: '600' },
  modalBackdrop: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.4)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  modalBox: {
    width: '90%',
    backgroundColor: 'white',
    borderRadius: 10,
    padding: 24,
    elevation: 4,
  },
  modalTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    marginBottom: 16,
    textAlign: 'center',
  },
  scroll: { maxHeight: 200, marginBottom: 20 },
  agreeRow: { flexDirection: 'row', alignItems: 'center', marginBottom: 12 },
  checkbox: { fontSize: 20, marginRight: 8 },
  agreeText: { fontSize: 16, color: '#333' },
  closeButton: {
    backgroundColor: '#4A90E2',
    paddingVertical: 12,
    borderRadius: 8,
    alignItems: 'center',
  },
  closeButtonText: { color: '#fff', fontSize: 16, fontWeight: '600' },
});
