# 第 3 章 状态管理与数据流转

## 学习目标

完成本章后，你将能够：

1. 深入理解 AgentState 的内部结构与工作原理
2. 掌握状态 Schema 的设计方法与最佳实践
3. 熟练使用状态的各种操作（读取、更新、删除）
4. 实现状态在工作流中的正确传递与更新
5. 掌握状态持久化的基本方法

## 建议学时

**2 课时** (90 分钟)

- 理论学习：50 分钟
- 实践操作：40 分钟

---

## 3.1 AgentState 深度解析

### 3.1.1 AgentState 是什么？

AgentState 是 LangGraph4J 中工作流的**状态容器**，它在节点之间传递，存储工作流执行过程中的所有数据。

```
┌─────────────────────────────────────────────────────────────┐
│                      AgentState                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  data: Map<String, Object>                            │  │
│  │       ┌─────────────────────────────────────────┐     │  │
│  │       │ "name"      →  "Alice"                  │     │  │
│  │       │ "messages"  →  ["Hello", "World"]       │     │  │
│  │       │ "counter"   →  5                        │     │  │
│  │       │ "status"    →  "processing"             │     │  │
│  │       └─────────────────────────────────────────┘     │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 3.1.2 AgentState 的核心方法

| 方法 | 说明 | 返回值类型 |
|------|------|-----------|
| `value(String key)` | 获取指定 key 的值 | `Optional<Object>` |
| `data()` | 获取底层数据 Map | `Map<String, Object>` |
| `keys()` | 获取所有 key 的集合 | `Set<String>` |

### 3.1.3 状态的读取操作

```java
import org.bsc.langgraph4j.state.AgentState;

// 方式一：使用 value() 方法（推荐）
String name = (String) state.value("name").orElse("默认值");
Integer counter = (Integer) state.value("counter").orElse(0);

// 方式二：直接从 data() 获取
Map<String, Object> data = state.data();
String name = (String) data.get("name");

// 检查 key 是否存在
if (state.value("optionalField").isPresent()) {
    String value = (String) state.value("optionalField").get();
}
```

### 3.1.4 状态的更新操作

```java
// 方式一：节点返回 Map 更新状态（推荐）
graph.addNode("updateName", node_async((state, config) -> {
    return Map.of("name", "新名字", "status", "updated");
}));

// 方式二：直接修改 data()（适用于简化版 API）
graph.addNode("updateName", state -> {
    state.data().put("name", "新名字");
    state.data().put("status", "updated");
    return state;
});
```

---

## 3.2 状态 Schema 设计

### 3.2.1 什么是状态 Schema？

状态 Schema 是工作流状态的**结构定义**，它声明了状态中可以包含哪些字段，以及每个字段的类型和更新策略。

### 3.2.2 使用 Channels 定义 Schema

```java
import org.bsc.langgraph4j.state.Channels;
import org.bsc.langgraph4j.state.Channel;

import java.util.Map;
import java.util.HashMap;
import java.util.function.Supplier;
import java.util.List;
import java.util.ArrayList;

Map<String, Channel<?>> channels = new HashMap<>();

// 字符串字段
channels.put("name", Channels.base((Supplier<String>) null));
channels.put("status", Channels.base((Supplier<String>) null));

// 整型字段
channels.put("counter", Channels.base((Supplier<Integer>) null));

// 列表字段
channels.put("messages", Channels.base((Supplier<List<String>>) null));
channels.put("history", Channels.base((Supplier<List<String>>) null));

// 布尔字段
channels.put("completed", Channels.base((Supplier<Boolean>) null));
```

### 3.2.3 Schema 设计最佳实践

1. **明确字段类型**：为每个字段声明明确的类型

```java
// ✅ 好的做法
channels.put("age", Channels.base((Supplier<Integer>) null));
channels.put("name", Channels.base((Supplier<String>) null));

// ❌ 不推荐：类型不明确
channels.put("data", Channels.base((Supplier<Object>) null));
```

2. **使用有意义的字段名**

```java
// ✅ 好的做法
channels.put("userName", Channels.base((Supplier<String>) null));
channels.put("errorMessage", Channels.base((Supplier<String>) null));

