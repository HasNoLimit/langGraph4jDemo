# 第 2 章 StateGraph 与节点详解

## 学习目标

完成本章后，你将能够：

1. 深入理解 StateGraph 的架构与工作原理
2. 掌握不同类型 Channel 的创建与使用场景
3. 熟练创建和配置各种类型的节点
4. 理解并应用多种边的类型（普通边、条件边、并行边）
5. 掌握图的编译与执行流程

## 建议学时

**2 课时** (90 分钟)

- 理论学习：50 分钟
- 实践操作：40 分钟

---

## 2.1 StateGraph 深度解析

### 2.1.1 StateGraph 是什么？

StateGraph（状态图）是 LangGraph4J 的核心组件，它是一个**有向图**数据结构，用于描述工作流的完整结构。

```
┌─────────────────────────────────────────────────────────────┐
│                      StateGraph                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌─────────┐                                              │
│   │ Nodes   │  Map<String, Node> 节点名称 -> 节点实例       │
│   ├─────────┤                                              │
│   │ Edges   │  List<Edge> 边的列表                         │
│   ├─────────┤                                              │
│   │ Channels│  Map<String, Channel> 状态通道定义            │
│   └─────────┘                                              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 2.1.2 StateGraph 的两种创建方式

#### 方式一：完整构造函数（推荐用于生产环境）

```java
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.Map;
import java.util.HashMap;
import java.util.function.Supplier;

// 1. 定义状态通道
Map<String, Channel<?>> channels = new HashMap<>();
channels.put("name", Channels.base((Supplier<String>) null));
channels.put("messages", Channels.base((Supplier<List<String>>) null));

// 2. 创建状态图 - 需要 channels 和 AgentStateFactory
StateGraph<AgentState> graph = new StateGraph<>(channels, AgentState::new);
```

**参数说明：**
- `channels`: 定义状态图中所有可用字段的 Schema
- `AgentState::new`: 工厂方法，用于创建新的 AgentState 实例

#### 方式二：简化构造函数（适用于快速原型）

```java
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.state.Channel;

import java.util.Map;
import java.util.HashMap;

// 1. 定义通道
Map<String, Channel<?>> channels = new HashMap<>();
channels.put("name", Channel.of());
channels.put("value", Channel.of());

// 2. 创建状态图（使用默认 AgentState）
StateGraph graph = new StateGraph(channels);
```

### 2.1.3 StateGraph 的核心方法

| 方法 | 说明 | 返回值 |
|------|------|--------|
| `addNode(String name, AsyncNodeAction action)` | 添加节点 | `StateGraph` |
| `addEdge(String from, String to)` | 添加无条件边 | `StateGraph` |
| `addConditionalEdges(String from, AsyncEdgeAction action, Map<String, String> mappings)` | 添加条件边 | `StateGraph` |
| `compile()` | 编译图为可执行对象 | `CompiledGraph` |

---

## 2.2 Channel 详解

### 2.2.1 什么是 Channel？

Channel（通道）是状态图中**字段的定义**，它决定了：
- 状态可以存储哪些字段
- 字段如何被更新（覆盖、追加、聚合等）

```
┌─────────────────────────────────────────────────────────┐
│                    Channel 结构                          │
├─────────────────────────────────────────────────────────┤
│  name: String          ───────▶  字段名称               │
│  type: Class<?>        ───────▶  字段类型               │
│  reducer: BiFunction   ───────▶  更新策略（可选）       │
│  supplier: Supplier    ───────▶  默认值提供者（可选）   │
└─────────────────────────────────────────────────────────┘
```

### 2.2.2 Channels.base() - 基础通道

基础通道使用**覆盖策略**：新值会完全替换旧值。

```java
import org.bsc.langgraph4j.state.Channels;
import java.util.function.Supplier;

// 使用 null Supplier（无默认值）
channels.put("name", Channels.base((Supplier<String>) null));

// 提供默认值
channels.put("name", Channels.base(() -> "Anonymous"));

// 带类型声明（推荐）
channels.put("counter", Channels.base((Supplier<Integer>) null));
```

### 2.2.3 实现消息累积

**注意**：在当前版本中，没有 `Channels.append()` 方法。如果需要实现消息累积功能，需要在节点中手动处理列表的追加逻辑。

```java
import org.bsc.langgraph4j.state.Channels;
import java.util.List;
import java.util.ArrayList;

// 1. 使用基础通道定义列表字段
channels.put("messages", Channels.base((Supplier<List<String>>) null));

