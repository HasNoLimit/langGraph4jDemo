# 第 2 章 练习答案

## 练习 1：三节点线性工作流

### 题目要求
- 创建一个包含三个节点的工作流：input → process → output
- 每个节点打印当前步骤
- process 节点将输入文本转为大写

### 参考代码

```java
package com.langgraph4j.exercises;

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
 * 练习 1：三节点线性工作流
 */
public class Exercise1_Solution {

    public static void main(String[] args) throws Exception {
        // 1. 定义通道
        Map<String, Channel<?>> channels = new HashMap<>();
        channels.put("input", Channels.base((Supplier<String>) null));
        channels.put("processed", Channels.base((Supplier<String>) null));
        channels.put("output", Channels.base((Supplier<String>) null));

        // 2. 创建状态图
        StateGraph<AgentState> graph = new StateGraph<>(channels, AgentState::new);

        // 3. 添加节点

        // input 节点：接收并打印输入
        graph.addNode("input", node_async((state, config) -> {
            String input = (String) state.value("input").orElse("");
            System.out.println("[input] 接收输入：" + input);
            return Map.of();
        }));

        // process 节点：将文本转为大写
        graph.addNode("process", node_async((state, config) -> {
            String input = (String) state.value("input").orElse("");
            String processed = input.toUpperCase();
            System.out.println("[process] 处理结果：" + processed);
            return Map.of("processed", processed);
        }));

        // output 节点：输出最终结果
        graph.addNode("output", node_async((state, config) -> {
            String processed = (String) state.value("processed").orElse("");
            String output = "最终输出：" + processed;
            System.out.println("[output] " + output);
            return Map.of("output", output);
        }));

        // 4. 添加边
        graph.addEdge(START, "input");
        graph.addEdge("input", "process");
        graph.addEdge("process", "output");
        graph.addEdge("output", END);

        // 5. 编译
        CompiledGraph<AgentState> compiled = graph.compile();

        // 6. 执行
        Map<String, Object> inputData = Map.of("input", "Hello, LangGraph4J!");
        Optional<AgentState> result = compiled.invoke(inputData);

        // 7. 输出结果
        result.ifPresent(state -> {
            String output = (String) state.value("output").orElse("");
            System.out.println("\n=== 最终结果 ===");
            System.out.println(output);
        });
    }
}
```

**运行输出：**
```
[input] 接收输入：Hello, LangGraph4J!
[process] 处理结果：HELLO, LANGGRAPH4J!
[output] 最终输出：HELLO, LANGGRAPH4J!

=== 最终结果 ===
最终输出：HELLO, LANGGRAPH4J!
```

---

## 练习 2：实现条件分支

### 题目要求
- 创建一个根据数字奇偶性分支的工作流
- 奇数分支返回"奇数"，偶数分支返回"偶数"
- 使用 addConditionalEdges 实现

### 参考代码

```java
package com.langgraph4j.exercises;

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
 */
public class Exercise2_Solution {

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
            edge_async(Exercise2_Solution::routeByParity),
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
```

**运行输出：**
```
=== 测试 1：奇数 ===
[check] 检查数字：7
[odd] 处理奇数
结果：7 是 奇数

=== 测试 2：偶数 ===
[check] 检查数字：10
[even] 处理偶数
结果：10 是 偶数

=== 测试 3：零 ===
[check] 检查数字：0
[even] 处理偶数
结果：0 是 偶数
```

---

## 练习 3：消息累积工作流

### 题目要求
- 创建一个消息累积工作流
- 使用基础通道定义消息列表字段
- 三个节点分别添加不同消息，最终输出完整消息列表

### 参考代码

```java
package com.langgraph4j.exercises;

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
 * 练习 3：消息累积工作流
 */
public class Exercise3_Solution {

    public static void main(String[] args) throws Exception {
        // 1. 定义通道
        Map<String, Channel<?>> channels = new HashMap<>();
        // 使用基础通道，在节点中手动处理列表追加
        channels.put("messages", Channels.base((Supplier<List<String>>) null));

        // 2. 创建状态图
        StateGraph<AgentState> graph = new StateGraph<>(channels, AgentState::new);

        // 3. 添加节点

        // 节点 1：添加第一条消息
        graph.addNode("step1", node_async((state, config) -> {
            System.out.println("[step1] 初始化");
            List<String> messages = new ArrayList<>();
            messages.add("步骤 1：开始初始化");
            return Map.of("messages", messages);
        }));

        // 节点 2：添加第二条消息
        graph.addNode("step2", node_async((state, config) -> {
            System.out.println("[step2] 处理中");
            List<String> existing = (List<String>) state.value("messages").orElse(new ArrayList<>());
            List<String> messages = new ArrayList<>(existing);
            messages.add("步骤 2：数据处理中");
            return Map.of("messages", messages);
        }));

        // 节点 3：添加第三条消息
        graph.addNode("step3", node_async((state, config) -> {
            System.out.println("[step3] 完成");
            List<String> existing = (List<String>) state.value("messages").orElse(new ArrayList<>());
            List<String> messages = new ArrayList<>(existing);
            messages.add("步骤 3：处理完成");
            return Map.of("messages", messages);
        }));

        // 4. 添加边
        graph.addEdge(START, "step1");
        graph.addEdge("step1", "step2");
        graph.addEdge("step2", "step3");
        graph.addEdge("step3", END);

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
```

**运行输出：**
```
=== 消息累积工作流执行开始 ===

[step1] 初始化
[step2] 处理中
[step3] 完成

=== 最终消息列表 ===
  - 步骤 1：开始初始化
  - 步骤 2：数据处理中
  - 步骤 3：处理完成
```

---

## 思考题解答

### 1. 如何实现消息累积功能？

在当前版本中，没有 `Channels.append()` 方法，需要在节点中手动处理列表的追加：

```java
// 1. 使用基础通道定义列表字段
channels.put("messages", Channels.base((Supplier<List<String>>) null));

// 2. 在节点中手动追加
graph.addNode("addMessage", node_async((state, config) -> {
    List<String> existing = (List<String>) state.value("messages").orElse(new ArrayList<>());
    List<String> messages = new ArrayList<>(existing);
    messages.add("新消息");
    return Map.of("messages", messages);
}));
```

**工作原理：**
- 从状态中获取现有列表
- 创建新列表并复制原有元素
- 添加新元素
- 返回新列表更新状态

### 2. 什么情况下使用条件边？

当工作流的执行路径需要根据**运行时状态**动态决定时，使用条件边。

**典型场景：**
- 情感分析后根据情感倾向选择不同的回复策略
- 根据用户权限决定访问路径
- 根据任务类型分配不同的处理节点

### 3. 节点返回值有什么要求？

节点必须返回 `Map<String, Object>`，用于更新状态：
- Map 的 key 必须是通道中定义的字段名
- Map 的 value 会被用来更新对应字段
- 返回空 Map 表示不更新任何字段
- 使用追加通道时，value 应该是 List 或单个元素
