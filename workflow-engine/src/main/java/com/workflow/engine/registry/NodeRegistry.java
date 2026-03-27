package com.workflow.engine.registry;

import com.workflow.engine.config.NodeConfig;
import com.workflow.engine.executor.NodeExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 节点注册表
 * 维护所有节点执行器的映射关系
 */
@Slf4j
@Component
public class NodeRegistry {

    /** 节点类型 -> 执行器映射 */
    @SuppressWarnings("rawtypes")
    private final Map<String, NodeExecutor> executors = new ConcurrentHashMap<>();

    /** 节点类型 -> 配置类映射 */
    private final Map<String, Class<? extends NodeConfig>> configClasses = new ConcurrentHashMap<>();

    /**
     * 注册节点执行器
     *
     * @param executor 节点执行器
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void register(NodeExecutor executor) {
        String type = executor.getType();
        executors.put(type, executor);
        configClasses.put(type, executor.getConfigClass());
        log.info("Registered node executor: type={}, class={}",
                type, executor.getClass().getSimpleName());
    }

    /**
     * 获取节点执行器
     *
     * @param type 节点类型
     * @return 执行器，不存在返回 null
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <C extends NodeConfig> NodeExecutor<C> getExecutor(String type) {
        return (NodeExecutor<C>) executors.get(type);
    }

    /**
     * 获取配置类
     *
     * @param type 节点类型
     * @return 配置类，不存在返回 null
     */
    public Class<? extends NodeConfig> getConfigClass(String type) {
        return configClasses.get(type);
    }

    /**
     * 检查节点类型是否已注册
     *
     * @param type 节点类型
     * @return 是否已注册
     */
    public boolean hasType(String type) {
        return executors.containsKey(type);
    }

    /**
     * 获取所有已注册的节点类型
     *
     * @return 节点类型集合
     */
    public Collection<String> getAllTypes() {
        return executors.keySet();
    }
}
