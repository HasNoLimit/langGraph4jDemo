package com.workflow.engine.definition;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 工作流定义
 * 对应 JSON 配置的完整结构
 */
@Data
public class WorkflowDefinition {

    /** 工作流唯一标识 */
    private String id;

    /** 工作流名称 */
    private String name;

    /** 工作流描述 */
    private String description;

    /** 节点列表 */
    private List<NodeDefinition> nodes;

    /** 边列表（连接关系） */
    private List<EdgeDefinition> edges;

    /**
     * 节点定义
     */
    @Data
    public static class NodeDefinition {
        /** 节点唯一ID */
        private String id;

        /** 节点类型，如 start, llm, end */
        private String type;

        /** 节点在画布中的位置 */
        private Position position;

        /** 节点配置（具体类型取决于 type） */
        private Map<String, Object> config;
    }

    /**
     * 位置定义
     */
    @Data
    public static class Position {
        private Double x;
        private Double y;
    }

    /**
     * 边定义（节点连接关系）
     */
    @Data
    public static class EdgeDefinition {
        /** 源节点ID */
        private String source;

        /** 目标节点ID */
        private String target;

        /** 条件表达式（可选，用于条件分支） */
        private String condition;
    }
}
