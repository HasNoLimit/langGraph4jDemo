package com.workflow.engine.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.workflow.engine.entity.WorkflowDefinitionEntity;
import com.workflow.engine.entity.WorkflowInstanceEntity;
import com.workflow.engine.service.WorkflowDefinitionService;
import com.workflow.engine.service.WorkflowExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流 REST API 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowDefinitionService definitionService;
    private final WorkflowExecutionService executionService;

    /**
     * 创建工作流
     */
    @PostMapping
    public ResponseEntity<WorkflowDefinitionEntity> createWorkflow(
            @RequestBody JsonNode definition) {
        log.info("Creating workflow");
        WorkflowDefinitionEntity entity = definitionService.save(
                definitionService.getObjectMapper().convertValue(definition, com.workflow.engine.definition.WorkflowDefinition.class));
        return ResponseEntity.ok(entity);
    }

    /**
     * 获取工作流定义
     */
    @GetMapping("/{id}")
    public ResponseEntity<String> getWorkflow(@PathVariable String id) {
        log.info("Getting workflow: id={}", id);
        String json = definitionService.getDefinitionJson(id);
        return ResponseEntity.ok(json);
    }

    /**
     * 列出活跃的工作流
     */
    @GetMapping
    public ResponseEntity<List<WorkflowDefinitionEntity>> listWorkflows() {
        log.info("Listing active workflows");
        List<WorkflowDefinitionEntity> list = definitionService.listActive();
        return ResponseEntity.ok(list);
    }

    /**
     * 执行工作流（使用已保存的定义）
     */
    @PostMapping("/{id}/execute")
    public ResponseEntity<WorkflowInstanceEntity> executeWorkflow(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, Object> inputs) {
        log.info("Executing workflow: id={}", id);
        if (inputs == null) {
            inputs = new HashMap<>();
        }
        WorkflowInstanceEntity instance = executionService.start(id, inputs);
        return ResponseEntity.ok(instance);
    }

    /**
     * 执行工作流（使用传入的定义）
     */
    @PostMapping("/execute")
    public ResponseEntity<WorkflowInstanceEntity> executeWorkflowWithDefinition(
            @RequestBody JsonNode request) {
        log.info("Executing workflow with inline definition");

        JsonNode definitionNode = request.get("definition");
        JsonNode inputsNode = request.get("inputs");

        if (definitionNode == null) {
            return ResponseEntity.badRequest().build();
        }

        Map<String, Object> inputs = new HashMap<>();
        if (inputsNode != null) {
            inputs = definitionService.getObjectMapper().convertValue(inputsNode, Map.class);
        }

        String jsonDefinition = definitionNode.toString();
        WorkflowInstanceEntity instance = executionService.startWithDefinition(jsonDefinition, inputs);
        return ResponseEntity.ok(instance);
    }

    /**
     * 查询实例状态
     */
    @GetMapping("/instances/{instanceId}")
    public ResponseEntity<WorkflowInstanceEntity> getInstance(@PathVariable String instanceId) {
        log.info("Getting instance: id={}", instanceId);
        WorkflowInstanceEntity entity = executionService.getStatus(instanceId);
        if (entity == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(entity);
    }

    /**
     * 停用工作流
     */
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateWorkflow(@PathVariable String id) {
        log.info("Deactivating workflow: id={}", id);
        definitionService.deactivate(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 删除工作流
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkflow(@PathVariable String id) {
        log.info("Deleting workflow: id={}", id);
        definitionService.delete(id);
        return ResponseEntity.ok().build();
    }
}
