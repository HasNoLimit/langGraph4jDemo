package com.workflow.engine.builtin;

import com.workflow.engine.context.NodeContext;
import com.workflow.engine.variable.DefaultVariableResolver;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 节点执行器单元测试
 */
class NodeExecutorTest {

    @Test
    void testStartNode() {
        StartNodeExecutor executor = new StartNodeExecutor();
        StartConfig config = new StartConfig();
        config.setNodeId("start1");
        config.setNodeName("开始");

        Map<String, Object> runtimeParams = new HashMap<>();
        runtimeParams.put("query", "Hello");

        NodeContext context = NodeContext.builder()
                .runtimeParams(runtimeParams)
                .variableResolver(new DefaultVariableResolver())
                .build();

        Map<String, Object> result = executor.execute(context, config);

        assertTrue(result.containsKey("query"));
        assertEquals("Hello", result.get("query"));
        assertTrue(result.containsKey("__node_start_executed"));
    }

    @Test
    void testEndNodeWithTemplate() {
        EndNodeExecutor executor = new EndNodeExecutor();
        EndConfig config = new EndConfig();
        config.setNodeId("end1");
        config.setNodeName("结束");

        Map<String, String> template = new HashMap<>();
        template.put("answer", "{{aiResponse}}");
        config.setOutputTemplate(template);

        // 使用 Map 作为运行时参数来模拟状态
        Map<String, Object> stateData = new HashMap<>();
        stateData.put("aiResponse", "This is the answer");

        NodeContext context = NodeContext.builder()
                .runtimeParams(stateData)
                .variableResolver(new DefaultVariableResolver())
                .build();

        Map<String, Object> result = executor.execute(context, config);

        assertEquals("This is the answer", result.get("answer"));
        assertEquals("completed", result.get("__status"));
    }

    @Test
    void testEndNodeWithoutTemplate() {
        EndNodeExecutor executor = new EndNodeExecutor();
        EndConfig config = new EndConfig();
        config.setNodeId("end1");

        Map<String, Object> stateData = new HashMap<>();
        stateData.put("key", "value");

        org.bsc.langgraph4j.state.AgentState state = new org.bsc.langgraph4j.state.AgentState(stateData);

        NodeContext context = NodeContext.builder()
                .globalState(state)
                .variableResolver(new DefaultVariableResolver())
                .build();

        Map<String, Object> result = executor.execute(context, config);

        assertEquals("value", result.get("key"));
        assertEquals("completed", result.get("__status"));
    }
}
