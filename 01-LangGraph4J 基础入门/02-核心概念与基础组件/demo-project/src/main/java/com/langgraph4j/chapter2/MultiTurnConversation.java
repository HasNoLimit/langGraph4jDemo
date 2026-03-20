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
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;

/**
 * 多轮对话工作流示例
 *
 * 工作流：START → greet → analyze → [positive|negative|neutral] → END
 */
public class MultiTurnConversation {

    // 情感分析路由函数
    private static String routeBySentiment(AgentState state) {
        String sentiment = (String) state.value("sentiment").orElse("neutral");
        return switch (sentiment) {
            case "positive" -> "handlePositive";
            case "negative" -> "handleNegative";
            default -> "handleNeutral";
        };
    }

    public static void main(String[] args) throws Exception {
        // 1. 定义通道
        Map<String, Channel<?>> channels = new HashMap<>();
        channels.put("message", Channels.base((Supplier<String>) null));
        channels.put("sentiment", Channels.base((Supplier<String>) null));
        channels.put("response", Channels.base((Supplier<String>) null));

        // 2. 创建状态图
        StateGraph<AgentState> graph = new StateGraph<>(channels, AgentState::new);

        // 3. 添加节点

        // 问候节点
        graph.addNode("greet", node_async((state, config) -> {
            String message = (String) state.value("message").orElse("");
            System.out.println("[greet] 收到消息：" + message);
            return Map.of("greeted", true);
        }));

        // 情感分析节点（模拟）
        graph.addNode("analyze", node_async((state, config) -> {
            String message = (String) state.value("message").orElse("");
            // 简单的情感分析逻辑
            String sentiment = message.toLowerCase().contains("好") ? "positive"
                           : message.toLowerCase().contains("坏") ? "negative"
                           : "neutral";
            System.out.println("[analyze] 情感分析结果：" + sentiment);
            return Map.of("sentiment", sentiment);
        }));

        // 正面情感处理
        graph.addNode("handlePositive", node_async((state, config) -> {
            System.out.println("[handlePositive] 处理正面情感");
            return Map.of("response", "很高兴听到您的正面反馈！");
        }));

        // 负面情感处理
        graph.addNode("handleNegative", node_async((state, config) -> {
            System.out.println("[handleNegative] 处理负面情感");
            return Map.of("response", "很抱歉听到您的不愉快，我们会尽快改进。");
        }));

        // 中性情感处理
        graph.addNode("handleNeutral", node_async((state, config) -> {
            System.out.println("[handleNeutral] 处理中性情感");
            return Map.of("response", "感谢您的反馈，我们会继续努力。");
        }));

        // 4. 添加边

        // 无条件边
        graph.addEdge(START, "greet");
        graph.addEdge("greet", "analyze");

        // 条件边
        graph.addConditionalEdges(
            "analyze",
            edge_async(MultiTurnConversation::routeBySentiment),
            Map.of(
                "handlePositive", "handlePositive",
                "handleNegative", "handleNegative",
                "handleNeutral", "handleNeutral"
            )
        );

        // 结束边
        graph.addEdge("handlePositive", END);
        graph.addEdge("handleNegative", END);
        graph.addEdge("handleNeutral", END);

        // 5. 编译
        CompiledGraph<AgentState> compiled = graph.compile();

        // 6. 测试
        testConversation(compiled, "今天天气真好", "=== 测试 1：正面消息 ===");
        testConversation(compiled, "这个产品太坏了", "=== 测试 2：负面消息 ===");
        testConversation(compiled, "一般般吧", "=== 测试 3：中性消息 ===");
    }

    private static void testConversation(CompiledGraph<AgentState> compiled,
                                         String message, String label) {
        System.out.println("\n" + label);
        Map<String, Object> input = Map.of("message", message);
        Optional<AgentState> result = compiled.invoke(input);
        result.ifPresent(state -> {
            String response = (String) state.value("response").orElse("");
            System.out.println("回复：" + response);
        });
    }
}
