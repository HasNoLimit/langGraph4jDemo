# 第 4 章 工具调用与外部系统集成

## 学习目标

完成本章后，你将能够：

1. 理解 LangGraph4J 中工具调用的基本概念与工作原理
2. 掌握在节点中调用外部 API 服务的方法
3. 实现 HTTP 请求、数据库操作等外部系统集成
4. 掌握工具调用的错误处理与重试机制
5. 设计可扩展的工具封装架构

## 建议学时

**2 课时** (90 分钟)

- 理论学习：50 分钟
- 实践操作：40 分钟

---

## 4.1 工具调用概述

### 4.1.1 什么是工具调用？

在 LangGraph4J 中，**工具（Tool）** 是指智能体可以调用的外部能力，包括：
- HTTP API 调用（REST、GraphQL 等）
- 数据库操作（查询、更新等）
- 文件系统操作
- 第三方服务集成（邮件、短信、支付等）
- 自定义业务逻辑

### 4.1.2 工具调用的基本模式

在 LangGraph4J 中，工具调用通常发生在**节点内部**，节点作为工具调用的封装层：

```
┌─────────────────────────────────────────────────────────────┐
│                    工具调用架构                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────┐                                           │
│  │  StateGraph │                                           │
│  │             │                                           │
│  │  ┌───────┐  │    ┌─────────────┐    ┌───────────────┐  │
│  │  │ Node1 │──┼───▶│ Tool Service│───▶│ External API  │  │
│  │  └───────┘  │    │  (封装层)    │    │ (第三方服务)   │  │
│  │             │    └─────────────┘    └───────────────┘  │
│  │  ┌───────┐  │                                           │
│  │  │ Node2 │  │                                           │
│  │  └───────┘  │                                           │
│  └─────────────┘                                           │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 4.2 在节点中调用外部服务

### 4.2.1 基本的工具调用模式

```java
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
 * 天气查询工具示例
 */
public class WeatherToolExample {

    // 模拟天气查询服务
    public static class WeatherService {
        public String getWeather(String city) {
            // 模拟 API 调用延迟
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 模拟返回数据
            return switch (city.toLowerCase()) {
                case "beijing" -> "北京：晴，25°C";
                case "shanghai" -> "上海：多云，22°C";
                case "guangzhou" -> "广州：小雨，20°C";
                default -> city + "：数据暂无";
            };
        }
    }

    public static void main(String[] args) throws Exception {
        // 1. 定义状态通道
        Map<String, Channel<?>> channels = new HashMap<>();
        channels.put("city", Channels.base((Supplier<String>) null));
        channels.put("weatherResult", Channels.base((Supplier<String>) null));

        // 2. 创建状态图
        StateGraph<AgentState> graph = new StateGraph<>(channels, AgentState::new);

        // 3. 创建服务实例
        WeatherService weatherService = new WeatherService();

        // 4. 添加调用工具的节点
        graph.addNode("queryWeather", node_async((state, config) -> {
            String city = (String) state.value("city").orElse("");
            System.out.println("[queryWeather] 查询城市：" + city);

            // 调用工具
            String weather = weatherService.getWeather(city);
            System.out.println("[queryWeather] 天气信息：" + weather);

            // 返回结果
            return Map.of("weatherResult", weather);
        }));

        // 5. 添加边
        graph.addEdge(START, "queryWeather");
        graph.addEdge("queryWeather", END);

        // 6. 编译
        CompiledGraph<AgentState> compiled = graph.compile();

        // 7. 测试
        testWeather(compiled, "beijing", "=== 测试 1：北京天气 ===");
        testWeather(compiled, "shanghai", "=== 测试 2：上海天气 ===");
        testWeather(compiled, "chengdu", "=== 测试 3：成都天气 ===");
    }

    private static void testWeather(CompiledGraph<AgentState> compiled,
                                    String city, String label) {
        System.out.println("\n" + label);
        Map<String, Object> input = Map.of("city", city);
        Optional<AgentState> result = compiled.invoke(input);
        result.ifPresent(state -> {
            String weather = (String) state.value("weatherResult").orElse("");
            System.out.println("结果：" + weather);
        });
    }
}
```

**运行输出：**
```
=== 测试 1：北京天气 ===
[queryWeather] 查询城市：beijing
[queryWeather] 天气信息：北京：晴，25°C
结果：北京：晴，25°C

