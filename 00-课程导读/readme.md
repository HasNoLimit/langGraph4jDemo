# 课程导读

## 学习路线图

```
入门阶段                    进阶阶段                   实战阶段
    │                          │                         │
    ▼                          ▼                         ▼
┌──────────┐            ┌──────────┐             ┌──────────┐
│ 环境搭建 │ ──────────▶│ 状态管理 │ ──────────▶ │ 多智能体 │
│ 核心概念 │            │ 工具调用 │             │  协同    │
│ 基础组件 │            │ 复杂流程 │             │ 性能优化 │
└──────────┘            └──────────┘             └──────────┘
                              │                         │
                              ▼                         ▼
                        ┌──────────┐             ┌──────────┐
                        │ 最佳实践 │             │ 企业实战 │
                        └──────────┘             └──────────┘
```

## 学习建议

| 阶段 | 建议学时 | 重点内容 | 产出物 |
|------|---------|---------|--------|
| 基础入门 | 4 课时 | 核心概念、环境搭建 | 第一个工作流程序 |
| 核心开发 | 6 课时 | 状态管理、工具调用、复杂流程 | 完整的工作流应用 |
| 高级特性 | 4 课时 | 多智能体、性能优化 | 分布式智能体系统 |
| 企业实战 | 4 课时 | 智能客服系统 | 可部署的企业级应用 |

## 环境检查清单

在开始学习前，请确保你的开发环境满足以下要求：

### 必需软件

- [ ] **JDK 17+** (推荐 JDK 21 LTS)
  ```bash
  java -version  # 验证版本
  ```
- [ ] **Maven 3.8+**
  ```bash
  mvn -version  # 验证版本
  ```
- [ ] **Git**
  ```bash
  git --version  # 验证版本
  ```

### 开发工具

- [ ] **IDE**: IntelliJ IDEA / Eclipse / VS Code (推荐 IntelliJ IDEA 2023+)
- [ ] **Lombok 插件** (如使用 IDE)
- [ ] **API 调试工具**: Postman / curl

### API 密钥

- [ ] **大模型 API 密钥** (任选其一):
  - OpenAI API Key
  - Anthropic API Key
  - 阿里云百炼 API Key
  - 其他兼容 OpenAI 格式的 API

### 环境验证

运行以下命令验证 Java 环境：

```bash
# 检查 Java 版本
java -version

# 检查 Maven 版本
mvn -version

# 创建测试项目
mvn archetype:generate -DgroupId=com.test -DartifactId=langgraph-test -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false
```

## 课程代码仓库

- 示例代码位置：各章节目录下的 `examples/` 文件夹
- 练习答案位置：`05-附录与资源/练习答案/`
- 常见问题：`05-附录与资源/常见问题 FAQ/`

## 学习支持

- 每章包含 `readme.md` 详细说明本章内容
- 每章包含 `examples/` 提供完整可运行的示例代码
- 每章包含 `exercises/` 提供练习题巩固知识

---

**下一步**: 开始 [第 1 章：LangGraph4J 概述与环境搭建](../01-LangGraph4J 基础入门/01-LangGraph4J 概述与环境搭建/readme.md)
