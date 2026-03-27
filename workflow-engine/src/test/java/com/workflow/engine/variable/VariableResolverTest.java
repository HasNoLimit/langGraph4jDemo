package com.workflow.engine.variable;

import org.bsc.langgraph4j.state.AgentState;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 变量解析器单元测试
 */
class VariableResolverTest {

    private final DefaultVariableResolver resolver = new DefaultVariableResolver();

    @Test
    void testSimpleVariable() {
        Map<String, Object> params = new HashMap<>();
        params.put("name", "Alice");

        VariableContext context = VariableContext.builder()
                .runtimeParams(params)
                .build();

        String result = resolver.resolve("Hello {{name}}!", context);
        assertEquals("Hello Alice!", result);
    }

    @Test
    void testMultipleVariables() {
        Map<String, Object> params = new HashMap<>();
        params.put("greeting", "Hi");
        params.put("name", "Bob");

        VariableContext context = VariableContext.builder()
                .runtimeParams(params)
                .build();

        String result = resolver.resolve("{{greeting}} {{name}}!", context);
        assertEquals("Hi Bob!", result);
    }

    @Test
    void testNestedProperty() {
        Map<String, Object> address = new HashMap<>();
        address.put("city", "Beijing");

        Map<String, Object> user = new HashMap<>();
        user.put("name", "Charlie");
        user.put("address", address);

        Map<String, Object> params = new HashMap<>();
        params.put("user", user);

        VariableContext context = VariableContext.builder()
                .runtimeParams(params)
                .build();

        String result = resolver.resolve("User: {{user.name}}, City: {{user.address.city}}", context);
        assertEquals("User: Charlie, City: Beijing", result);
    }

    @Test
    void testSystemVariable() {
        VariableContext context = VariableContext.builder().build();

        Object now = resolver.getValue("__now", context);
        assertNotNull(now);
        assertTrue(now.toString().contains("T")); // ISO 格式包含 T
    }

    @Test
    void testMissingVariable() {
        VariableContext context = VariableContext.builder().build();

        String result = resolver.resolve("Hello {{unknown}}!", context);
        assertEquals("Hello !", result);
    }

    @Test
    void testEmptyTemplate() {
        VariableContext context = VariableContext.builder().build();

        String result = resolver.resolve("", context);
        assertEquals("", result);

        result = resolver.resolve(null, context);
        assertNull(result);
    }
}
