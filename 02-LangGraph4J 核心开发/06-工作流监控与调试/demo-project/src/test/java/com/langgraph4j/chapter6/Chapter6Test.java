package com.langgraph4j.chapter6;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 第 6 章示例测试 - 工作流监控与调试
 */
public class Chapter6Test {

    @Test
    public void testWorkflowWithMonitoring() throws Exception {
        WorkflowWithMonitoring.main(new String[]{});
    }

    @Test
    public void testFrontendStateReceiver() throws Exception {
        FrontendStateReceiver.main(new String[]{});
    }
}
