package com.authentication.auth.filter;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 필터 조건 인터페이스
 * 필터 적용 조건을 정의하는 인터페이스
 */
public interface FilterCondition {
    
    /**
     * 조건의 고유 ID를 반환
     * @return 조건의 고유 ID (UUID)
     */
    String getId();
    
    /**
     * 요청에 필터를 적용할지 여부를 결정
     * @param request HTTP 요청
     * @return true이면 필터를 적용하지 않음, false이면 필터 적용
     * @Description 현재 HTTP 요청에 대해 이 조건을 만족하여 필터 적용을 건너뛸지 여부를 반환합니다.
     */
    boolean shouldNotFilter(HttpServletRequest request);
    
    /**
     * 조건에 설명을 제공
     * @return 조건 설명
     * @Description 이 필터 조건에 대한 설명을 반환합니다. 주로 로깅이나 디버깅 목적으로 사용됩니다.
     */
    String getDescription();

    /**
     * 조건의 활성화 상태를 반환합니다.
     * @return true이면 조건이 활성화되어 필터 로직에 사용됨, false이면 비활성화되어 무시됨
     */
    boolean isEnabled();

    /**
     * 조건의 활성화 상태를 설정합니다.
     * @param enabled 활성화 상태 (true 또는 false)
     */
    void setEnabled(boolean enabled);

    /**
     * 조건의 유형을 문자열로 반환합니다. (예: "path", "header", "ip")
     * @return 조건 유형 문자열
     */
    String getType();
}
