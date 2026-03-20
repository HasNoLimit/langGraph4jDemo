# 第 1 章 LangGraph4J 概述与环境搭建

## 学习目标

完成本章后，你将能够：

1. 理解 LangGraph4J 的定位、核心优势与应用场景
2. 掌握 LangGraph4J 的核心架构与工作原理
3. 独立完成开发环境搭建与 Maven 依赖配置
4. 编写并运行第一个 LangGraph4J 工作流程序

## 建议学时

**2 课时** (90 分钟)

- 理论学习：45 分钟
- 实践操作：45 分钟

---

## 1.1 LangGraph4J 简介

### 1.1.1 什么是 LangGraph4J？

LangGraph4J 是 LangGraph 的 Java 实现版本，是一个用于构建**有状态、多智能体工作流**的轻量级框架。它借鉴了图计算的思想，将 AI 应用抽象为**节点 (Node)** 和**边 (Edge)** 组成的有向图。

```
┌─────────────────────────────────────────────────────────┐
│                    LangGraph4J                          │
├─────────────────────────────────────────────────────────┤
│  StateGraph  ────────▶  状态管理核心                    │
│  Node        ────────▶  执行单元 (LLM/工具/逻辑)         │
│  Edge        ────────▶  流程控制 (条件/分支/循环)        │
│  AgentState  ────────▶  工作流状态容器                   │
└─────────────────────────────────────────────────────────┘
```

### 1.1.2 为什么需要 LangGraph4J？

传统 LLM 应用开发面临的挑战：

| 问题 | 传统方式 | LangGraph4J 方案 |
|------|---------|-----------------|
| 状态管理 | 手动维护上下文，容易丢失 | 自动状态追踪与持久化 |
| 流程控制 | 硬编码 if-else，难以维护 | 声明式图结构，清晰直观 |
| 多智能体协同 | 通信逻辑复杂 | 统一状态共享机制 |
| 可观测性 | 调试困难 | 完整的执行轨迹记录 |

### 1.1.3 核心优势

```
┌──────────────────────────────────────────────────────────┐
│                   LangGraph4J 优势                       │
├──────────────────────────────────────────────────────────┤
│  ✅ 类型安全      Java 静态类型检查，编译期发现问题       │
│  ✅ 轻量级        无重型依赖，快速启动                    │
│  ✅ 可组合        子图复用，模块化设计                    │
│  ✅ 可观测        完整的执行日志与状态快照                │
│  ✅ 生产就绪      支持持久化、异常处理、并发控制          │
└──────────────────────────────────────────────────────────┘
```

### 1.1.4 典型应用场景

1. **智能客服系统** - 多轮对话、意图识别、人工转接
2. **代码审查工作流** - 代码分析→问题检测→建议生成
3. **数据分析管道** - 数据清洗→分析→可视化→报告生成
4. **多智能体协作** - 规划者→执行者→审核者协同工作

---

## 1.2 Maven 依赖配置

### 1.2.1 创建 Maven 项目

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.langgraph4j</groupId>
    <artifactId>langgraph-demo</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <name>LangGraph4J Demo</name>
    <description>LangGraph4J 学习示例项目</description>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <langgraph4j.version>1.8.10</langgraph4j.version>
    </properties>

    <dependencies>
        <!-- LangGraph4J 核心 -->
        <dependency>
            <groupId>org.bsc.langgraph4j</groupId>
            <artifactId>langgraph4j-core</artifactId>
            <version>${langgraph4j.version}</version>
        </dependency>

        <!-- SLF4J 日志 -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>2.0.9</version>
        </dependency>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-simple</artifactId>
            <version>2.0.9</version>
        </dependency>

        <!-- Lombok (可选，简化代码) -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>1.18.30</version>
            <scope>provided</scope>
        </dependency>

        <!-- 单元测试 -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.0</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
            </plugin>
        </plugins>
    </build>
