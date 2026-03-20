# 第 5 章 复杂工作流设计

## 学习目标

完成本章后，你将能够：

1. 掌握复杂条件分支的设计模式
2. 实现循环和迭代工作流
3. 理解并行节点调度与执行机制
4. 掌握子图的封装与复用方法
5. 设计带人工干预节点的工作流

## 建议学时

**2 课时** (90 分钟)

- 理论学习：50 分钟
- 实践操作：40 分钟

---

## 5.1 复杂条件分支

### 5.1.1 多条件路由

在实际应用中，工作流往往需要根据多个条件决定执行路径：

```java
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
 * 多条件路由示例：订单处理
 */
public class MultiConditionRouting {

    // 路由函数：根据订单金额和类型决定处理方式
    private static String routeOrder(AgentState state) {
        Double amount = (Double) state.value("amount").orElse(0.0);
        String orderType = (String) state.value("orderType").orElse("standard");

        // 大额订单需要审批
        if (amount >= 10000) {
            return "needsApproval";
        }

        // 根据订单类型分流
        return switch (orderType) {
            case "express" -> "expressProcess";
            case "bulk" -> "bulkProcess";
            default -> "standardProcess";
        };
    }

    public static void main(String[] args) throws Exception {
        // 1. 定义状态通道
        Map<String, Channel<?>> channels = new HashMap<>();
        channels.put("orderId", Channels.base((Supplier<String>) null));
        channels.put("amount", Channels.base((Supplier<Double>) null));
        channels.put("orderType", Channels.base((Supplier<String>) null));
        channels.put("processResult", Channels.base((Supplier<String>) null));

        // 2. 创建状态图
        StateGraph<AgentState> graph = new StateGraph<>(channels, AgentState::new);

        // 3. 添加节点

        // 需要审批的节点
        graph.addNode("needsApproval", node_async((state, config) -> {
            Double amount = (Double) state.value("amount").orElse(0.0);
            System.out.println("[needsApproval] 大额订单待审批，金额：" + amount);
            // 模拟审批通过
            return Map.of("processResult", "已审批，进入标准流程");
        }));

        // 快速处理
        graph.addNode("expressProcess", node_async((state, config) -> {
            String orderId = (String) state.value("orderId").orElse("");
            System.out.println("[expressProcess] 快速处理订单：" + orderId);
            return Map.of("processResult", "快速处理完成");
        }));

        // 批量处理
        graph.addNode("bulkProcess", node_async((state, config) -> {
            String orderId = (String) state.value("orderId").orElse("");
            System.out.println("[bulkProcess] 批量处理订单：" + orderId);
            return Map.of("processResult", "批量处理完成");
        }));

        // 标准处理
        graph.addNode("standardProcess", node_async((state, config) -> {
            String orderId = (String) state.value("orderId").orElse("");
            System.out.println("[standardProcess] 标准处理订单：" + orderId);
            return Map.of("processResult", "标准处理完成");
        }));

        // 4. 添加边
        graph.addEdge(START, "route");

        // 条件路由
        graph.addConditionalEdges(
            "route",
            edge_async(MultiConditionRouting::routeOrder),
            Map.of(
                "needsApproval", "needsApproval",
                "expressProcess", "expressProcess",
                "bulkProcess", "bulkProcess",
                "standardProcess", "standardProcess"
            )
        );

        // 所有分支结束
        graph.addEdge("needsApproval", END);
        graph.addEdge("expressProcess", END);
        graph.addEdge("bulkProcess", END);
        graph.addEdge("standardProcess", END);

        // 5. 编译
        CompiledGraph<AgentState> compiled = graph.compile();

        System.out.println("第 5 章 复杂工作流设计 - 示例程序");
        System.out.println("=====================================");
    }
}
```

### 5.1.2 嵌套条件分支

```java
// 第一层路由：根据用户类型
private static String routeByUserType(AgentState state) {
    String userType = (String) state.value("userType").orElse("guest");
    return switch (userType) {
        case "vip" -> "vipHandler";
        case "member" -> "checkMemberLevel";  // 进入第二层判断
        default -> "guestHandler";
    };
}

// 第二层路由：根据会员等级
private static String checkMemberLevel(AgentState state) {
    Integer level = (Integer) state.value("memberLevel").orElse(1);
    return level >= 3 ? "goldMemberHandler" : "silverMemberHandler";
}
```

