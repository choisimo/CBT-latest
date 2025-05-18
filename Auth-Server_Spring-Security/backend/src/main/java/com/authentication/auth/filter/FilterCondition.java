package com.authentication.auth.filter;

import jakarta.servlet.http.HttpServletRequest;

/**
 * @Author: choisimo
 * @Date: 2025-05-05
 * @Description: 필터 조건 인터페이스
 * @Details: 필터 적용 조건을 정의하는 인터페이스
 */
public interface FilterCondition {
    
    /**
     * 요청에 필터를 적용할지 여부를 결정
     * @param request HTTP 요청
     * @return true이면 필터를 적용하지 않음, false이면 필터 적용
     */
    boolean shouldNotFilter(HttpServletRequest request);
    
    /**
     * 조건에 설명을 제공
     * @return 조건 설명
     */
    String getDescription();
}