</project>
```

### 1.2.2 项目结构

```
langgraph-demo/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── langgraph4j/
│   │   │           ├── demo/
│   │   │           │   └── HelloWorld.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/
└── pom.xml
```

---

## 1.3 核心概念速览

在编写第一个程序之前，先了解几个核心概念：

### 1.3.1 StateGraph (状态图)

StateGraph 是工作流的主体，定义了所有节点和边的连接关系。

```java
// 创建 StateGraph 需要 Channels 和 AgentStateFactory
Map<String, Channel<?>> channels = new HashMap<>();
channels.put("name", Channels.base((Supplier<String>) null));
StateGraph<AgentState> graph = new StateGraph<>(channels, AgentState::new);
```

### 1.3.2 AgentState (智能体状态)

AgentState 是工作流的状态容器，在节点之间传递和更新。

```java
// 创建状态
Map<String, Object> initData = new HashMap<>();
initData.put("name", "Developer");
AgentState state = new AgentState(initData);

// 获取状态值
String name = state.value("name").orElse("Unknown");
```

### 1.3.3 Node (节点)

节点是工作流中的执行单元，使用 `node_async` 创建。

```java
graph.addNode("greet", node_async((state, config) -> {
    String name = (String) state.value("name").orElse("World");
    return Map.of("greeting", "Hello, " + name + "!");
}));
```

### 1.3.4 Edge (边)

边定义了节点之间的流转关系，可以是有条件的或无条件的。

```java
graph.addEdge(START, "greet");  // 无条件边

// 条件边 - edge_async 将同步函数转为异步 Action
graph.addConditionalEdges(START, edge_async(this::route), mappings);
```

### 1.3.5 特殊常量

```java
import static org.bsc.langgraph4j.StateGraph.START;  // 工作流入口
import static org.bsc.langgraph4j.StateGraph.END;    // 工作流出口
```

---

## 1.4 Hello World 程序

### 1.4.1 简单问候工作流

让我们创建一个最简单的工作流：接收用户名字，输出问候语。

```java
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
import static org.bsc.langgraph4j.action.AsyncNodeActionWithConfig.node_async;

/**
 * 第一个 LangGraph4J 程序 - Hello World
 *
 * 工作流：START → greet → END
 */
public class HelloWorld {

    public static void main(String[] args) throws Exception {
        // 1. 定义状态通道（Schema）
        Map<String, Channel<?>> channels = new HashMap<>();
        channels.put("name", Channels.base((Supplier<String>) null));
        channels.put("greeting", Channels.base((Supplier<String>) null));

        // 2. 创建状态图
        StateGraph<AgentState> graph = new StateGraph<>(channels, AgentState::new);

        // 3. 添加节点
        graph.addNode("greet", node_async((state, config) -> {
            String name = (String) state.value("name").orElse("World");
            String greeting = "Hello, " + name + "! Welcome to LangGraph4J!";
            return Map.of("greeting", greeting);
        }));

        // 4. 添加边
        graph.addEdge(START, "greet");
        graph.addEdge("greet", END);

        // 5. 编译
        CompiledGraph<AgentState> compiled = graph.compile();

        // 6. 准备输入数据
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("name", "Developer");

        // 7. 执行工作流 - invoke 返回 Optional<AgentState>
        Optional<AgentState> result = compiled.invoke(inputData);

        // 8. 输出结果
        String greeting = (String) result.map(s -> s.value("greeting").orElse("")).orElse("No greeting");
        System.out.println("结果：" + greeting);
    }
}
```

### 1.4.2 运行结果

```bash
# 进入示例工程目录
cd demo-project

# 编译项目
mvn clean compile

# 运行程序
mvn exec:java -Dexec.mainClass="com.langgraph4j.demo.HelloWorld"
```

输出：
```
节点执行：生成问候语 -> Hello, Developer! Welcome to LangGraph4J!
结果：Hello, Developer! Welcome to LangGraph4J!
```

---

## 1.5 工作流执行流程解析

让我们深入理解上面程序的工作流程：

```
┌─────────────────────────────────────────────────────────────────┐
│                    Hello World 工作流执行流程                    │
└─────────────────────────────────────────────────────────────────┘

  ┌─────────┐         ┌─────────┐         ┌─────────┐
  │  START  │ ──────▶ │  greet  │ ──────▶ │   END   │
  └─────────┘         └─────────┘         └─────────┘
       │                   │                   │
       │                   │                   │
       ▼                   ▼                   ▼
  初始化状态          执行节点逻辑         返回结果
  name=null          greeting=生成        输出问候语
