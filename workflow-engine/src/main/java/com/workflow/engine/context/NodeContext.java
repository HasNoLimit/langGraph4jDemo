package com.workflow.engine.context;

import com.workflow.engine.variable.VariableResolver;
import lombok.Builder;
import lombok.Data;
import org.bsc.langgraph4j.state.AgentState;

import java.util.Map;

/**
 * 节点执行时传入的上下文
 * 封装执行所需的全部信息
 */
@Data
@Builder
public class NodeContext {

    /** 当前工作流实例ID */
    private String workflowId;

    /** 当前节点ID */
    private String nodeId;

    /** 全局状态（AgentState） */
    private AgentState globalState;

    /** 变量解析器 */
    private VariableResolver variableResolver;

    /** 原始输入参数 */
    private Map<String, Object> runtimeParams;
}
