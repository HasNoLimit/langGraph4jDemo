package com.langgraph4j.chapter4;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 第 4 章示例测试
 */
public class Chapter4Test {

    @Test
    public void testWeatherToolExample() throws Exception {
        WeatherToolExample.main(new String[]{});
    }

    @Test
    public void testSmartAssistantWorkflow() throws Exception {
        SmartAssistantWorkflow.main(new String[]{});
    }

    @Test
    public void testToolCallWithErrorHandling() throws Exception {
        ToolCallWithErrorHandling.main(new String[]{});
    }
}
