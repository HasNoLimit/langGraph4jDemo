package com.workflow.engine.variable;

/**
 * 变量解析器接口
 * 负责将模板字符串中的变量占位符替换为实际值
 */
public interface VariableResolver {

    /**
     * 解析模板字符串
     *
     * @param template 包含 {{variable}} 的模板
     * @param context  变量上下文
     * @return 解析后的字符串
     */
    String resolve(String template, VariableContext context);

    /**
     * 从路径获取值（支持嵌套）
     *
     * @param path    变量路径，如 "user.address.city"
     * @param context 变量上下文
     * @return 值，不存在返回 null
     */
    Object getValue(String path, VariableContext context);
}
