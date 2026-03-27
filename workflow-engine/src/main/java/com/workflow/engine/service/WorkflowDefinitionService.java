package com.workflow.engine.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.engine.definition.WorkflowDefinition;
import com.workflow.engine.entity.WorkflowDefinitionEntity;
import com.workflow.engine.mapper.WorkflowDefinitionMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工作流定义服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowDefinitionService extends ServiceImpl<WorkflowDefinitionMapper, WorkflowDefinitionEntity> {

    private final ObjectMapper objectMapper;

    /**
     * 获取 ObjectMapper
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    /**
     * 保存工作流定义
     *
     * @param definition 工作流定义
     * @return 保存后的实体
     */
    @Transactional
    @SneakyThrows
    public WorkflowDefinitionEntity save(WorkflowDefinition definition) {
        // 校验定义
        validate(definition);

        // 检查是否已存在
        WorkflowDefinitionEntity existing = getById(definition.getId());

        WorkflowDefinitionEntity entity = new WorkflowDefinitionEntity();
        entity.setId(definition.getId());
        entity.setName(definition.getName());
        entity.setDescription(definition.getDescription());
        entity.setDefinitionJson(objectMapper.writeValueAsString(definition));
        entity.setNodeCount(definition.getNodes() != null ? definition.getNodes().size() : 0);
        entity.setStatus("ACTIVE");

        if (existing != null) {
            // 更新版本
            entity.setVersion(existing.getVersion() + 1);
            entity.setUpdatedAt(LocalDateTime.now());
            updateById(entity);
            log.info("Updated workflow definition: id={}, version={}", entity.getId(), entity.getVersion());
        } else {
            entity.setVersion(1);
            save(entity);
            log.info("Created workflow definition: id={}", entity.getId());
        }

        return entity;
    }

    /**
     * 获取工作流定义
     *
     * @param id 工作流 ID
     * @return 定义 JSON
     */
    public String getDefinitionJson(String id) {
        WorkflowDefinitionEntity entity = getById(id);
        if (entity == null) {
            throw new IllegalArgumentException("Workflow definition not found: " + id);
        }
        return entity.getDefinitionJson();
    }

    /**
     * 获取工作流定义对象
     *
     * @param id 工作流 ID
     * @return 定义对象
     */
    @SneakyThrows
    public WorkflowDefinition getDefinition(String id) {
        String json = getDefinitionJson(id);
        return objectMapper.readValue(json, WorkflowDefinition.class);
    }

    /**
     * 列出活跃的工作流
     *
     * @return 活跃工作流列表
     */
    public List<WorkflowDefinitionEntity> listActive() {
        return baseMapper.selectActive();
    }

    /**
     * 校验定义合法性
     *
     * @param definition 工作流定义
     */
    public void validate(WorkflowDefinition definition) {
        if (!StringUtils.hasText(definition.getId())) {
            throw new IllegalArgumentException("Workflow id is required");
        }
        if (!StringUtils.hasText(definition.getName())) {
            throw new IllegalArgumentException("Workflow name is required");
        }
        if (definition.getNodes() == null || definition.getNodes().isEmpty()) {
            throw new IllegalArgumentException("Workflow must have at least one node");
        }
        if (definition.getEdges() == null || definition.getEdges().isEmpty()) {
            throw new IllegalArgumentException("Workflow must have at least one edge");
        }

        // 检查节点引用是否有效
        for (WorkflowDefinition.EdgeDefinition edge : definition.getEdges()) {
            String source = edge.getSource();
            String target = edge.getTarget();

            // 特殊节点不检查
            if (!"__START__".equals(source) && !"__END__".equals(target)) {
                boolean sourceExists = definition.getNodes().stream()
                        .anyMatch(n -> n.getId().equals(source));
                boolean targetExists = definition.getNodes().stream()
                        .anyMatch(n -> n.getId().equals(target));

                if (!sourceExists) {
                    throw new IllegalArgumentException("Edge references non-existent source node: " + source);
                }
                if (!targetExists) {
                    throw new IllegalArgumentException("Edge references non-existent target node: " + target);
                }
            }
        }
    }

    /**
     * 停用工作流
     *
     * @param id 工作流 ID
     */
    @Transactional
    public void deactivate(String id) {
        WorkflowDefinitionEntity entity = new WorkflowDefinitionEntity();
        entity.setId(id);
        entity.setStatus("INACTIVE");
        entity.setUpdatedAt(LocalDateTime.now());
        updateById(entity);
        log.info("Deactivated workflow: id={}", id);
    }

    /**
     * 删除工作流（逻辑删除）
     *
     * @param id 工作流 ID
     */
    @Transactional
    public void delete(String id) {
        removeById(id);
        log.info("Deleted workflow: id={}", id);
    }
}