=== 测试 2：上海天气 ===
[queryWeather] 查询城市：shanghai
[queryWeather] 天气信息：上海：多云，22°C
结果：上海：多云，22°C

=== 测试 3：成都天气 ===
[queryWeather] 查询城市：chengdu
[queryWeather] 天气信息：chengdu：数据暂无
结果：chengdu：数据暂无
```

---

## 4.3 集成 HTTP API 服务

### 4.3.1 使用 Java HttpClient 调用 REST API

```java
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;

/**
 * HTTP 客户端工具类
 */
public class HttpClientUtil {

    private static final HttpClient httpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    /**
     * 发送 GET 请求
     */
    public static String get(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/json")
            .build();

        HttpResponse<String> response = httpClient.send(request,
            HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP 请求失败：" + response.statusCode());
        }

        return response.body();
    }

    /**
     * 发送 POST 请求
     */
    public static String post(String url, String jsonBody) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/json")
            .build();

        HttpResponse<String> response = httpClient.send(request,
            HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200 && response.statusCode() != 201) {
            throw new RuntimeException("HTTP 请求失败：" + response.statusCode());
        }

        return response.body();
    }
}
```

### 4.3.2 集成外部 API 的完整示例

```java
package com.langgraph4j.chapter4;

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
 * API 集成示例：翻译服务
 */
public class TranslationApiExample {

    /**
     * 翻译服务（模拟实现）
     */
    public static class TranslationService {

        /**
         * 翻译文本
         * @param text 待翻译文本
         * @param targetLang 目标语言
         * @return 翻译结果
         */
        public String translate(String text, String targetLang) {
            // 实际项目中这里会调用真实的翻译 API
            // 此处为模拟实现
            return switch (targetLang.toLowerCase()) {
                case "en" -> "[EN] " + text;
                case "zh" -> "[中文] " + text;
                case "ja" -> "[日本語] " + text;
                default -> "[" + targetLang + "] " + text;
            };
        }
    }

    public static void main(String[] args) throws Exception {
        // 1. 定义状态通道
        Map<String, Channel<?>> channels = new HashMap<>();
        channels.put("inputText", Channels.base((Supplier<String>) null));
        channels.put("targetLang", Channels.base((Supplier<String>) null));
        channels.put("translatedText", Channels.base((Supplier<String>) null));

        // 2. 创建状态图
        StateGraph<AgentState> graph = new StateGraph<>(channels, AgentState::new);

        // 3. 创建服务实例
        TranslationService translationService = new TranslationService();

        // 4. 添加翻译节点
        graph.addNode("translate", node_async((state, config) -> {
            String inputText = (String) state.value("inputText").orElse("");
            String targetLang = (String) state.value("targetLang").orElse("en");

            System.out.println("[translate] 原文：" + inputText);
            System.out.println("[translate] 目标语言：" + targetLang);

            // 调用翻译服务
            String translated = translationService.translate(inputText, targetLang);
            System.out.println("[translate] 译文：" + translated);

            return Map.of("translatedText", translated);
        }));

        // 5. 添加边
        graph.addEdge(START, "translate");
        graph.addEdge("translate", END);

        // 6. 编译
        CompiledGraph<AgentState> compiled = graph.compile();

        // 7. 测试
        testTranslation(compiled, "你好", "zh", "=== 测试 1：中文翻译 ===");
        testTranslation(compiled, "Hello", "en", "=== 测试 2：英文翻译 ===");
        testTranslation(compiled, "こんにちは", "ja", "=== 测试 3：日文翻译 ===");
    }

    private static void testTranslation(CompiledGraph<AgentState> compiled,
                                        String text, String lang, String label) {
        System.out.println("\n" + label);
        Map<String, Object> input = Map.of(
            "inputText", text,
            "targetLang", lang
        );
        Optional<AgentState> result = compiled.invoke(input);
        result.ifPresent(state -> {
            String translated = (String) state.value("translatedText").orElse("");
            System.out.println("结果：" + translated);
        });
    }
}
```

---

## 4.4 工具封装与复用

### 4.4.1 创建可复用的工具类

```java
package com.langgraph4j.chapter4.tools;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * 通用工具类示例
 */
public class CommonTools {

