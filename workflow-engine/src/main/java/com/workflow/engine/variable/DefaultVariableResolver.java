package com.workflow.engine.variable;

import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.state.AgentState;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 默认变量解析器实现
 * 支持 {{varName}} 语法和嵌套属性访问
 */
@Slf4j
public class DefaultVariableResolver implements VariableResolver {

    /** 变量占位符正则：匹配 {{variable}} 或 {{variable.property}} */
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z_][a-zA-Z0-9_.]*)\\s*}}");

    @Override
    public String resolve(String template, VariableContext context) {
        if (template == null || template.isEmpty()) {
            return template;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String variablePath = matcher.group(1);
            Object value = getValue(variablePath, context);
            String replacement = value != null ? value.toString() : "";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    @Override
    public Object getValue(String path, VariableContext context) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        // 系统变量以 __ 开头
        if (path.startsWith("__")) {
            return context.getSystemProvider().getVariable(path);
        }

        // 从全局状态获取 - 先获取 AgentState 的 data Map
        if (context.getGlobalState() != null) {
            Map<String, Object> stateData = context.getGlobalState().data();
            Object value = getNestedValue(stateData, path);
            if (value != null) {
                return value;
            }
        }

        // 从运行时参数获取
        if (context.getRuntimeParams() != null) {
            Object value = getNestedValue(context.getRuntimeParams(), path);
            if (value != null) {
                return value;
            }
        }

        return null;
    }

    /**
     * 获取嵌套属性值
     *
     * @param source 源对象（Map 或 AgentState）
     * @param path   属性路径，如 "user.name"
     * @return 属性值
     */
    @SuppressWarnings("unchecked")
    private Object getNestedValue(Object source, String path) {
        if (source == null || path == null) {
            return null;
        }

        String[] keys = path.split("\\.");
        Object current = source;

        for (String key : keys) {
            if (current == null) {
                return null;
            }

            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(key);
            } else if (current instanceof AgentState) {
                java.util.Optional<?> opt = ((AgentState) current).value(key);
                current = opt.orElse(null);
            } else {
                // 不支持其他类型的嵌套访问
                return null;
            }
        }

        return current;
    }
}
