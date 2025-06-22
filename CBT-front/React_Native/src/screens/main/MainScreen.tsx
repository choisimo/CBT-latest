// src/screens/main/MainScreen.tsx

import React, { useState, useEffect, useMemo, useContext } from 'react';
import {
  View,
  Text,
  TextInput,
  FlatList,
  StyleSheet,
  TouchableOpacity,
  Keyboard,
  Image,
  Alert,
} from 'react-native';
import { Calendar } from 'react-native-calendars';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { AppStackParamList } from '../../navigation/AppStack';
import { AuthContext } from '../../context/AuthContext';
import { BASIC_URL } from '../../constants/api';

function toQueryString(obj: Record<string, any>): string {
  const params: string[] = [];
  Object.entries(obj).forEach(([key, value]) => {
    if (value !== undefined && value !== null) {
      params.push(`${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`);
    }
  });
  return params.length ? `?${params.join('&')}` : '';
}

export type Props = NativeStackScreenProps<AppStackParamList, 'Main'>;
type Post = { id: string; title: string; date: string };

interface DairyData {
  status: string;
  message: string;
  diaries: Post[];
  totalCount: number;
}

interface DateData {
  status: string;
  message: string;
  dates: string[];
}

export default function MainScreen({ navigation }: Props) {
  const { user, fetchWithAuth, isAuthLoading } = useContext(AuthContext);

  // 검색어, 선택 날짜, 달력 토글 상태
  const [searchText, setSearchText] = useState('');
  const [selectedDate, setSelectedDate] = useState<string | null>(null);
  const [calendarVisible, setCalendarVisible] = useState(false);

  // 달력에 표시할 날짜 목록
  const [allDates, setAllDates] = useState<string[]>([]);

  // 페이징 상태 및 조회된 일기들
  const ITEMS_PER_PAGE = 10;
  const [currentPage, setCurrentPage] = useState(0);
  const [totalCount, setTotalCount] = useState(0);
  const [filteredPosts, setFilteredPosts] = useState<Post[]>([]);

  /** 1) 달력에 표시할 날짜들(월별 조회) */
  useEffect(() => {
    const loadDates = async () => {
      if (!user) return;
      try {
        // 1. 오늘 기준 YYYY-MM 생성
        const today = new Date();
        const year = today.getFullYear();
        const month = String(today.getMonth() + 1).padStart(2, '0');
        const payload: Record<string, any> = {
          "month":`${year}-${month}`
        };
        const qs = toQueryString(payload);
        // 2. backend: GET /api/diaryposts/calendar?month=YYYY-MM
        const res = await fetchWithAuth(
          `${BASIC_URL}/api/diaryposts/calendar?month=${qs}`
        );
        // 3. JSON 파싱
        const calendar_json:DateData = await res.json() 
        // 4. JSON의 status로 성공/실패 분기
        if (calendar_json.status !== 'success' || !calendar_json.dates) {
          Alert.alert('dlf 조회 실패', calendar_json.message);
          return;
        }
        // 5. 정상 처리
        setAllDates(calendar_json.dates);
      } catch (e: any) {
        // 네트워크 오류 등 예외 시 Alert 띄우기
        Alert.alert('달력 조회 중 오류', e.message || '알 수 없는 오류가 발생했습니다.');
      }
      try {
        const payload: Record<string, any> = {
          "page":0,
          "size":10,
          "sort":"createdAt,desc", 
        };
        const qs = toQueryString(payload);
        const res = await fetchWithAuth(`${BASIC_URL}/api/diaries${qs}`)
        const dairy_json: DairyData = await res.json();
        if (dairy_json.status !== 'success' || !dairy_json.diaries) {
          Alert.alert('일기 조회 실패', dairy_json.message);
          return;
        }
        setFilteredPosts(dairy_json.diaries);
        setTotalCount(dairy_json.totalCount);
      } catch (e: any) {
        Alert.alert('일기 조회 중 오류', e.message || '알 수 없는 오류가 발생했습니다.');
      }
    };
    loadDates();
  }, [user]);

  /** 2) 전체 조회 또는 검색어 조회 (page가 바뀔 때마다) */
  const loadAllOrSearch = async (page: number) => {
    if (!user) return;
    try {
      const payload: Record<string, any> = {
        "q":searchText,
        "page":page,
        "size":10,
        "sort":"createdAt,desc", 
      };
      const qs = toQueryString(payload);
      const res = await fetchWithAuth(`${BASIC_URL}/api/diaries${qs}`)
      const dairy_json: DairyData = await res.json();
      if (dairy_json.status !== 'success' || !dairy_json.diaries) {
        Alert.alert('일기 조회 실패', dairy_json.message);
        return;
      }
      setFilteredPosts(dairy_json.diaries);
      setTotalCount(dairy_json.totalCount);
    } catch (e: any) {
      Alert.alert('일기 조회 중 오류', e.message || '알 수 없는 오류가 발생했습니다.');
    }
  };

  /** 3) 날짜 선택 시: 해당 날짜 일기만 조회 */
  const loadByDate = async (date: string, page: number) => {
    if (!user) return;
    try {
      const payload: Record<string, any> = {
        date,
        "page":page,
        "size":10,
        "sort":"createdAt,desc", 
      };
      const qs = toQueryString(payload);
      const res = await fetchWithAuth(`${BASIC_URL}/api/diaries${qs}`)
      const dairy_json: DairyData = await res.json();
      if (dairy_json.status !== 'success' || !dairy_json.diaries) {
        Alert.alert('일기 조회 실패', dairy_json.message);
        return;
      }
      setFilteredPosts(dairy_json.diaries);
      setTotalCount(dairy_json.totalCount);
    } catch (e: any) {
      Alert.alert('일기 조회 중 오류', e.message || '알 수 없는 오류가 발생했습니다.');
    }
  };

  /** 초기 로드: 전체(첫 페이지) */
  useEffect(() => {
    setSelectedDate(null);
    setCurrentPage(0);
    loadAllOrSearch(0);
  }, [user]);

  /** 페이지 변경 시 로직: 날짜 선택 모드 vs 검색/전체 모드 분기 */
  const goToPage = (page: number) => {
    setCurrentPage(page);
    if (selectedDate) {
      loadByDate(selectedDate, page);
    } else {
      loadAllOrSearch(page);
    }
  };

  /** 4) 날짜 선택 핸들러: 날짜를 선택하면 날짜별 조회 + 페이지 리셋 */
  const handleDateSelect = (date: string) => {
    Keyboard.dismiss();
    setSelectedDate(date);
    setSearchText('');
    setCurrentPage(0);
    loadByDate(date, 0);
  };

  /** 5) 검색 핸들러: 검색어 있으면 검색, 날짜 선택 해제, 페이지 리셋 */
  const handleSearch = () => {
    Keyboard.dismiss();
    setSelectedDate(null);
    setCurrentPage(0);
    loadAllOrSearch(0);
  };

  /** 달력 UI에 표시할 markedDates 객체 생성 */
  const markedDates = useMemo(() => {
    const marks: { [key: string]: { marked: boolean } } = {};
    allDates.forEach((date) => {
      marks[date] = { marked: true };
    });
    return marks;
  }, [allDates]);

  /** 페이지 수 및 이전/다음 버튼 활성화 여부 */
  const totalPages = Math.ceil(totalCount / ITEMS_PER_PAGE);
  const hasPrev = currentPage > 0;
  const hasNext = currentPage < totalPages - 1;

  /** FlatList 아이템 렌더러 */
  const renderPostItem = ({ item }: { item: Post }) => (
    <TouchableOpacity
      style={styles.postItem}
      onPress={() => navigation.navigate('View', { diaryId: item.id })}
    >
      <Text style={{ fontWeight: 'bold', marginBottom: 4 }}>{item.title}</Text>
      <Text style={{ color: '#777' }}>{item.date}</Text>
    </TouchableOpacity>
  );

  return (
    <View style={styles.container}>
      <Image
        source={require('../../../assets/images/logo.png')}
        style={styles.logo}
        resizeMode="cover"
      />

      {/* 검색창 */}
      <TextInput
        style={styles.searchInput}
        placeholder="🔍 제목/내용 검색"
        placeholderTextColor="#555"
        selectionColor="#4a90e2"
        value={searchText}
        onChangeText={setSearchText}
        onSubmitEditing={handleSearch}
        returnKeyType="search"
      />

      {/* 달력 토글 버튼 */}
      <TouchableOpacity
        style={styles.toggleButton}
        onPress={() => setCalendarVisible((prev) => !prev)}
      >
        <Text style={styles.toggleButtonText}>
          {calendarVisible ? '달력 숨기기' : '달력 보기'}
        </Text>
      </TouchableOpacity>

      {/* 달력 (표시할 날짜는 markedDates로) */}
      {calendarVisible && (
        <View style={styles.calendarContainer}>
          <Text style={styles.sheetTitle}>📅 일기 달력</Text>
          <Calendar
            markedDates={markedDates}
            onDayPress={(day) => handleDateSelect(day.dateString)}
          />
        </View>
      )}

      {/* 글 목록 (FlatList) */}
      <FlatList
        data={filteredPosts}
        keyExtractor={(item) => item.id}
        renderItem={renderPostItem}
        showsVerticalScrollIndicator={false}
        style={{ flex: 1, marginBottom: 12 }}
      />

      {/* 페이지 네비게이션 */}
      <View style={styles.paginationContainer}>
        <TouchableOpacity
          disabled={!hasPrev}
          onPress={() => hasPrev && goToPage(currentPage - 1)}
          style={[styles.pageButton, !hasPrev && styles.pageButtonDisabled]}
        >
          <Text style={styles.pageButtonText}>{'< 이전'}</Text>
        </TouchableOpacity>
        <Text style={styles.pageIndicator}>
          {currentPage + 1} / {totalPages || 1}
        </Text>
        <TouchableOpacity
          disabled={!hasNext}
          onPress={() => hasNext && goToPage(currentPage + 1)}
          style={[styles.pageButton, !hasNext && styles.pageButtonDisabled]}
        >
          <Text style={styles.pageButtonText}>{'다음 >'}</Text>
        </TouchableOpacity>
      </View>

      {/* 새로운 일기 작성 버튼 (FAB) */}
      <TouchableOpacity
        style={styles.fab}
        onPress={() => navigation.navigate('Write')}
      >
        <Text style={styles.fabIcon}>+</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, paddingTop: 40, paddingHorizontal: 20 },
  title: {
    fontSize: 28,
    fontWeight: 'bold',
    color: '#333',
    letterSpacing: 1,
    textAlign: 'center',
    marginBottom: 20,
  },
  logo: {
    width: 180,
    height: 90,
    alignSelf: 'center',
    marginBottom: 20,
  },

  searchInput: {
    borderWidth: 1,
    borderColor: '#ccc',
    padding: 10,
    borderRadius: 8,
    marginBottom: 12,
  },
  toggleButton: {
    backgroundColor: '#d0e8ff',
    paddingVertical: 12,
    borderRadius: 8,
    alignItems: 'center',
    marginBottom: 12,
  },
  toggleButtonText: {
    fontSize: 16,
    fontWeight: '500',
  },
  postItem: {
    padding: 16,
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    marginBottom: 12,
    backgroundColor: '#f9f9f9',
  },
  calendarContainer: {
    marginBottom: 16,
  },
  sheetTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    marginBottom: 10,
  },
  paginationContainer: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 24,
  },
  pageButton: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    backgroundColor: '#4a90e2',
    borderRadius: 6,
    marginHorizontal: 8,
  },
  pageButtonDisabled: {
    backgroundColor: '#ccc',
  },
  pageButtonText: {
    color: '#fff',
    fontSize: 14,
  },
  pageIndicator: {
    fontSize: 16,
    fontWeight: '500',
  },
  fab: {
    position: 'absolute',
    right: 24,
    bottom: 24,
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: '#00B8B0',
    justifyContent: 'center',
    alignItems: 'center',
    elevation: 6,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.3,
    shadowRadius: 4,
  },
  fabIcon: {
    color: '#fff',
    fontSize: 48,
    textAlign: 'center',
    lineHeight: 56,
  },
});