// 2. 在节点中手动处理列表追加
graph.addNode("addMessage", node_async((state, config) -> {
    // 获取现有列表
    List<String> existing = (List<String>) state.value("messages").orElse(new ArrayList<>());
    // 创建新列表并添加新消息
    List<String> messages = new ArrayList<>(existing);
    messages.add("新消息");
    return Map.of("messages", messages);
}));
```

### 2.2.4 通道类型对比

| 创建方法 | 更新策略 | 适用场景 |
|----------|---------|---------|
| `Channels.base(Supplier)` | 覆盖 | 单个值、配置项 |
| `Channel.of()` | 默认覆盖 | 快速原型 |

**注意**：如需实现消息累积功能，需要在节点中手动获取现有列表并添加新元素。

---

## 2.3 节点（Node）详解

### 2.3.1 节点的两种类型

#### 同步节点

```java
graph.addNode("greet", state -> {
    String name = state.value("name").orElse("World");
    String greeting = "Hello, " + name + "!";
    return Map.of("greeting", greeting);
});
```

#### 异步节点（推荐）

```java
import static org.bsc.langgraph4j.action.AsyncNodeActionWithConfig.node_async;

graph.addNode("greet", node_async((state, config) -> {
    String name = state.value("name").orElse("World");
    String greeting = "Hello, " + name + "!";
    return Map.of("greeting", greeting);
}));
```

### 2.3.2 节点函数的签名

```java
// 同步节点函数
Function<AgentState, Map<String, Object>>

// 异步节点函数（带配置）
BiFunction<AgentState, RunnableConfig, Map<String, Object>>
```

**参数说明：**
- `AgentState`: 当前状态
- `RunnableConfig`: 运行时配置（包含线程池、超时等）

### 2.3.3 节点返回值

节点必须返回 `Map<String, Object>`，用于更新状态：

```java
// 返回单个字段
return Map.of("greeting", "Hello!");

// 返回多个字段
return Map.of(
    "greeting", "Hello!",
    "timestamp", System.currentTimeMillis(),
    "processed", true
);

// 返回空 Map（不更新任何字段）
return Map.of();
```

### 2.3.4 节点命名规则

```java
// ✅ 合法的节点名称
graph.addNode("greet", ...);
graph.addNode("process_data", ...);
graph.addNode("node1", ...);

// ❌ 非法的节点名称
graph.addNode("__start__", ...);  // 保留字
graph.addNode("__end__", ...);    // 保留字
graph.addNode("my-node", ...);    // 不能包含特殊字符
```

---

## 2.4 边（Edge）详解

### 2.4.1 无条件边（Unconditional Edge）

无条件边表示**总是**从一个节点流转到另一个节点。

```java
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.StateGraph.END;

// 从入口到第一个节点
graph.addEdge(START, "first");

// 节点间流转
graph.addEdge("first", "second");

// 从最后一个节点到出口
graph.addEdge("last", END);
```

### 2.4.2 条件边（Conditional Edge）

条件边根据**路由函数**的返回值决定流转到哪个节点。

```java
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;

// 路由函数
private static String route(AgentState state) {
    String sentiment = state.value("sentiment").orElse("neutral");
    return switch (sentiment) {
        case "positive" -> "positiveHandler";
        case "negative" -> "negativeHandler";
        default -> "neutralHandler";
    };
}

// 添加条件边
graph.addConditionalEdges(
    "analyze",                          // 源节点
    edge_async(MyClass::route),         // 路由函数（异步包装）
    Map.of(                              // 映射关系
        "positiveHandler", "positiveHandler",
        "negativeHandler", "negativeHandler",
        "neutralHandler", "neutralHandler"
    )
);
```

### 2.4.3 边的映射关系简化写法

当路由函数的返回值与目标节点名称相同时，可以简化：

```java
// 完整写法
graph.addConditionalEdges(
    "analyze",
    edge_async(this::route),
    Map.of(
        "positive", "positiveHandler",
        "negative", "negativeHandler"
    )
);

// 简化写法（返回值 = 节点名时）
graph.addConditionalEdges(
    "analyze",
    edge_async(this::route),
    // 自动映射：返回值 -> 同名节点
);
```

---

## 2.5 图的编译与执行

### 2.5.1 编译图

```java
// 编译为可执行图
CompiledGraph<AgentState> compiled = graph.compile();

