import React, { useState, useContext, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ActivityIndicator,
  ScrollView,
  Alert,
} from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { AppStackParamList } from '../../navigation/AppStack';
import { AuthContext } from '../../context/AuthContext';
import { BASIC_URL } from '../../constants/api';

type Props = NativeStackScreenProps<AppStackParamList, 'View'>;

// 백엔드에서 받아올 글 데이터 타입 예시
interface PostData {
  id: string;
  date: string;        // YYYY-MM-DD 형태를 가정
  title: string;
  content: string;
  aiResponse: boolean; // AI 분석 결과(없으면 undefined 또는 빈 문자열)
}

export default function ViewScreen({ route, navigation }: Props) {
  const { diaryId } = route.params as { diaryId: string };
  const { fetchWithAuth, user, isAuthLoading } = useContext(AuthContext);

  const [post, setPost] = useState<PostData | null>(null);
  const [error, setError] = useState<string>('');

  // 화면이 포커스될 때마다 최신 글 정보를 가져오기 위해 useFocusEffect 사용
  useFocusEffect(
    useCallback(() => {
      const loadPost = async () => {
        if (!user) {
          // 로그인 상태가 아니면 로드하지 않음
          return;
        }

        // 데이터 로딩 전 이전 상태 초기화
        setError('');

        try {
          const res = await fetchWithAuth(
            `${BASIC_URL}/api/diaries/${diaryId}`,
            { method: 'GET' },
          );
          if (!res.ok) {
            if (res.status === 404) {
              throw new Error('해당 글을 찾을 수 없습니다.');
            } else {
              const errJson = await res.json();
              throw new Error(errJson.message || '서버 에러가 발생했습니다.');
            }
          }
          const data: PostData = await res.json();
          setPost(data); // 최신 데이터로 상태 업데이트
        } catch (err: any) {
          setError(err.message);
        }
      };

      loadPost();

      // 클린업 함수 (optional): 화면을 벗어날 때 특정 작업을 수행할 수 있습니다.
      return () => {
        // 예를 들어, 상태를 초기화할 수 있습니다.
        // setPost(null);
      };
    }, [diaryId, fetchWithAuth, user]),
  );

  // "수정하기" 버튼을 눌렀을 때: Write 화면으로 이동
  const handleEdit = () => {
    navigation.navigate('Write', { diaryId });
  };

  // "AI 분석 보러가기" 또는 "분석하기" 버튼을 눌렀을 때: Analyze 화면으로 이동
  const handleAnalyze = async () => {
    if (!post) return;

    // AI 분석 결과가 이미 있다면 바로 분석 화면으로 이동
    if (post.aiResponse) {
      navigation.navigate('Analyze', { diaryId: post.id });
      return;
    }

    try {
      // AI 분석 결과가 없다면 분석 요청 후 분석 화면으로 이동
      const res = await fetchWithAuth(
        `${BASIC_URL}/api/diaries/${post.id}/analysis`,
        { method: 'POST' }
      );

      if (!res.ok) {
        // 분석 요청 실패 시 에러 메시지 표시
        const errJson = await res.json();
        Alert.alert('분석 오류', errJson.message || 'AI 분석 요청에 실패했습니다.');
        return;
      }

      // 분석 요청이 성공하면 분석 화면으로 이동 (분석 진행 상황은 AnalyzeScreen에서 처리)
      navigation.navigate('Analyze', { diaryId: post.id });
      
    } catch (e: any) {
      console.warn('AI 분석 요청 중 오류:', e);
      Alert.alert('오류', 'AI 분석 중에 문제가 발생했습니다.');
    }
  };

  // 1) 로딩 중인 상태
  if (isAuthLoading) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" color="#4A90E2" />
      </View>
    );
  }

  // 2) 에러가 있는 상태
  if (error) {
    return (
      <View style={styles.centered}>
        <Text style={styles.errorText}>{error}</Text>
      </View>
    );
  }

  // 3) post가 null일 수도 있으니, 데이터가 없을 때를 별도로 처리
  if (!post) {
    return (
      <View style={styles.centered}>
        <Text style={styles.errorText}>데이터가 없습니다.</Text>
      </View>
    );
  }

  return (
    <ScrollView contentContainerStyle={styles.container}>
      {/* 헤더 영역 */}
      <View style={styles.header}>
        <View style={styles.dateContainer}>
          <Text style={styles.dateIcon}>📅</Text>
          <Text style={styles.postDate}>{post.date}</Text>
        </View>
        {post.aiResponse && (
          <View style={styles.aiStatusBadge}>
            <Text style={styles.aiStatusText}>AI 분석 완료</Text>
          </View>
        )}
      </View>

      {/* 글 내용 카드 */}
      <View style={styles.card}>
        <Text style={styles.postTitle}>{post.title}</Text>
        <View style={styles.contentContainer}>
          <Text style={styles.postContent}>{post.content}</Text>
        </View>
      </View>

      {/* 2) AI 분석 결과가 있는 경우: "AI 분석 보러가기" 버튼만 노출 */}
      {post.aiResponse ? (
        <View style={styles.buttonWrapper}>
          <TouchableOpacity
            style={[styles.button, styles.analyzeButton]}
            onPress={handleAnalyze}
          >
            <Text style={styles.buttonIcon}>🧠</Text>
            <Text style={styles.buttonText}>AI 분석 결과 보기</Text>
          </TouchableOpacity>
        </View>
      ) : (
        /* 3) AI 분석 결과가 없으면: "수정하기" / "분석하기" 버튼 노출 */
        <View style={styles.buttonRow}>
          <TouchableOpacity
            style={[styles.button, styles.editButton]}
            onPress={() => navigation.navigate('Write', { diaryId: post.id })}
          >
            <Text style={styles.buttonIcon}>✏️</Text>
            <Text style={styles.buttonText}>수정하기</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.button, styles.analyzeButton]}
            onPress={handleAnalyze}
          >
            <Text style={styles.buttonIcon}>🧠</Text>
            <Text style={styles.buttonText}>AI 분석하기</Text>
          </TouchableOpacity>
        </View>
      )}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    padding: 20,
    paddingBottom: 40,
    backgroundColor: '#f8f9fa',
  },
  centered: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#f8f9fa',
  },
  errorText: {
    color: '#D32F2F',
    fontSize: 16,
  },

  // 헤더 영역
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 20,
  },
  dateContainer: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  dateIcon: {
    fontSize: 16,
    marginRight: 8,
  },
  aiStatusBadge: {
    backgroundColor: '#e8f5e8',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: '#4caf50',
  },
  aiStatusText: {
    fontSize: 12,
    color: '#2e7d32',
    fontWeight: '600',
  },

  // 카드(글 내용) 스타일
  card: {
    backgroundColor: '#FFFFFF',
    borderRadius: 16,
    padding: 24,
    marginBottom: 24,
    elevation: 3,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.15,
    shadowRadius: 4,
  },
  postDate: {
    fontSize: 16,
    color: '#666',
    fontWeight: '500',
  },
  postTitle: {
    fontSize: 24,
    fontWeight: '700',
    color: '#1a1a1a',
    marginBottom: 16,
    lineHeight: 32,
  },
  postContent: {
    fontSize: 16,
    color: '#333',
    lineHeight: 26,
  },
  contentContainer: {
    paddingTop: 8,
  },

  // 버튼 행 (AI 분석 결과 없을 때)
  buttonRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    gap: 12,
  },
  buttonWrapper: {
    alignItems: 'center',
  },
  button: {
    flex: 1,
    flexDirection: 'row',
    height: 56,
    borderRadius: 28,
    justifyContent: 'center',
    alignItems: 'center',
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.2,
    shadowRadius: 2,
  },
  editButton: {
    backgroundColor: '#FFD54F', // 노란색 계열
  },
  analyzeButton: {
    backgroundColor: '#4A90E2', // 파란색 계열
  },
  buttonIcon: {
    fontSize: 20,
    marginRight: 8,
  },
  buttonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '600',
  },
});