---

## 5.2 循环与迭代工作流

### 5.2.1 使用状态实现简单循环

```java
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
 * 循环工作流示例：批量处理列表
 */
public class LoopWorkflow {

    // 路由函数：检查是否还有项目需要处理
    private static String checkNextItem(AgentState state) {
        List<String> items = (List<String>) state.value("items").orElse(new ArrayList<>());
        Integer currentIndex = (Integer) state.value("currentIndex").orElse(0);

        if (currentIndex < items.size()) {
            return "processItem";
        } else {
            return "finalize";
        }
    }

    public static void main(String[] args) throws Exception {
        // 1. 定义状态通道
        Map<String, Channel<?>> channels = new HashMap<>();
        channels.put("items", Channels.base((Supplier<List<String>>) null));
        channels.put("currentIndex", Channels.base((Supplier<Integer>) null));
        channels.put("processedItems", Channels.base((Supplier<List<String>>) null));
        channels.put("finalResult", Channels.base((Supplier<String>) null));

        // 2. 创建状态图
        StateGraph<AgentState> graph = new StateGraph<>(channels, AgentState::new);

        // 3. 添加节点

        // 初始化节点
        graph.addNode("init", node_async((state, config) -> {
            System.out.println("[init] 初始化循环");
            return Map.of("currentIndex", 0, "processedItems", new ArrayList<>());
        }));

        // 处理单个项目
        graph.addNode("processItem", node_async((state, config) -> {
            List<String> items = (List<String>) state.value("items").orElse(new ArrayList<>());
            Integer currentIndex = (Integer) state.value("currentIndex").orElse(0);

            if (currentIndex < items.size()) {
                String currentItem = items.get(currentIndex);
                System.out.println("[processItem] 处理第 " + (currentIndex + 1) + " 项：" + currentItem);

                // 更新已处理列表
                List<String> processed = (List<String>) state.value("processedItems")
                    .orElse(new ArrayList<>());
                List<String> newProcessed = new ArrayList<>(processed);
                newProcessed.add(currentItem + " [已处理]");

                return Map.of(
                    "currentIndex", currentIndex + 1,
                    "processedItems", newProcessed
                );
            }

            return Map.of();
        }));

        // 最终汇总
        graph.addNode("finalize", node_async((state, config) -> {
            List<String> processed = (List<String>) state.value("processedItems")
                .orElse(new ArrayList<>());
            System.out.println("[finalize] 处理完成，共处理 " + processed.size() + " 项");

            String result = "处理完成：" + String.join(", ", processed);
            return Map.of("finalResult", result);
        }));

        // 4. 添加边
        graph.addEdge(START, "init");

        // 条件边：检查是否有下一个项目
        graph.addConditionalEdges(
            "init",
            edge_async(LoopWorkflow::checkNextItem),
            Map.of(
                "processItem", "processItem",
                "finalize", "finalize"
            )
        );

        // 处理完一个项目后，回到检查
        graph.addConditionalEdges(
            "processItem",
            edge_async(LoopWorkflow::checkNextItem),
            Map.of(
                "processItem", "processItem",
                "finalize", "finalize"
            )
        );

        graph.addEdge("finalize", END);

        // 5. 编译
        CompiledGraph<AgentState> compiled = graph.compile();

        // 6. 测试
        List<String> items = List.of("任务 A", "任务 B", "任务 C", "任务 D");
        Map<String, Object> input = new HashMap<>();
        input.put("items", items);

        System.out.println("=== 循环工作流测试 ===\n");
        Optional<AgentState> result = compiled.invoke(input);

        result.ifPresent(state -> {
            String finalResult = (String) state.value("finalResult").orElse("");
            System.out.println("\n" + finalResult);
        });
    }
}
```

