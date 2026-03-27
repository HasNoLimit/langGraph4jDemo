package com.workflow.engine.builtin;

import com.workflow.engine.context.NodeContext;
import com.workflow.engine.executor.NodeExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 开始节点执行器
 * 职责：接收输入参数，初始化全局状态
 */
@Slf4j
@Component
public class StartNodeExecutor implements NodeExecutor<StartConfig> {

    @Override
    public String getType() {
        return "start";
    }

    @Override
    public Class<StartConfig> getConfigClass() {
        return StartConfig.class;
    }

    @Override
    public Map<String, Object> execute(NodeContext context, StartConfig config) {
        log.info("[{}] Start node executing", config.getNodeId());

        // 开始节点直接将输入透传到输出
        // 实际项目中可进行参数校验
        Map<String, Object> result = new HashMap<>();

        // 传递所有运行时参数
        if (context.getRuntimeParams() != null) {
            result.putAll(context.getRuntimeParams());
        }

        // 添加开始标记
        result.put("__node_start_executed", true);

        return result;
    }

    @Override
    public void validate(StartConfig config) {
        // 开始节点配置可为空
        // 如有需要可以校验 inputs 定义
    }
}
