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
 * 示例 2：智能助手工作流
 *
 * 演示多工具调用的综合应用
 */
public class SmartAssistantWorkflow {

    // 模拟天气服务
    static class WeatherService {
        public String getWeather(String city) {
            return city + " 的天气：晴，25°C";
        }
    }

    // 模拟新闻服务
    static class NewsService {
        public String getNews(String category) {
            return category + " 新闻：今日热点新闻...";
        }
    }

    // 模拟搜索服务
    static class SearchService {
        public String search(String query) {
            return "搜索结果：" + query;
        }
    }

    // 路由函数：根据意图路由
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

            // 简单的意图识别
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
        System.out.println("=== 智能助手工作流示例 ===\n");
        testAssistant(compiled, "北京今天天气怎么样", "=== 测试 1：天气查询 ===");
        testAssistant(compiled, "看看有什么新闻", "=== 测试 2：新闻查询 ===");
        testAssistant(compiled, "Java 17 新特性", "=== 测试 3：通用搜索 ===");
    }

    private static void testAssistant(CompiledGraph<AgentState> compiled,
                                      String input, String label) {
        System.out.println(label);
        Map<String, Object> params = Map.of("userInput", input);
        Optional<AgentState> result = compiled.invoke(params);
        result.ifPresent(state -> {
            String output = (String) state.value("finalOutput").orElse("");
            System.out.println(output);
        });
        System.out.println();
    }

    private static String extractCity(String input) {
        // 简化实现
        if (input.contains("北京")) return "北京";
        if (input.contains("上海")) return "上海";
        if (input.contains("广州")) return "广州";
        if (input.contains("深圳")) return "深圳";
        return "北京";
    }
}