**运行输出：**
```
=== 循环工作流测试 ===

[init] 初始化循环
[processItem] 处理第 1 项：任务 A
[processItem] 处理第 2 项：任务 B
[processItem] 处理第 3 项：任务 C
[processItem] 处理第 4 项：任务 D
[finalize] 处理完成，共处理 4 项

处理完成：任务 A [已处理], 任务 B [已处理], 任务 C [已处理], 任务 D [已处理]
```

---

## 5.3 并行节点设计

### 5.3.1 并行执行模式

在某些场景下，我们需要多个节点并行执行：

```java
import java.util.concurrent.*;

/**
 * 并行执行工具类
 */
public class ParallelExecutor {

    private static final ExecutorService executor = Executors.newFixedThreadPool(4);

    /**
     * 并行执行多个任务
     */
    public static <T> List<T> executeParallel(List<Callable<T>> tasks) throws Exception {
        List<Future<T>> futures = executor.invokeAll(tasks);

        List<T> results = new ArrayList<>();
        for (Future<T> future : futures) {
            results.add(future.get());
        }
        return results;
    }

    public static void shutdown() {
        executor.shutdown();
    }
}
```

### 5.3.2 在工作流中实现并行

```java
import java.util.concurrent.*;

/**
 * 并行工作流示例
 */
public class ParallelWorkflow {

    public static void main(String[] args) throws Exception {
        Map<String, Channel<?>> channels = new HashMap<>();
        channels.put("input", Channels.base((Supplier<String>) null));
        channels.put("result1", Channels.base((Supplier<String>) null));
        channels.put("result2", Channels.base((Supplier<String>) null));
        channels.put("result3", Channels.base((Supplier<String>) null));
        channels.put("mergedResult", Channels.base((Supplier<String>) null));

        StateGraph<AgentState> graph = new StateGraph<>(channels, AgentState::new);

        // 并行处理节点 1
        graph.addNode("parallelTask1", node_async((state, config) -> {
            String input = (String) state.value("input").orElse("");
            System.out.println("[parallelTask1] 处理：" + input);
            Thread.sleep(100); // 模拟耗时操作
            return Map.of("result1", "任务 1 完成：" + input);
        }));

        // 并行处理节点 2
        graph.addNode("parallelTask2", node_async((state, config) -> {
            String input = (String) state.value("input").orElse("");
            System.out.println("[parallelTask2] 处理：" + input);
            Thread.sleep(100);
            return Map.of("result2", "任务 2 完成：" + input);
        }));

        // 并行处理节点 3
        graph.addNode("parallelTask3", node_async((state, config) -> {
            String input = (String) state.value("input").orElse("");
            System.out.println("[parallelTask3] 处理：" + input);
            Thread.sleep(100);
            return Map.of("result3", "任务 3 完成：" + input);
        }));

        // 合并结果
        graph.addNode("merge", node_async((state, config) -> {
            String r1 = (String) state.value("result1").orElse("");
            String r2 = (String) state.value("result2").orElse("");
            String r3 = (String) state.value("result3").orElse("");

            String merged = String.join(" | ", r1, r2, r3);
            System.out.println("[merge] 合并结果");

            return Map.of("mergedResult", merged);
        }));

        // 添加边 - 并行分叉
        graph.addEdge(START, "parallelTask1");
        graph.addEdge("parallelTask1", "parallelTask2");
        graph.addEdge("parallelTask2", "parallelTask3");
        graph.addEdge("parallelTask3", "merge");
        graph.addEdge("merge", END);

        CompiledGraph<AgentState> compiled = graph.compile();

        System.out.println("=== 并行工作流示例 ===");
        Optional<AgentState> result = compiled.invoke(Map.of("input", "测试数据"));

        result.ifPresent(state -> {
            String merged = (String) state.value("mergedResult").orElse("");
            System.out.println("\n最终结果：" + merged);
        });
    }
}
```

---

## 5.4 子图封装与复用

### 5.4.1 子图的概念

子图（Subgraph）是将一个完整的工作流封装为一个节点，在更大的工作流中复用。

### 5.4.2 创建可复用的子图

