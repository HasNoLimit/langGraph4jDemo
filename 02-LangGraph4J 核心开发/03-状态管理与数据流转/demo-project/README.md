# Chapter 3 Demo Project

## 示例项目说明

本项目包含第 3 章 状态管理与数据流转的所有示例代码。

## 运行示例

### 示例 1：状态传递演示

```bash
mvn exec:java -Dexec.mainClass="com.langgraph4j.chapter3.StatePropagationDemo"
```

**预期输出：**
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

### 示例 2：订单处理工作流

```bash
mvn exec:java -Dexec.mainClass="com.langgraph4j.chapter3.OrderProcessingWorkflow"
```

**预期输出：**
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

## 编译项目

```bash
mvn clean compile
```

## 运行测试

```bash
mvn test
```
