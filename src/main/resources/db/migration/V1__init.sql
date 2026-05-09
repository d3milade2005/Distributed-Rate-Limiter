CREATE TABLE plans (
    plan_name     VARCHAR(50) PRIMARY KEY,
    capacity      INT NOT NULL,
    refill_rate   DOUBLE PRECISION NOT NULL,
    window_ms     BIGINT NOT NULL
);

CREATE TABLE tenant_plans (
    tenant_id      VARCHAR(100) PRIMARY KEY,
    plan_name      VARCHAR(50) NOT NULL REFERENCES plans(plan_name),
    fail_behavior  VARCHAR(20) NOT NULL DEFAULT 'fail_open'
);

CREATE TABLE audit_log (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   VARCHAR(100) NOT NULL,
    user_id     VARCHAR(100) NOT NULL,
    action      VARCHAR(100) NOT NULL,
    result      VARCHAR(10)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

INSERT INTO plans (plan_name, capacity, refill_rate, window_ms) VALUES
    ('free', 5,0.1, 10000),
    ('pro', 50,1.0, 10000),
    ('pro_plus', 200,5.0, 10000);

INSERT INTO tenant_plans (tenant_id, plan_name, fail_behavior) VALUES
    ('tenant-abc', 'free', 'fail_open'),
    ('tenant-pro', 'pro',  'fail_open');