    /**
     * 字符串处理工具
     */
    public static class StringUtil {

        public static String toUpperCase(String input) {
            return input != null ? input.toUpperCase() : "";
        }

        public static String toLowerCase(String input) {
            return input != null ? input.toLowerCase() : "";
        }

        public static String reverse(String input) {
            return input != null ? new StringBuilder(input).reverse().toString() : "";
        }

        public static int wordCount(String input) {
            if (input == null || input.trim().isEmpty()) {
                return 0;
            }
            return input.trim().split("\\s+").length;
        }
    }

    /**
     * 列表处理工具
     */
    public static class ListUtil {

        public static <T> List<T> concat(List<T> list1, List<T> list2) {
            List<T> result = new ArrayList<>();
            if (list1 != null) result.addAll(list1);
            if (list2 != null) result.addAll(list2);
            return result;
        }

        public static <T> List<T> unique(List<T> list) {
            return new ArrayList<>(new java.util.HashSet<>(list != null ? list : new ArrayList<>()));
        }
    }

    /**
     * JSON 处理工具（简化版）
     */
    public static class JsonUtil {

        /**
         * 将 Map 转为 JSON 字符串（简化实现）
         */
        public static String toJson(Map<String, Object> map) {
            if (map == null) return "null";
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(entry.getKey()).append("\":");
                Object value = entry.getValue();
                if (value instanceof String) {
                    sb.append("\"").append(value).append("\"");
                } else {
                    sb.append(value);
                }
                first = false;
            }
            sb.append("}");
            return sb.toString();
        }
    }
}
```

### 4.4.2 在工作流中使用工具类

```java
import com.langgraph4j.chapter4.tools.CommonTools;

// ...

graph.addNode("processText", node_async((state, config) -> {
    String text = (String) state.value("inputText").orElse("");

    // 使用字符串工具
    String upper = CommonTools.StringUtil.toUpperCase(text);
    int words = CommonTools.StringUtil.wordCount(text);

    Map<String, Object> result = new HashMap<>();
    result.put("upperCase", upper);
    result.put("wordCount", words);

    return result;
}));
```

---

## 4.5 错误处理与重试机制

### 4.5.1 基础错误处理

```java
graph.addNode("callExternalApi", node_async((state, config) -> {
    Map<String, Object> result = new HashMap<>();

    try {
        // 调用外部服务
        String apiResult = callExternalService();
        result.put("apiResult", apiResult);
        result.put("success", true);
    } catch (Exception e) {
        // 记录错误
        System.err.println("API 调用失败：" + e.getMessage());

        // 返回错误信息
        result.put("success", false);
        result.put("errorMessage", e.getMessage());
        result.put("errorType", e.getClass().getSimpleName());
    }

    return result;
}));
```

### 4.5.2 重试机制实现

```java
import java.util.function.Supplier;

/**
 * 重试工具类
 */
public class RetryUtil {

    /**
     * 带重试的执行
     * @param action 要执行的操作
     * @param maxRetries 最大重试次数
     * @param delayMs 重试间隔（毫秒）
     * @return 操作结果
     */
    public static <T> T withRetry(Supplier<T> action, int maxRetries, long delayMs)
            throws Exception {

        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return action.get();
            } catch (Exception e) {
                lastException = e;
                System.err.println("第 " + attempt + " 次尝试失败：" + e.getMessage());

                if (attempt < maxRetries) {
                    Thread.sleep(delayMs);
                }
            }
        }

        throw lastException;
    }
}

// 在节点中使用
graph.addNode("reliableApiCall", node_async((state, config) -> {
    try {
        // 使用重试机制调用 API
        String result = RetryUtil.withRetry(
            () -> callUnreliableService(),
            3,  // 最多重试 3 次
            1000 // 每次间隔 1 秒
        );
        return Map.of("result", result, "success", true);
    } catch (Exception e) {
        return Map.of(
            "success", false,
            "errorMessage", "重试后仍然失败：" + e.getMessage()
        );
    }
}));
```

### 4.5.3 超时处理

```java
import java.util.concurrent.*;

/**
 * 超时工具类
 */
