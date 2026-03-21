package com.langgraph4j.chapter6.monitor;

import org.bsc.langgraph4j.state.AgentState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 工作流状态追踪器
 * 追踪工作流执行状态，支持实时推送给前台
 */
public class WorkflowStateTracker {

    /**
     * 节点执行状态
     */
    public enum NodeStatus {
        PENDING,     // 等待执行
        RUNNING,     // 正在执行
        COMPLETED,   // 执行完成
        FAILED       // 执行失败
    }

    /**
     * 节点执行记录
     */
    public static class NodeExecutionRecord {
        private final String nodeName;
        private NodeStatus status;
        private final long startTime;
        private Long endTime;
        private String message;
        private Map<String, Object> inputData;
        private Map<String, Object> outputData;
        private Throwable error;

        public NodeExecutionRecord(String nodeName) {
            this.nodeName = nodeName;
            this.status = NodeStatus.PENDING;
            this.startTime = System.currentTimeMillis();
        }

        public void setStatus(NodeStatus status) {
            this.status = status;
        }

        public void setEndTime(Long endTime) {
            this.endTime = endTime;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public void setInputData(Map<String, Object> inputData) {
            this.inputData = inputData;
        }

        public void setOutputData(Map<String, Object> outputData) {
            this.outputData = outputData;
        }

        public void setError(Throwable error) {
            this.error = error;
        }

        public String getNodeName() {
            return nodeName;
        }

        public NodeStatus getStatus() {
            return status;
        }

        public long getStartTime() {
            return startTime;
        }

        public Long getEndTime() {
            return endTime;
        }

        public long getDuration() {
            if (endTime != null) {
                return endTime - startTime;
            }
            return System.currentTimeMillis() - startTime;
        }

        public String getMessage() {
            return message;
        }

        public Map<String, Object> getInputData() {
            return inputData;
        }

        public Map<String, Object> getOutputData() {
            return outputData;
        }

        public Throwable getError() {
            return error;
        }

        public Map<String, Object> toJson() {
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("nodeName", nodeName);
            json.put("status", status.name());
            json.put("startTime", startTime);
            json.put("endTime", endTime);
            json.put("duration", getDuration());
            json.put("message", message != null ? message : "");
            json.put("inputKeys", inputData != null ? inputData.keySet() : Collections.emptySet());
            json.put("outputKeys", outputData != null ? outputData.keySet() : Collections.emptySet());
            if (error != null) {
                json.put("error", error.getMessage());
            }
            return json;
        }
    }

    /**
     * 工作流执行上下文
     */
    public static class WorkflowExecutionContext {
        private final String workflowId;
        private final String workflowName;
        private final long startTime;
        private NodeStatus status = NodeStatus.PENDING;
        final Map<String, NodeExecutionRecord> nodeRecords = new ConcurrentHashMap<>();
        final List<WorkflowEvent> events = new CopyOnWriteArrayList<>();
        private String current_node;
        private final List<String> executionPath = new ArrayList<>();

        public WorkflowExecutionContext(String workflowId, String workflowName) {
            this.workflowId = workflowId;
            this.workflowName = workflowName;
            this.startTime = System.currentTimeMillis();
        }

        public void addEvent(WorkflowEvent event) {
            events.add(event);
        }

        public void addNodeRecord(NodeExecutionRecord record) {
            nodeRecords.put(record.getNodeName(), record);
        }

        public NodeExecutionRecord getNodeRecord(String nodeName) {
            return nodeRecords.get(nodeName);
        }

        public NodeExecutionRecord getOrCreateNodeRecord(String nodeName) {
            return nodeRecords.computeIfAbsent(nodeName, NodeExecutionRecord::new);
        }

        public void setCurrentNode(String currentNode) {
            this.current_node = currentNode;
            if (currentNode != null) {
                executionPath.add(currentNode);
            }
        }

        public String getWorkflowId() {
            return workflowId;
        }

        public String getWorkflowName() {
            return workflowName;
        }

        public long getStartTime() {
            return startTime;
        }

        public NodeStatus getStatus() {
            return status;
        }

        public void setStatus(NodeStatus status) {
            this.status = status;
        }

        public String getCurrentNode() {
            return current_node;
        }

        public List<String> getExecutionPath() {
            return new ArrayList<>(executionPath);
        }

        public Map<String, Object> toJson() {
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("workflowId", workflowId);
            json.put("workflowName", workflowName);
            json.put("status", status.name());
            json.put("startTime", startTime);
            json.put("currentNode", current_node);
            json.put("executionPath", executionPath);

            List<Map<String, Object>> nodesJson = new ArrayList<>();
            for (NodeExecutionRecord record : nodeRecords.values()) {
                nodesJson.add(record.toJson());
            }
            json.put("nodes", nodesJson);

            return json;
        }
    }

    // 当前的执行上下文
    private WorkflowExecutionContext currentContext;

    // 事件监听器
    private final WorkflowExecutionListener listener = new WorkflowExecutionListener();

    // 用于实时推送的事件队列
    private final BlockingQueue<WorkflowEvent> eventQueue = new LinkedBlockingQueue<>();

    /**
     * 开始追踪一个新的工作流执行
     */
    public void startWorkflow(String workflowId, String workflowName) {
        currentContext = new WorkflowExecutionContext(workflowId, workflowName);

        WorkflowEvent event = WorkflowEvent.builder()
            .type(WorkflowEvent.EventType.WORKFLOW_START)
            .message("工作流开始执行：" + workflowName)
            .data(Map.of("workflowId", workflowId))
            .build();

        currentContext.addEvent(event);
        listener.fireEvent(event);
        offerEvent(event);
    }

