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
 * 练习 2：条件分支 - 奇偶数判断
 *
 * 工作流：START → check → [odd|even] → END
 */
public class Exercise2_ConditionalBranch {

    // 路由函数：判断奇偶
    private static String routeByParity(AgentState state) {
        Integer number = (Integer) state.value("number").orElse(0);
        return number % 2 == 0 ? "even" : "odd";
    }

    public static void main(String[] args) throws Exception {
        // 1. 定义通道
        Map<String, Channel<?>> channels = new HashMap<>();
        channels.put("number", Channels.base((Supplier<Integer>) null));
        channels.put("result", Channels.base((Supplier<String>) null));

        // 2. 创建状态图
        StateGraph<AgentState> graph = new StateGraph<>(channels, AgentState::new);

        // 3. 添加节点

        // 检查节点
        graph.addNode("check", node_async((state, config) -> {
            Integer number = (Integer) state.value("number").orElse(0);
            System.out.println("[check] 检查数字：" + number);
            return Map.of();
        }));

        // 奇数处理节点
        graph.addNode("odd", node_async((state, config) -> {
            System.out.println("[odd] 处理奇数");
            return Map.of("result", "奇数");
        }));

        // 偶数处理节点
        graph.addNode("even", node_async((state, config) -> {
            System.out.println("[even] 处理偶数");
            return Map.of("result", "偶数");
        }));

        // 4. 添加边
        graph.addEdge(START, "check");

        // 条件边
        graph.addConditionalEdges(
            "check",
            edge_async(Exercise2_ConditionalBranch::routeByParity),
            Map.of(
                "odd", "odd",
                "even", "even"
            )
        );

        // 结束边
        graph.addEdge("odd", END);
        graph.addEdge("even", END);

        // 5. 编译
        CompiledGraph<AgentState> compiled = graph.compile();

        // 6. 测试
        testNumber(compiled, 7, "=== 测试 1：奇数 ===");
        testNumber(compiled, 10, "=== 测试 2：偶数 ===");
        testNumber(compiled, 0, "=== 测试 3：零 ===");
    }

    private static void testNumber(CompiledGraph<AgentState> compiled,
                                   Integer number, String label) {
        System.out.println("\n" + label);
        Map<String, Object> input = Map.of("number", number);
        Optional<AgentState> result = compiled.invoke(input);
        result.ifPresent(state -> {
            String resultStr = (String) state.value("result").orElse("");
            System.out.println("结果：" + number + " 是 " + resultStr);
        });
    }
}
