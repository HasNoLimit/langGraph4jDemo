package com.langgraph4j.chapter6;

import com.langgraph4j.chapter6.monitor.WorkflowEvent;
import com.langgraph4j.chapter6.monitor.WorkflowStateTracker;
import com.langgraph4j.chapter6.monitor.WorkflowStateTracker.NodeStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 模拟前端接收工作流执行状态推送
 *
 * 演示前端如何通过以下方式接收状态更新：
 * 1. 轮询事件队列
 * 2. 监听器回调
 * 3. 获取完整执行上下文
 */
public class FrontendStateReceiver {

    /**
     * 前端状态显示组件（模拟）
     */
    static class FrontendDisplay {

        private final AtomicBoolean running = new AtomicBoolean(true);
        private final List<WorkflowEvent> receivedEvents = new ArrayList<>();
        private WorkflowStateTracker.WorkflowExecutionContext currentContext;

        /**
         * 方式 1：轮询事件队列（模拟 HTTP 轮询）
         */
        public void pollEvents(WorkflowStateTracker tracker) {
            System.out.println("[前端] 开始轮询事件...");

            while (running.get()) {
                try {
                    WorkflowEvent event = tracker.getEventQueue().poll(100, TimeUnit.MILLISECONDS);
                    if (event != null) {
                        receivedEvents.add(event);
                        handleEvent(event);
                    }

                    // 检查是否结束
                    WorkflowStateTracker.WorkflowExecutionContext ctx = tracker.getCurrentContext();
                    if (ctx != null && (ctx.getStatus() == NodeStatus.COMPLETED ||
                        ctx.getStatus() == NodeStatus.FAILED)) {
                        // 处理完所有事件后退出
                        if (tracker.getEventQueue().isEmpty()) {
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }

            System.out.println("[前端] 轮询结束，共接收 " + receivedEvents.size() + " 个事件");
        }

        /**
         * 方式 2：通过监听器接收（模拟 WebSocket 推送）
         */
        public void subscribeToEvents(WorkflowStateTracker tracker) {
            System.out.println("[前端] 订阅事件流...");

            tracker.addEventListener(event -> {
                receivedEvents.add(event);
                handleEvent(event);
            });
        }

        /**
         * 处理单个事件
         */
        private void handleEvent(WorkflowEvent event) {
            System.out.print("[前端UI] ");

            switch (event.getType()) {
                case WORKFLOW_START:
                    System.out.println("▶️  工作流开始：" + event.getData().get("workflowId"));
                    break;

                case NODE_START:
                    System.out.println("▶️  节点开始：" + event.getNodeName());
                    updateNodeStatus(event.getNodeName(), "RUNNING");
                    break;

                case NODE_COMPLETE:
                    System.out.println("✅ 节点完成：" + event.getNodeName());
                    updateNodeStatus(event.getNodeName(), "COMPLETED");
                    break;

                case NODE_ERROR:
                    System.out.println("❌ 节点失败：" + event.getNodeName() +
                        " - " + (event.getError() != null ? event.getError().getMessage() : ""));
                    updateNodeStatus(event.getNodeName(), "FAILED");
                    break;

                case STATE_UPDATE:
                    System.out.println("📝 状态更新：" + event.getNodeName() +
                        " - " + event.getData());
                    break;

                case WORKFLOW_COMPLETE:
                    System.out.println("✅ 工作流完成！最终状态：" + event.getData());
                    showFinalResult(event.getData());
                    break;

                case WORKFLOW_ERROR:
                    System.out.println("❌ 工作流失败：" +
                        (event.getError() != null ? event.getError().getMessage() : ""));
                    break;

                default:
                    System.out.println("📊 " + event.getType() + ": " + event.getMessage());
            }
        }

        /**
         * 更新节点状态显示
         */
        private void updateNodeStatus(String nodeName, String status) {
            // 模拟前端更新节点状态
            // 实际项目中会更新 UI 组件
        }

        /**
         * 显示最终结果
         */
        private void showFinalResult(Map<String, Object> finalState) {
            System.out.println("\n===== 最终结果 =====");
            finalState.forEach((key, value) ->
                System.out.println("  " + key + ": " + value));
        }

        /**
         * 获取工作流执行上下文（用于获取完整状态）
         */
        public WorkflowStateTracker.WorkflowExecutionContext getExecutionContext(WorkflowStateTracker tracker) {
            this.currentContext = tracker.getCurrentContext();
            return this.currentContext;
        }

        /**
         * 获取所有接收的事件
         */
        public List<WorkflowEvent> getReceivedEvents() {
            return new ArrayList<>(receivedEvents);
        }

        /**
         * 停止轮询
         */
        public void stop() {
            running.set(false);
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== 前端状态接收器示例 ===\n");

        // 1. 创建追踪器
        WorkflowStateTracker tracker = new WorkflowStateTracker();

        // 2. 创建前端显示组件
        FrontendDisplay frontend = new FrontendDisplay();

        // 3. 订阅事件（模拟 WebSocket 连接）
        frontend.subscribeToEvents(tracker);

        // 4. 启动追踪器
        tracker.startWorkflow("demo-workflow-001", "前端监控演示");

        // 5. 模拟工作流节点执行
        simulateWorkflow(tracker);

        // 6. 等待事件处理完成
        Thread.sleep(500);

        // 7. 获取并显示执行上下文
        WorkflowStateTracker.WorkflowExecutionContext context = frontend.getExecutionContext(tracker);
        if (context != null) {
            System.out.println("\n===== 执行上下文 JSON（可用于前端渲染） =====");
            printJson(context.toJson(), 0);
        }
    }

    /**
     * 模拟工作流执行
     */
    private static void simulateWorkflow(WorkflowStateTracker tracker) {
        // 节点 1
        tracker.onNodeStart("validate", Map.of("input", "test data"));
        try {
            Thread.sleep(100);
            tracker.onNodeComplete("validate", Map.of("isValid", true));
            tracker.onStateUpdate("validate", Map.of("isValid", true));
        } catch (InterruptedException e) {
            tracker.onNodeError("validate", e);
            return;
        }

        // 边转换
        tracker.onEdgeTransition("validate", "process");

        // 节点 2
        tracker.onNodeStart("process", Map.of("validatedData", "test data"));
        try {
            Thread.sleep(150);
            tracker.onNodeComplete("process", Map.of("processedResult", "processed: test data"));
            tracker.onStateUpdate("process", Map.of("processedResult", "processed: test data"));
        } catch (InterruptedException e) {
            tracker.onNodeError("process", e);
            return;
        }

        // 边转换
        tracker.onEdgeTransition("process", "output");

        // 节点 3
        tracker.onNodeStart("output", Map.of("result", "processed: test data"));
        try {
            Thread.sleep(100);
            tracker.onNodeComplete("output", Map.of("finalOutput", "最终输出：processed: test data"));
            tracker.onStateUpdate("output", Map.of("finalOutput", "最终输出：processed: test data"));
        } catch (InterruptedException e) {
            tracker.onNodeError("output", e);
            return;
        }

        // 工作流完成
        tracker.onWorkflowComplete(Map.of(
            "finalOutput", "最终输出：processed: test data",
            "status", "SUCCESS"
        ));
    }

    /**
     * 简单打印 JSON（用于演示）
     */
    private static void printJson(Map<String, Object> json, int indent) {
        String prefix = "  ".repeat(indent);
        for (Map.Entry<String, Object> entry : json.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                System.out.println(prefix + "\"" + entry.getKey() + "\": {");
                printJson((Map<String, Object>) value, indent + 1);
                System.out.println(prefix + "}");
            } else if (value instanceof List) {
                System.out.println(prefix + "\"" + entry.getKey() + "\": [");
                for (Object item : (List<?>) value) {
                    if (item instanceof Map) {
                        System.out.print(prefix + "  ");
                        printJson((Map<String, Object>) item, 0);
                    } else {
                        System.out.println(prefix + "  " + item);
                    }
                }
                System.out.println(prefix + "]");
            } else {
                System.out.println(prefix + "\"" + entry.getKey() + "\": " + value);
            }
        }
    }
}
