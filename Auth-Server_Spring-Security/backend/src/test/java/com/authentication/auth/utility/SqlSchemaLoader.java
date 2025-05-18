package com.authentication.auth.utility;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public class SqlSchemaLoader {

    /**
     * ClassPathReource 에서 SQL 파일을 읽어옴
     */
    public static String loadSqlFromClasspath(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        }
    }
    
    /**
     * API에서 SQL 스키마를 가져옴 (Mock 구현)
     */
    public static String loadSqlFromApi(String entityName) {
        // 실제 구현에서는 API 호출 코드로 대체
        if ("AuthProvider".equals(entityName)) {
            return "CREATE TABLE Auth_Provider (\n" +
                   "    id INT AUTO_INCREMENT,\n" +
                   "    provider_name VARCHAR(50) NOT NULL DEFAULT 'SERVER',\n" +
                   "    description VARCHAR(255),\n" +
                   "    is_active BOOLEAN DEFAULT TRUE,\n" +
                   "    PRIMARY KEY (id)\n" +
                   ");";
        }
        return "";
    }
}
