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
 * 示例 3：带错误处理的工具调用
 *
 * 演示如何在工具调用中实现错误处理和重试机制
 */
public class ToolCallWithErrorHandling {

    // 模拟可能失败的服务
    static class UnreliableService {
        private int callCount = 0;
        private final int failUntilCall;

        public UnreliableService(int failUntilCall) {
            this.failUntilCall = failUntilCall;
        }

        public String call() throws Exception {
            callCount++;
            if (callCount <= failUntilCall) {
                throw new Exception("服务暂时不可用（第 " + callCount + " 次调用）");
            }
            return "服务调用成功（第 " + callCount + " 次尝试）";
        }
    }

    // 重试工具类
    public static class RetryUtil {
        public static <T> T withRetry(java.util.function.Supplier<T> action, int maxRetries, long delayMs)
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

    public static void main(String[] args) throws Exception {
        System.out.println("=== 带错误处理的工具调用示例 ===\n");

        // 测试 1：不使用重试，直接失败
        System.out.println("--- 测试 1：不使用重试 ---");
        testWithoutRetry();

        // 测试 2：使用重试机制
        System.out.println("\n--- 测试 2：使用重试机制（最多 3 次，间隔 500ms）---");
        testWithRetry();
    }

    private static void testWithoutRetry() throws Exception {
        Map<String, Channel<?>> channels = new HashMap<>();
        channels.put("apiResult", Channels.base((Supplier<String>) null));
        channels.put("success", Channels.base((Supplier<Boolean>) null));
        channels.put("errorMessage", Channels.base((Supplier<String>) null));

        StateGraph<AgentState> graph = new StateGraph<>(channels, AgentState::new);

        // 添加节点：调用不可靠服务
        graph.addNode("callService", node_async((state, config) -> {
            Map<String, Object> result = new HashMap<>();
            UnreliableService service = new UnreliableService(2); // 前 2 次调用会失败

            try {
                String apiResult = service.call();
                result.put("apiResult", apiResult);
                result.put("success", true);
            } catch (Exception e) {
                System.err.println("API 调用失败：" + e.getMessage());
                result.put("success", false);
                result.put("errorMessage", e.getMessage());
            }

            return result;
        }));

        graph.addEdge(START, "callService");
        graph.addEdge("callService", END);

        CompiledGraph<AgentState> compiled = graph.compile();
        Optional<AgentState> result = compiled.invoke(Map.of());

        result.ifPresent(state -> {
            Boolean success = (Boolean) state.value("success").orElse(false);
            String message = (String) state.value("apiResult").orElse(
                (String) state.value("errorMessage").orElse(""));
            System.out.println("结果：" + (success ? "成功 - " : "失败 - ") + message);
        });
    }

    private static void testWithRetry() throws Exception {
        Map<String, Channel<?>> channels = new HashMap<>();
        channels.put("apiResult", Channels.base((Supplier<String>) null));
        channels.put("success", Channels.base((Supplier<Boolean>) null));
        channels.put("attempts", Channels.base((Supplier<Integer>) null));

        StateGraph<AgentState> graph = new StateGraph<>(channels, AgentState::new);

        // 添加节点：使用重试机制调用不可靠服务
        graph.addNode("callServiceWithRetry", node_async((state, config) -> {
            Map<String, Object> result = new HashMap<>();
            UnreliableService service = new UnreliableService(2); // 前 2 次调用会失败

            try {
                String apiResult = RetryUtil.withRetry(
                    () -> {
                        try {
                            return service.call();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    },
                    3,  // 最多重试 3 次
                    500 // 每次间隔 500ms
                );
                result.put("apiResult", apiResult);
                result.put("success", true);
            } catch (Exception e) {
                result.put("success", false);
                result.put("errorMessage", "重试后仍然失败：" + e.getMessage());
            }

            return result;
        }));

        graph.addEdge(START, "callServiceWithRetry");
        graph.addEdge("callServiceWithRetry", END);

        CompiledGraph<AgentState> compiled = graph.compile();
        Optional<AgentState> result = compiled.invoke(Map.of());

        result.ifPresent(state -> {
            Boolean success = (Boolean) state.value("success").orElse(false);
            String apiResult = (String) state.value("apiResult").orElse("");
            String errorMessage = (String) state.value("errorMessage").orElse("");
            System.out.println("结果：" + (success ? "成功 - " : "失败 - ") +
                (success ? apiResult : errorMessage));
        });
    }
}
