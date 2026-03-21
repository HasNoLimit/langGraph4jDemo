package com.langgraph4j.chapter6.monitor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 工作流执行监听器
 * 用于监听和接收工作流执行过程中的各种事件
 */
public class WorkflowExecutionListener {

    /**
     * 事件处理器接口
     */
    @FunctionalInterface
    public interface EventHandler {
        void onEvent(WorkflowEvent event);
    }

    // 使用线程安全的列表存储监听器
    private final List<EventHandler> eventHandlers = new CopyOnWriteArrayList<>();

    /**
     * 添加事件监听器
     */
    public void addEventHandler(EventHandler handler) {
        if (handler != null) {
            eventHandlers.add(handler);
        }
    }

    /**
     * 移除事件监听器
     */
    public void removeEventHandler(EventHandler handler) {
        eventHandlers.remove(handler);
    }

    /**
     * 触发事件，通知所有监听器
     */
    public void fireEvent(WorkflowEvent event) {
        for (EventHandler handler : eventHandlers) {
            try {
                handler.onEvent(event);
            } catch (Exception e) {
                System.err.println("事件处理器执行失败：" + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * 清空所有监听器
     */
    public void clear() {
        eventHandlers.clear();
    }

    /**
     * 获取监听器数量
     */
    public int getHandlerCount() {
        return eventHandlers.size();
    }
}
