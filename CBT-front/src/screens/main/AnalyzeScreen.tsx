// src/screens/AnalyzeScreen.tsx

import React, { useState, useEffect, useContext } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  ActivityIndicator,
  Alert,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { AppStackParamList } from '../../navigation/AppStack';
import { AuthContext } from '../../context/AuthContext';
import { BASIC_URL } from '../../constants/api';

type Props = NativeStackScreenProps<AppStackParamList, 'Analyze'>;

// API 응답 타입 정의
interface AnalysisResult {
  id: number;
  emotionDetection: {
    joy: number;
    sadness: number;
    surprise: number;
    calm: number;
  };
  emotionSummary: string;
  automaticThought: string;
  promptForChange: string;
  alternativeThought: string;
  status: 'POSITIVE' | 'NEGATIVE' | 'NEUTRAL';
  confidence: number;
  analyzedAt: string; // ISO 8601, 예: "2024-01-15T10:35:00Z"
}

export default function AnalyzeScreen({ route }: Props) {
  const { postId } = route.params;
  const { fetchWithAuth, user } = useContext(AuthContext);

  // 로딩/에러 상태
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string>('');

  // “분석 진행 중” 상태일 경우
  const [inProgress, setInProgress] = useState<{
    message: string;
    progress: number;
    estimatedRemaining: string;
  } | null>(null);

  // 실제 분석 결과가 내려왔을 때
  const [analysis, setAnalysis] = useState<AnalysisResult | null>(null);

  useEffect(() => {
    let isMounted = true;

    const loadAnalysis = async () => {
      if (!user) {
        Alert.alert('로그인이 필요합니다.');
        setIsLoading(false);
        return;
      }

      try {
        const res = await fetchWithAuth(
          `https://${BASIC_URL}/api/diaries/${postId}/analysis`,
          { method: 'GET' }
        );

        if (!res.ok) {
          // 4xx/5xx 에러 처리
          const errJson = await res.json();
          throw new Error(errJson.message || `서버 에러: ${res.status}`);
        }

        // 성공적으로 JSON을 받으면…
        const data = await res.json();

        // “분석 진행 중” 응답인지 확인 (message, progress, estimatedRemaining)
        if (data.message && typeof data.progress === 'number') {
          if (isMounted) {
            setInProgress({
              message: data.message,
              progress: data.progress,
              estimatedRemaining: data.estimatedRemaining,
            });
          }
        } else if (data.analysis && typeof data.analysis === 'object') {
          // “분석 완료” 응답일 때
          if (isMounted) {
            setAnalysis(data.analysis as AnalysisResult);
          }
        } else {
          throw new Error('알 수 없는 응답 형식입니다.');
        }
      } catch (err: any) {
        if (isMounted) {
          setError(err.message);
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    };

    loadAnalysis();

    return () => {
      isMounted = false;
    };
  }, [postId, fetchWithAuth, user]);

  // 로딩 중
  if (isLoading) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" color="#4A90E2" />
      </View>
    );
  }

  // 에러가 난 경우
  if (error) {
    return (
      <View style={styles.centered}>
        <Text style={styles.errorText}>{error}</Text>
      </View>
    );
  }

  // 분석 진행 중인 경우
  if (inProgress) {
    return (
      <SafeAreaView style={styles.safeArea}>
        <View style={styles.centered}>
          <Text style={styles.progressText}>
            {inProgress.message}
          </Text>
          <Text style={styles.progressText}>
            진행률: {inProgress.progress}%
          </Text>
          <Text style={styles.subtext}>
            예상 남은 시간: {inProgress.estimatedRemaining}
          </Text>
        </View>
      </SafeAreaView>
    );
  }

  // 최종 분석 결과가 있는 경우
  if (analysis) {
    return (
      <SafeAreaView style={styles.safeArea}>
        <ScrollView contentContainerStyle={styles.scrollContainer}>
          {/* ⚠️ TODO: postId로 제목/내용을 따로 가져오고 싶다면
              여기서 /api/diaryposts/{postId}를 fetch하여 title/content를 세팅하세요. */}
          <View style={styles.block}>
            <Text style={styles.heading}>📘 글 제목</Text>
            <Text style={styles.text}>{/* 제목을 넣어주세요 */}</Text>

            <Text style={[styles.heading, { marginTop: 20 }]}>📝 글 내용</Text>
            <Text style={styles.text}>{/* 내용을 넣어주세요 */}</Text>
          </View>

          {/* 1) 감정 식별 결과 */}
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>🧠 1. 감정 식별하기</Text>
            <View style={styles.row}>
              <Text style={styles.boldText}>기쁨:</Text>
              <Text style={styles.text}>{analysis.emotionDetection.joy}%</Text>
            </View>
            <View style={styles.row}>
              <Text style={styles.boldText}>슬픔:</Text>
              <Text style={styles.text}>{analysis.emotionDetection.sadness}%</Text>
            </View>
            <View style={styles.row}>
              <Text style={styles.boldText}>놀람:</Text>
              <Text style={styles.text}>{analysis.emotionDetection.surprise}%</Text>
            </View>
            <View style={styles.row}>
              <Text style={styles.boldText}>평온:</Text>
              <Text style={styles.text}>{analysis.emotionDetection.calm}%</Text>
            </View>
            <Text style={styles.subtext}>
              {analysis.emotionSummary}
            </Text>
          </View>

          {/* 2) 자동적 사고 */}
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>🔍 2. 자동적 사고 탐색</Text>
            <Text style={styles.text}>
              {analysis.automaticThought}
            </Text>
          </View>

          {/* 3) 사고 교정 프롬프트 */}
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>💡 3. 사고 교정 프롬프트</Text>
            <Text style={styles.text}>
              {analysis.promptForChange}
            </Text>
          </View>

          {/* 4) 대안적 사고 */}
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>🌱 4. 대안적 사고 정리</Text>
            <Text style={styles.text}>
              {analysis.alternativeThought}
            </Text>
          </View>

          {/* 부가 정보 (상태, 확신도, 분석 시각 등) */}
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>🔖 부가 정보</Text>
            <Text style={styles.text}>
              상태: {analysis.status} {'\n'}
              확신도: {(analysis.confidence * 100).toFixed(1)}% {'\n'}
              분석 완료 시각: {new Date(analysis.analyzedAt).toLocaleString()}
            </Text>
          </View>
        </ScrollView>
      </SafeAreaView>
    );
  }

  // 여기까지 오면, 로딩/에러/진행 중/결과 네 가지 경우 모두 처리되었으므로
  // 그 외 케이스는 딱히 없다고 간주할 수 있습니다.
  return null;
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: '#f9f9f9' },
  scrollContainer: { padding: 20 },
  centered: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  errorText: { color: '#D32F2F', fontSize: 16, textAlign: 'center' },
  progressText: { fontSize: 18, marginBottom: 8, color: '#333' },
  subtext: { fontSize: 14, color: '#666' },

  block: {
    backgroundColor: '#fff',
    padding: 15,
    borderRadius: 10,
    marginBottom: 20,
    elevation: 1,
  },
  heading: {
    fontSize: 22,
    fontWeight: 'bold',
    marginBottom: 10,
  },
  text: { fontSize: 16, lineHeight: 24, marginBottom: 8, color: '#333' },

  section: {
    backgroundColor: '#ffffff',
    padding: 15,
    borderRadius: 10,
    marginTop: 15,
    elevation: 2,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '600',
    marginBottom: 10,
    color: '#333',
  },
  boldText: {
    fontSize: 16,
    fontWeight: '600',
    marginRight: 8,
    color: '#333',
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 4,
  },
});
