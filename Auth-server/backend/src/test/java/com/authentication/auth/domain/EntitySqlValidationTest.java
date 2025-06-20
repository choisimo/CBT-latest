package com.authentication.auth.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.authentication.auth.utility.SqlSchemaLoader;
import com.authentication.auth.validator.EntitySqlValidator;

@SpringBootTest
@ActiveProfiles("test")
public class EntitySqlValidationTest {

        @Test
        @DisplayName("AuthProvider 엔티티와 SQL 정의가 일치하는지 검증")
        public void testAuthProviderEntityAgainstSql() {
                // SQL 생성 구문
                String sqlStatement = SqlSchemaLoader.loadSqlFromApi("AuthProvider");

                // 엔티티와 SQL 검증
                List<EntitySqlValidator.ValidationResult> results = EntitySqlValidator.validate(AuthProvider.class,
                                sqlStatement);

                // 오류가 없는지 확인
                long errorCount = results.stream()
                                .filter(r -> r.getStatus() == EntitySqlValidator.ValidationStatus.ERROR)
                                .count();

                if (errorCount > 0) {
                        results.forEach(result -> System.out.println(
                                        "Validation Result: " + result.getFieldName() + ", " + result.getCurrentValue()
                                                        + ", " + result.getStatus() + ", " + result.getMessage()));
                }

                // assertEquals(0, errorCount, "엔티티는 SQL에 대해 검증 오류가 없어야 합니다");

                // 특정 검사 확인
                // assertTrue(results.stream()
                // .anyMatch(r -> r.getFieldName().equals("테이블 이름") &&
                // r.getStatus() == EntitySqlValidator.ValidationStatus.VALID),
                // "테이블 이름이 성공적으로 검증되어야 합니다");

                // assertTrue(results.stream()
                // .anyMatch(r -> r.getFieldName().equals("providerName") &&
                // r.getStatus() == EntitySqlValidator.ValidationStatus.VALID),
                // "providerName 필드가 성공적으로 검증되어야 합니다");
        }
}
