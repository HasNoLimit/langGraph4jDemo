# Chapter 4 Demo Project

## 示例项目说明

本项目包含第 4 章 工具调用与外部系统集成的所有示例代码。

## 运行示例

### 示例 1：天气查询工具

```bash
mvn exec:java -Dexec.mainClass="com.langgraph4j.chapter4.WeatherToolExample"
```

**预期输出：**
```
=== 天气查询工具示例 ===

=== 测试 1：北京天气 ===
[queryWeather] 查询城市：beijing
[queryWeather] 天气信息：北京：晴，25°C
结果：北京：晴，25°C

=== 测试 2：上海天气 ===
[queryWeather] 查询城市：shanghai
[queryWeather] 天气信息：上海：多云，22°C
结果：上海：多云，22°C
```

### 示例 2：智能助手工作流

```bash
mvn exec:java -Dexec.mainClass="com.langgraph4j.chapter4.SmartAssistantWorkflow"
```

**预期输出：**
```
=== 智能助手工作流示例 ===

=== 测试 1：天气查询 ===
[parse] 解析输入：北京今天天气怎么样
[parse] 意图：weather
[getWeather] 查询天气：北京
[format] 格式化输出
助手回复：北京 的天气：晴，25°C
```

### 示例 3：带错误处理的工具调用

```bash
mvn exec:java -Dexec.mainClass="com.langgraph4j.chapter4.ToolCallWithErrorHandling"
```

**预期输出：**
```
=== 带错误处理的工具调用示例 ===

--- 测试 1：不使用重试 ---
API 调用失败：服务暂时不可用
结果：失败 - 服务暂时不可用

--- 测试 2：使用重试机制（最多 3 次，间隔 500ms）---
第 1 次尝试失败：服务暂时不可用
第 2 次尝试失败：服务暂时不可用
结果：成功 - 服务调用成功
```

## 编译项目

```bash
mvn clean compile
```

## 运行测试

```bash
mvn test
```
