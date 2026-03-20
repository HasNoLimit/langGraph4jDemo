package com.langgraph4j.chapter3;

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
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;

/**
 * 示例 2：订单处理工作流
 *
 * 综合演示状态管理、条件路由、错误处理等概念
 */
public class OrderProcessingWorkflow {

    // 路由函数：根据验证结果决定流程
    private static String routeAfterValidate(AgentState state) {
        Boolean isValid = (Boolean) state.value("isValid").orElse(false);
        return isValid ? "calculate" : "handleError";
    }

    public static void main(String[] args) throws Exception {
        // 1. 定义状态通道
        Map<String, Channel<?>> channels = new HashMap<>();

        // 订单信息
        channels.put("orderId", Channels.base((Supplier<String>) null));
        channels.put("customerId", Channels.base((Supplier<String>) null));
        channels.put("amount", Channels.base((Supplier<Double>) null));
        channels.put("items", Channels.base((Supplier<List<String>>) null));

        // 处理状态
        channels.put("isValid", Channels.base((Supplier<Boolean>) null));
        channels.put("validationErrors", Channels.base((Supplier<List<String>>) null));
        channels.put("finalAmount", Channels.base((Supplier<Double>) null));
        channels.put("orderStatus", Channels.base((Supplier<String>) null));
        channels.put("notificationSent", Channels.base((Supplier<Boolean>) null));

        // 2. 创建状态图
        StateGraph<AgentState> graph = new StateGraph<>(channels, AgentState::new);

        // 3. 添加节点

        // 验证节点
        graph.addNode("validate", node_async((state, config) -> {
            System.out.println("=== 验证订单 ===");
            List<String> errors = new ArrayList<>();

            String orderId = (String) state.value("orderId").orElse("");
            Double amount = (Double) state.value("amount").orElse(0.0);

            if (orderId.isEmpty()) {
                errors.add("订单 ID 不能为空");
            }
            if (amount <= 0) {
                errors.add("订单金额必须大于 0");
            }

            boolean isValid = errors.isEmpty();
            System.out.println("验证结果：" + (isValid ? "通过" : "失败"));

            return Map.of(
                "isValid", isValid,
                "validationErrors", errors
            );
        }));

        // 计算节点
        graph.addNode("calculate", node_async((state, config) -> {
            System.out.println("=== 计算金额 ===");
            Double amount = (Double) state.value("amount").orElse(0.0);

            // 计算折扣
            double discount = amount > 1000 ? 0.9 : 1.0;
            double finalAmount = amount * discount;

            if (discount < 1.0) {
                System.out.println("应用 9 折优惠");
            }

            System.out.println("原始金额：" + amount + ", 最终金额：" + finalAmount);

            return Map.of("finalAmount", finalAmount);
        }));

        // 处理节点
        graph.addNode("process", node_async((state, config) -> {
            System.out.println("=== 处理订单 ===");
            String orderId = (String) state.value("orderId").orElse("");
            Double finalAmount = (Double) state.value("finalAmount").orElse(0.0);

            // 模拟订单处理
            System.out.println("处理订单：" + orderId);
            System.out.println("收款金额：" + finalAmount);

            return Map.of("orderStatus", "completed");
        }));

        // 通知节点
        graph.addNode("notify", node_async((state, config) -> {
            System.out.println("=== 发送通知 ===");
            String orderId = (String) state.value("orderId").orElse("");
            String status = (String) state.value("orderStatus").orElse("");

            System.out.println("订单 " + orderId + " 状态：" + status);
            System.out.println("已发送确认邮件给客户");

            return Map.of("notificationSent", true);
        }));

        // 错误处理节点
        graph.addNode("handleError", node_async((state, config) -> {
            System.out.println("=== 处理错误 ===");
            List<String> errors = (List<String>) state.value("validationErrors")
                .orElse(new ArrayList<>());

            System.out.println("验证失败，错误列表：");
            for (String error : errors) {
                System.out.println("  - " + error);
            }

            return Map.of("orderStatus", "failed");
        }));

        // 4. 添加边
        graph.addEdge(START, "validate");

        // 条件边：根据验证结果分流
        graph.addConditionalEdges(
            "validate",
            edge_async(OrderProcessingWorkflow::routeAfterValidate),
            Map.of(
                "calculate", "calculate",
                "handleError", "handleError"
            )
        );

        // 正常流程
        graph.addEdge("calculate", "process");
        graph.addEdge("process", "notify");
        graph.addEdge("notify", END);

        // 错误流程
        graph.addEdge("handleError", END);

        // 5. 编译
        CompiledGraph<AgentState> compiled = graph.compile();

        // 6. 测试
        testValidOrder(compiled, "ORDER-001", "CUST-001", 1500.0, "=== 测试 1：有效订单 ===");
        testInvalidOrder(compiled, "", "CUST-002", 0.0, "=== 测试 2：无效订单 ===");
    }

    private static void testValidOrder(CompiledGraph<AgentState> compiled,
                                       String orderId, String customerId,
                                       Double amount, String label) {
        System.out.println("\n" + label);

        List<String> items = List.of("商品 A", "商品 B");
        Map<String, Object> input = Map.of(
            "orderId", orderId,
            "customerId", customerId,
            "amount", amount,
            "items", items
        );

        Optional<AgentState> result = compiled.invoke(input);

        result.ifPresent(state -> {
            String status = (String) state.value("orderStatus").orElse("");
            Boolean notificationSent = (Boolean) state.value("notificationSent").orElse(false);
            System.out.println("\n最终状态：" + status);
            System.out.println("通知已发送：" + notificationSent);
        });
    }

    private static void testInvalidOrder(CompiledGraph<AgentState> compiled,
                                         String orderId, String customerId,
                                         Double amount, String label) {
        System.out.println("\n" + label);

        Map<String, Object> input = Map.of(
            "orderId", orderId,
            "customerId", customerId,
            "amount", amount
        );

        Optional<AgentState> result = compiled.invoke(input);

        result.ifPresent(state -> {
            String status = (String) state.value("orderStatus").orElse("");
            System.out.println("\n最终状态：" + status);
        });
    }
}
