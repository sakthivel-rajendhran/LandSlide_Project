-- ========================================================
-- SlopeGuard Database Migration: V100__ragul_module1_schema.sql
-- Module 1: Accounts, Public Site and Alerts (Owner: Ragul)
-- Range: V100 to V199
-- ========================================================

-- 1. Password Reset Tokens
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME NOT NULL,
    used_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pwd_reset_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_pwd_reset_token (token_hash),
    INDEX idx_pwd_reset_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Monitored Areas (Administrative Geographic Registry)
CREATE TABLE IF NOT EXISTS monitored_areas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    state_region VARCHAR(100) NOT NULL,
    district VARCHAR(100) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    description TEXT NULL,
    monitoring_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_monitored_areas_status (monitoring_status),
    INDEX idx_monitored_areas_coords (latitude, longitude)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Workflow Alerts (Draft, Review, Approve, Publish, Resolve, Withdraw)
CREATE TABLE IF NOT EXISTS workflow_alerts (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    area_name VARCHAR(255) NOT NULL,
    area_id BIGINT NULL,
    assessment_id VARCHAR(64) NULL,
    severity VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    recommended_actions TEXT NULL,
    source_type VARCHAR(50) NOT NULL DEFAULT 'REVIEWED_PLATFORM',
    reference_url VARCHAR(500) NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at DATETIME NULL,
    resolved_at DATETIME NULL,
    version INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_wf_alert_creator FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_wf_alert_area FOREIGN KEY (area_id) REFERENCES monitored_areas (id) ON DELETE SET NULL,
    INDEX idx_wf_alerts_status (status),
    INDEX idx_wf_alerts_severity (severity),
    INDEX idx_wf_alerts_published (published_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Alert Reviews (Audit trail of alert state transitions)
CREATE TABLE IF NOT EXISTS alert_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    alert_id VARCHAR(64) NOT NULL,
    action VARCHAR(50) NOT NULL,
    actor_id BIGINT NOT NULL,
    reason TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_alert_rev_alert FOREIGN KEY (alert_id) REFERENCES workflow_alerts (id) ON DELETE CASCADE,
    CONSTRAINT fk_alert_rev_actor FOREIGN KEY (actor_id) REFERENCES users (id),
    INDEX idx_alert_rev_alert_id (alert_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Alert Deliveries (Broadcast dispatch tracking)
CREATE TABLE IF NOT EXISTS alert_deliveries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    alert_id VARCHAR(64) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    recipient_group VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    attempted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    failure_reason TEXT NULL,
    CONSTRAINT fk_alert_deliv_alert FOREIGN KEY (alert_id) REFERENCES workflow_alerts (id) ON DELETE CASCADE,
    INDEX idx_alert_deliv_alert_id (alert_id),
    INDEX idx_alert_deliv_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
