package com.authentication.auth.validator;

import java.util.List;

public class EntityValidationOutput {
    
    /**
     * 검증 결과를 테이블 형식으로 표시
     */
    public static void displayResults(List<EntitySqlValidator.ValidationResult> results) {
        // 테이블 헤더
        System.out.println("+------------------------+------------------------+-----------+------------------------------------+");
        System.out.println("| 필드                    | 현재 값                | 상태      | 메시지                               |");
        System.out.println("+------------------------+------------------------+-----------+------------------------------------+");
        
        // 각 결과 행 출력
        for (EntitySqlValidator.ValidationResult result : results) {
            System.out.printf("| %-22s | %-22s | %-9s | %-34s |\n", 
                    truncate(result.getFieldName(), 22),
                    truncate(result.getCurrentValue(), 22), 
                    result.getStatus().getSymbol(),
                    truncate(result.getMessage(), 34));
        }
        
        // 테이블 푸터
        System.out.println("+------------------------+------------------------+-----------+------------------------------------+");
        
        // 요약 통계
        long errorCount = results.stream()
                .filter(r -> r.getStatus() == EntitySqlValidator.ValidationStatus.ERROR)
                .count();
        long warningCount = results.stream()
                .filter(r -> r.getStatus() == EntitySqlValidator.ValidationStatus.WARNING)
                .count();
        
        System.out.println("\n요약: " + 
                errorCount + " 오류, " + 
                warningCount + " 경고, " + 
                (results.size() - errorCount - warningCount) + " 유효한 검사");
    }
    
    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 3) + "...";
    }
}
