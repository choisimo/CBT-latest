package com.authentication.auth.filter;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
    private final List<PluggableFilter> orderedFilters = Collections.synchronizedList(new ArrayList<>());
    
    // 필터 조건 저장 맵 (필터 ID -> 필터 조건 맵)
    private final Map<String, Map<String, FilterCondition>> filterConditions = new ConcurrentHashMap<>();

    /**
     * 필터 등록
     * @param filter 등록할 {@link PluggableFilter} 구현 객체
     * @Description 제공된 필터를 레지스트리에 등록하고, 우선순위에 따라 정렬된 리스트에 삽입합니다.
     */
    public void registerFilter(PluggableFilter filter) {
        String filterId = filter.getFilterId();
        filterMap.put(filterId, filter);
        
        // 우선순위에 따라 정렬된 위치에 필터 삽입
        insertFilterInOrder(filter);
        
        filterConditions.putIfAbsent(filterId, new ConcurrentHashMap<>());
        log.info("필터 등록 완료: {}, 우선순위: {}", filterId, filter.getOrder());
    }

    /**
     * 등록된 모든 필터를 반환
     * @return 등록된 필터들의 Map
     */
    public Map<String, PluggableFilter> getFilters() {
        return filterMap;
    }

    /**
     * 필터에 조건 추가
     * @param filterId 조건을 추가할 필터의 ID
     * @param condition 추가할 {@link FilterCondition} 객체
     * @Description 특정 필터 ID에 해당하는 필터에 적용될 조건을 추가합니다.
     */
    public void addCondition(String filterId, FilterCondition condition) {
        filterConditions.computeIfPresent(filterId, (key, conditionsMap) -> {
            conditionsMap.put(condition.getId(), condition);
            log.info("Condition '{}' added to filter '{}'.", condition.getId(), filterId);
            return conditionsMap;
        });
        filterConditions.putIfAbsent(filterId, new ConcurrentHashMap<>(Map.of(condition.getId(), condition)));
        log.info("Condition '{}' added to filter '{}'.", condition.getId(), filterId);
    }

    /**
     * 특정 필터에서 조건을 제거
     * @param filterId 조건을 제거할 필터의 ID
     * @param conditionId 제거할 조건의 ID
     * @return 조건 제거 성공 여부
     */
    public boolean removeCondition(String filterId, String conditionId) {
        Map<String, FilterCondition> conditionsMap = filterConditions.get(filterId);
        if (conditionsMap != null) {
            FilterCondition removed = conditionsMap.remove(conditionId);
            if (removed != null) {
                log.info("Condition '{}' removed from filter '{}'.", conditionId, filterId);
                return true;
            } else {
                log.warn("Condition with ID '{}' not found for filter '{}'.", conditionId, filterId);
            }
        } else {
            log.warn("Filter with ID '{}' not found, cannot remove condition '{}'.", filterId, conditionId);
        }
        return false;
    }

    /**
     * 특정 요청에 대해 필터를 적용해야 하는지 확인
     * @param filterId 확인할 필터의 ID
     * @param request 현재 HTTP 요청
     * @return boolean 필터를 적용해야 하면 true, 그렇지 않으면 false (건너뛰어야 하면 false)
     * @Description 해당 필터에 등록된 모든 조건을 검사하여, 하나라도 '건너뛰지 않아야 한다(shouldNotFilter == false)'고 판단하면 true를 반환합니다.
     *              즉, 모든 조건이 '건너뛰어야 한다(shouldNotFilter == true)'고 판단해야만 false (건너뜀)를 반환합니다.
     */
    public boolean shouldApplyFilter(String filterId, HttpServletRequest request) {
        Map<String, FilterCondition> conditions = filterConditions.get(filterId);
        if (conditions == null || conditions.isEmpty()) {
            return false; // 조건이 없으면 항상 필터 적용
        }
        
        // 어느 하나의 조건이라도 true를 반환하면 필터를 적용하지 않음 (OR 조건)
        return conditions.values().stream().anyMatch(condition -> condition.shouldNotFilter(request));
    }

    /**
     * 우선순위에 따라 정렬된 위치에 필터 삽입
     * @param filter 삽입할 {@link PluggableFilter}
     * @Description 필터의 getOrder() 값을 기준으로 정렬된 `orderedFilters` 리스트의 올바른 위치에 필터를 삽입합니다.
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
     * @Description 등록된 모든 필터를 순서대로 가져와 각각의 configure 메서드를 호출하여 HttpSecurity에 적용합니다.
     *              주로 Spring Security 설정 클래스에서 사용됩니다.
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
     * @Description 레지스트리에 등록된 모든 {@link PluggableFilter}를 우선순위(order)에 따라 정렬하여 반환합니다.
     */
    public List<PluggableFilter> getRegisteredFilters() {
        return Collections.unmodifiableList(orderedFilters);
    }
    
    /**
     * 특정 필터 조회
     * @param filterId 필터 ID
     * @return Optional<PluggableFilter> 해당 ID의 필터 (존재하지 않으면 Optional.empty())
     * @Description 주어진 ID에 해당하는 {@link PluggableFilter} 인스턴스를 반환합니다.
     */
    public Optional<PluggableFilter> getFilter(String filterId) {
        return Optional.ofNullable(filterMap.get(filterId));
    }

    /**
     * 특정 필터에 대한 모든 조건들을 반환
     * @param filterId 조건을 조회할 필터의 ID
     * @return List<FilterCondition> 해당 필터에 적용된 조건 리스트 (없으면 빈 리스트)
     * @Description 특정 필터 ID에 연관된 모든 {@link FilterCondition} 리스트를 반환합니다.
     */
    public List<FilterCondition> getConditionsForFilter(String filterId) {
        Map<String, FilterCondition> conditionsMap = filterConditions.get(filterId);
        return conditionsMap != null ? new ArrayList<>(conditionsMap.values()) : Collections.emptyList();
    }

    /**
     * 필터 등록 해제
     * @param filterId 등록 해제할 필터의 ID
     * @Description 지정된 ID의 필터를 레지스트리에서 제거합니다. 관련된 조건들도 함께 제거됩니다.
     */
    public void unregisterFilter(String filterId) {
        PluggableFilter removedFilter = filterMap.remove(filterId);
        if (removedFilter != null) {
            orderedFilters.remove(removedFilter);
            filterConditions.remove(filterId);
            log.info("필터 등록 해제 완료: {}", filterId);
        } else {
            log.warn("등록 해제할 필터를 찾을 수 없음: {}", filterId);
        }
    }

    /**
     * 모든 필터와 조건 초기화
     * @Description 레지스트리에 등록된 모든 필터와 조건을 제거하여 초기 상태로 되돌립니다.
     */
    public void clearAll() {
        filterMap.clear();
        orderedFilters.clear();
        filterConditions.clear();
        log.info("모든 필터와 조건이 초기화됨");
    }

    /**
     * 특정 필터에 속한 특정 조건 ID의 조건을 반환합니다.
     * @param filterId 필터 ID
     * @param conditionId 조건 ID
     * @return Optional<FilterCondition> 해당 ID의 조건 (존재하지 않으면 Optional.empty())
     */
    public Optional<FilterCondition> getCondition(String filterId, String conditionId) {
        Map<String, FilterCondition> conditionsMap = filterConditions.get(filterId);
        if (conditionsMap != null) {
            return Optional.ofNullable(conditionsMap.get(conditionId));
        }
        return Optional.empty();
    }
}
