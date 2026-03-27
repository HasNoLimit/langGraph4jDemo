# Workflow Engine MVP

基于 LangGraph4j 的工作流引擎 MVP 实现。

## 项目结构

```
workflow-engine/
├── pom.xml                           # Maven 配置
├── src/
│   ├── main/
│   │   ├── java/com/workflow/engine/
│   │   │   ├── Application.java                  # Spring Boot 启动类
│   │   │   ├── WorkflowEngine.java               # 工作流引擎核心
│   │   │   ├── WorkflowInstance.java             # 工作流实例
│   │   │   ├── api/
│   │   │   │   └── WorkflowController.java       # REST API
│   │   │   ├── builtin/
│   │   │   │   ├── StartNodeExecutor.java        # 开始节点
│   │   │   │   ├── EndNodeExecutor.java          # 结束节点
│   │   │   │   ├── LLMNodeExecutor.java          # LLM 节点
│   │   │   │   ├── StartConfig.java              # 开始节点配置
│   │   │   │   ├── EndConfig.java                # 结束节点配置
│   │   │   │   └── LLMConfig.java                # LLM 节点配置
│   │   │   ├── config/
│   │   │   │   ├── NodeConfig.java               # 节点配置基类
│   │   │   │   ├── EngineConfig.java             # 引擎配置
│   │   │   │   └── MyBatisPlusConfig.java        # MyBatis-Plus 配置
│   │   │   ├── context/
│   │   │   │   └── NodeContext.java              # 节点执行上下文
│   │   │   ├── definition/
│   │   │   │   └── WorkflowDefinition.java       # 工作流定义 POJO
│   │   │   ├── entity/
│   │   │   │   ├── WorkflowDefinitionEntity.java # 定义实体
│   │   │   │   └── WorkflowInstanceEntity.java   # 实例实体
│   │   │   ├── executor/
│   │   │   │   └── NodeExecutor.java             # 节点执行器接口
│   │   │   ├── mapper/
│   │   │   │   ├── WorkflowDefinitionMapper.java # 定义 Mapper
│   │   │   │   └── WorkflowInstanceMapper.java   # 实例 Mapper
│   │   │   ├── registry/
│   │   │   │   └── NodeRegistry.java             # 节点注册表
│   │   │   ├── service/
│   │   │   │   ├── WorkflowDefinitionService.java # 定义服务
│   │   │   │   └── WorkflowExecutionService.java  # 执行服务
│   │   │   └── variable/
│   │   │       ├── VariableResolver.java         # 变量解析器接口
│   │   │       ├── VariableContext.java          # 变量上下文
│   │   │       ├── DefaultVariableResolver.java  # 默认变量解析器
│   │   │       └── SystemVariableProvider.java   # 系统变量提供者
│   │   └── resources/
│   │       ├── application.yml       # 应用配置
│   │       ├── schema.sql            # 数据库脚本
│   │       └── workflows/            # 示例工作流
│   │           ├── simple-chat.json
│   │           └── greeting-bot.json
│   └── test/
│       └── java/com/workflow/engine/
│           ├── variable/VariableResolverTest.java
│           └── builtin/NodeExecutorTest.java
```

## 技术栈

- Java 17
- Spring Boot 3.2.x
- LangGraph4j 1.8.x
- LangChain4j 0.30.x
- MyBatis-Plus 3.5.x
- PostgreSQL
- Lombok

## 核心特性

1. **节点与引擎完全解耦** - 新增节点只需实现 `NodeExecutor` 接口并注册为 Spring Bean
2. **变量系统** - 支持 `{{variable}}` 模板语法和系统变量（`__now`, `__date`, `__uuid` 等）
3. **内置节点** - Start、LLM、End
4. **数据库持久化** - PostgreSQL + MyBatis-Plus

## API 端点

- `POST /api/workflows` - 创建工作流
- `GET /api/workflows/{id}` - 获取工作流定义
- `GET /api/workflows` - 列出活跃工作流
- `POST /api/workflows/{id}/execute` - 执行工作流
- `POST /api/workflows/execute` - 使用传入定义执行工作流
- `GET /api/instances/{instanceId}` - 查询实例状态

## 快速开始

### 1. 配置数据库

```bash
# 创建 PostgreSQL 数据库
createdb workflow_engine

# 执行 schema.sql 创建表
psql -d workflow_engine -f src/main/resources/schema.sql
```

### 2. 配置应用

编辑 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/workflow_engine
    username: your_username
    password: your_password

langchain4j:
  open-ai:
    api-key: your_openai_api_key
```

### 3. 编译运行

```bash
# 使用 IDEA 编译（推荐，因为已配置 Lombok 注解处理器）
# 或者使用 Maven（需要先解决 Lombok 问题）

# 运行应用
mvn spring-boot:run
```

### 4. 测试工作流

```bash
# 创建并执行工作流
curl -X POST http://localhost:8080/api/workflows/execute \
  -H "Content-Type: application/json" \
  -d '{
    "definition": {
      "id": "test-chat",
      "name": "测试对话",
      "nodes": [
        {"id": "start", "type": "start", "config": {"nodeId": "start"}},
        {"id": "llm", "type": "llm", "config": {
          "nodeId": "llm",
          "promptTemplate": "用户说：{{query}}\n\n请回复：",
          "outputMapping": {"output": "response"}
        }},
        {"id": "end", "type": "end", "config": {
          "nodeId": "end",
          "outputTemplate": {"answer": "{{response}}"}
        }}
      ],
      "edges": [
        {"source": "__START__", "target": "start"},
        {"source": "start", "target": "llm"},
        {"source": "llm", "target": "end"},
        {"source": "end", "target": "__END__"}
      ]
    },
    "inputs": {"query": "你好"}
  }'
```

## 注意事项

### Lombok 编译问题

当前项目使用 Lombok 注解生成代码。如果在命令行使用 `mvn compile` 编译失败，请：

1. 使用 IntelliJ IDEA 打开项目并编译（推荐）
2. 或者在 `pom.xml` 中检查 Lombok 注解处理器配置

IDEA 配置已自动识别（见 `.idea/compiler.xml`）。

## 扩展开发

### 添加自定义节点

```java
@Component
public class MyNodeExecutor implements NodeExecutor<MyConfig> {

    @Override
    public String getType() {
        return "my-node";
    }

    @Override
    public Class<MyConfig> getConfigClass() {
        return MyConfig.class;
    }

    @Override
    public Map<String, Object> execute(NodeContext context, MyConfig config) {
        // 实现节点逻辑
        return Map.of("result", "processed");
    }
}
```

## 许可证

MIT
