package com.workflow.engine.variable;

import lombok.Builder;
import lombok.Data;
import org.bsc.langgraph4j.state.AgentState;

import java.util.Map;

/**
 * 变量解析的上下文
 * 包含所有可用的变量来源
 */
@Data
@Builder
public class VariableContext {

    /** 全局状态 */
    private AgentState globalState;

    /** 系统变量提供者 */
    private SystemVariableProvider systemProvider;

    /** 原始输入参数 */
    private Map<String, Object> runtimeParams;

    /**
     * 获取系统变量提供者（懒加载）
     */
    public SystemVariableProvider getSystemProvider() {
        if (systemProvider == null) {
            systemProvider = new SystemVariableProvider();
        }
        return systemProvider;
    }
}
