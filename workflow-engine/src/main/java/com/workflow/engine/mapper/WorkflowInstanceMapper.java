package com.workflow.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workflow.engine.entity.WorkflowInstanceEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 工作流实例 Mapper
 */
@Mapper
public interface WorkflowInstanceMapper extends BaseMapper<WorkflowInstanceEntity> {

    /**
     * 根据工作流 ID 和状态查询实例
     */
    @Select("SELECT * FROM workflow_instance WHERE workflow_id = #{workflowId} AND status = #{status} AND deleted = 0")
    List<WorkflowInstanceEntity> selectByWorkflowAndStatus(
            @Param("workflowId") String workflowId,
            @Param("status") String status);

    /**
     * 更新实例状态
     */
    @Update("UPDATE workflow_instance SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") String id, @Param("status") String status);

    /**
     * 更新当前节点
     */
    @Update("UPDATE workflow_instance SET current_node = #{currentNode}, updated_at = NOW() WHERE id = #{id}")
    int updateCurrentNode(@Param("id") String id, @Param("currentNode") String currentNode);
}