// ❌ 不推荐
channels.put("str1", Channels.base((Supplier<String>) null));
channels.put("tmp", Channels.base((Supplier<String>) null));
```

3. **为列表字段提供默认值**

```java
// 列表字段建议提供空列表作为默认值
channels.put("messages", Channels.base(() -> new ArrayList<>()));
channels.put("errors", Channels.base(() -> new ArrayList<>()));
```

---

## 3.3 状态在工作流中的传递

### 3.3.1 状态传递流程

```
┌─────────────────────────────────────────────────────────────┐
│                    状态传递流程                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  START ──▶ Node1 ──▶ Node2 ──▶ Node3 ──▶ END               │
│    │          │          │          │                       │
│    │          │          │          │                       │
│    ▼          ▼          ▼          ▼                       │
│   State     State      State      State                    │
│   ┌───┐    ┌───┐     ┌───┐     ┌───┐                       │
│   │input│ →│ + │  →  │ + │  →  │ + │                       │
│   └───┘    │ A │     │ B │     │ C │                       │
│            └───┘     └───┘     └───┘                       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

每个节点接收当前状态，返回更新数据，新数据与现有状态合并后传递给下一个节点。

### 3.3.2 状态更新示例

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
```

**运行输出：**
```
[step1] 输入：hello → 输出：Step1: HELLO
[step2] 输入：Step1: HELLO → 输出：Step2: Step1: HELLO [Processed]
[step3] 输入：Step2: Step1: HELLO [Processed] → 输出：最终输出：Step2: Step1: HELLO [Processed]

=== 最终状态 ===
input: hello
step1Result: Step1: HELLO
step2Result: Step2: Step1: HELLO [Processed]
finalOutput: 最终输出：Step2: Step1: HELLO [Processed]
```

---

## 3.4 复杂状态设计

### 3.4.1 嵌套状态结构

当需要管理复杂数据时，可以使用嵌套的 Map 结构：

```java
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

Map<String, Channel<?>> channels = new HashMap<>();

// 用户信息（可以使用嵌套 Map）
channels.put("user", Channels.base((Supplier<Map<String, Object>>) null));
channels.put("userInfo", Channels.base((Supplier<Map<String, Object>>) null));

// 订单列表
channels.put("orders", Channels.base((Supplier<List<Map<String, Object>>>) null));

// 在节点中使用嵌套状态
graph.addNode("createUser", node_async((state, config) -> {
    Map<String, Object> user = new HashMap<>();
    user.put("id", 1);
    user.put("name", "Alice");
    user.put("email", "alice@example.com");
    return Map.of("user", user);
}));

graph.addNode("processOrder", node_async((state, config) -> {
    Map<String, Object> user = (Map<String, Object>) state.value("user").orElse(null);
    if (user != null) {
        String userName = (String) user.get("name");
        System.out.println("为 " + userName + " 处理订单");
    }
    return Map.of();
}));
```

### 3.4.2 状态字段分组

对于大型工作流，建议将相关字段分组：

```java
Map<String, Channel<?>> channels = new HashMap<>();

// 用户相关
channels.put("userId", Channels.base((Supplier<Integer>) null));
channels.put("userName", Channels.base((Supplier<String>) null));
channels.put("userEmail", Channels.base((Supplier<String>) null));

// 会话相关
channels.put("sessionId", Channels.base((Supplier<String>) null));
channels.put("conversationHistory", Channels.base((Supplier<List<String>>) null));

