package com.workflow.engine.builtin;

import com.workflow.engine.config.NodeConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * LLM 节点配置
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LLMConfig extends NodeConfig {

    /** 模型名称，如 gpt-4, gpt-3.5-turbo */
    private String model = "gpt-3.5-turbo";

    /** 温度参数 */
    private Double temperature = 0.7;

    /** 最大 Token 数 */
    private Integer maxTokens = 2000;

    /**
     * Prompt 模板
     * 支持变量：如 "用户问题：{{userInput}}"
     */
    private String promptTemplate;

    /**
     * 输出映射
     * 如：{"output": "llmResponse"} 表示将 output 映射为 llmResponse
     */
    private Map<String, String> outputMapping;
}
