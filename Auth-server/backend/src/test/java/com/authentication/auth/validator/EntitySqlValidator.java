package com.authentication.auth.validator;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;

import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;

/**
 * JPA 엔티티와 SQL 생성 구문을 비교 검증하는 검증기
 */
public class EntitySqlValidator {

    public enum ValidationStatus {
        VALID(""),
        WARNING(""),
        ERROR("");

        private final String symbol;

        ValidationStatus(String symbol) {
            this.symbol = symbol;
        }

        public String getSymbol() {
            return symbol;
        }
    }

    /**
     * 필드 검증 결과
     */
    public static class ValidationResult {
        private String fieldName;
        private String currentValue;
        private ValidationStatus status;
        private String message;

        public ValidationResult(String fieldName, String currentValue, ValidationStatus status, String message) {
            this.fieldName = fieldName;
            this.currentValue = currentValue;
            this.status = status;
            this.message = message;
        }

        // Getters
        public String getFieldName() { return fieldName; }
        public String getCurrentValue() { return currentValue; }
        public ValidationStatus getStatus() { return status; }
        public String getMessage() { return message; }
    }

    /**
     * JPA 엔티티와 SQL 생성 구문 검증
     */
    public static List<ValidationResult> validate(Class<?> entityClass, String sqlStatement) {
        List<ValidationResult> results = new ArrayList<>();

        try {
            // SQL 파싱
            Statement statement = CCJSqlParserUtil.parse(sqlStatement);
            if (!(statement instanceof CreateTable)) {
                results.add(new ValidationResult("SQL", sqlStatement, ValidationStatus.ERROR,
                            "유효한 CREATE TABLE 구문이 아닙니다"));
                return results;
            }

            CreateTable createTable = (CreateTable) statement;

            // 테이블 이름 검증
            validateTableName(entityClass, createTable, results);

            // SQL에서 컬럼 추출
            Map<String, ColumnDefinition> sqlColumns = extractSqlColumns(createTable);

            // 엔티티 필드 검증
            validateEntityFields(entityClass, sqlColumns, results);

            // SQL에 있는 컬럼 중 엔티티에 없는 컬럼 확인
            validateSqlColumns(entityClass, sqlColumns, results);

            // 기본 키 검증
            validatePrimaryKey(entityClass, createTable, results);

        } catch (JSQLParserException e) {
            results.add(new ValidationResult("SQL 파싱 오류", e.getMessage(),
                       ValidationStatus.ERROR, "SQL 구문 파싱에 실패했습니다"));
        }

        return results;
    }

    private static Map<String, ColumnDefinition> extractSqlColumns(CreateTable createTable) {
        Map<String, ColumnDefinition> columns = new HashMap<>();

        if (createTable.getColumnDefinitions() != null) {
            for (ColumnDefinition colDef : createTable.getColumnDefinitions()) {
                columns.put(colDef.getColumnName().replaceAll("[`\\\"]", ""), colDef);
            }
        }

        return columns;
    }

    private static void validateTableName(Class<?> entityClass, CreateTable createTable,
                                       List<ValidationResult> results) {
        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        String entityTableName = tableAnnotation != null && !tableAnnotation.name().isEmpty() ?
                tableAnnotation.name() : entityClass.getSimpleName();

        String sqlTableName = createTable.getTable().getName().replaceAll("[`\\\"]", "");

        if (entityTableName.equals(sqlTableName)) {
            results.add(new ValidationResult("테이블 이름", entityTableName, ValidationStatus.VALID,
                       "테이블 이름이 일치합니다"));
        } else {
            results.add(new ValidationResult("테이블 이름", entityTableName, ValidationStatus.ERROR,
                       "엔티티 테이블 이름이 SQL 테이블 이름과 일치하지 않습니다: " + sqlTableName));
        }
    }

