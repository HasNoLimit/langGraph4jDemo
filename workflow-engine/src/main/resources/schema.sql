-- 工作流定义表（PostgreSQL）
CREATE TABLE IF NOT EXISTS workflow_definition (
    id              VARCHAR(64) PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    version         INT DEFAULT 1,

    -- 核心：完整的 JSON 定义（包含 nodes 和 edges）
    definition_json TEXT NOT NULL,

    -- 元数据（方便查询）
    node_count      INT DEFAULT 0,
    entry_node      VARCHAR(64),

    -- 状态管理
    status          VARCHAR(32) DEFAULT 'ACTIVE',

    -- 审计字段
    created_by      VARCHAR(64),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- 逻辑删除
    deleted         INT DEFAULT 0,

    -- 状态约束
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'DEPRECATED'))
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_workflow_status ON workflow_definition(status);
CREATE INDEX IF NOT EXISTS idx_workflow_created ON workflow_definition(created_at);
CREATE INDEX IF NOT EXISTS idx_workflow_name ON workflow_definition(name);

-- 工作流执行实例表（PostgreSQL）
CREATE TABLE IF NOT EXISTS workflow_instance (
    id                  VARCHAR(64) PRIMARY KEY,
    workflow_id         VARCHAR(64) NOT NULL,
    workflow_version    INT DEFAULT 1,

    -- 执行状态
    status              VARCHAR(32) DEFAULT 'PENDING',

    -- 输入输出（快照）
    input_params        TEXT,
    output_result       TEXT,

    -- 执行信息
    current_node        VARCHAR(64),
    error_message       TEXT,

    -- 时间追踪
    started_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at        TIMESTAMP,

    -- 审计字段
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- 逻辑删除
    deleted             INT DEFAULT 0,

    -- 状态约束
    CONSTRAINT chk_instance_status CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED'))
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_instance_status ON workflow_instance(status);
CREATE INDEX IF NOT EXISTS idx_instance_workflow ON workflow_instance(workflow_id, status);
CREATE INDEX IF NOT EXISTS idx_instance_started ON workflow_instance(started_at);
