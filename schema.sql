CREATE TABLE IF NOT EXISTS payment_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    buyer_id BIGINT NOT NULL,
    is_payment_done TINYINT NOT NULL DEFAULT 0,
    payment_key VARCHAR(255) UNIQUE,
    order_id VARCHAR(255) NOT NULL UNIQUE,
    type VARCHAR(50),
    order_name VARCHAR(255) NOT NULL,
    method VARCHAR(50),
    psp_raw_data JSON,
    approved_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS payment_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_event_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    order_id VARCHAR(255) NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    payment_order_status VARCHAR(50) NOT NULL,
    ledger_updated TINYINT NOT NULL DEFAULT 0,
    wallet_updated TINYINT NOT NULL DEFAULT 0,
    failed_count TINYINT NOT NULL DEFAULT 0,
    threshold TINYINT NOT NULL DEFAULT 5,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (payment_event_id) REFERENCES payment_events(id)
);

CREATE TABLE IF NOT EXISTS payment_order_histories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_order_id BIGINT NOT NULL,
    previous_status VARCHAR(50),
    new_status VARCHAR(50),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    changed_by VARCHAR(255),
    reason VARCHAR(255),
    FOREIGN KEY (payment_order_id) REFERENCES payment_orders(id)
);
