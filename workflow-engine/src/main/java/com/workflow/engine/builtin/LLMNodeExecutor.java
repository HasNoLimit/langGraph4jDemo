package com.workflow.engine.builtin;

import com.workflow.engine.context.NodeContext;
import com.workflow.engine.executor.NodeExecutor;
import com.workflow.engine.variable.VariableContext;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * LLM 节点执行器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LLMNodeExecutor implements NodeExecutor<LLMConfig> {

    private final ChatLanguageModel chatLanguageModel;

    @Override
    public String getType() {
        return "llm";
    }

    @Override
    public Class<LLMConfig> getConfigClass() {
        return LLMConfig.class;
    }

    @Override
    public void validate(LLMConfig config) {
        if (!StringUtils.hasText(config.getPromptTemplate())) {
            throw new IllegalArgumentException("LLM 节点的 promptTemplate 不能为空");
        }
    }

    @Override
    public Map<String, Object> execute(NodeContext context, LLMConfig config) {
        log.info("[{}] LLM node executing with model: {}", config.getNodeId(), config.getModel());

        // 1. 渲染 Prompt 模板
        String prompt = context.getVariableResolver().resolve(
                config.getPromptTemplate(),
                VariableContext.builder()
                        .globalState(context.getGlobalState())
                        .runtimeParams(context.getRuntimeParams())
                        .build()
        );

        log.debug("Rendered prompt: {}", prompt);

        // 2. 调用 LLM
        String response;
        try {
            response = chatLanguageModel.generate(prompt);
            log.debug("LLM response: {}", response);
        } catch (Exception e) {
            log.error("LLM call failed: {}", e.getMessage(), e);
            throw new RuntimeException("LLM 调用失败: " + e.getMessage(), e);
        }

        // 3. 构建输出
        Map<String, Object> output = new HashMap<>();
        output.put("output", response);

        // 4. 应用输出映射
        if (config.getOutputMapping() != null) {
            config.getOutputMapping().forEach((source, target) -> {
                Object value = "output".equals(source) ? response : null;
                output.put(target, value);
            });
        }

        return output;
    }
}
