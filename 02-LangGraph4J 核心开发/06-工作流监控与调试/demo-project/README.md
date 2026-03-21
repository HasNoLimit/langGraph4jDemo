# Chapter 6 Demo Project - 工作流监控与调试

## 示例项目说明

本项目包含第 6 章 工作流监控与调试的所有示例代码，演示如何追踪工作流执行状态并实时推送给前台。

## 核心概念

### 工作流执行状态追踪架构

```
┌─────────────────────────────────────────────────────────────────┐
│                    工作流监控架构                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐                                               │
│  │  Workflow   │                                               │
│  │   Graph     │                                               │
│  │             │                                               │
│  │  ┌───────┐  │    ┌─────────────────┐    ┌───────────────┐  │
│  │  │ Node1 │──┼───▶│ StateTracker    │───▶│ EventQueue    │  │
│  │  └───────┘  │    │ (状态追踪器)     │    │ (事件队列)     │  │
│  │             │    │                 │    └───────┬───────┘  │
│  │  ┌───────┐  │    │ - onNodeStart   │            │         │
│  │  │ Node2 │──┼───▶│ - onNodeComplete│            ▼         │
│  │  └───────┘  │    │ - onNodeError   │    ┌───────────────┐  │
│  │             │    │ - onStateUpdate │    │  Frontend     │  │
│  └─────────────┘    └─────────────────┘    │  (前端 UI)     │  │
│         ▲                                  └───────────────┘  │
│         │                                                    │
│         └─────────── Listener Callback ──────────────────►   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 实现方式

1. **WorkflowEvent** - 工作流事件类
   - 定义各种事件类型（节点开始/完成/错误、状态更新等）
   - 包含时间戳、节点名、事件数据等信息

2. **WorkflowExecutionListener** - 事件监听器
   - 注册和管理事件处理器
   - 当事件发生时通知所有处理器

3. **WorkflowStateTracker** - 状态追踪器
   - 记录工作流执行的全过程
   - 提供事件队列用于实时推送
   - 维护执行上下文用于获取完整状态

## 运行示例

### 示例 1：带监控的工作流

```bash
mvn exec:java -Dexec.mainClass="com.langgraph4j.chapter6.WorkflowWithMonitoring"
```

**预期输出：**
```
=== 工作流执行状态监控示例 ===

📊 [HH:mm:ss.SSS] WORKFLOW_START 工作流开始执行：状态监控示例工作流
[EventPublisher] 启动，等待事件...
📊 [HH:mm:ss.SSS] NODE_START [step1] 节点开始执行
[step1] 处理：hello
📊 [HH:mm:ss.SSS] NODE_COMPLETE [step1] 节点执行完成
📊 [HH:mm:ss.SSS] STATE_UPDATE [step1] 状态更新
📡 [推送前台] NODE_START - step1
📊 [HH:mm:ss.SSS] NODE_START [step2] 节点开始执行
[step2] 处理：Step1: HELLO
📊 [HH:mm:ss.SSS] NODE_COMPLETE [step2] 节点执行完成
📊 [HH:mm:ss.SSS] STATE_UPDATE [step2] 状态更新
📡 [推送前台] NODE_COMPLETE - step1
...
=== 最终状态 ===
finalOutput: 最终输出：Step2: Step1: HELLO [Processed]

=== 执行统计 ===
工作流 ID: xxx-xxx-xxx
工作流名称：状态监控示例工作流
执行状态：COMPLETED
执行路径：step1 → step2 → step3
事件总数：12

节点执行情况:
  - step1: COMPLETED (耗时：201ms)
  - step2: COMPLETED (耗时：202ms)
  - step3: COMPLETED (耗时：203ms)
```

### 示例 2：前端状态接收器

```bash
mvn exec:java -Dexec.mainClass="com.langgraph4j.chapter6.FrontendStateReceiver"
```

**预期输出：**
```
=== 前端状态接收器示例 ===