public class TimeoutUtil {

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * 带超时的执行
     * @param action 要执行的操作
     * @param timeoutMs 超时时间（毫秒）
     * @return 操作结果
     */
    public static <T> T withTimeout(Supplier<T> action, long timeoutMs) throws Exception {
        Future<T> future = executor.submit(action::get);
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new RuntimeException("操作超时");
        }
    }
}
```

---

## 4.6 综合示例：智能助手工作流

### 4.6.1 工作流设计

```
┌─────────────────────────────────────────────────────────────────┐
│                   智能助手工作流                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  START ──▶ parse ──▶ route ──┬──▶ weather ──┐                  │
│                              │              │                  │
│                              ├──▶ news  ────┼──▶ format ──▶ END│
│                              │              │                  │
│                              └──▶ search ───┘                  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 4.6.2 完整代码

```java
package com.langgraph4j.chapter4;

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
 * 智能助手工作流示例
 */
public class SmartAssistantWorkflow {

    // 模拟服务
    static class WeatherService {
        public String getWeather(String city) {
            return city + " 的天气：晴，25°C";
        }
    }

    static class NewsService {
        public String getNews(String category) {
            return category + " 新闻：今日热点新闻...";
        }
    }

    static class SearchService {
        public String search(String query) {
            return "搜索结果：" + query;
        }
    }

    // 路由函数
    private static String routeByIntent(AgentState state) {
        String intent = (String) state.value("intent").orElse("search");
        return switch (intent) {
            case "weather" -> "getWeather";
            case "news" -> "getNews";
            default -> "doSearch";
        };
    }

    public static void main(String[] args) throws Exception {
        // 1. 定义状态通道
        Map<String, Channel<?>> channels = new HashMap<>();
        channels.put("userInput", Channels.base((Supplier<String>) null));
        channels.put("intent", Channels.base((Supplier<String>) null));
        channels.put("entities", Channels.base((Supplier<Map<String, Object>>) null));
        channels.put("result", Channels.base((Supplier<String>) null));
        channels.put("finalOutput", Channels.base((Supplier<String>) null));

        // 2. 创建状态图
        StateGraph<AgentState> graph = new StateGraph<>(channels, AgentState::new);

        // 3. 服务实例
        WeatherService weatherService = new WeatherService();
        NewsService newsService = new NewsService();
        SearchService searchService = new SearchService();

        // 4. 添加节点

        // 解析用户输入
        graph.addNode("parse", node_async((state, config) -> {
            String input = (String) state.value("userInput").orElse("");
            System.out.println("[parse] 解析输入：" + input);

            // 简单的意图识别（实际项目应使用 NLP 模型）
            String intent;
            Map<String, Object> entities = new HashMap<>();

            if (input.contains("天气") || input.contains("晴") || input.contains("雨")) {
                intent = "weather";
                entities.put("city", extractCity(input));
            } else if (input.contains("新闻") || input.contains("热点")) {
                intent = "news";
                entities.put("category", "综合");
            } else {
                intent = "search";
                entities.put("query", input);
            }

            System.out.println("[parse] 意图：" + intent);

            Map<String, Object> result = new HashMap<>();
            result.put("intent", intent);
            result.put("entities", entities);
            return result;
        }));

        // 天气查询
        graph.addNode("getWeather", node_async((state, config) -> {
            Map<String, Object> entities =
                (Map<String, Object>) state.value("entities").orElse(new HashMap<>());
            String city = (String) entities.getOrDefault("city", "北京");

            System.out.println("[getWeather] 查询天气：" + city);
            String weather = weatherService.getWeather(city);

            return Map.of("result", weather);
        }));

        // 新闻获取
        graph.addNode("getNews", node_async((state, config) -> {
            Map<String, Object> entities =
                (Map<String, Object>) state.value("entities").orElse(new HashMap<>());
            String category = (String) entities.getOrDefault("category", "综合");

            System.out.println("[getNews] 获取新闻：" + category);
            String news = newsService.getNews(category);

            return Map.of("result", news);
        }));

        // 通用搜索
        graph.addNode("doSearch", node_async((state, config) -> {
            Map<String, Object> entities =
                (Map<String, Object>) state.value("entities").orElse(new HashMap<>());
            String query = (String) entities.getOrDefault("query", "");

            System.out.println("[doSearch] 搜索：" + query);
            String result = searchService.search(query);

            return Map.of("result", result);
        }));

        // 格式化输出
        graph.addNode("format", node_async((state, config) -> {
            String result = (String) state.value("result").orElse("");
            System.out.println("[format] 格式化输出");

            String output = "助手回复：" + result;
            return Map.of("finalOutput", output);
        }));

        // 5. 添加边
        graph.addEdge(START, "parse");

        // 条件边：根据意图路由
        graph.addConditionalEdges(
            "parse",
            edge_async(SmartAssistantWorkflow::routeByIntent),
            Map.of(
                "getWeather", "getWeather",
                "getNews", "getNews",
                "doSearch", "doSearch"
            )
        );

        // 所有分支汇聚到 format
        graph.addEdge("getWeather", "format");
        graph.addEdge("getNews", "format");
        graph.addEdge("doSearch", "format");
        graph.addEdge("format", END);

        // 6. 编译
        CompiledGraph<AgentState> compiled = graph.compile();

        // 7. 测试
        testAssistant(compiled, "北京今天天气怎么样", "=== 测试 1：天气查询 ===");
        testAssistant(compiled, "看看有什么新闻", "=== 测试 2：新闻查询 ===");
        testAssistant(compiled, "Java 17 新特性", "=== 测试 3：通用搜索 ===");
    }

    private static void testAssistant(CompiledGraph<AgentState> compiled,
                                      String input, String label) {
        System.out.println("\n" + label);
        Map<String, Object> params = Map.of("userInput", input);
        Optional<AgentState> result = compiled.invoke(params);
        result.ifPresent(state -> {
            String output = (String) state.value("finalOutput").orElse("");
            System.out.println(output);
        });
    }

    private static String extractCity(String input) {
        // 简化实现
        if (input.contains("北京")) return "北京";
        if (input.contains("上海")) return "上海";
        if (input.contains("广州")) return "广州";
        return "北京";
    }
}
```

