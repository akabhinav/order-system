-- ============================================================================
-- V1__create_orders.sql
-- PostgreSQL migration for Order Management System
-- ============================================================================

-- ============================================================================
-- Orders table
-- ============================================================================
CREATE TABLE orders (
    id                   UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id          UUID            NOT NULL,
    status               VARCHAR(25)     NOT NULL DEFAULT 'PENDING',
    currency_code        CHAR(3)         NOT NULL,
    subtotal_amount      NUMERIC(12, 2)  NOT NULL,
    tax_amount           NUMERIC(12, 2)  NOT NULL,
    shipping_amount      NUMERIC(12, 2)  NOT NULL,
    total_amount         NUMERIC(12, 2)  NOT NULL,
    shipping_address_json TEXT,
    idempotency_key      UUID            NOT NULL,
    version              BIGINT          NOT NULL DEFAULT 1,
    created_at           TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT uq_orders_idempotency_key UNIQUE (idempotency_key)
);

CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_status_created ON orders(status, created_at DESC);
CREATE INDEX idx_orders_cursor ON orders(created_at DESC, id DESC);

-- ============================================================================
-- Order items table
-- ============================================================================
CREATE TABLE order_items (
    id           UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id     UUID            NOT NULL REFERENCES orders(id),
    product_id   UUID            NOT NULL,
    sku          VARCHAR(64)     NOT NULL,
    quantity     INT             NOT NULL CHECK (quantity > 0),
    unit_price   NUMERIC(10, 2)  NOT NULL,
    total_price  NUMERIC(10, 2)  NOT NULL
);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);

-- ============================================================================
-- Order events table (event store / audit)
-- ============================================================================
CREATE TABLE order_events (
    event_id      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id      UUID            NOT NULL,
    event_type    VARCHAR(64)     NOT NULL,
    schema_version INT            NOT NULL DEFAULT 1,
    payload       JSONB           NOT NULL,
    metadata      JSONB           NOT NULL DEFAULT '{}',
    occurred_at   TIMESTAMPTZ     NOT NULL DEFAULT now(),
    sequence_num  BIGINT          GENERATED ALWAYS AS IDENTITY
);

CREATE INDEX idx_order_events_order_seq ON order_events(order_id, sequence_num);

-- ============================================================================
-- Outbox table (transactional outbox pattern)
-- ============================================================================
CREATE TABLE outbox (
    id            UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    topic         VARCHAR(128)    NOT NULL,
    partition_key VARCHAR(128)    NOT NULL,
    payload       TEXT            NOT NULL,
    processed     BOOLEAN         NOT NULL DEFAULT false,
    created_at    TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_outbox_unprocessed ON outbox(created_at ASC) WHERE processed = false;

-- ============================================================================
-- Saga state table
-- ============================================================================
CREATE TABLE saga_state (
    saga_id       UUID            PRIMARY KEY,
    order_id      UUID            NOT NULL,
    saga_type     VARCHAR(64)     NOT NULL,
    current_step  VARCHAR(64)     NOT NULL,
    status        VARCHAR(16)     NOT NULL,
    state_data    TEXT            NOT NULL DEFAULT '{}',
    version       BIGINT          NOT NULL DEFAULT 1,
    created_at    TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_saga_order_id ON saga_state(order_id);
