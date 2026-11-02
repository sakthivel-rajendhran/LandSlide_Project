-- ========================================================
-- SlopeGuard Phase 9: Production-Structured Schema Migration
-- Compatible with MySQL 8.x and strict schema validation
-- ========================================================

-- 1. Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone_number VARCHAR(30) NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'CITIZEN',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_role ON users (role);

-- 2. Incident Reports table
CREATE TABLE IF NOT EXISTS incident_reports (
    id BIGINT NOT NULL AUTO_INCREMENT,
    reporter_user_id BIGINT NULL,
    reporter_name VARCHAR(150) NULL,
    description TEXT NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    location_description VARCHAR(255) NULL,
    incident_category VARCHAR(50) NOT NULL,
    reported_timestamp DATETIME(6) NOT NULL,
    observation_timestamp DATETIME(6) NULL,
    environmental_observations TEXT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    updated_at DATETIME(6) NOT NULL,
    status_updated_at DATETIME(6) NULL,
    review_notes TEXT NULL,
    CONSTRAINT pk_incident_reports PRIMARY KEY (id),
    CONSTRAINT fk_incident_reports_user FOREIGN KEY (reporter_user_id) REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX idx_reports_reporter ON incident_reports (reporter_user_id);
CREATE INDEX idx_reports_status ON incident_reports (status);
CREATE INDEX idx_reports_category ON incident_reports (incident_category);
CREATE INDEX idx_reports_timestamp ON incident_reports (reported_timestamp);
CREATE INDEX idx_reports_coords ON incident_reports (latitude, longitude);

-- 3. Report Photographs metadata table
CREATE TABLE IF NOT EXISTS report_photos (
    id VARCHAR(64) NOT NULL,
    report_id BIGINT NOT NULL,
    storage_filename VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    checksum VARCHAR(64) NULL,
    uploader_user_id BIGINT NULL,
    uploaded_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_report_photos PRIMARY KEY (id),
    CONSTRAINT fk_report_photos_report FOREIGN KEY (report_id) REFERENCES incident_reports (id) ON DELETE CASCADE,
    CONSTRAINT fk_report_photos_user FOREIGN KEY (uploader_user_id) REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX idx_photos_report ON report_photos (report_id);
CREATE INDEX idx_photos_uploader ON report_photos (uploader_user_id);

-- 4. Risk Assessments table
CREATE TABLE IF NOT EXISTS risk_assessments (
    id VARCHAR(64) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    assessment_timestamp DATETIME(6) NOT NULL,
    model_version VARCHAR(50) NOT NULL,
    model_status VARCHAR(50) NOT NULL,
    risk_status VARCHAR(50) NOT NULL,
    risk_score DOUBLE NULL,
    risk_category VARCHAR(50) NULL,
    is_provisional_category BOOLEAN NOT NULL DEFAULT TRUE,
    data_freshness VARCHAR(50) NULL,
    data_quality VARCHAR(50) NULL,
    observed_precipitation DOUBLE NULL,
    forecast_precipitation DOUBLE NULL,
    slope_deg DOUBLE NULL,
    elevation_m DOUBLE NULL,
    lithology VARCHAR(100) NULL,
    soil_type VARCHAR(100) NULL,
    alert_triggered BOOLEAN NOT NULL DEFAULT FALSE,
    alert_id VARCHAR(64) NULL,
    provenance_json TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_risk_assessments PRIMARY KEY (id)
);

CREATE INDEX idx_assessments_coords ON risk_assessments (latitude, longitude);
CREATE INDEX idx_assessments_timestamp ON risk_assessments (assessment_timestamp);
CREATE INDEX idx_assessments_category ON risk_assessments (risk_category);

-- 5. Early Warning Alerts table
CREATE TABLE IF NOT EXISTS early_warning_alerts (
    id VARCHAR(64) NOT NULL,
    assessment_id VARCHAR(64) NULL,
    location_name VARCHAR(255) NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    severity VARCHAR(30) NOT NULL,
    assessment_status VARCHAR(50) NULL,
    designation VARCHAR(50) NULL,
    creation_timestamp DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    evidence_json TEXT NULL,
    lifecycle_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    source VARCHAR(255) NULL,
    acknowledged_by_user_id BIGINT NULL,
    admin_review_note TEXT NULL,
    CONSTRAINT pk_early_warning_alerts PRIMARY KEY (id),
    CONSTRAINT fk_alerts_user FOREIGN KEY (acknowledged_by_user_id) REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX idx_alerts_status ON early_warning_alerts (lifecycle_status);
CREATE INDEX idx_alerts_severity ON early_warning_alerts (severity);
CREATE INDEX idx_alerts_coords ON early_warning_alerts (latitude, longitude);
CREATE INDEX idx_alerts_timestamp ON early_warning_alerts (creation_timestamp);

-- 6. Alert Status Transition History table (Append-only)
CREATE TABLE IF NOT EXISTS alert_status_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    alert_id VARCHAR(64) NOT NULL,
    previous_status VARCHAR(30) NULL,
    new_status VARCHAR(30) NOT NULL,
    actor_user_id BIGINT NULL,
    actor_email VARCHAR(255) NULL,
    reason TEXT NULL,
    transition_timestamp DATETIME(6) NOT NULL,
    CONSTRAINT pk_alert_status_history PRIMARY KEY (id),
    CONSTRAINT fk_alert_hist_alert FOREIGN KEY (alert_id) REFERENCES early_warning_alerts (id) ON DELETE CASCADE,
    CONSTRAINT fk_alert_hist_user FOREIGN KEY (actor_user_id) REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX idx_alert_hist_alert_id ON alert_status_history (alert_id);
CREATE INDEX idx_alert_hist_timestamp ON alert_status_history (transition_timestamp);

-- 7. Audit Events table
CREATE TABLE IF NOT EXISTS audit_events (
    id VARCHAR(64) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    actor_user_id BIGINT NULL,
    related_report_id BIGINT NULL,
    timestamp DATETIME(6) NOT NULL,
    outcome VARCHAR(20) NOT NULL,
    description TEXT NOT NULL,
    previous_status VARCHAR(30) NULL,
    new_status VARCHAR(30) NULL,
    metadata_json TEXT NULL,
    CONSTRAINT pk_audit_events PRIMARY KEY (id)
);

CREATE INDEX idx_audit_type ON audit_events (event_type);
CREATE INDEX idx_audit_actor ON audit_events (actor_user_id);
CREATE INDEX idx_audit_report ON audit_events (related_report_id);
CREATE INDEX idx_audit_timestamp ON audit_events (timestamp);

-- 8. Report Status Audit Logs (Legacy / Specific Report Transitions)
CREATE TABLE IF NOT EXISTS report_status_audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    report_id BIGINT NOT NULL,
    previous_status VARCHAR(30) NULL,
    new_status VARCHAR(30) NOT NULL,
    admin_user_id BIGINT NULL,
    admin_email VARCHAR(255) NULL,
    review_notes TEXT NULL,
    timestamp DATETIME(6) NOT NULL,
    CONSTRAINT pk_report_status_audit_logs PRIMARY KEY (id),
    CONSTRAINT fk_status_audit_report FOREIGN KEY (report_id) REFERENCES incident_reports (id) ON DELETE CASCADE
);

CREATE INDEX idx_status_audit_report ON report_status_audit_logs (report_id);