```java
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.CompiledGraph;

/**
 * 子图示例：用户验证流程
 */
public class UserValidationSubgraph {

    /**
     * 创建用户验证子图
     * @return 编译后的验证流程子图
     */
    public static CompiledGraph<AgentState> createValidationGraph() {
        Map<String, Channel<?>> channels = new HashMap<>();
        channels.put("userId", Channels.base((Supplier<String>) null));
        channels.put("token", Channels.base((Supplier<String>) null));
        channels.put("isValid", Channels.base((Supplier<Boolean>) null));
        channels.put("validationMessage", Channels.base((Supplier<String>) null));

        StateGraph<AgentState> graph = new StateGraph<>(channels, AgentState::new);

        // 验证用户 ID
        graph.addNode("validateUserId", node_async((state, config) -> {
            String userId = (String) state.value("userId").orElse("");
            boolean isValid = !userId.isEmpty() && userId.length() >= 3;

            if (!isValid) {
                return Map.of(
                    "isValid", false,
                    "validationMessage", "用户 ID 无效"
                );
            }

            return Map.of("validationMessage", "用户 ID 验证通过");
        }));

        // 验证 Token
        graph.addNode("validateToken", node_async((state, config) -> {
            String token = (String) state.value("token").orElse("");
            boolean isValid = token != null && token.startsWith("valid_");

            if (!isValid) {
                return Map.of(
                    "isValid", false,
                    "validationMessage", "Token 无效"
                );
            }

            return Map.of(
                "isValid", true,
                "validationMessage", "Token 验证通过"
            );
        }));

        // 添加边
        graph.addEdge(START, "validateUserId");
        graph.addEdge("validateUserId", "validateToken");
        graph.addEdge("validateToken", END);

        return graph.compile();
    }

    /**
     * 在主工作流中使用子图
     */
    public static void main(String[] args) throws Exception {
        // 获取验证子图
        CompiledGraph<AgentState> validationGraph = createValidationGraph();

        // 主工作流
        Map<String, Channel<?>> mainChannels = new HashMap<>();
        mainChannels.put("userId", Channels.base((Supplier<String>) null));
        mainChannels.put("token", Channels.base((Supplier<String>) null));
        mainChannels.put("authResult", Channels.base((Supplier<String>) null));

        StateGraph<AgentState> mainGraph = new StateGraph<>(mainChannels, AgentState::new);

        // 添加子图作为节点
        graph.addNode("authenticate", node_async((state, config) -> {
            // 准备子图输入
            String userId = (String) state.value("userId").orElse("");
            String token = (String) state.value("token").orElse("");

            Map<String, Object> subgraphInput = new HashMap<>();
            subgraphInput.put("userId", userId);
            subgraphInput.put("token", token);

            // 执行子图
            Optional<AgentState> subgraphResult = validationGraph.invoke(subgraphInput);

            // 获取子图结果
            if (subgraphResult.isPresent()) {
                Boolean isValid = (Boolean) subgraphResult.get().value("isValid").orElse(false);
                String message = (String) subgraphResult.get().value("validationMessage").orElse("");

                return Map.of(
                    "authResult", isValid ? "认证成功：" + message : "认证失败：" + message
                );
            }

            return Map.of("authResult", "认证失败：子图执行异常");
        }));

        mainGraph.addEdge(START, "authenticate");
        mainGraph.addEdge("authenticate", END);

        CompiledGraph<AgentState> compiled = mainGraph.compile();

        // 测试
        testAuth(compiled, "user123", "valid_token123", "=== 测试 1：有效认证 ===");
        testAuth(compiled, "ab", "invalid_token", "=== 测试 2：无效认证 ===");
    }

    private static void testAuth(CompiledGraph<AgentState> compiled,
                                 String userId, String token, String label) {
        System.out.println("\n" + label);
        Map<String, Object> input = Map.of("userId", userId, "token", token);
        Optional<AgentState> result = compiled.invoke(input);
        result.ifPresent(state -> {
            String authResult = (String) state.value("authResult").orElse("");
            System.out.println("结果：" + authResult);
        });
    }
}
```

---

## 5.5 人工干预节点

### 5.5.1 带人工审核的工作流

在某些场景下，工作流需要等待人工确认后才能继续：

