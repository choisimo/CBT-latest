// src/screens/WriteScreen.tsx

import React, { useState, useContext, useEffect } from 'react';
import {
  View,
  TextInput,
  StyleSheet,
  KeyboardAvoidingView,
  Platform,
  TouchableOpacity,
  Text,
  Alert,
  ActivityIndicator,
  Image,
} from 'react-native';
import DateTimePicker, {
  DateTimePickerEvent,
} from '@react-native-community/datetimepicker';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { AppStackParamList } from '../../navigation/AppStack';
import { AuthContext } from '../../context/AuthContext';
import { BASIC_URL } from '../../constants/api';

type Props = NativeStackScreenProps<AppStackParamList, 'Write'>;

export default function WriteScreen({ route, navigation }: Props) {
  const { fetchWithAuth, userToken } = useContext(AuthContext);

  // route.params.postId가 있으면 “수정 모드”, 없으면 “새 글 작성 모드”
  const postId = (route.params as { postId?: string })?.postId;

  // 1) 날짜 상태 (Date 객체)와 Picker 보임 여부
  const [date, setDate] = useState<Date | undefined>(undefined);
  const [showPicker, setShowPicker] = useState(false);

  // 2) 제목과 내용 상태
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');

  // 3) 로딩 상태 (API 요청 중 혹은 기존 글 불러오는 중)
  const [isLoading, setIsLoading] = useState(false);

  /** ──────────────── “수정 모드”일 때 기존 글 데이터를 불러오기 ──────────────── */
  useEffect(() => {
    if (!postId) {
      // postId가 없으면 새 글 모드이므로 아무것도 안 함
      return;
    }

    const loadExistingPost = async () => {
      if (!userToken) {
        Alert.alert('로그인이 필요합니다.');
        return;
      }
      setIsLoading(true);
      try {
        const res = await fetchWithAuth(
          `${BASIC_URL}/api/diaryposts/${postId}`,
          { method: 'GET' }
        );
        if (!res.ok) {
          if (res.status === 404) {
            Alert.alert('해당 글을 찾을 수 없습니다.');
            navigation.goBack();
            return;
          }
          const errJson = await res.json();
          throw new Error(errJson.message || '서버 에러');
        }
        const data = await res.json();
        /**
         * data 타입 예시 (PostData 인터페이스 형태):
         * {
         *   id: string;
         *   date: '2025-06-01';
         *   title: '기존 제목';
         *   content: '기존 내용';
         *   aiResponse: boolean;
         * }
         */
        // 1) date 문자열을 Date 객체로 변환
        const [y, m, d] = data.date.split('-').map((v: string) => parseInt(v, 10));
        setDate(new Date(y, m - 1, d));

        // 2) 나머지 상태 채우기
        setTitle(data.title);
        setContent(data.content);
      } catch (err: any) {
        console.warn('기존 글 불러오기 오류:', err);
        Alert.alert('불러오기 실패', err.message || '오류가 발생했습니다.');
        navigation.goBack();
      } finally {
        setIsLoading(false);
      }
    };

    loadExistingPost();
  }, [postId, fetchWithAuth, userToken, navigation]);

  /** ──────────────── DateTimePicker용 핸들러 ──────────────── */
  const onPressDate = () => {
    setShowPicker(true);
  };

  const onChangeDate = (event: DateTimePickerEvent, selected?: Date) => {
    setShowPicker(false);
    if (event.type === 'set' && selected) {
      setDate(selected);
    }
  };

  const formatDateString = (d: Date | undefined): string => {
    if (!d) return '';
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  };

  /** ──────────────── 제출 핸들러 (새 글 vs 수정) ──────────────── */
  const handleSubmit = async () => {
    // 1) 유효성 검사
    if (!date) {
      Alert.alert('날짜를 선택하세요.');
      return;
    }
    if (!title.trim()) {
      Alert.alert('제목을 입력하세요.');
      return;
    }
    if (!content.trim()) {
      Alert.alert('내용을 입력하세요.');
      return;
    }
    if (!userToken) {
      Alert.alert('로그인이 필요합니다.');
      return;
    }

    setIsLoading(true);
    try {
      // “수정 모드”일 때: PUT(또는 PATCH) 요청
      if (postId) {
        const res = await fetchWithAuth(
          `${BASIC_URL}/api/diaryposts/${postId}`,
          {
            method: 'PUT', // 백엔드가 PATCH를 받으면 'PATCH'로 수정
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              title: title.trim(),
              content: content.trim(),
            }),
          }
        );
        if (!res.ok) {
          const errJson = await res.json();
          Alert.alert('수정 실패', errJson.message || '서버 에러가 발생했습니다.');
          return;
        }
        // 수정이 성공하면, 상세 보기나 AI 분석 화면으로 돌아가거나 원하는 화면으로 네비게이션
        navigation.goBack();
        return;
      }

      // “새 글 작성 모드”일 때: POST 요청
      const response = await fetchWithAuth(
        `${BASIC_URL}/api/diaries`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            date: formatDateString(date),
            title: title.trim(),
            content: content.trim(),
          }),
        }
      );
      if (!response.ok) {
        const errorData = await response.json();
        Alert.alert('작성 실패', errorData.message || '서버 에러가 발생했습니다.');
        return;
      }

      // 성공 시, 백엔드가 리턴한 postId를 얻어서 분석 화면(또는 상세 보기)으로 이동
      const responseData = await response.json();
      const newPostId = responseData.id;
      navigation.navigate('Analyze', { diaryId: newPostId });
    } catch (err: any) {
      console.warn('글 작성/수정 중 오류:', err);
      Alert.alert('오류', '네트워크 오류가 발생했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  /** ──────────────── 로딩 중에는 가운데에 스피너만 표시 ──────────────── */
  if (isLoading) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" color="#4A90E2" />
      </View>
    );
  }

  /** ──────────────── 실제 입력 폼 ──────────────── */
  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <View style={styles.inputContainer}>
        <Image
          source={require('../../../assets/images/logo.png')}
          style={styles.logo}
          resizeMode="cover"
        />


        {/* 날짜 선택 필드 */}
        <TouchableOpacity onPress={onPressDate} style={styles.dateWrapper}>
          <TextInput
            style={styles.dateInput}
            placeholder="날짜 선택"
            placeholderTextColor="#999"
            value={formatDateString(date)}
            editable={false}
          />
        </TouchableOpacity>

        {showPicker && (
          <DateTimePicker
            value={date || new Date()}
            mode="date"
            display={Platform.OS === 'ios' ? 'spinner' : 'calendar'}
            onChange={onChangeDate}
          />
        )}

        {/* 제목 입력 필드 */}
        <TextInput
          style={[styles.input, styles.titleInput]}
          value={title}
          onChangeText={setTitle}
          placeholder="제목"
          placeholderTextColor="#999"
        />

        {/* 내용 입력 필드 */}
        <TextInput
          style={[styles.input, styles.contentInput]}
          value={content}
          onChangeText={setContent}
          placeholder="내용"
          placeholderTextColor="#999"
          multiline
          textAlignVertical="top"
        />
      </View>

      {/* 작성/수정 완료 버튼 */}
      <View style={styles.buttonContainer}>
        <TouchableOpacity
          style={[styles.button, isLoading && styles.buttonDisabled]}
          onPress={handleSubmit}
          disabled={isLoading}
        >
          <Text style={styles.buttonText}>
            {postId ? '수정 완료' : '새 일기 작성하기'}
          </Text>
        </TouchableOpacity>
      </View>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  /** 공통 스타일 **/
  container: {
    flex: 1,
    backgroundColor: '#F0F4F8',
    paddingTop: 40,
  },
  centered: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F0F4F8',
  },
  title: {
    fontSize: 28,
    fontWeight: 'bold',
    color: '#333',
    letterSpacing: 1,
    textAlign: 'center',
    marginBottom: 20,
  },
  inputContainer: {
    flex: 1,
    paddingHorizontal: 16,
    paddingTop: 20,
  },

  dateWrapper: {
    marginBottom: 16,
  },
  dateInput: {
    height: 50,
    backgroundColor: '#FFFFFF',
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#00B8B0',
    paddingHorizontal: 12,
    fontSize: 16,
    color: '#333',
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.1,
    shadowRadius: 2,
  },

  input: {
    backgroundColor: '#FFFFFF',
    borderWidth: 1,
    borderColor: '#00B8B0',
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 16,
    color: '#333',
    elevation: 1,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.05,
    shadowRadius: 1,
  },
  titleInput: {
    height: 50,
    marginBottom: 16,
  },
  contentInput: {
    flex: 1,
    paddingTop: 12,
    textAlignVertical: 'top',
  },

  buttonContainer: {
    paddingHorizontal: 16,
    paddingVertical: 16,
    backgroundColor: '#F0F4F8',
  },
  button: {
    backgroundColor: '#00B8B0',
    height: 50,
    borderRadius: 25,
    alignItems: 'center',
    justifyContent: 'center',
    elevation: 3,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.2,
    shadowRadius: 3,
    marginBottom: 50,
  },
  buttonDisabled: {
    backgroundColor: '#9CCFFF',
  },
  buttonText: {
    color: '#FFFFFF',
    fontSize: 18,
    fontWeight: '600',
  },
  logo: {
    width: 180,
    height: 90,
    alignSelf: 'center',
    marginBottom: 20,
  },
});