// 处理相关
channels.put("currentStep", Channels.base((Supplier<String>) null));
channels.put("processingStatus", Channels.base((Supplier<String>) null));
channels.put("errorMessage", Channels.base((Supplier<String>) null));
```

---

## 3.5 状态操作的常见模式

### 3.5.1 累积模式

逐步累积数据到列表中：

```java
graph.addNode("collectData", node_async((state, config) -> {
    // 获取现有列表
    List<String> existing = (List<String>) state.value("collectedItems")
        .orElse(new ArrayList<>());

    // 创建新列表并添加新项
    List<String> items = new ArrayList<>(existing);
    items.add("新数据项");

    return Map.of("collectedItems", items);
}));
```

### 3.5.2 转换模式

将数据从一种格式转换为另一种：

```java
graph.addNode("transform", node_async((state, config) -> {
    String rawData = (String) state.value("rawData").orElse("");

    // 数据转换逻辑
    String transformed = rawData.trim().toUpperCase();

    // 提取结构化数据
    Map<String, Object> parsed = new HashMap<>();
    parsed.put("original", rawData);
    parsed.put("transformed", transformed);
    parsed.put("length", transformed.length());

    return Map.of("parsedData", parsed);
}));
```

### 3.5.3 验证模式

验证状态数据的有效性：

```java
graph.addNode("validate", node_async((state, config) -> {
    List<String> errors = new ArrayList<>();

    // 验证姓名字段
    String name = (String) state.value("name").orElse("");
    if (name.isEmpty()) {
        errors.add("姓名不能为空");
    }

    // 验证邮箱字段
    String email = (String) state.value("email").orElse("");
    if (!email.contains("@")) {
        errors.add("邮箱格式不正确");
    }

    // 返回验证结果
    return Map.of(
        "isValid", errors.isEmpty(),
        "validationErrors", errors
    );
}));
```

### 3.5.4 条件更新模式

根据条件选择性地更新状态：

```java
graph.addNode("conditionalUpdate", node_async((state, config) -> {
    Map<String, Object> updates = new HashMap<>();

    String status = (String) state.value("status").orElse("");

    // 只有在特定条件下才更新
    if ("pending".equals(status)) {
        updates.put("status", "processing");
        updates.put("startedAt", System.currentTimeMillis());
    }

    Integer retryCount = (Integer) state.value("retryCount").orElse(0);
    if (retryCount < 3) {
        updates.put("retryCount", retryCount + 1);
    }

    return updates;
}));
```

---

## 3.6 综合示例：订单处理工作流

### 3.6.1 工作流设计

```
┌─────────────────────────────────────────────────────────────────┐
│                    订单处理工作流                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  START ──▶ validate ──▶ calculate ──▶ process ──▶ notify ──▶ END│
│              │             │            │            │          │
│              ▼             ▼            ▼            ▼          │
│         验证订单      计算金额      处理订单      发送通知       │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 3.6.2 完整代码

```java
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
 * 订单处理工作流示例
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
```

### 3.6.3 运行结果

```
=== 测试 1：有效订单 ===
=== 验证订单 ===
验证结果：通过
=== 计算金额 ===
应用 9 折优惠
原始金额：1500.0, 最终金额：1350.0
=== 处理订单 ===
处理订单：ORDER-001
收款金额：1350.0
=== 发送通知 ===
订单 ORDER-001 状态：completed
已发送确认邮件给客户

最终状态：completed
通知已发送：true

=== 测试 2：无效订单 ===
=== 验证订单 ===
验证结果：失败
=== 处理错误 ===
验证失败，错误列表：
  - 订单 ID 不能为空
  - 订单金额必须大于 0

最终状态：failed
```

---

## 本章小结

### 关键知识点

| 概念 | 说明 |
|------|------|
| AgentState | 工作流状态容器，存储所有字段数据 |
| value(key) | 获取状态值，返回 Optional<Object> |
| data() | 获取底层数据 Map |
| Channels.base() | 定义基础通道，使用覆盖更新策略 |
| 状态传递 | 节点返回的 Map 与现有状态合并后传递给下一节点 |
| 嵌套状态 | 使用 Map<String, Object> 存储复杂数据结构 |

### 状态操作模式

| 模式 | 用途 |
|------|------|
| 累积模式 | 逐步添加数据到列表 |
| 转换模式 | 将数据从一种格式转为另一种 |
| 验证模式 | 检查状态数据的有效性 |
| 条件更新 | 根据条件选择性更新状态 |

---

## 课后练习

### 练习 1：状态读取与更新
- [ ] 创建一个工作流，包含两个节点
- [ ] 第一个节点设置初始值
- [ ] 第二个节点读取并修改该值

### 练习 2：列表状态累积
- [ ] 创建一个工作流，包含三个节点
- [ ] 每个节点向列表中添加一个元素
- [ ] 最终输出完整的列表

### 练习 3：嵌套状态管理
- [ ] 创建一个用户注册工作流
- [ ] 使用嵌套 Map 存储用户信息（name、email、age）
- [ ] 不同节点分别设置不同的用户属性

---

## 下一章预告

在 [第 4 章](../02-LangGraph4J 核心开发/04-工具调用与外部系统集成/readme.md) 中，我们将学习：
- 如何在 LangGraph4J 中集成工具调用
- 调用外部 API 服务的方法
- 数据库操作集成
- 错误处理与重试机制