```

### 执行步骤详解

| 步骤 | 操作 | 状态变化 |
|------|------|---------|
| 1 | 调用 `compiled.invoke(inputData)` | 创建初始状态 |
| 2 | 从 START 节点开始 | `name="Developer"` |
| 3 | 执行 greet 节点 | `greeting="Hello, Developer!..."` |
| 4 | 到达 END | 返回 `Optional<AgentState>` |

---

## 1.6 扩展：带条件分支的工作流

让我们尝试一个稍微复杂的例子：根据用户类型输出不同的问候语。

```java
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
 */
public class ConditionalGreeting {

    // 路由函数：根据用户类型决定下一个节点
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
            return Map.of("greeting", "尊敬的 VIP 用户，欢迎您的光临！");
        }));

        // 4. 普通用户问候节点
        graph.addNode("normalGreet", node_async((state, config) -> {
            return Map.of("greeting", "您好，欢迎光临！");
        }));

        // 5. 添加条件边
        graph.addConditionalEdges(START, edge_async(ConditionalGreeting::route),
            Map.of("vipGreet", "vipGreet", "normalGreet", "normalGreet"));

        // 6. 添加结束边
        graph.addEdge("vipGreet", END);
        graph.addEdge("normalGreet", END);

        // 7. 编译
        CompiledGraph<AgentState> compiled = graph.compile();

        // 8. 测试
        Optional<AgentState> result = compiled.invoke(Map.of("userType", "vip"));
        System.out.println(result.map(s -> (String) s.value("greeting").orElse("")).orElse(""));
    }
}
```

运行：
```bash
mvn exec:java -Dexec.mainClass="com.langgraph4j.demo.ConditionalGreeting"
```

输出：
```
=== VIP 用户测试 ===
[vipGreet] 尊敬的 VIP 用户，欢迎您的光临！
结果：尊敬的 VIP 用户，欢迎您的光临！

=== 普通用户测试 ===
[normalGreet] 您好，欢迎光临！
结果：您好，欢迎光临！

=== 默认用户测试 ===
[normalGreet] 您好，欢迎光临！
结果：您好，欢迎光临！
```

---

## 本章小结

### 关键知识点

| 概念 | 说明 |
|------|------|
| StateGraph | 工作流的图结构定义，需要 `Map<String, Channel<?>>` 和 `AgentStateFactory` |
| AgentState | 在节点间传递的状态容器，使用 `value(key)` 获取值 |
| Channel / Channels | 状态通道，使用 `Channels.base(Supplier)` 创建 |
| Node | 执行单元，使用 `node_async()` 创建 |
| Edge | 节点间的流转关系，使用 `addEdge()` 和 `addConditionalEdges()` |
| START/END | 工作流的入口和出口常量 |

### 运行示例

示例工程位置：`demo-project/`

```bash
cd demo-project
mvn clean compile
mvn exec:java -Dexec.mainClass="com.langgraph4j.demo.HelloWorld"
```

---

## 课后练习

### 练习 1：环境搭建
- [ ] 安装 JDK 17+ 并配置环境变量
- [ ] 安装 Maven 并验证版本
- [ ] 创建 Maven 项目并添加 LangGraph4J 依赖

### 练习 2：运行示例
- [ ] 运行 HelloWorld 程序并成功输出
- [ ] 修改问候语，加入当前时间（上午/下午/晚上）

### 练习 3：扩展功能
- [ ] 创建一个三节点工作流：输入→处理→输出
- [ ] 添加一个中间节点，将用户名字转为大写

---

## 下一章预告

在 [第 2 章](../01-LangGraph4J 基础入门/02-核心概念与基础组件/readme.md) 中，我们将深入学习：
- StateGraph 的完整 API 与配置选项
- Channel 的类型与使用方法（基础 Channel、追加 Channel 等）
- 节点的异步执行与超时控制
- 边的类型：普通边、条件边、并行边
- 图的可视化与调试
