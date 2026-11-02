-- ========================================================
-- SlopeGuard Database Migration: V101__rescue_teams_and_assignments.sql
-- Nearest Rescue Team Registration, Management & Assignment System
-- ========================================================

-- 1. Rescue Teams Table
CREATE TABLE IF NOT EXISTS rescue_teams (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    team_name VARCHAR(150) NOT NULL UNIQUE,
    team_category VARCHAR(50) NOT NULL,
    team_leader VARCHAR(100) NOT NULL,
    contact_number VARCHAR(30) NOT NULL,
    team_size INT NOT NULL DEFAULT 5,
    operating_district VARCHAR(100) NOT NULL,
    operating_region VARCHAR(100) NOT NULL,
    base_address VARCHAR(255) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    verification_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    operational_status VARCHAR(30) NOT NULL DEFAULT 'INACTIVE',
    availability VARCHAR(30) NOT NULL DEFAULT 'INACTIVE',
    rejection_reason TEXT NULL,
    verified_by_user_id BIGINT NULL,
    verified_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_rescue_teams_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_rescue_teams_verifier FOREIGN KEY (verified_by_user_id) REFERENCES users (id) ON DELETE SET NULL,
    INDEX idx_rescue_teams_verification (verification_status),
    INDEX idx_rescue_teams_operational (operational_status),
    INDEX idx_rescue_teams_availability (availability),
    INDEX idx_rescue_teams_coords (latitude, longitude)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Rescue Team Capabilities (One team has many capabilities)
CREATE TABLE IF NOT EXISTS rescue_team_capabilities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rescue_team_id BIGINT NOT NULL,
    capability VARCHAR(50) NOT NULL,
    CONSTRAINT fk_rt_cap_team FOREIGN KEY (rescue_team_id) REFERENCES rescue_teams (id) ON DELETE CASCADE,
    CONSTRAINT uk_rt_capability UNIQUE (rescue_team_id, capability),
    INDEX idx_rt_capability (capability)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Rescue Team Verification & State Audit Log
CREATE TABLE IF NOT EXISTS rescue_team_verifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rescue_team_id BIGINT NOT NULL,
    reviewed_by_user_id BIGINT NOT NULL,
    action VARCHAR(30) NOT NULL,
    notes TEXT NULL,
    reviewed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_rt_verif_team FOREIGN KEY (rescue_team_id) REFERENCES rescue_teams (id) ON DELETE CASCADE,
    CONSTRAINT fk_rt_verif_reviewer FOREIGN KEY (reviewed_by_user_id) REFERENCES users (id),
    INDEX idx_rt_verif_team (rescue_team_id),
    INDEX idx_rt_verif_date (reviewed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Rescue Assignments (Incident-to-Team Field Dispatches)
CREATE TABLE IF NOT EXISTS rescue_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    incident_report_id BIGINT NOT NULL,
    rescue_team_id BIGINT NOT NULL,
    assigned_by_user_id BIGINT NULL,
    priority VARCHAR(30) NOT NULL DEFAULT 'HIGH',
    instructions TEXT NULL,
    assignment_status VARCHAR(30) NOT NULL DEFAULT 'ASSIGNED',
    distance_km DOUBLE NULL,
    assigned_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    accepted_at DATETIME(6) NULL,
    declined_at DATETIME(6) NULL,
    decline_reason TEXT NULL,
    en_route_at DATETIME(6) NULL,
    on_scene_at DATETIME(6) NULL,
    completed_at DATETIME(6) NULL,
    response_notes TEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_ra_incident FOREIGN KEY (incident_report_id) REFERENCES incident_reports (id) ON DELETE CASCADE,
    CONSTRAINT fk_ra_team FOREIGN KEY (rescue_team_id) REFERENCES rescue_teams (id) ON DELETE RESTRICT,
    CONSTRAINT fk_ra_assigner FOREIGN KEY (assigned_by_user_id) REFERENCES users (id) ON DELETE SET NULL,
    INDEX idx_ra_incident (incident_report_id),
    INDEX idx_ra_team (rescue_team_id),
    INDEX idx_ra_status (assignment_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