### 4.6.3 运行结果

```
=== 测试 1：天气查询 ===
[parse] 解析输入：北京今天天气怎么样
[parse] 意图：weather
[getWeather] 查询天气：北京
[format] 格式化输出
助手回复：北京 的天气：晴，25°C

=== 测试 2：新闻查询 ===
[parse] 解析输入：看看有什么新闻
[parse] 意图：news
[getNews] 获取新闻：综合
[format] 格式化输出
助手回复：综合 新闻：今日热点新闻...

=== 测试 3：通用搜索 ===
[parse] 解析输入：Java 17 新特性
[parse] 意图：search
[doSearch] 搜索：Java 17 新特性
[format] 格式化输出
助手回复：搜索结果：Java 17 新特性
```

---

## 本章小结

### 关键知识点

| 概念 | 说明 |
|------|------|
| 工具调用 | 在节点内部调用外部服务 |
| HttpClient | Java 内置的 HTTP 客户端，用于调用 REST API |
| 工具封装 | 将通用功能封装为可复用的工具类 |
| 错误处理 | 使用 try-catch 捕获并处理异常 |
| 重试机制 | 失败时自动重试，提高可靠性 |
| 超时处理 | 防止长时间等待无响应 |

### 工具调用最佳实践

| 实践 | 说明 |
|------|------|
| 服务抽象 | 将外部服务封装为独立类，便于测试和替换 |
| 错误隔离 | 工具调用失败不应导致整个工作流崩溃 |
| 日志记录 | 记录工具调用的输入输出，便于调试 |
| 超时设置 | 为所有外部调用设置合理的超时时间 |
| 重试策略 | 对临时性失败实现自动重试 |

---

## 课后练习

### 练习 1：创建简单工具
- [ ] 创建一个计算器工具类，支持加减乘除运算
- [ ] 在工作流中调用该计算器

### 练习 2：集成 HTTP API
- [ ] 使用 HttpClient 调用一个公开的 REST API
- [ ] 将 API 返回结果存储到状态中

### 练习 3：错误处理
- [ ] 为一个可能失败的节点添加错误处理
- [ ] 实现失败时返回错误信息到状态中

---

## 下一章预告

在 [第 5 章](../02-LangGraph4J 核心开发/05-复杂工作流设计/readme.md) 中，我们将学习：
- 复杂条件分支的设计模式
- 循环和迭代工作流的实现
- 并行节点调度与执行
- 子图的封装与复用
- 人工干预节点的设计
