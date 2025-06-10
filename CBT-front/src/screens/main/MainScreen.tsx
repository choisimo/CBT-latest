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

interface PostData {
  dates: string[];
  diaries: Post[];
  totalCount: number;
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
        // 페이지/사이즈 값은 “한 달치 날짜를 전부 가져온다”고 가정.
        // 상황에 따라 직접 startDate/endDate 파라미터를 추가할 수도 있음
        const payload: Record<string, any> = {
          "page":0,
          "size":10,
          "sort":"createdAt,desc", 
          searchText,
        };
        const qs = toQueryString(payload);
        const res = await fetchWithAuth(`https://${BASIC_URL}/api/diaries${qs}`)
        const data: PostData = await res.json();
        setAllDates(data.dates);
      } catch (e) {
        console.warn('달력 용 날짜 조회 오류:', e);
      }
    };
    loadDates();
  }, [user]);

  /** 2) 전체 조회 또는 검색어 조회 (page가 바뀔 때마다) */
  const loadAllOrSearch = async (page: number) => {
    if (!user) return;
    try {
      const payload: Record<string, any> = {  
        page, 
        "size":10,
        "sort":"createdAt,desc", 
        searchText,
      };
      const qs = toQueryString(payload);
      const res = await fetchWithAuth(`https://${BASIC_URL}/api/diaries${qs}`)
      if (!res.ok) {
        console.warn(`서버 에러: ${res.status}`);
        return;
      }
      const data: PostData = await res.json();
      setFilteredPosts(data.diaries);
      setTotalCount(data.totalCount);
    } catch (e) {
      console.warn('전체/검색 일기 조회 오류:', e);
    }
  };

  /** 3) 날짜 선택 시: 해당 날짜 일기만 조회 */
  const loadByDate = async (date: string, page: number) => {
    if (!user) return;
    try {
      const payload: Record<string, any> = {  
        page, 
        "size":10,
        "sort":"createdAt,desc",
        "startDate":date,
        "endDate":date,
        searchText,
      };
      const qs = toQueryString(payload);
      const res = await fetchWithAuth(`https://${BASIC_URL}/api/diaries${qs}`)
      if (!res.ok) {
        console.warn(`서버 에러: ${res.status}`);
        return;
      }
      const data: PostData = await res.json();
      setFilteredPosts(data.diaries);
      setTotalCount(data.totalCount);
    } catch (e) {
      console.warn('전체/검색 일기 조회 오류:', e);
    }
  };

  /** 초기 로드: 전체(첫 페이지) */
  useEffect(() => {
    setSelectedDate(null);
    loadAllOrSearch(0);
    setCurrentPage(0);
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
      onPress={() => navigation.navigate('View', { postId: item.id })}
    >
      <Text style={{ fontWeight: 'bold', marginBottom: 4 }}>{item.title}</Text>
      <Text style={{ color: '#777' }}>{item.date}</Text>
    </TouchableOpacity>
  );

  return (
    <View style={styles.container}>
      <Text style={styles.title}>CBT Diary</Text>

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
    backgroundColor: '#4a90e2',
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