    // 엔티티 필드 검증
    private static void validateEntityFields(Class<?> entityClass, Map<String, ColumnDefinition> sqlColumns,
                                         List<ValidationResult> results) {
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Transient.class)) {
                continue;
            }

            String entityFieldName = field.getName();
            Column columnAnnotation = field.getAnnotation(Column.class);
            if (columnAnnotation != null && !columnAnnotation.name().isEmpty()) {
                entityFieldName = columnAnnotation.name();
            }

            ColumnDefinition sqlColumn = sqlColumns.get(entityFieldName.toLowerCase());

            if (sqlColumn == null) {
                results.add(new ValidationResult(entityFieldName, "", ValidationStatus.ERROR,
                           "엔티티 필드에 해당하는 SQL 컬럼을 찾을 수 없습니다"));
                continue;
            }

            validateColumnType(field, sqlColumn, results);
            validateNullable(field, sqlColumn, results);
            validateLength(field, sqlColumn, results);
        }
    }

    private static void validateSqlColumns(Class<?> entityClass, Map<String, ColumnDefinition> sqlColumns,
                                         List<ValidationResult> results) {
        for (String sqlColumnName : sqlColumns.keySet()) {
            boolean found = false;
            for (Field field : entityClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(Transient.class)) {
                    continue;
                }
                Column columnAnnotation = field.getAnnotation(Column.class);
                String entityFieldName = (columnAnnotation != null && !columnAnnotation.name().isEmpty())
                                         ? columnAnnotation.name()
                                         : field.getName();
                if (entityFieldName.equalsIgnoreCase(sqlColumnName)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                results.add(new ValidationResult(sqlColumnName, "", ValidationStatus.ERROR,
                           "SQL 컬럼이 엔티티에 존재하지 않습니다"));
            }
        }
    }

    private static void validatePrimaryKey(Class<?> entityClass, CreateTable createTable,
                                         List<ValidationResult> results) {
        // 엔티티의 기본 키 필드 찾기
        Field entityPkField = null;
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                entityPkField = field;
                break;
            }
        }

        if (entityPkField == null) {
            results.add(new ValidationResult("기본 키", "", ValidationStatus.WARNING,
                       "엔티티에 기본 키(@Id)가 정의되어 있지 않습니다"));
            return;
        }

        // SQL의 기본 키 찾기
        String sqlPkColumnName = null;
        if (createTable.getIndexes() != null) {
            for (net.sf.jsqlparser.statement.create.table.Index index : createTable.getIndexes()) {
                if (index.getType().equalsIgnoreCase("PRIMARY KEY")) {
                    if (!index.getColumns().isEmpty()) {
                        sqlPkColumnName = index.getColumns().get(0).getColumnName().replaceAll("[`\\\"]", "");
                        break;
                    }
                }
            }
        }

        String entityPkColumnName = entityPkField.getName();
        Column columnAnnotation = entityPkField.getAnnotation(Column.class);
        if (columnAnnotation != null && !columnAnnotation.name().isEmpty()) {
            entityPkColumnName = columnAnnotation.name();
        }

        if (sqlPkColumnName == null) {
            results.add(new ValidationResult(entityPkColumnName, "", ValidationStatus.ERROR,
                       "SQL에 기본 키가 정의되어 있지 않습니다"));
        } else if (entityPkColumnName.equalsIgnoreCase(sqlPkColumnName)) {
            results.add(new ValidationResult(entityPkColumnName, "", ValidationStatus.VALID,
                       "기본 키가 SQL 정의와 일치합니다"));
        } else {
            results.add(new ValidationResult(entityPkColumnName, "", ValidationStatus.ERROR,
                       "기본 키가 SQL 정의와 일치하지 않습니다 (SQL: " + sqlPkColumnName + ")"));
        }
    }

    private static void validateColumnType(Field field, ColumnDefinition sqlColumn,
                                       List<ValidationResult> results) {
        String javaType = field.getType().getSimpleName();
        String sqlType = sqlColumn.getColDataType().getDataType().toUpperCase();

        if (isTypeCompatible(field.getType(), sqlType)) {
            results.add(new ValidationResult(field.getName(), "타입: " + javaType,
                      ValidationStatus.VALID, "SQL 타입과 호환됩니다: " + sqlType));
        } else {
            results.add(new ValidationResult(field.getName(), "타입: " + javaType,
                      ValidationStatus.WARNING, "SQL 타입과 호환되지 않을 수 있습니다: " + sqlType));
        }
    }

    private static void validateNullable(Field field, ColumnDefinition sqlColumn,
                                     List<ValidationResult> results) {
        Column columnAnnotation = field.getAnnotation(Column.class);
        boolean entityNullable = columnAnnotation == null || columnAnnotation.nullable();

        // @Id가 있는 필드는 null이 될 수 없음
        if (field.isAnnotationPresent(Id.class)) {
            entityNullable = false;
        }

        boolean sqlNullable = !hasNotNullConstraint(sqlColumn);

        if (entityNullable == sqlNullable) {
            results.add(new ValidationResult(field.getName(), "Null 허용: " + entityNullable,
                      ValidationStatus.VALID, "Null 허용 여부가 SQL 정의와 일치합니다"));
        } else {
            results.add(new ValidationResult(field.getName(), "Null 허용: " + entityNullable,
                      ValidationStatus.ERROR, "Null 허용 여부가 SQL 정의와 일치하지 않습니다 (SQL: " + sqlNullable + ")"));
        }
    }

    private static void validateLength(Field field, ColumnDefinition sqlColumn,
                                   List<ValidationResult> results) {
        if (String.class.isAssignableFrom(field.getType())) {
            Column columnAnnotation = field.getAnnotation(Column.class);
            if (columnAnnotation != null && columnAnnotation.length() > 0) {
                int entityLength = columnAnnotation.length();
                Integer sqlLength = getColumnLength(sqlColumn);

                if (sqlLength == null) {
                    results.add(new ValidationResult(field.getName(), "길이: " + entityLength,
                              ValidationStatus.WARNING, "엔티티는 길이를 지정하지만 SQL 컬럼은 길이를 지정하지 않습니다"));
                } else if (entityLength == sqlLength) {
                    results.add(new ValidationResult(field.getName(), "길이: " + entityLength,
                              ValidationStatus.VALID, "길이가 SQL 정의와 일치합니다"));
                } else {
                    results.add(new ValidationResult(field.getName(), "길이: " + entityLength,
                              ValidationStatus.ERROR, "길이가 SQL 정의와 일치하지 않습니다 (SQL: " + sqlLength + ")"));
                }
            }
        }
    }


    private static boolean isTypeCompatible(Class<?> javaType, String sqlType) {
        sqlType = sqlType.toUpperCase();

        if (String.class.isAssignableFrom(javaType)) {
            return sqlType.contains("VARCHAR") || sqlType.contains("TEXT") || sqlType.contains("CHAR");
        } else if (Integer.class.isAssignableFrom(javaType) || int.class.isAssignableFrom(javaType)) {
            return sqlType.contains("INT");
        } else if (Long.class.isAssignableFrom(javaType) || long.class.isAssignableFrom(javaType)) {
            return sqlType.contains("BIGINT") || sqlType.contains("INT");
        } else if (Boolean.class.isAssignableFrom(javaType) || boolean.class.isAssignableFrom(javaType)) {
            return sqlType.contains("BOOLEAN") || sqlType.contains("BIT") || sqlType.contains("TINYINT");
        } else if (Double.class.isAssignableFrom(javaType) || double.class.isAssignableFrom(javaType)) {
            return sqlType.contains("DOUBLE") || sqlType.contains("DECIMAL");
        } else if (Float.class.isAssignableFrom(javaType) || float.class.isAssignableFrom(javaType)) {
            return sqlType.contains("FLOAT") || sqlType.contains("DECIMAL");
        } else if (java.util.Date.class.isAssignableFrom(javaType)) {
            return sqlType.contains("DATE") || sqlType.contains("TIMESTAMP");
        }

        return false;
    }

    private static boolean hasNotNullConstraint(ColumnDefinition columnDef) {
        if (columnDef.getColumnSpecs() != null) { 
            String specs = String.join(" ", columnDef.getColumnSpecs()).toUpperCase(); 
            return specs.contains("NOT NULL");
        }
        return false;
    }

    private static Integer getColumnLength(ColumnDefinition columnDef) {
        if (columnDef.getColDataType().getArgumentsStringList() != null &&
            !columnDef.getColDataType().getArgumentsStringList().isEmpty()) {
            try {
                return Integer.parseInt(columnDef.getColDataType().getArgumentsStringList().get(0));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}