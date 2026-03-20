# 第 1 章示例工程

## 项目结构

```
demo-project/
├── pom.xml                          # Maven 配置文件
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── langgraph4j/
│   │   │           └── demo/
│   │   │               └── Chapter1Placeholder.java    # 示例代码占位类
│   │   └── resources/
│   │       └── application.properties                   # 应用配置（可选）
│   └── test/
│       └── java/
│           └── com/
│               └── langgraph4j/
│                   └── demo/
│                       └── Chapter1Test.java            # 测试类占位
└── README.md                                            # 本说明文件
```

## 快速开始

### 1. 编译项目

```bash
cd demo-project
mvn clean compile
```

### 2. 运行示例

```bash
# 运行 HelloWorld 示例
mvn exec:java -Dexec.mainClass="com.langgraph4j.demo.HelloWorld"

# 运行条件分支示例
mvn exec:java -Dexec.mainClass="com.langgraph4j.demo.ConditionalGreeting"
```

### 3. 运行测试

```bash
mvn test
```

## 依赖说明

| 依赖 | 版本 | 说明 |
|------|------|------|
| langgraph4j-core | 1.8.10 | LangGraph4J 核心库 |
| slf4j-api | 2.0.9 | 日志 API |
| slf4j-simple | 2.0.9 | 简单日志实现 |
| lombok | 1.18.30 | 代码简化（可选） |
| junit-jupiter | 5.10.0 | 单元测试框架 |

## 环境要求

- JDK 17+
- Maven 3.8+

## 代码位置

将章节中的示例代码复制到 `src/main/java/com/langgraph4j/demo/` 目录下：

- `HelloWorld.java` - 简单问候工作流
- `ConditionalGreeting.java` - 条件分支工作流
