package com.langgraph4j.chapter6.monitor;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 工作流执行事件
 * 记录工作流执行过程中的各种事件
 */
public class WorkflowEvent {

    /**
     * 事件类型
     */
    public enum EventType {
        NODE_START,      // 节点开始执行
        NODE_COMPLETE,   // 节点执行完成
        NODE_ERROR,      // 节点执行错误
        STATE_UPDATE,    // 状态更新
        EDGE_TRANSITION, // 边转换（节点间跳转）
        WORKFLOW_START,  // 工作流开始
        WORKFLOW_COMPLETE, // 工作流完成
        WORKFLOW_ERROR     // 工作流错误
    }

    private final EventType type;
    private final String nodeName;
    private final String message;
    private final LocalDateTime timestamp;
    private final Map<String, Object> data;
    private final Throwable error;

    private WorkflowEvent(Builder builder) {
        this.type = builder.type;
        this.nodeName = builder.nodeName;
        this.message = builder.message;
        this.timestamp = builder.timestamp;
        this.data = builder.data;
        this.error = builder.error;
    }

    public EventType getType() {
        return type;
    }

    public String getNodeName() {
        return nodeName;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public Throwable getError() {
        return error;
    }

    public String getFormattedTimestamp() {
        return timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(getFormattedTimestamp()).append("] ");
        sb.append(type).append(" ");
        if (nodeName != null) {
            sb.append("[").append(nodeName).append("] ");
        }
        sb.append(message);
        return sb.toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private EventType type;
        private String nodeName;
        private String message = "";
        private LocalDateTime timestamp = LocalDateTime.now();
        private Map<String, Object> data = Map.of();
        private Throwable error;

        public Builder type(EventType type) {
            this.type = type;
            return this;
        }

        public Builder nodeName(String nodeName) {
            this.nodeName = nodeName;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder data(Map<String, Object> data) {
            this.data = data;
            return this;
        }

        public Builder error(Throwable error) {
            this.error = error;
            return this;
        }

        public WorkflowEvent build() {
            return new WorkflowEvent(this);
        }
    }
}
