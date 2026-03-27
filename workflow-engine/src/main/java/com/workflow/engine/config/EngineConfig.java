package com.workflow.engine.config;

import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 引擎配置
 * 配置 LangGraph4j 所需的状态通道
 */
@Configuration
public class EngineConfig {

    /**
     * 创建默认状态通道配置
     * 每个通道定义了状态的更新策略
     */
    @Bean
    public Map<String, Channel<?>> defaultChannels() {
        Map<String, Channel<?>> channels = new HashMap<>();

        // 消息通道 - 使用 base（覆盖模式）
        channels.put("messages", Channels.base(ArrayList::new));

        // 其他通道使用默认的 LastValue（覆盖模式）
        // 不需要特别配置

        return channels;
    }
}
