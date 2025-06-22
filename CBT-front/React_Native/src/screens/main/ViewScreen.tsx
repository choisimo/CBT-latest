// src/screens/PostDetailScreen.tsx

import React, { useState, useEffect, useContext } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ActivityIndicator,
  ScrollView,
  Alert,
} from 'react-native';
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

  // 백엔드에서 글 정보를 가져오는 함수
  useEffect(() => {
    let isMounted = true;

    const loadPost = async () => {
      if (!user) {
        Alert.alert('로그인이 필요합니다.');
        return;
      }

      try {
        const res = await fetchWithAuth(
          `${BASIC_URL}/api/diaryposts/${diaryId}`,
          { method: 'GET' }
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
        if (isMounted) {
          setPost(data);
        }
      } catch (err: any) {
        if (isMounted) {
          setError(err.message);
        }
      }
      
    };

    loadPost();
    return () => {
      isMounted = false;
    };
  }, [diaryId, fetchWithAuth, user]);

  // “수정하기” 버튼을 눌렀을 때: Write 화면으로 이동
  const handleEdit = () => {
    navigation.navigate('Write', { diaryId });
  };

  // “AI 분석 보러가기” 또는 “분석하기” 버튼을 눌렀을 때: Analyze 화면으로 이동
// … (이전 부분 그대로) …

// “AI 분석 보러가기” 또는 “분석하기” 버튼을 눌렀을 때: Analyze 화면으로 이동
  const handleAnalyze = async () => {
    if (!post) return;

    try {
      // 1) AI 분석 결과가 없다면(=aiResponse === false), 백엔드에 분석 요청
      if (!post.aiResponse) {
        // /api/diaries/{postId}/analysis 형식에 맞춰서 호출
        const res = await fetchWithAuth(
          `${BASIC_URL}/api/diaries/${post.id}/analysis`,
          { method: 'POST' } // 만약 GET이 아니라 POST여야 한다면 POST로 바꿔주세요
        );

        if (!res.ok) {
          // 분석 요청 실패 시 에러 메시지 표시
          const errJson = await res.json();
          Alert.alert('분석 오류', errJson.message || 'AI 분석 요청에 실패했습니다.');
          return;
        }

        // 백엔드에서 분석을 완료하고, 최종 결과물(aiResponse 등)을 업데이트해 주었다고 가정
        // (필요하다면 이 시점에 `const updatedPost: PostData = await res.json()` 을 받아서
        //  post 상태를 갱신해 주셔도 됩니다.)
      }

      // 2) AI 분석 결과가 이미 있거나, 방금 분석이 끝났다면 결과 화면으로 이동
      navigation.navigate('Analyze', { diaryId: post.id });
    } catch (e: any) {
      console.warn('AI 분석 요청 중 오류:', e);
      Alert.alert('오류', 'AI 분석 중에 문제가 발생했습니다.');
    }
  };

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
      {/* 1) 글 내용(Card) */}
      <View style={styles.card}>
        <Text style={styles.postDate}>{post.date}</Text>
        <Text style={styles.postTitle}>{post.title}</Text>
        <Text style={styles.postContent}>{post.content}</Text>
      </View>

      {/* 2) AI 분석 결과가 있는 경우: “AI 분석 보러가기” 버튼만 노출 */}
      {post.aiResponse ? (
        <View style={styles.buttonWrapper}>
          <TouchableOpacity
            style={[styles.button, styles.analyzeButton]}
            onPress={handleAnalyze}
          >
            <Text style={styles.buttonText}>AI 분석 보러가기</Text>
          </TouchableOpacity>
        </View>
      ) : (
        /* 3) AI 분석 결과가 없으면: “수정하기” / “분석하기” 버튼 노출 */
        <View style={styles.buttonRow}>
          <TouchableOpacity
            style={[styles.button, styles.editButton]}
            onPress={() => navigation.navigate('Write', { diaryId: post.id })}
          >
            <Text style={styles.buttonText}>수정하기</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.button, styles.analyzeButton]}
            onPress={handleAnalyze}
          >
            <Text style={styles.buttonText}>분석하기</Text>
          </TouchableOpacity>
        </View>
      )}
    </ScrollView>
  );

}

const styles = StyleSheet.create({
  container: {
    padding: 16,
    paddingBottom: 40,
    backgroundColor: '#F0F4F8',
  },
  centered: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F0F4F8',
  },
  errorText: {
    color: '#D32F2F',
    fontSize: 16,
  },

  // 카드(글 내용) 스타일
  card: {
    backgroundColor: '#FFFFFF',
    borderRadius: 8,
    padding: 16,
    marginBottom: 20,
    // Android 그림자
    elevation: 2,
    // iOS 그림자
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.1,
    shadowRadius: 2,
  },
  postDate: {
    fontSize: 14,
    color: '#777',
    marginBottom: 8,
  },
  postTitle: {
    fontSize: 22,
    fontWeight: '700',
    color: '#333',
    marginBottom: 12,
  },
  postContent: {
    fontSize: 16,
    color: '#444',
    lineHeight: 24,
  },

  // 버튼 행 (AI 분석 결과 없을 때)
  buttonRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  buttonWrapper: {
    alignItems: 'center',
  },
  button: {
    flex: 1,
    height: 48,
    borderRadius: 24,
    justifyContent: 'center',
    alignItems: 'center',
  },
  editButton: {
    backgroundColor: '#FFD54F', // 노란색 계열
    marginRight: 8,
  },
  analyzeButton: {
    backgroundColor: '#4A90E2', // 파란색 계열
    marginLeft: 8,
    flex: 1,                     // “AI 분석 보러가기” 전용일 땐 width 전체 사용
  },
  buttonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '600',
  },
});
