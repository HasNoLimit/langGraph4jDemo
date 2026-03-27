package com.workflow.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workflow.engine.entity.WorkflowDefinitionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 工作流定义 Mapper
 */
@Mapper
public interface WorkflowDefinitionMapper extends BaseMapper<WorkflowDefinitionEntity> {

    /**
     * 根据状态查询工作流列表
     */
    @Select("SELECT * FROM workflow_definition WHERE status = #{status} AND deleted = 0")
    List<WorkflowDefinitionEntity> selectByStatus(@Param("status") String status);

    /**
     * 查询活跃的工作流
     */
    @Select("SELECT * FROM workflow_definition WHERE status = 'ACTIVE' AND deleted = 0 ORDER BY created_at DESC")
    List<WorkflowDefinitionEntity> selectActive();
}
