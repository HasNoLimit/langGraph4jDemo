package com.langgraph4j.demo;

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
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeActionWithConfig.node_async;

/**
 * 带条件分支的工作流示例
 *
 * 根据用户类型 (VIP/普通) 输出不同的问候语
 *
 * 工作流:
 *           ┌─────────────┐
 *           │    START    │
 *           └──────┬──────┘
 *                  │
 *         ┌────────┴────────┐
 *         ▼                 ▼
 *   ┌───────────┐     ┌───────────┐
 *   │ vipGreet  │     │normalGreet│
 *   └─────┬─────┘     └─────┬─────┘
 *         │                 │
 *         └────────┬────────┘
 *                  ▼
 *           ┌───────────┐
 *           │    END    │
 *           └───────────┘
 */
public class ConditionalGreeting {

    // 路由函数：根据用户类型决定下一个节点（同步版本）
    public static String route(AgentState state) {
        String userType = (String) state.value("userType").orElse("normal");
        if ("vip".equals(userType)) {
            return "vipGreet";
        } else {
            return "normalGreet";
        }
    }

    public static void main(String[] args) throws Exception {
        // 1. 定义状态通道
        Map<String, Channel<?>> channels = new HashMap<>();
        channels.put("userType", Channels.base((Supplier<String>) null));
        channels.put("greeting", Channels.base((Supplier<String>) null));

        // 2. 创建状态图
        StateGraph<AgentState> graph = new StateGraph<>(channels, AgentState::new);

        // 3. VIP 问候节点
        graph.addNode("vipGreet", node_async((state, config) -> {
            String greeting = "尊敬的 VIP 用户，欢迎您的光临！";
            System.out.println("[vipGreet] " + greeting);
            return Map.of("greeting", greeting);
        }));

        // 4. 普通用户问候节点
        graph.addNode("normalGreet", node_async((state, config) -> {
            String greeting = "您好，欢迎光临！";
            System.out.println("[normalGreet] " + greeting);
            return Map.of("greeting", greeting);
        }));

        // 5. 添加条件边 - edge_async 将同步函数转换为异步 Action
        graph.addConditionalEdges(START, edge_async(ConditionalGreeting::route),
            Map.of("vipGreet", "vipGreet", "normalGreet", "normalGreet"));

        // 6. 添加结束边
        graph.addEdge("vipGreet", END);
        graph.addEdge("normalGreet", END);

        // 7. 编译
        CompiledGraph<AgentState> compiled = graph.compile();

        // 8. 测试 VIP 用户 - invoke 返回 Optional<AgentState>
        System.out.println("=== VIP 用户测试 ===");
        Map<String, Object> vipInput = new HashMap<>();
        vipInput.put("userType", "vip");
        Optional<AgentState> vipResult = compiled.invoke(vipInput);
        System.out.println("结果：" + vipResult.map(s -> (String) s.value("greeting").orElse("")).orElse("无结果"));

        // 9. 测试普通用户
        System.out.println("\n=== 普通用户测试 ===");
        Map<String, Object> normalInput = new HashMap<>();
        normalInput.put("userType", "normal");
        Optional<AgentState> normalResult = compiled.invoke(normalInput);
        System.out.println("结果：" + normalResult.map(s -> (String) s.value("greeting").orElse("")).orElse("无结果"));

        // 10. 测试默认用户 (null 按普通用户处理)
        System.out.println("\n=== 默认用户测试 ===");
        Map<String, Object> defaultInput = new HashMap<>();
        defaultInput.put("userType", null);
        Optional<AgentState> defaultResult = compiled.invoke(defaultInput);
        System.out.println("结果：" + defaultResult.map(s -> (String) s.value("greeting").orElse("")).orElse("无结果"));
    }
}
