# 第 1 章 练习题

## 练习 1：环境搭建验证

**目标**：确保开发环境配置正确

**要求**：
1. 安装 JDK 17 或更高版本
2. 安装 Maven 3.8 或更高版本
3. 验证安装并记录版本号

**提交内容**：
```bash
# 请运行以下命令并记录输出
java -version
mvn -version
```

**预期输出示例**：
```
java version "17.0.8" 2023-07-18 LTS
Java(TM) SE Runtime Environment (build 17.0.8+9-LTS-211)
Java HotSpot(TM) 64-Bit Server VM (build 17.0.8+9-LTS-211, mixed mode, sharing)

Apache Maven 3.9.4
Maven home: /usr/local/Cellar/maven/3.9.4/libexec
Java version: 17.0.8
```

---

## 练习 2：修改 Hello World

**目标**：理解工作流的基本结构

**要求**：
1. 复制 `examples/HelloWorld.java`
2. 修改问候节点，根据当前时间输出不同的问候语：
   - 5:00-11:59 → "早上好"
   - 12:00-17:59 → "下午好"
   - 18:00-4:59 → "晚上好"

**参考代码框架**：
```java
graph.addNode("greet", state -> {
    int hour = java.time.LocalTime.now().getHour();
    String timeGreeting;
    if (hour >= 5 && hour < 12) {
        timeGreeting = "早上好";
    } else if (hour >= 12 && hour < 18) {
        timeGreeting = "下午好";
    } else {
        timeGreeting = "晚上好";
    }
    String name = state.value("name").orElse("World");
    state.data().put("greeting", timeGreeting + ", " + name + "!");
    return state;
});
```

**验证**：运行程序，确认输出包含正确的时间问候语

---

## 练习 3：三节点工作流

**目标**：掌握多节点工作流的构建

**要求**：
创建一个三节点工作流，实现以下功能：

```
START → extract → transform → output → END

1. extract 节点：从输入中提取用户名
2. transform 节点：将用户名转换为大写
3. output 节点：生成最终输出
```

**状态通道设计**：
```java
Map<String, Channel<?>> channels = new HashMap<>();
channels.put("rawInput", Channel.of());
channels.put("extractedName", Channel.of());
channels.put("upperName", Channel.of());
channels.put("finalOutput", Channel.of());
```

**测试用例**：
- 输入：`rawInput = "hello, alice"`
- 预期输出：`finalOutput = "处理完成：ALICE"`

---

## 练习 4：多条件分支

**目标**：掌握条件边的使用

**要求**：
基于 `ConditionalGreeting.java`，扩展为三分支：
- VIP 用户 → "尊敬的 VIP 用户，欢迎您的光临！"
- 会员用户 → "亲爱的会员，欢迎光临！"
- 普通用户 → "您好，欢迎光临！"

**提示**：
1. 添加新的节点 `memberGreet`
2. 修改 `route` 函数，返回三个可能的值

---

## 练习 5：工作流执行轨迹

**目标**：理解工作流的执行过程

**要求**：
1. 在 `HelloWorld` 的每个节点中添加日志输出
2. 记录工作流的执行路径

**示例**：
```java
graph.addNode("greet", state -> {
    System.out.println("[TRACE] 进入 greet 节点");
    String name = state.value("name").orElse("Unknown");
    System.out.println("[TRACE] 输入 name: " + name);
    // ... 处理逻辑
    String greeting = "Hello, " + name + "!";
    state.data().put("greeting", greeting);
    System.out.println("[TRACE] 输出 greeting: " + greeting);
    return state;
});
```

**思考题**：
- 如果节点执行失败，会发生什么？
- 如何记录完整的执行历史？

---

## 提交方式

将完成的代码文件放入 `exercises/` 目录下：

```
exercises/
├── exercise1-env-check.txt      # 环境检查输出
├── exercise2-timeday-greeting.java
├── exercise3-three-nodes.java
├── exercise4-multi-branch.java
└── exercise5-trace.log          # 执行轨迹日志
```

---

## 评分标准

| 练习 | 分值 | 评分标准 |
|------|------|---------|
| 练习 1 | 10 分 | 环境配置正确，能正常运行 Maven 项目 |
| 练习 2 | 20 分 | 时间判断逻辑正确，输出符合预期 |
| 练习 3 | 30 分 | 三节点工作流完整，数据流转正确 |
| 练习 4 | 20 分 | 三分支条件判断正确 |
| 练习 5 | 20 分 | 轨迹日志完整，能清晰展示执行流程 |

**总分：100 分**

---

## 参考答案

参考答案位置：`../../../05-附录与资源/练习答案/第 1 章答案.md`