    /**
     * 记录节点开始执行
     */
    public void onNodeStart(String nodeName, Map<String, Object> inputData) {
        if (currentContext == null) return;

        NodeExecutionRecord record = currentContext.getOrCreateNodeRecord(nodeName);
        record.setStatus(NodeStatus.RUNNING);
        record.setInputData(inputData);
        record.setMessage("节点开始执行");

        currentContext.setCurrentNode(nodeName);

        WorkflowEvent event = WorkflowEvent.builder()
            .type(WorkflowEvent.EventType.NODE_START)
            .nodeName(nodeName)
            .message("节点开始执行")
            .data(inputData != null ? inputData : Map.of())
            .build();

        currentContext.addEvent(event);
        listener.fireEvent(event);
        offerEvent(event);
    }

    /**
     * 记录节点执行完成
     */
    public void onNodeComplete(String nodeName, Map<String, Object> outputData) {
        if (currentContext == null) return;

        NodeExecutionRecord record = currentContext.getOrCreateNodeRecord(nodeName);
        record.setStatus(NodeStatus.COMPLETED);
        record.setEndTime(System.currentTimeMillis());
        record.setOutputData(outputData);
        record.setMessage("节点执行完成");

        WorkflowEvent event = WorkflowEvent.builder()
            .type(WorkflowEvent.EventType.NODE_COMPLETE)
            .nodeName(nodeName)
            .message("节点执行完成")
            .data(outputData != null ? outputData : Map.of())
            .build();

        currentContext.addEvent(event);
        listener.fireEvent(event);
        offerEvent(event);
    }

    /**
     * 记录节点执行错误
     */
    public void onNodeError(String nodeName, Throwable error) {
        if (currentContext == null) return;

        NodeExecutionRecord record = currentContext.getOrCreateNodeRecord(nodeName);
        record.setStatus(NodeStatus.FAILED);
        record.setEndTime(System.currentTimeMillis());
        record.setError(error);
        record.setMessage("节点执行失败：" + error.getMessage());

        WorkflowEvent event = WorkflowEvent.builder()
            .type(WorkflowEvent.EventType.NODE_ERROR)
            .nodeName(nodeName)
            .message("节点执行失败：" + error.getMessage())
            .error(error)
            .build();

        currentContext.addEvent(event);
        listener.fireEvent(event);
        offerEvent(event);
    }

    /**
     * 记录边转换
     */
    public void onEdgeTransition(String fromNode, String toNode) {
        if (currentContext == null) return;

        WorkflowEvent event = WorkflowEvent.builder()
            .type(WorkflowEvent.EventType.EDGE_TRANSITION)
            .nodeName(fromNode)
            .message("从节点 " + fromNode + " 转换到节点 " + toNode)
            .data(Map.of("from", fromNode, "to", toNode))
            .build();

        currentContext.addEvent(event);
        listener.fireEvent(event);
        offerEvent(event);
    }

    /**
     * 记录状态更新
     */
    public void onStateUpdate(String nodeName, Map<String, Object> stateUpdates) {
        if (currentContext == null) return;

        WorkflowEvent event = WorkflowEvent.builder()
            .type(WorkflowEvent.EventType.STATE_UPDATE)
            .nodeName(nodeName)
            .message("状态更新")
            .data(stateUpdates)
            .build();

        currentContext.addEvent(event);
        listener.fireEvent(event);
        offerEvent(event);
    }

    /**
     * 工作流执行完成
     */
    public void onWorkflowComplete(Map<String, Object> finalState) {
        if (currentContext == null) return;

        currentContext.setStatus(NodeStatus.COMPLETED);

        WorkflowEvent event = WorkflowEvent.builder()
            .type(WorkflowEvent.EventType.WORKFLOW_COMPLETE)
            .message("工作流执行完成")
            .data(finalState)
            .build();

        currentContext.addEvent(event);
        listener.fireEvent(event);
        offerEvent(event);
    }

    /**
     * 工作流执行失败
     */
    public void onWorkflowError(Throwable error) {
        if (currentContext == null) return;

        currentContext.setStatus(NodeStatus.FAILED);

        WorkflowEvent event = WorkflowEvent.builder()
            .type(WorkflowEvent.EventType.WORKFLOW_ERROR)
            .message("工作流执行失败：" + error.getMessage())
            .error(error)
            .build();

        currentContext.addEvent(event);
        listener.fireEvent(event);
        offerEvent(event);
    }

    /**
     * 添加事件监听器
     */
    public void addEventListener(WorkflowExecutionListener.EventHandler handler) {
        listener.addEventHandler(handler);
    }

    /**
     * 获取当前执行上下文
     */
    public WorkflowExecutionContext getCurrentContext() {
        return currentContext;
    }

    /**
     * 获取事件队列（用于实时推送）
     */
    public BlockingQueue<WorkflowEvent> getEventQueue() {
        return eventQueue;
    }

    /**
     * 获取所有历史事件
     */
    public List<WorkflowEvent> getHistoryEvents() {
        if (currentContext == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(currentContext.events);
    }

    /**
     * 重置追踪器
     */
    public void reset() {
        currentContext = null;
    }

    private void offerEvent(WorkflowEvent event) {
        try {
            eventQueue.offer(event);
        } catch (Exception e) {
            // 队列已满时忽略
        }
    }
}
