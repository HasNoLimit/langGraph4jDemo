package com.workflow.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流实例实体
 */
@Data
@TableName("workflow_instance")
public class WorkflowInstanceEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 工作流定义 ID */
    private String workflowId;

    /** 工作流版本 */
    private Integer workflowVersion;

    /** 状态：PENDING, RUNNING, COMPLETED, FAILED, CANCELLED */
    private String status;

    /** 输入参数 JSON */
    private String inputParams;

    /** 输出结果 JSON */
    private String outputResult;

    /** 当前节点 ID */
    private String currentNode;

    /** 错误信息 */
    private String errorMessage;

    /** 开始时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime startedAt;

    /** 完成时间 */
    private LocalDateTime completedAt;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
