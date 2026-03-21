package com.langgraph4j.chapter6;

import com.langgraph4j.chapter6.monitor.WorkflowEvent;
import com.langgraph4j.chapter6.monitor.WorkflowExecutionListener;
import com.langgraph4j.chapter6.monitor.WorkflowStateTracker;
import com.langgraph4j.chapter6.monitor.WorkflowStateTracker.NodeStatus;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bsc.langgraph4j.GraphStateException;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncNodeActionWithConfig.node_async;

/**
 * 工作流执行状态监控示例
 *
 * 演示如何追踪工作流执行状态并实时推送给前台
 *
 * 核心实现思路：
 * 1. 创建 WorkflowStateTracker 追踪器实例
 * 2. 在每个节点执行前后记录事件
 * 3. 通过监听器或事件队列将事件推送给前台
 * 4. 前台可以通过轮询或 WebSocket 接收事件
 */
public class WorkflowWithMonitoring {

    /**
     * 包装节点动作，添加监控逻辑
     */
    static class MonitoredNode {

        /**
         * 创建带监控的节点
         */
        public static void createNode(
            StateGraph<AgentState> graph,
            String nodeName,
            NodeExecutor executor,
            WorkflowStateTracker tracker
        ) throws GraphStateException {
            graph.addNode(nodeName, node_async((state, config) -> {
                // 节点开始 - 记录事件
                tracker.onNodeStart(nodeName, state.data());

                try {
                    // 执行节点逻辑
                    Map<String, Object> result = executor.execute(state, tracker);

                    // 节点完成 - 记录事件
                    tracker.onNodeComplete(nodeName, result);
                    tracker.onStateUpdate(nodeName, result);

                    return result;
                } catch (Exception e) {
                    // 节点错误 - 记录事件
                    tracker.onNodeError(nodeName, e);
                    throw e;
                }
            }));
        }
    }

    @FunctionalInterface
    interface NodeExecutor {
        Map<String, Object> execute(AgentState state, WorkflowStateTracker tracker) throws Exception;
    }

    public static void main(String[] args) throws Exception, GraphStateException {
        System.out.println("=== 工作流执行状态监控示例 ===\n");

        // 1. 创建状态追踪器
        WorkflowStateTracker tracker = new WorkflowStateTracker();

        // 2. 添加事件监听器 - 打印日志
        tracker.addEventListener(event -> {
            System.out.println("📊 " + event);
        });

        // 3. 启动事件队列消费者（模拟实时推送给前台）
        Thread eventPublisher = new Thread(() -> {
            System.out.println("\n[EventPublisher] 启动，等待事件...");
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    WorkflowEvent event = tracker.getEventQueue().poll(100, TimeUnit.MILLISECONDS);
                    if (event != null) {
                        // 模拟推送到前台（实际项目中可能通过 WebSocket、SSE 等）
                        pushToFrontend(event);
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        eventPublisher.setDaemon(true);
        eventPublisher.start();

        // 4. 定义状态通道
        Map<String, Channel<?>> channels = new HashMap<>();
        channels.put("input", Channels.base((Supplier<String>) null));
        channels.put("step1Result", Channels.base((Supplier<String>) null));
        channels.put("step2Result", Channels.base((Supplier<String>) null));
        channels.put("step3Result", Channels.base((Supplier<String>) null));
        channels.put("finalOutput", Channels.base((Supplier<String>) null));

        // 5. 创建状态图
        StateGraph<AgentState> graph = new StateGraph<>(channels, AgentState::new);

        // 6. 创建带监控的节点
        MonitoredNode.createNode(graph, "step1", (state, t) -> {
            String input = (String) state.value("input").orElse("");
            System.out.println("[step1] 处理：" + input);
            // 模拟处理延迟
            Thread.sleep(200);
            String result = "Step1: " + input.toUpperCase();
            return Map.of("step1Result", result);
        }, tracker);

        MonitoredNode.createNode(graph, "step2", (state, t) -> {
            String step1Result = (String) state.value("step1Result").orElse("");
            System.out.println("[step2] 处理：" + step1Result);
            Thread.sleep(200);
            String result = "Step2: " + step1Result + " [Processed]";
            return Map.of("step2Result", result);
        }, tracker);

        MonitoredNode.createNode(graph, "step3", (state, t) -> {
            String step2Result = (String) state.value("step2Result").orElse("");
            System.out.println("[step3] 处理：" + step2Result);
            Thread.sleep(200);
            String result = "最终输出：" + step2Result;
            return Map.of("step3Result", result, "finalOutput", result);
        }, tracker);

        // 7. 添加边
        graph.addEdge(START, "step1");
        graph.addEdge("step1", "step2");
        graph.addEdge("step2", "step3");
        graph.addEdge("step3", END);

        // 8. 编译
        CompiledGraph<AgentState> compiled = graph.compile();

        // 9. 执行工作流并追踪
        String workflowId = UUID.randomUUID().toString();
        tracker.startWorkflow(workflowId, "状态监控示例工作流");

        try {
            Map<String, Object> input = Map.of("input", "hello");
            Optional<AgentState> result = compiled.invoke(input);

            tracker.onWorkflowComplete(result.map(AgentState::data).orElse(Map.of()));

            // 10. 输出最终状态
            result.ifPresent(state -> {
                System.out.println("\n=== 最终状态 ===");
                System.out.println("finalOutput: " + state.value("finalOutput").orElse(""));
            });

            // 11. 输出执行统计
            printExecutionStats(tracker);

        } catch (Exception e) {
            tracker.onWorkflowError(e);
            throw e;
        }

        // 等待事件处理完成
        Thread.sleep(500);
    }

    /**
     * 模拟将事件推送到前台
     * 实际项目中可以通过 WebSocket、SSE、HTTP 轮询等方式
     */
    private static void pushToFrontend(WorkflowEvent event) {
        // 这里只是模拟，实际项目中会发送到前端
        // 例如：websocketSession.send(event.toJson())
        System.out.println("📡 [推送前台] " + event.getType() + " - " + event.getNodeName());
    }

    /**
     * 打印执行统计信息
     */
    private static void printExecutionStats(WorkflowStateTracker tracker) {
        WorkflowStateTracker.WorkflowExecutionContext context = tracker.getCurrentContext();
        if (context == null) return;

        System.out.println("\n=== 执行统计 ===");
        System.out.println("工作流 ID: " + context.getWorkflowId());
        System.out.println("工作流名称：" + context.getWorkflowName());
        System.out.println("执行状态：" + context.getStatus());
        System.out.println("执行路径：" + String.join(" → ", context.getExecutionPath()));
        System.out.println("事件总数：" + tracker.getHistoryEvents().size());

        System.out.println("\n节点执行情况:");
        // 通过 toJson 获取节点信息（实际项目中可以添加 getter 方法）
        Map<String, Object> json = context.toJson();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) json.get("nodes");
        if (nodes != null) {
            for (Map<String, Object> node : nodes) {
                System.out.printf("  - %s: %s (耗时：%dms)%n",
                    node.get("nodeName"),
                    node.get("status"),
                    node.get("duration"));
            }
        }
    }
}
