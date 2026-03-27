package com.workflow.engine.builtin;

import com.workflow.engine.config.NodeConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 开始节点配置
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StartConfig extends NodeConfig {

    /**
     * 输入参数定义
     * 如：[{"name": "query", "type": "string", "required": true}]
     */
    private List<InputParam> inputs;

    /**
     * 输入参数定义
     */
    @Data
    public static class InputParam {
        private String name;
        private String type = "string";
        private boolean required = true;
        private String description;
    }
}
