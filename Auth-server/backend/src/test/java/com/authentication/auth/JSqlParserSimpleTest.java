package com.authentication.auth;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.JSQLParserException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class JSqlParserSimpleTest {

    @Test
    void testJsqlParserLoads() {
        try {
            Statement statement = CCJSqlParserUtil.parse("SELECT * FROM mytable");
            assertNotNull(statement);
            System.out.println("JSqlParserSimpleTest: Successfully parsed statement.");
        } catch (JSQLParserException e) {
            fail("JSqlParserSimpleTest: Failed to parse statement: " + e.getMessage());
        }
    }
}
