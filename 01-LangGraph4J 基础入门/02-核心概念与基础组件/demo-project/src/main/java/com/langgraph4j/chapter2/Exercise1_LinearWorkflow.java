package com.langgraph4j.chapter2;

import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Supplier;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncNodeActionWithConfig.node_async;

/**
 * 练习 1：三节点线性工作流
 *
 * 工作流：START → input → process → output → END
 */
public class Exercise1_LinearWorkflow {

    public static void main(String[] args) throws Exception {
        // 1. 定义通道
        Map<String, Channel<?>> channels = new HashMap<>();
        channels.put("input", Channels.base((Supplier<String>) null));
        channels.put("processed", Channels.base((Supplier<String>) null));
        channels.put("output", Channels.base((Supplier<String>) null));

        // 2. 创建状态图
        StateGraph<AgentState> graph = new StateGraph<>(channels, AgentState::new);

        // 3. 添加节点

        // input 节点：接收并打印输入
        graph.addNode("input", node_async((state, config) -> {
            String input = (String) state.value("input").orElse("");
            System.out.println("[input] 接收输入：" + input);
            return Map.of();
        }));

        // process 节点：将文本转为大写
        graph.addNode("process", node_async((state, config) -> {
            String input = (String) state.value("input").orElse("");
            String processed = input.toUpperCase();
            System.out.println("[process] 处理结果：" + processed);
            return Map.of("processed", processed);
        }));

        // output 节点：输出最终结果
        graph.addNode("output", node_async((state, config) -> {
            String processed = (String) state.value("processed").orElse("");
            String output = "最终输出：" + processed;
            System.out.println("[output] " + output);
            return Map.of("output", output);
        }));

        // 4. 添加边
        graph.addEdge(START, "input");
        graph.addEdge("input", "process");
        graph.addEdge("process", "output");
        graph.addEdge("output", END);

        // 5. 编译
        CompiledGraph<AgentState> compiled = graph.compile();

        // 6. 执行
        Map<String, Object> inputData = Map.of("input", "Hello, LangGraph4J!");
        Optional<AgentState> result = compiled.invoke(inputData);

        // 7. 输出结果
        result.ifPresent(state -> {
            String output = (String) state.value("output").orElse("");
            System.out.println("\n=== 最终结果 ===");
            System.out.println(output);
        });
    }
}
