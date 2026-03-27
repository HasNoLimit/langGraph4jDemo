package com.workflow.engine.executor;

import com.workflow.engine.config.NodeConfig;
import com.workflow.engine.context.NodeContext;

import java.util.Map;

/**
 * 节点执行器接口
 * 每个具体节点必须实现此接口，并注册为 Spring Bean
 *
 * @param <C> 节点配置类型
 */
public interface NodeExecutor<C extends NodeConfig> {

    /**
     * 节点类型标识，如 "start", "llm", "end"
     * 用于 JSON 中 "type" 字段匹配
     */
    String getType();

    /**
     * 配置类类型，用于 JSON 反序列化
     */
    Class<C> getConfigClass();

    /**
     * 执行节点逻辑
     *
     * @param context 执行上下文（包含全局状态、变量等）
     * @param config  节点配置
     * @return 节点输出，会被合并到全局状态
     */
    Map<String, Object> execute(NodeContext context, C config);

    /**
     * 校验配置是否合法（可选实现）
     *
     * @param config 节点配置
     * @throws IllegalArgumentException 配置不合法时抛出
     */
    default void validate(C config) {
        // 默认不执行校验，子类可覆盖
    }
}
