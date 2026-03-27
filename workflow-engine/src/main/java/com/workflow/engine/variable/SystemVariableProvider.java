package com.workflow.engine.variable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 系统变量提供者
 * 提供内置的系统变量，如 __now, __date, __uuid 等
 */
public class SystemVariableProvider {

    private final Map<String, Supplier<Object>> variables = new HashMap<>();

    public SystemVariableProvider() {
        // 注册默认系统变量
        registerDefaultVariables();
    }

    private void registerDefaultVariables() {
        // 当前日期时间（ISO 格式）
        variables.put("__now", () -> LocalDateTime.now().toString());

        // 当前日期
        variables.put("__date", () -> LocalDate.now().toString());

        // 当前时间（HH:mm:ss）
        variables.put("__time", () -> LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));

        // UUID
        variables.put("__uuid", () -> UUID.randomUUID().toString());

        // 短 UUID（8位）
        variables.put("__uuid_short", () -> UUID.randomUUID().toString().substring(0, 8));

        // 时间戳（毫秒）
        variables.put("__timestamp", System::currentTimeMillis);
    }

    /**
     * 获取系统变量值
     *
     * @param name 变量名
     * @return 变量值，不存在返回 null
     */
    public Object getVariable(String name) {
        Supplier<Object> supplier = variables.get(name);
        return supplier != null ? supplier.get() : null;
    }

    /**
     * 注册自定义系统变量
     *
     * @param name     变量名
     * @param supplier 值提供者
     */
    public void register(String name, Supplier<Object> supplier) {
        variables.put(name, supplier);
    }

    /**
     * 检查变量是否存在
     *
     * @param name 变量名
     * @return 是否存在
     */
    public boolean hasVariable(String name) {
        return variables.containsKey(name);
    }
}
