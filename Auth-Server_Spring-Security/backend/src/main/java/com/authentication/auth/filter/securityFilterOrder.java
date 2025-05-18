package com.authentication.auth.filter;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;

/**
 * @Author : choisimo 
 * @Date : 2025-05-05
 * @Description : SecurityFilterOrder enum class
 * @Details : 필터 체인에서 필터의 순서를 지정하는 열거형 클래스
 */

@Getter
public enum SecurityFilterOrder {
    /**
     * 필터 실행 순서를 나타내는 enum
     * order 값이 낮을수록 우선순위가 높음
     */
    AUTHENTICATION_FILTER(100),
    AUTHORIZATION_FILTER(200),
    SNS_REQUEST_FILTER(300);

    // 필터 순서 값
    private final int order;
    
    // order 값으로 필터를 빠르게 조회하기 위한 맵 (BigO(1))
    private static final Map<Integer, SecurityFilterOrder> ORDER_MAP = new HashMap<>();

    static {
        for (SecurityFilterOrder filter : SecurityFilterOrder.values()) {
            ORDER_MAP.put(filter.order, filter);
        }
    }

    SecurityFilterOrder(int order) {
        this.order = order;
    }

    /**
     * 순서 값으로 필터 열거형을 조회
     * @param order 필터 순서 값
     * @return 해당 순서의 필터 열거형
     * @throws IllegalArgumentException 유효하지 않은 순서값 입력 시
     */
    public static SecurityFilterOrder fromOrder(int order) {
        SecurityFilterOrder filter = ORDER_MAP.get(order);
        if (filter == null) {
            throw new IllegalArgumentException("유효하지 않은 필터 순서: " + order);
        }
        return filter;
    }

    /**
     * 문자열을 안전하게 필터 열거형으로 변환
     * @param name 필터 이름
     * @return 해당 이름의 필터 열거형, 없으면 기본값 반환
     */
    public static SecurityFilterOrder safeValueOf(String name) {
        try {
            return SecurityFilterOrder.valueOf(name);
        } catch (IllegalArgumentException e) {
            return SecurityFilterOrder.AUTHENTICATION_FILTER; // 기본값
        }
    }
}
