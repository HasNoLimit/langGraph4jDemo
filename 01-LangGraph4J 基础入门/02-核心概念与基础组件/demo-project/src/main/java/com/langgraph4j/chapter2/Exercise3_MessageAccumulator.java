package com.langgraph4j.chapter2;

import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Supplier;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncNodeActionWithConfig.node_async;

/**
 * 练习 3：使用追加通道的消息累积工作流
 *
 * 工作流：START → node1 → node2 → node3 → END
 * 每个节点追加一条消息到消息列表
 */
public class Exercise3_MessageAccumulator {

    public static void main(String[] args) throws Exception {
        // 1. 定义通道
        Map<String, Channel<?>> channels = new HashMap<>();
        // 使用基础通道，在节点中手动处理列表追加
        channels.put("messages", Channels.base((Supplier<List<String>>) null));

        // 2. 创建状态图
        StateGraph<AgentState> graph = new StateGraph<>(channels, AgentState::new);

        // 3. 添加节点

        // 节点 1：添加第一条消息
        graph.addNode("node1", node_async((state, config) -> {
            System.out.println("[node1] 添加第一条消息");
            List<String> messages = new ArrayList<>();
            messages.add("第一条消息：开始处理");
            return Map.of("messages", messages);
        }));

        // 节点 2：添加第二条消息
        graph.addNode("node2", node_async((state, config) -> {
            System.out.println("[node2] 添加第二条消息");
            List<String> existing = (List<String>) state.value("messages").orElse(new ArrayList<>());
            List<String> messages = new ArrayList<>(existing);
            messages.add("第二条消息：处理中");
            return Map.of("messages", messages);
        }));

        // 节点 3：添加第三条消息
        graph.addNode("node3", node_async((state, config) -> {
            System.out.println("[node3] 添加第三条消息");
            List<String> existing = (List<String>) state.value("messages").orElse(new ArrayList<>());
            List<String> messages = new ArrayList<>(existing);
            messages.add("第三条消息：处理完成");
            return Map.of("messages", messages);
        }));

        // 4. 添加边
        graph.addEdge(START, "node1");
        graph.addEdge("node1", "node2");
        graph.addEdge("node2", "node3");
        graph.addEdge("node3", END);

        // 5. 编译
        CompiledGraph<AgentState> compiled = graph.compile();

        // 6. 执行
        System.out.println("=== 消息累积工作流执行开始 ===\n");
        Optional<AgentState> result = compiled.invoke(Map.of());

        // 7. 输出结果
        System.out.println("\n=== 最终消息列表 ===");
        result.ifPresent(state -> {
            List<?> messages = (List<?>) state.value("messages").orElse(new ArrayList<>());
            for (Object msg : messages) {
                System.out.println("  - " + msg);
            }
        });
    }
}