```java
import java.util.concurrent.*;

/**
 * 人工干预工作流示例：内容审核流程
 */
public class HumanInterventionWorkflow {

    // 模拟人工审核服务
    static class ReviewService {
        private final BlockingQueue<Map<String, Object>> pendingReviews = new LinkedBlockingQueue<>();

        // 提交待审核内容
        public void submitForReview(String contentId, String content) {
            Map<String, Object> review = new HashMap<>();
            review.put("contentId", contentId);
            review.put("content", content);
            pendingReviews.offer(review);
            System.out.println("[ReviewService] 内容 " + contentId + " 已提交待审核");
        }

        // 模拟人工审核（轮询）
        public Map<String, Object> waitForReview() throws InterruptedException {
            Map<String, Object> review = pendingReviews.poll(5, TimeUnit.SECONDS);
            if (review != null) {
                // 模拟审核结果（随机通过或拒绝）
                boolean approved = Math.random() > 0.3;
                review.put("approved", approved);
                review.put("reviewComment", approved ? "审核通过" : "内容违规，拒绝发布");
                System.out.println("[ReviewService] 审核完成：" + (approved ? "通过" : "拒绝"));
            }
            return review;
        }
    }

    public static void main(String[] args) throws Exception {
        Map<String, Channel<?>> channels = new HashMap<>();
        channels.put("contentId", Channels.base((Supplier<String>) null));
        channels.put("content", Channels.base((Supplier<String>) null));
        channels.put("autoCheckResult", Channels.base((Supplier<String>) null));
        channels.put("needsManualReview", Channels.base((Supplier<Boolean>) null));
        channels.put("reviewResult", Channels.base((Supplier<String>) null));
        channels.put("finalStatus", Channels.base((Supplier<String>) null));

        StateGraph<AgentState> graph = new StateGraph<>(channels, AgentState::new);
        ReviewService reviewService = new ReviewService();

        // 自动检查节点
        graph.addNode("autoCheck", node_async((state, config) -> {
            String content = (String) state.value("content").orElse("");
            System.out.println("[autoCheck] 自动检查内容");

            // 简单检查：包含敏感词则需要人工审核
            boolean needsReview = content.contains("敏感词") || content.length() > 100;

            return Map.of(
                "autoCheckResult", needsReview ? "需要人工审核" : "自动检查通过",
                "needsManualReview", needsReview
            );
        }));

        // 人工审核节点
        graph.addNode("manualReview", node_async((state, config) -> {
            String contentId = (String) state.value("contentId").orElse("");
            String content = (String) state.value("content").orElse("");

            System.out.println("[manualReview] 进入人工审核流程");

            // 提交给审核服务
            reviewService.submitForReview(contentId, content);

            // 等待审核结果
            Map<String, Object> reviewResult = reviewService.waitForReview();

            if (reviewResult != null) {
                boolean approved = (Boolean) reviewResult.getOrDefault("approved", false);
                String comment = (String) reviewResult.getOrDefault("reviewComment", "");

                return Map.of(
                    "reviewResult", comment,
                    "finalStatus", approved ? "approved" : "rejected"
                );
            }

            return Map.of("finalStatus", "timeout");
        }));

        // 自动通过节点
        graph.addNode("autoApprove", node_async((state, config) -> {
            System.out.println("[autoApprove] 自动审核通过");
            return Map.of("finalStatus", "approved", "reviewResult", "自动审核通过");
        }));

        // 路由函数
        graph.addNode("route", node_async((state, config) -> {
            Boolean needsReview = (Boolean) state.value("needsManualReview").orElse(false);
            return needsReview ? "manualReview" : "autoApprove";
        }));

        // 添加边
        graph.addEdge(START, "autoCheck");
        graph.addEdge("autoCheck", "route");

        // 条件分支
        graph.addConditionalEdges(
            "route",
            edge_async(s -> s.value("needsManualReview").orElse(false) ? "manualReview" : "autoApprove"),
            Map.of(
                "manualReview", "manualReview",
                "autoApprove", "autoApprove"
            )
        );

        graph.addEdge("manualReview", END);
        graph.addEdge("autoApprove", END);

        CompiledGraph<AgentState> compiled = graph.compile();

        System.out.println("=== 人工干预工作流示例 ===");
        testContent(compiled, "001", "正常内容", "=== 测试 1：正常内容 ===");
        testContent(compiled, "002", "包含敏感词的内容", "=== 测试 2：敏感内容 ===");
    }

    private static void testContent(CompiledGraph<AgentState> compiled,
                                    String contentId, String content, String label) {
        System.out.println("\n" + label);
        Map<String, Object> input = Map.of("contentId", contentId, "content", content);
        Optional<AgentState> result = compiled.invoke(input);
        result.ifPresent(state -> {
            String status = (String) state.value("finalStatus").orElse("");
            String reviewResult = (String) state.value("reviewResult").orElse("");
            System.out.println("最终状态：" + status);
            System.out.println("审核结果：" + reviewResult);
        });
    }
}
```

