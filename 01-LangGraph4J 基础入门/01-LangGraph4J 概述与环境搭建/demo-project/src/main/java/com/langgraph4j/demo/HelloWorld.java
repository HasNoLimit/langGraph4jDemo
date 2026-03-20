package com.langgraph4j.demo;

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
 * 第一个 LangGraph4J 程序 - Hello World
 *
 * 工作流：START → greet → END
 *
 * 运行方式:
 * mvn exec:java -Dexec.mainClass="com.langgraph4j.demo.HelloWorld"
 */
public class HelloWorld {

    public static void main(String[] args) throws Exception {
        // 1. 定义状态通道（Schema）
        // Channels.base 需要一个 Supplier 或 Reducer，这里使用 null Supplier
        Map<String, Channel<?>> channels = new HashMap<>();
        channels.put("name", Channels.base((Supplier<String>) null));
        channels.put("greeting", Channels.base((Supplier<String>) null));

        // 2. 创建状态图
        StateGraph<AgentState> graph = new StateGraph<>(channels, AgentState::new);

        // 3. 添加节点
        graph.addNode("greet", node_async((state, config) -> {
            // 问候节点：生成问候语
            String name = (String) state.value("name").orElse("World");
            String greeting = "Hello, " + name + "! Welcome to LangGraph4J!";
            System.out.println("节点执行：生成问候语 -> " + greeting);
            return Map.of("greeting", greeting);
        }));

        // 4. 添加边
        graph.addEdge(START, "greet");  // 从 START 到 greet
        graph.addEdge("greet", END);    // 从 greet 到 END

        // 5. 编译
        CompiledGraph<AgentState> compiled = graph.compile();

        // 6. 准备输入数据
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("name", "Developer");

        // 7. 执行工作流 - invoke 返回 Optional<AgentState>
        Optional<AgentState> resultOptional = compiled.invoke(inputData);

        // 8. 输出结果
        if (resultOptional.isPresent()) {
            AgentState result = resultOptional.get();
            String greeting = (String) result.value("greeting").orElse("No greeting");
            System.out.println("结果：" + greeting);
        } else {
            System.out.println("工作流执行无结果");
        }
        // 输出：Hello, Developer! Welcome to LangGraph4J!
    }
}
