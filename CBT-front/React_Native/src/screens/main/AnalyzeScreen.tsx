// src/screens/main/AnalyzeScreen.tsx

import React, { useState, useEffect, useContext } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  ActivityIndicator,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { AppStackParamList } from '../../navigation/AppStack';
import { AuthContext } from '../../context/AuthContext';
import { BASIC_URL } from '../../constants/api';
import { createSSEService } from '../../utils/sseService';

type Props = NativeStackScreenProps<AppStackParamList, 'Analyze'>;

// Updated API response type to match backend AIResponseDto
interface AnalysisResult {
  id: string;
  diaryId: number;
  status?: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  emotions?: { category: string; intensity?: number }[];
  coaching?: string;
  summary?: string;
  errorMessage?: string;
  createdAt?: string;
  updatedAt?: string;
}

export default function AnalyzeScreen({ route }: Props) {
  const { diaryId } = route.params;
  const { fetchWithAuth, userToken } = useContext(AuthContext);

  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string>('');
  const [analysis, setAnalysis] = useState<AnalysisResult | null>(null);
  const [analysisStatus, setAnalysisStatus] = useState<string>('대기 중...');

  // 폴백 폴링 함수 정의
  const startFallbackPolling = async () => {
    console.log('폴백 폴링 시작');

    const pollForAnalysis = async () => {
      try {
        const response = await fetchWithAuth(`/api/diaries/${diaryId}/analysis`, {
          method: 'GET',
        });

        if (response.ok) {
          const data = await response.json();

          if (data && data.status) {
            switch (data.status) {
              case 'PENDING':
                setAnalysisStatus('분석 대기 중...');
                setTimeout(pollForAnalysis, 2000);
                break;
              case 'PROCESSING':
                setAnalysisStatus('AI가 일기를 분석하고 있습니다...');
                setTimeout(pollForAnalysis, 2000);
                break;
              case 'COMPLETED':
                setAnalysis(data as AnalysisResult);
                setAnalysisStatus('분석 완료!');
                setIsLoading(false);
                break;
              case 'FAILED':
                setError(data.errorMessage || '분석에 실패했습니다.');
                setIsLoading(false);
                break;
            }
          } else {
            // 아직 분석이 시작되지 않음
            setTimeout(pollForAnalysis, 2000);
          }
        } else {
          setError('분석 상태를 확인할 수 없습니다.');
          setIsLoading(false);
        }
      } catch (error) {
        console.error('폴링 오류:', error);
        setError('분석 상태 확인 중 오류가 발생했습니다.');
        setIsLoading(false);
      }
    };

    pollForAnalysis();
  };

  useEffect(() => {
    let isMounted = true;

    // SSE 서비스 생성
    const sseService = createSSEService(BASIC_URL, () => userToken);

    // 실시간 모니터링 시작
    sseService.connect({
      onConnect: () => {
        console.log('실시간 분석 모니터링 연결됨');
        setAnalysisStatus('분석 연결됨...');
      },

      onStatusUpdate: (data) => {
        if (!isMounted) return;

        console.log('분석 상태 업데이트:', data);
        switch (data.status) {
          case 'PENDING':
            setAnalysisStatus('분석 대기 중...');
            break;
          case 'PROCESSING':
            setAnalysisStatus('AI가 일기를 분석하고 있습니다...');
            break;
        }
      },

      onComplete: (data) => {
        if (!isMounted) return;

        console.log('분석 완료:', data);
        setAnalysis(data as AnalysisResult);
        setAnalysisStatus('분석 완료!');
        setIsLoading(false);
      },

      onFailed: (data) => {
        if (!isMounted) return;

        console.log('분석 실패:', data);
        setError(data.errorMessage || '분석에 실패했습니다.');
        setIsLoading(false);
      },

      onError: (errorMessage) => {
        if (!isMounted) return;

        console.error('SSE 오류:', errorMessage);
        // 실시간 연결 실패 시 폴백으로 폴링 방식 사용
        startFallbackPolling();
      },

      onDisconnect: () => {
        console.log('실시간 분석 모니터링 연결 해제');
      }
    });

    // 특정 다이어리 모니터링 시작
    sseService.monitorDiary(Number(diaryId));

    return () => {
      isMounted = false;
      sseService.disconnect();
    };
  }, [diaryId, fetchWithAuth, userToken]);

  if (isLoading) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" color="#4A90E2" />
        <Text style={styles.progressText}>{analysisStatus}</Text>
        <Text style={styles.subtext}>
          잠시만 기다려주세요. 최대 1분 정도 소요될 수 있습니다.
        </Text>
      </View>
    );
  }

  if (error) {
    return (
      <View style={styles.centered}>
        <Text style={styles.errorText}>{error}</Text>
      </View>
    );
  }

  if (analysis) {
    return (
      <SafeAreaView style={styles.safeArea}>
        <ScrollView contentContainerStyle={styles.scrollContainer}>
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>🎭 감정 분석 결과</Text>
            <Text style={styles.text}>
              {analysis.emotions?.map(e => e.category).join(', ') || '감정 정보 없음'}
            </Text>
          </View>

          {analysis.summary && (
            <View style={styles.section}>
              <Text style={styles.sectionTitle}>📝 AI 요약</Text>
              <Text style={styles.text}>{analysis.summary}</Text>
            </View>
          )}

          <View style={styles.section}>
            <Text style={styles.sectionTitle}>💡 인지 행동 치료 제안</Text>
            <Text style={styles.text}>
              {analysis.coaching || '제안 정보 없음'}
            </Text>
          </View>

          <View style={styles.section}>
            <Text style={styles.sectionTitle}>ℹ️ 부가 정보</Text>
            <View style={styles.row}>
              <Text style={styles.boldText}>분석 상태:</Text>
              <Text style={[styles.text, { color: '#28a745', fontWeight: 'bold' }]}>완료</Text>
            </View>
            <View style={styles.row}>
              <Text style={styles.boldText}>분석 완료:</Text>
              <Text style={styles.text}>
                {analysis.updatedAt
                  ? new Date(analysis.updatedAt).toLocaleString()
                  : analysis.createdAt
                    ? new Date(analysis.createdAt).toLocaleString()
                    : '시간 정보 없음'
                }
              </Text>
            </View>
          </View>
        </ScrollView>
      </SafeAreaView>
    );
  }

  return null;
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: '#f9f9f9' },
  scrollContainer: { padding: 20 },
  centered: { flex: 1, justifyContent: 'center', alignItems: 'center', padding: 20 },
  errorText: { color: '#D32F2F', fontSize: 16, textAlign: 'center' },
  progressText: { fontSize: 18, marginBottom: 12, color: '#333', textAlign: 'center' },
  subtext: { fontSize: 14, color: '#666', textAlign: 'center' },
  section: {
    backgroundColor: '#ffffff',
    padding: 20,
    borderRadius: 10,
    marginBottom: 20,
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.22,
    shadowRadius: 2.22,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '600',
    marginBottom: 10,
    color: '#333',
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
    paddingBottom: 8,
  },
  text: { fontSize: 16, lineHeight: 24, color: '#555' },
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