[前端] 订阅事件流...
[前端 UI] ▶️  工作流开始：demo-workflow-001
[前端 UI] ▶️  节点开始：validate
[前端 UI] ✅ 节点完成：validate
[前端 UI] 📝 状态更新：validate - {isValid=true}
[前端 UI] 📊 EDGE_TRANSITION: 从节点 validate 转换到节点 process
[前端 UI] ▶️  节点开始：process
[前端 UI] ✅ 节点完成：process
...
✅ 工作流完成！最终状态：{finalOutput=最终输出：processed: test data, status=SUCCESS}

===== 执行上下文 JSON（可用于前端渲染） =====
{
  "workflowId": "demo-workflow-001"
  "workflowName": "前端监控演示"
  "status": "COMPLETED"
  "startTime": 1234567890
  "currentNode": "output"
  "executionPath": ["validate", "process", "output"]
  "nodes": [
    {...},
    {...}
  ]
}
```

## 如何集成到实际项目

### 1. 在 Spring Boot 项目中使用

```java
@Service
public class WorkflowService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate; // WebSocket

    public void executeWorkflow(Map<String, Object> input, String sessionId) {
        WorkflowStateTracker tracker = new WorkflowStateTracker();

        // 添加监听器，通过 WebSocket 推送给前端
        tracker.addEventListener(event -> {
            messagingTemplate.convertAndSendToUser(
                sessionId,
                "/queue/workflow-events",
                event
            );
        });

        // 执行工作流...
    }
}
```

### 2. 前端接收事件（JavaScript）

```javascript
// 使用 WebSocket 接收事件
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, () => {
    stompClient.subscribe('/user/queue/workflow-events', (event) => {
        const data = JSON.parse(event.body);
        updateWorkflowUI(data);
    });
});

function updateWorkflowUI(event) {
    switch(event.type) {
        case 'NODE_START':
            highlightNode(event.nodeName, 'running');
            break;
        case 'NODE_COMPLETE':
            highlightNode(event.nodeName, 'completed');
            break;
        case 'NODE_ERROR':
            highlightNode(event.nodeName, 'failed');
            break;
    }
}
```

### 3. HTTP 轮询方式

```java
@RestController
@RequestMapping("/api/workflow")
public class WorkflowController {

    private final Map<String, WorkflowStateTracker> trackers = new ConcurrentHashMap<>();

    @PostMapping("/start")
    public String startWorkflow(@RequestBody Map<String, Object> input) {
        String workflowId = UUID.randomUUID().toString();
        WorkflowStateTracker tracker = new WorkflowStateTracker();
        trackers.put(workflowId, tracker);
        // 执行工作流...
        return workflowId;
    }

    @GetMapping("/{id}/events")
    public List<WorkflowEvent> getEvents(@PathVariable String id) {
        WorkflowStateTracker tracker = trackers.get(id);
        if (tracker == null) return Collections.emptyList();
        return tracker.getHistoryEvents();
    }

    @GetMapping("/{id}/status")
    public WorkflowStateTracker.WorkflowExecutionContext getStatus(@PathVariable String id) {
        WorkflowStateTracker tracker = trackers.get(id);
        return tracker != null ? tracker.getCurrentContext() : null;
    }
}
```

## 编译项目

```bash
mvn clean compile
```

## 运行测试

```bash
mvn test
```

## 关键 API 说明

| 方法 | 说明 | 使用时机 |
|------|------|---------|
| `tracker.startWorkflow(id, name)` | 开始追踪工作流 | 工作流执行前 |
| `tracker.onNodeStart(name, inputData)` | 记录节点开始 | 节点执行前 |
| `tracker.onNodeComplete(name, outputData)` | 记录节点完成 | 节点执行成功 |
| `tracker.onNodeError(name, error)` | 记录节点错误 | 节点执行失败 |
| `tracker.onStateUpdate(name, updates)` | 记录状态更新 | 状态变更后 |
| `tracker.onWorkflowComplete(state)` | 工作流完成 | 工作流结束 |
| `tracker.onWorkflowError(error)` | 工作流错误 | 工作流异常 |
| `tracker.addEventListener(handler)` | 添加事件监听器 | 订阅实时事件 |
| `tracker.getEventQueue()` | 获取事件队列 | 用于轮询或推送 |
| `tracker.getCurrentContext()` | 获取执行上下文 | 获取完整状态 |
| `tracker.getHistoryEvents()` | 获取历史事件 | 回放或日志 |
