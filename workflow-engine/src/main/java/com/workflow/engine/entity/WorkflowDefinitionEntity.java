package com.workflow.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流定义实体
 */
@Data
@TableName("workflow_definition")
public class WorkflowDefinitionEntity {

    @TableId(type = IdType.INPUT)
    private String id;

    /** 工作流名称 */
    private String name;

    /** 工作流描述 */
    private String description;

    /** 版本号 */
    private Integer version;

    /** 定义 JSON（包含 nodes 和 edges） */
    private String definitionJson;

    /** 节点数量 */
    private Integer nodeCount;

    /** 入口节点 ID */
    private String entryNode;

    /** 状态：ACTIVE, INACTIVE, DEPRECATED */
    private String status;

    /** 创建人 */
    @TableField(fill = FieldFill.INSERT)
    private String createdBy;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新人 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
