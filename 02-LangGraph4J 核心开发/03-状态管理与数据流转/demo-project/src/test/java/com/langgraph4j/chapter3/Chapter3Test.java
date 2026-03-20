package com.langgraph4j.chapter3;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 第 3 章示例测试
 */
public class Chapter3Test {

    @Test
    public void testStatePropagationDemo() throws Exception {
        StatePropagationDemo.main(new String[]{});
    }

    @Test
    public void testOrderProcessingWorkflow() throws Exception {
        OrderProcessingWorkflow.main(new String[]{});
    }
}
