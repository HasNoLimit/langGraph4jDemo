package com.langgraph4j.chapter2;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 第 2 章示例测试
 */
public class Chapter2Test {

    @Test
    public void testLinearWorkflow() throws Exception {
        Exercise1_LinearWorkflow.main(new String[]{});
    }

    @Test
    public void testConditionalBranch() throws Exception {
        Exercise2_ConditionalBranch.main(new String[]{});
    }

    @Test
    public void testMessageAccumulator() throws Exception {
        Exercise3_MessageAccumulator.main(new String[]{});
    }
}
