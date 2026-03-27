package com.workflow.engine;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.state.AgentState;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 工作流实例
 * 封装编译后的执行图，提供执行接口
 */
@Data
@AllArgsConstructor
public class WorkflowInstance {

    private final String workflowId;
    private final String workflowName;
    private final CompiledGraph<AgentState> compiledGraph;

    /**
     * 执行工作流
     *
     * @param inputs 输入参数
     * @return 最终状态
     */
    public Optional<AgentState> execute(Map<String, Object> inputs) {
        // 添加运行时参数到状态
        Map<String, Object> initialState = new HashMap<>();
        if (inputs != null) {
            initialState.putAll(inputs);
        }
        initialState.put("__workflow_id", workflowId);
        initialState.put("__runtime_params", inputs != null ? inputs : new HashMap<>());

        return compiledGraph.invoke(initialState);
    }

    /**
     * 带配置的执行
     *
     * @param inputs 输入参数
     * @param config 运行配置
     * @return 最终状态
     */
    public Optional<AgentState> execute(Map<String, Object> inputs, RunnableConfig config) {
        Map<String, Object> initialState = new HashMap<>();
        if (inputs != null) {
            initialState.putAll(inputs);
        }
        initialState.put("__workflow_id", workflowId);
        initialState.put("__runtime_params", inputs != null ? inputs : new HashMap<>());

        return compiledGraph.invoke(initialState, config);
    }
}
