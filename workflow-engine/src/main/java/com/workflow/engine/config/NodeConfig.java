package com.workflow.engine.config;

import lombok.Data;

/**
 * 所有节点配置的基类
 * 后续可扩展：重试策略、错误处理、输出映射等
 */
@Data
public abstract class NodeConfig {

    /** 节点ID */
    private String nodeId;

    /** 节点名称（用于展示） */
    private String nodeName;
}
