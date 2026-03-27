package com.workflow.engine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.engine.WorkflowEngine;
import com.workflow.engine.WorkflowInstance;
import com.workflow.engine.definition.WorkflowDefinition;
import com.workflow.engine.entity.WorkflowInstanceEntity;
import com.workflow.engine.mapper.WorkflowInstanceMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.state.AgentState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * 工作流执行服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowExecutionService {

    private final WorkflowEngine workflowEngine;
    private final WorkflowDefinitionService definitionService;
    private final WorkflowInstanceMapper instanceMapper;
    private final ObjectMapper objectMapper;

    /**
     * 启动工作流
     *
     * @param workflowId 工作流 ID
     * @param inputs     输入参数
     * @return 实例实体
     */
    @Transactional
    public WorkflowInstanceEntity start(String workflowId, Map<String, Object> inputs) {
        log.info("Starting workflow: id={}, inputs={}", workflowId, inputs);

        // 1. 获取工作流定义
        WorkflowDefinition definition = definitionService.getDefinition(workflowId);

        // 2. 创建实例记录
        WorkflowInstanceEntity entity = new WorkflowInstanceEntity();
        entity.setWorkflowId(workflowId);
        entity.setWorkflowVersion(1); // TODO: 支持版本选择
        entity.setStatus("RUNNING");
        entity.setInputParams(toJson(inputs));
        entity.setStartedAt(LocalDateTime.now());
        instanceMapper.insert(entity);

        try {
            // 3. 创建工作流实例并执行
            WorkflowInstance instance = workflowEngine.createInstance(definition);

            // 4. 执行工作流
            Optional<AgentState> result = instance.execute(inputs);

            // 5. 更新结果
            if (result.isPresent()) {
                AgentState state = result.get();
                entity.setOutputResult(toJson(state.data()));
                entity.setStatus("COMPLETED");
            } else {
                entity.setStatus("FAILED");
                entity.setErrorMessage("No result returned");
            }

        } catch (Exception e) {
            log.error("Workflow execution failed: id={}, error={}", workflowId, e.getMessage(), e);
            entity.setStatus("FAILED");
            entity.setErrorMessage(e.getMessage());
        }

        entity.setCompletedAt(LocalDateTime.now());
        instanceMapper.updateById(entity);

        return entity;
    }

    /**
     * 启动工作流（使用 JSON 定义）
     *
     * @param jsonDefinition 工作流定义 JSON
     * @param inputs         输入参数
     * @return 实例实体
     */
    @SneakyThrows
    @Transactional
    public WorkflowInstanceEntity startWithDefinition(String jsonDefinition, Map<String, Object> inputs) {
        log.info("Starting workflow with definition, inputs={}", inputs);

        // 1. 解析定义
        WorkflowDefinition definition = objectMapper.readValue(jsonDefinition, WorkflowDefinition.class);

        // 2. 创建实例记录
        WorkflowInstanceEntity entity = new WorkflowInstanceEntity();
        entity.setWorkflowId(definition.getId());
        entity.setWorkflowVersion(1);
        entity.setStatus("RUNNING");
        entity.setInputParams(toJson(inputs));
        entity.setStartedAt(LocalDateTime.now());
        instanceMapper.insert(entity);

        try {
            // 3. 创建工作流实例并执行
            WorkflowInstance instance = workflowEngine.createInstance(definition);

            // 4. 执行工作流
            Optional<AgentState> result = instance.execute(inputs);

            // 5. 更新结果
            if (result.isPresent()) {
                AgentState state = result.get();
                entity.setOutputResult(toJson(state.data()));
                entity.setStatus("COMPLETED");
            } else {
                entity.setStatus("FAILED");
                entity.setErrorMessage("No result returned");
            }

        } catch (Exception e) {
            log.error("Workflow execution failed: error={}", e.getMessage(), e);
            entity.setStatus("FAILED");
            entity.setErrorMessage(e.getMessage());
        }

        entity.setCompletedAt(LocalDateTime.now());
        instanceMapper.updateById(entity);

        return entity;
    }

    /**
     * 查询执行状态
     *
     * @param instanceId 实例 ID
     * @return 实例实体
     */
    public WorkflowInstanceEntity getStatus(String instanceId) {
        return instanceMapper.selectById(instanceId);
    }

    /**
     * 获取执行结果
     *
     * @param instanceId 实例 ID
     * @return 结果 JSON
     */
    public String getResult(String instanceId) {
        WorkflowInstanceEntity entity = instanceMapper.selectById(instanceId);
        if (entity == null) {
            throw new IllegalArgumentException("Instance not found: " + instanceId);
        }
        return entity.getOutputResult();
    }

    @SneakyThrows
    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        return objectMapper.writeValueAsString(obj);
    }
}
