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
 * 示例 1：天气查询工具
 *
 * 演示如何在节点中调用外部服务
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
                case "shenzhen" -> "深圳：多云，23°C";
                default -> city + "：天气数据暂无";
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
        System.out.println("=== 天气查询工具示例 ===\n");
        testWeather(compiled, "beijing", "=== 测试 1：北京天气 ===");
        testWeather(compiled, "shanghai", "=== 测试 2：上海天气 ===");
        testWeather(compiled, "guangzhou", "=== 测试 3：广州天气 ===");
        testWeather(compiled, "chengdu", "=== 测试 4：成都天气 ===");
    }

    private static void testWeather(CompiledGraph<AgentState> compiled,
                                    String city, String label) {
        System.out.println(label);
        Map<String, Object> input = Map.of("city", city);
        Optional<AgentState> result = compiled.invoke(input);
        result.ifPresent(state -> {
            String weather = (String) state.value("weatherResult").orElse("");
            System.out.println("结果：" + weather);
        });
        System.out.println();
    }
}