---

## 5.6 综合示例：多阶段审批工作流

### 5.6.1 工作流设计

```
┌─────────────────────────────────────────────────────────────────────┐
│                     多阶段审批工作流                                 │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  START ──▶ 提交申请 ──▶ 初审 ──┬──▶ 拒绝 ──▶ END                   │
│                                │                                    │
│                                ▼                                    │
│                            通过？───┬── 是 ──▶ 复审 ──┬──▶ 拒绝 ──┐│
│                                     │                │            ││
│                                     └── 否 ─────────┘            ││
│                                                                  ▼│
│                                                           终审 ──▶ 发布 ──▶ END│
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 5.6.2 完整代码

```java
package com.langgraph4j.chapter5;

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
 * 多阶段审批工作流示例
 */
public class MultiStageApprovalWorkflow {

    public static void main(String[] args) throws Exception {
        // 1. 定义状态通道
        Map<String, Channel<?>> channels = new HashMap<>();

        // 申请信息
        channels.put("applicationId", Channels.base((Supplier<String>) null));
        channels.put("applicant", Channels.base((Supplier<String>) null));
        channels.put("amount", Channels.base((Supplier<Double>) null));
        channels.put("description", Channels.base((Supplier<String>) null));

        // 审批状态
        channels.put("currentStage", Channels.base((Supplier<String>) null));
        channels.put("approvalStatus", Channels.base((Supplier<String>) null));
        channels.put("comments", Channels.base((Supplier<String>) null));
        channels.put("finalResult", Channels.base((Supplier<String>) null));

        // 2. 创建状态图
        StateGraph<AgentState> graph = new StateGraph<>(channels, AgentState::new);

        // 3. 添加节点

        // 提交申请
        graph.addNode("submit", node_async((state, config) -> {
            String appId = (String) state.value("applicationId").orElse("");
            System.out.println("[submit] 提交申请：" + appId);
            return Map.of("currentStage", "submitted", "approvalStatus", "pending");
        }));

        // 初审
        graph.addNode("initialReview", node_async((state, config) -> {
            Double amount = (Double) state.value("amount").orElse(0.0);
            System.out.println("[initialReview] 初审，金额：" + amount);

            // 金额小于 1000 直接通过
            if (amount < 1000) {
                return Map.of(
                    "approvalStatus", "approved",
                    "comments", "初审通过（小额快速审批）",
                    "currentStage", "initial_passed"
                );
            }

            // 大额需要进一步审核
            return Map.of(
                "approvalStatus", "pending",
                "comments", "初审通过，进入复审",
                "currentStage", "initial_passed"
            );
        }));

        // 复审
        graph.addNode("secondaryReview", node_async((state, config) -> {
            Double amount = (Double) state.value("amount").orElse(0.0);
            System.out.println("[secondaryReview] 复审，金额：" + amount);

            // 金额小于 10000 通过
            if (amount < 10000) {
                return Map.of(
                    "approvalStatus", "approved",
                    "comments", "复审通过",
                    "currentStage", "secondary_passed"
                );
            }

            // 大额进入终审
            return Map.of(
                "approvalStatus", "pending",
                "comments", "复审通过，进入终审",
                "currentStage", "secondary_passed"
            );
        }));

        // 终审
        graph.addNode("finalReview", node_async((state, config) -> {
            Double amount = (Double) state.value("amount").orElse(0.0);
            System.out.println("[finalReview] 终审，金额：" + amount);

            return Map.of(
                "approvalStatus", "approved",
                "comments", "终审通过",
                "currentStage", "final_passed"
            );
        }));

        // 发布结果
        graph.addNode("publish", node_async((state, config) -> {
            String appId = (String) state.value("applicationId").orElse("");
            String status = (String) state.value("approvalStatus").orElse("");
            String comments = (String) state.value("comments").orElse("");

            String result = String.format("申请 %s 审批完成：状态=%s, 意见=%s", appId, status, comments);
            System.out.println("[publish] " + result);

            return Map.of("finalResult", result);
        }));

        // 4. 添加边
        graph.addEdge(START, "submit");
        graph.addEdge("submit", "initialReview");

        // 根据初审结果分流
        graph.addConditionalEdges(
            "initialReview",
            edge_async(s -> {
                String stage = (String) s.value("currentStage").orElse("");
                return "initial_passed".equals(stage) ? "secondaryReview" : "reject";
            }),
            Map.of(
                "secondaryReview", "secondaryReview",
                "reject", "publish"
            )
        );

        // 根据复审结果分流
        graph.addConditionalEdges(
            "secondaryReview",
            edge_async(s -> {
                String stage = (String) s.value("currentStage").orElse("");
                return "secondary_passed".equals(stage) ? "finalReview" : "reject";
            }),
            Map.of(
                "finalReview", "finalReview",
                "reject", "publish"
            )
        );

        graph.addEdge("finalReview", "publish");
        graph.addEdge("publish", END);

        // 5. 编译
        CompiledGraph<AgentState> compiled = graph.compile();

        // 6. 测试
        testApproval(compiled, "APP001", "张三", 500.0, "=== 测试 1：小额申请（快速审批）===");
        testApproval(compiled, "APP002", "李四", 5000.0, "=== 测试 2：中等金额申请（需复审）===");
        testApproval(compiled, "APP003", "王五", 50000.0, "=== 测试 3：大额申请（需终审）===");
    }

