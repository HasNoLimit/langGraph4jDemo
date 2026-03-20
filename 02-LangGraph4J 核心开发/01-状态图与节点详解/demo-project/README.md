# Chapter 2 Demo Project

## 示例项目说明

本项目包含第 2 章 StateGraph 与节点详解的所有示例代码。

## 运行示例

### 1. 多轮对话工作流

```bash
mvn exec:java -Dexec.mainClass="com.langgraph4j.chapter2.MultiTurnConversation"
```

**预期输出：**
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

### 2. 练习 1：三节点线性工作流

```bash
mvn exec:java -Dexec.mainClass="com.langgraph4j.chapter2.Exercise1_LinearWorkflow"
```

### 3. 练习 2：条件分支 - 奇偶数判断

```bash
mvn exec:java -Dexec.mainClass="com.langgraph4j.chapter2.Exercise2_ConditionalBranch"
```

### 4. 练习 3：消息累积工作流

```bash
mvn exec:java -Dexec.mainClass="com.langgraph4j.chapter2.Exercise3_MessageAccumulator"
```

## 编译项目

```bash
mvn clean compile
```

## 运行测试

```bash
mvn test
```
