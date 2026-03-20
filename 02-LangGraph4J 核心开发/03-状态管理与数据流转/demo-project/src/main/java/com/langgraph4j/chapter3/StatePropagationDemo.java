package com.langgraph4j.chapter3;

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
 * 示例 1：状态传递演示
 *
 * 展示状态在工作流节点之间如何传递和更新
 */
public class StatePropagationDemo {

    public static void main(String[] args) throws Exception {
        // 1. 定义通道
        Map<String, Channel<?>> channels = new HashMap<>();
        channels.put("input", Channels.base((Supplier<String>) null));
        channels.put("step1Result", Channels.base((Supplier<String>) null));
        channels.put("step2Result", Channels.base((Supplier<String>) null));
        channels.put("finalOutput", Channels.base((Supplier<String>) null));

        // 2. 创建状态图
        StateGraph<AgentState> graph = new StateGraph<>(channels, AgentState::new);

        // 3. 添加节点 - 每个节点更新不同的字段
        graph.addNode("step1", node_async((state, config) -> {
            String input = (String) state.value("input").orElse("");
            String result = "Step1: " + input.toUpperCase();
            System.out.println("[step1] 输入：" + input + " → 输出：" + result);
            return Map.of("step1Result", result);
        }));

        graph.addNode("step2", node_async((state, config) -> {
            String step1Result = (String) state.value("step1Result").orElse("");
            String result = "Step2: " + step1Result + " [Processed]";
            System.out.println("[step2] 输入：" + step1Result + " → 输出：" + result);
            return Map.of("step2Result", result);
        }));

        graph.addNode("step3", node_async((state, config) -> {
            String step2Result = (String) state.value("step2Result").orElse("");
            String result = "最终输出：" + step2Result;
            System.out.println("[step3] 输入：" + step2Result + " → 输出：" + result);
            return Map.of("finalOutput", result);
        }));

        // 4. 添加边
        graph.addEdge(START, "step1");
        graph.addEdge("step1", "step2");
        graph.addEdge("step2", "step3");
        graph.addEdge("step3", END);

        // 5. 编译
        CompiledGraph<AgentState> compiled = graph.compile();

        // 6. 执行
        Map<String, Object> inputData = Map.of("input", "hello");
        Optional<AgentState> result = compiled.invoke(inputData);

        // 7. 查看最终状态
        result.ifPresent(state -> {
            System.out.println("\n=== 最终状态 ===");
            System.out.println("input: " + state.value("input").orElse(""));
            System.out.println("step1Result: " + state.value("step1Result").orElse(""));
            System.out.println("step2Result: " + state.value("step2Result").orElse(""));
            System.out.println("finalOutput: " + state.value("finalOutput").orElse(""));
        });
    }
}