// 编译时可配置选项（如果支持）
CompiledGraph<AgentState> compiled = graph.compile(c -> c
    .withInterruptBefore("review")  // 在 review 节点前中断
    .withInterruptAfter("generate") // 在 generate 节点后中断
);
```

### 2.5.2 执行图

#### invoke() 方法（返回 Optional）

```java
// 准备输入
Map<String, Object> input = Map.of("name", "Alice");

// 执行 - 返回 Optional<AgentState>
Optional<AgentState> result = compiled.invoke(input);

// 处理结果
result.ifPresent(state -> {
    String greeting = (String) state.value("greeting").orElse("");
    System.out.println(greeting);
});
```

#### stream() 方法（流式执行）

```java
// 流式执行 - 获取每个节点的输出
Stream<AgentState> stream = compiled.stream(input);

stream.forEach(state -> {
    System.out.println("节点执行完成，当前状态：" + state);
});
```

---

## 2.6 综合示例：多轮对话工作流

### 2.6.1 工作流设计

```
┌─────────────────────────────────────────────────────────────┐
│                  多轮对话工作流                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   START ──▶ greet ──▶ analyze ──┬──▶ positive ──┐          │
│                                 │               │          │
│                                 ├──▶ negative ──┼──▶ END   │
│                                 │               │          │
│                                 └──▶ neutral ───┘          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 2.6.2 完整代码

```java
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
            String message = state.value("message").orElse("");
            System.out.println("[greet] 收到消息：" + message);
            return Map.of("greeted", true);
        }));

        // 情感分析节点（模拟）
        graph.addNode("analyze", node_async((state, config) -> {
            String message = state.value("message").orElse("");
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
            String response = state.value("response").orElse("");
            System.out.println("回复：" + response);
        });
    }
}
```

### 2.6.3 运行结果

```
=== 测试 1：正面消息 ===
[greet] 收到消息：今天天气真好
[analyze] 情感分析结果：positive
[handlePositive] 处理正面情感
回复：很高兴听到您的正面反馈！

=== 测试 2：负面消息 ===
[greet] 收到消息：这个产品太坏了
[analyze] 情感分析结果：negative
[handleNegative] 处理负面情感
回复：很抱歉听到您的不愉快，我们会尽快改进。

=== 测试 3：中性消息 ===
[greet] 收到消息：一般般吧
[analyze] 情感分析结果：neutral
[handleNeutral] 处理中性情感
回复：感谢您的反馈，我们会继续努力。
```

---

## 2.7 调试与可视化

### 2.7.1 打印图结构

```java
// 编译前打印节点列表
System.out.println("节点列表：" + graph.getNodes().keySet());

// 打印边列表
graph.getEdges().forEach(edge -> {
    System.out.println("边：" + edge.from() + " -> " + edge.to());
});
```

### 2.7.2 使用日志

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

private static final Logger logger = LoggerFactory.getLogger(MyGraph.class);

graph.addNode("process", node_async((state, config) -> {
    logger.info("进入 process 节点");
    logger.debug("当前状态：{}", state.data());
    // ...
    return result;
}));
```

---

## 本章小结

### 关键知识点

| 概念 | 说明 |
|------|------|
| StateGraph | 工作流的图结构，包含 Nodes、Edges、Channels |
| Channel | 状态字段定义，决定更新策略 |
| Channels.base() | 基础通道，覆盖更新 |
| Channels.append() | 追加通道，追加更新 |
| addNode() | 添加节点，使用 node_async 包装 |
| addEdge() | 添加无条件边 |
| addConditionalEdges() | 添加条件边，使用 edge_async 包装路由函数 |
| START/END | 工作流入口和出口常量 |
| invoke() | 执行图，返回 Optional<AgentState> |

---

## 课后练习

### 练习 1：创建三节点线性工作流
- [ ] 创建一个包含三个节点的工作流：input → process → output
- [ ] 每个节点打印当前步骤
- [ ] process 节点将输入文本转为大写

### 练习 2：实现条件分支
- [ ] 创建一个根据数字奇偶性分支的工作流
- [ ] 奇数分支返回"奇数"，偶数分支返回"偶数"
- [ ] 使用 addConditionalEdges 实现

### 练习 3：使用追加通道
- [ ] 创建一个消息累积工作流
- [ ] 使用 Channels.append() 定义消息通道
- [ ] 三个节点分别添加不同消息，最终输出完整消息列表

---

## 下一章预告

在 [第 3 章](../02-LangGraph4J 核心开发/03-状态管理与数据流转/readme.md) 中，我们将深入学习：
- AgentState 的高级用法与自定义状态类
- 状态更新策略与数据聚合
- 大型状态的管理与优化
- 状态持久化与恢复
