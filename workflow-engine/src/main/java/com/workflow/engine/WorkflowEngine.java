package com.workflow.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.engine.config.NodeConfig;
import com.workflow.engine.definition.WorkflowDefinition;
import com.workflow.engine.executor.NodeExecutor;
import com.workflow.engine.registry.NodeRegistry;
import com.workflow.engine.variable.DefaultVariableResolver;
import com.workflow.engine.variable.VariableResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 工作流引擎核心
 * 职责：解析定义、构建执行图、协调执行
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowEngine {

    private final NodeRegistry nodeRegistry;
    private final ObjectMapper objectMapper;
    private final Map<String, Channel<?>> defaultChannels;

    /**
     * 启动时自动注册所有节点执行器
     * 从 Spring 容器中获取所有 NodeExecutor 实现
     */
    @PostConstruct
    public void initialize() {
        // 节点执行器已通过 @Component 自动注册到 Spring 容器
        // NodeRegistry 会在需要时从 Spring 获取
        log.info("WorkflowEngine initialized with node types: {}",
                nodeRegistry.getAllTypes());
    }

    /**
     * 从 JSON 创建工作流实例
     *
     * @param jsonDefinition JSON 定义
     * @return 工作流实例
     * @throws Exception 解析或编译失败
     */
    public WorkflowInstance createInstance(String jsonDefinition) throws Exception {
        WorkflowDefinition definition = objectMapper.readValue(jsonDefinition, WorkflowDefinition.class);
        return createInstance(definition);
    }

    /**
     * 从工作流定义创建实例
     *
     * @param definition 工作流定义
     * @return 工作流实例
     */
    public WorkflowInstance createInstance(WorkflowDefinition definition) {
        log.info("Creating workflow instance: id={}, name={}", definition.getId(), definition.getName());

        // 1. 校验所有节点类型是否已注册
        validateNodeTypes(definition);

        // 2. 构建 LangGraph4j 执行图
        CompiledGraph<AgentState> compiledGraph = buildGraph(definition);

        // 3. 返回工作流实例
        return new WorkflowInstance(definition.getId(), definition.getName(), compiledGraph);
    }

    /**
     * 校验节点类型
     */
    private void validateNodeTypes(WorkflowDefinition definition) {
        for (WorkflowDefinition.NodeDefinition node : definition.getNodes()) {
            String type = node.getType();
            if (!nodeRegistry.hasType(type)) {
                throw new IllegalArgumentException(
                        "Unknown node type: " + type + ", please register a NodeExecutor for this type");
            }
        }
    }

    /**
     * 构建执行图
     */
    private CompiledGraph<AgentState> buildGraph(WorkflowDefinition definition) {
        try {
            // 创建状态图
            StateGraph<AgentState> graph = new StateGraph<>(defaultChannels, AgentState::new);

            // 添加节点
            for (WorkflowDefinition.NodeDefinition nodeDef : definition.getNodes()) {
                addNodeToGraph(graph, nodeDef);
            }

            // 添加边
            for (WorkflowDefinition.EdgeDefinition edgeDef : definition.getEdges()) {
                addEdgeToGraph(graph, edgeDef);
            }

            // 编译图
            return graph.compile();
        } catch (GraphStateException e) {
            throw new RuntimeException("Failed to build workflow graph: " + e.getMessage(), e);
        }
    }

    /**
     * 添加节点到图
     */
    @SuppressWarnings("unchecked")
    private void addNodeToGraph(StateGraph<AgentState> graph, WorkflowDefinition.NodeDefinition nodeDef) {
        try {
            String nodeId = nodeDef.getId();
            String nodeType = nodeDef.getType();

            NodeExecutor<NodeConfig> executor = (NodeExecutor<NodeConfig>) nodeRegistry.getExecutor(nodeType);
            NodeConfig config = parseConfig(nodeDef.getConfig(), executor.getConfigClass());
            config.setNodeId(nodeId);
            if (config.getNodeName() == null) {
                config.setNodeName(nodeId);
            }

            // 校验配置
            executor.validate(config);

            // 包装为 LangGraph4j 节点
            AsyncNodeAction<AgentState> action = wrapNodeExecutor(executor, config);
            graph.addNode(nodeId, action);

            log.debug("Added node to graph: id={}, type={}", nodeId, nodeType);
        } catch (GraphStateException e) {
            throw new RuntimeException("Failed to add node to graph: " + nodeDef.getId(), e);
        }
    }

    /**
     * 添加边到图
     */
    private void addEdgeToGraph(StateGraph<AgentState> graph, WorkflowDefinition.EdgeDefinition edgeDef) {
        try {
            String source = edgeDef.getSource();
            String target = edgeDef.getTarget();

            // 处理 START/END 特殊节点
            if ("__START__".equals(source)) {
                graph.addEdge(StateGraph.START, target);
            } else if ("__END__".equals(target)) {
                graph.addEdge(source, StateGraph.END);
            } else {
                graph.addEdge(source, target);
            }

            log.debug("Added edge to graph: {} -> {}", source, target);
        } catch (GraphStateException e) {
            throw new RuntimeException("Failed to add edge to graph: " + edgeDef.getSource() + " -> " + edgeDef.getTarget(), e);
        }
    }

    /**
     * 解析配置
     */
    private <C extends NodeConfig> C parseConfig(Map<String, Object> configMap, Class<C> configClass) {
        return objectMapper.convertValue(configMap, configClass);
    }

    /**
     * 包装节点执行器为 LangGraph4j 节点动作
     */
    private <C extends NodeConfig> AsyncNodeAction<AgentState> wrapNodeExecutor(
            NodeExecutor<C> executor, C config) {

        VariableResolver variableResolver = new DefaultVariableResolver();

        return node_async((state) -> {
            // 构建节点上下文
            com.workflow.engine.context.NodeContext context =
                    com.workflow.engine.context.NodeContext.builder()
                            .workflowId(state.value("__workflow_id", (String) null))
                            .nodeId(config.getNodeId())
                            .globalState(state)
                            .variableResolver(variableResolver)
                            .runtimeParams(state.value("__runtime_params", new HashMap<String, Object>()))
                            .build();

            // 执行节点
            Map<String, Object> output = executor.execute(context, config);

            // 合并输出到状态
            Map<String, Object> updates = new HashMap<>(output);
            return updates;
        });
    }
}