    private static void testApproval(CompiledGraph<AgentState> compiled,
                                     String appId, String applicant,
                                     Double amount, String label) {
        System.out.println("\n" + label);
        Map<String, Object> input = Map.of(
            "applicationId", appId,
            "applicant", applicant,
            "amount", amount,
            "description", "测试申请"
        );
        Optional<AgentState> result = compiled.invoke(input);
        result.ifPresent(state -> {
            String finalResult = (String) state.value("finalResult").orElse("");
            System.out.println("最终结果：" + finalResult);
        });
    }
}
```

---

## 本章小结

### 关键知识点

| 概念 | 说明 |
|------|------|
| 多条件路由 | 根据多个条件组合决定执行路径 |
| 循环工作流 | 使用状态追踪进度实现迭代处理 |
| 并行执行 | 多个任务同时执行提高效率 |
| 子图封装 | 将工作流封装为可复用的组件 |
| 人工干预 | 在流程中等待人工确认 |

### 工作流设计模式

| 模式 | 适用场景 |
|------|---------|
| 条件分支 | 根据状态选择不同的执行路径 |
| 循环迭代 | 批量处理多个相似项目 |
| 并行汇聚 | 多个独立任务并行执行后合并结果 |
| 子图复用 | 通用流程的封装与重用 |
| 人工审核 | 需要人工判断的关键环节 |

---

## 课后练习

### 练习 1：条件分支
- [ ] 创建一个根据用户等级提供不同服务的工作流
- [ ] 普通会员→标准服务，黄金会员→优先服务，钻石会员→专属服务

### 练习 2：循环处理
- [ ] 创建一个处理订单列表的工作流
- [ ] 每个订单经过验证→处理→记录三个步骤

### 练习 3：子图复用
- [ ] 将日志记录功能封装为子图
- [ ] 在多个工作流中复用该日志子图

---

## 下一章预告

在 [第 6 章](../03-高级特性与最佳实践/06-多智能体协同开发/readme.md) 中，我们将进入模块三的学习：
- 多智能体系统架构设计
- 智能体角色定义与任务分配
- 智能体间通信与协作机制
- 群体决策的实现方法
