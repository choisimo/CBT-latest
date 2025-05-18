package com.authentication.auth.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 필터 레지스트리
 * 플러그인 방식의 필터를 등록하고 관리하는 중앙 컴포넌트
 */
@Slf4j
@Component
public class FilterRegistry {

    // 필터 인스턴스 저장 맵 (필터 ID -> 필터 인스턴스)
    private final Map<String, PluggableFilter> filterMap = new ConcurrentHashMap<>();
    
    // 필터 우선순위 순서대로 저장된 리스트
    private final List<PluggableFilter> orderedFilters = new CopyOnWriteArrayList<>();
    
    // 필터 조건 저장 맵 (필터 ID -> 필터 조건 리스트)
    private final Map<String, List<FilterCondition>> filterConditions = new ConcurrentHashMap<>();

    /**
     * 필터 등록
     * @param filter 등록할 필터
     */
    public void registerFilter(PluggableFilter filter) {
        String filterId = filter.getFilterId();
        filterMap.put(filterId, filter);
        
        // 우선순위에 따라 정렬된 위치에 필터 삽입
        insertFilterInOrder(filter);
        
        log.info("필터 등록 완료: {}, 우선순위: {}", filterId, filter.getOrder());
    }

    /**
     * 필터에 조건 추가
     * @param filterId 필터 ID
     * @param condition 필터 조건
     */
    public void addCondition(String filterId, FilterCondition condition) {
        filterConditions.computeIfAbsent(filterId, k -> new CopyOnWriteArrayList<>())
                        .add(condition);
        log.info("필터 '{}' 에 조건 추가: {}", filterId, condition.getDescription());
    }

    /**
     * 필터에 조건 제거
     * @param filterId 필터 ID
     * @param condition 제거할 조건
     * @return 제거 성공 여부
     */
    public boolean removeCondition(String filterId, FilterCondition condition) {
        List<FilterCondition> conditions = filterConditions.get(filterId);
        if (conditions != null) {
            boolean result = conditions.remove(condition);
            if (result) {
                log.info("필터 '{}' 에서 조건 제거: {}", filterId, condition.getDescription());
            }
            return result;
        }
        return false;
    }

    /**
     * 특정 요청에 대해 필터를 적용해야 하는지 확인
     * @param filterId 필터 ID
     * @param request HTTP 요청
     * @return true면 필터를 적용하지 않음, false면 필터 적용
     */
    public boolean shouldNotFilter(String filterId, jakarta.servlet.http.HttpServletRequest request) {
        List<FilterCondition> conditions = filterConditions.get(filterId);
        if (conditions == null || conditions.isEmpty()) {
            return false; // 조건이 없으면 항상 필터 적용
        }
        
        // 어느 하나의 조건이라도 true를 반환하면 필터를 적용하지 않음 (OR 조건)
        return conditions.stream().anyMatch(condition -> condition.shouldNotFilter(request));
    }

    /**
     * 우선순위에 따라 정렬된 위치에 필터 삽입
     * @param filter 삽입할 필터
     */
    private void insertFilterInOrder(PluggableFilter filter) {
        // 기존 필터가 있으면 제거
        orderedFilters.remove(filter);
        
        // 새 필터의 순서
        int newFilterOrder = filter.getOrder();
        
        int insertIndex = 0;
        while (insertIndex < orderedFilters.size() && 
               orderedFilters.get(insertIndex).getOrder() <= newFilterOrder) {
            insertIndex++;
        }
        
        orderedFilters.add(insertIndex, filter);
    }

    /**
     * 등록된 모든 필터를 SecurityConfig에 등록
     * @param http HttpSecurity 객체
     * @throws Exception 설정 중 예외 발생 시
     */
    public void configureFilters(HttpSecurity http) throws Exception {
        for (PluggableFilter filter : orderedFilters) {
            filter.configure(http);
            log.debug("필터 구성 적용: {}", filter.getFilterId());
        }
        log.info("총 {}개의 필터가 SecurityConfig에 등록됨", orderedFilters.size());
    }

    /**
     * 등록된 모든 필터 목록 반환
     * @return 필터 목록
     */
    public List<PluggableFilter> getRegisteredFilters() {
        return Collections.unmodifiableList(orderedFilters);
    }
    
    /**
     * 특정 필터 조회
     * @param filterId 필터 ID
     * @return 찾은 필터, 없으면 null
     */
    public PluggableFilter getFilter(String filterId) {
        return filterMap.get(filterId);
    }
}
