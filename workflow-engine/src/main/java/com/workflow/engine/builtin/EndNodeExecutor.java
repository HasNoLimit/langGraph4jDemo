package com.workflow.engine.builtin;

import com.workflow.engine.context.NodeContext;
import com.workflow.engine.executor.NodeExecutor;
import com.workflow.engine.variable.VariableContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 结束节点执行器
 */
@Slf4j
@Component
public class EndNodeExecutor implements NodeExecutor<EndConfig> {

    @Override
    public String getType() {
        return "end";
    }

    @Override
    public Class<EndConfig> getConfigClass() {
        return EndConfig.class;
    }

    @Override
    public Map<String, Object> execute(NodeContext context, EndConfig config) {
        log.info("[{}] End node executing", config.getNodeId());

        Map<String, Object> result = new HashMap<>();

        if (config.getOutputTemplate() != null) {
            // 根据模板从全局状态提取值
            config.getOutputTemplate().forEach((key, varPath) -> {
                Object value = context.getVariableResolver().getValue(varPath,
                        VariableContext.builder()
                                .globalState(context.getGlobalState())
                                .build());
                result.put(key, value);
            });
        } else {
            // 如果没有配置输出模板，返回整个全局状态
            if (context.getGlobalState() != null) {
                result.putAll(context.getGlobalState().data());
            }
        }

        // 添加结束标记
        result.put("__status", "completed");
        result.put("__node_end_executed", true);

        return result;
    }

    @Override
    public void validate(EndConfig config) {
        // 结束节点配置可为空
    }
}
