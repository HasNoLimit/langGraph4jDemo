package com.workflow.engine.builtin;

import com.workflow.engine.config.NodeConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 结束节点配置
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EndConfig extends NodeConfig {

    /**
     * 输出模板
     * 定义最终输出的结构，key 是输出字段名，value 是变量路径
     * 如：{"answer": "{{aiResponse}}", "originalQuery": "{{query}}"}
     */
    private Map<String, String> outputTemplate;
}
