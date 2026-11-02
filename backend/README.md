# SlopeGuard Backend (Phase 9: Production-Structured MySQL Persistence)

**SlopeGuard** is an AI-assisted landslide risk monitoring and early warning system designed for mountainous hill tracts across Northeast India (covering Assam, Arunachal Pradesh, Manipur, Meghalaya, Mizoram, Nagaland, Sikkim, and Tripura).

Phase 9 transitions the backend from ephemeral in-memory storage to **production-grade MySQL 8.x relational persistence** using Spring Data JPA, Hibernate 6, HikariCP connection pooling, and Flyway database migrations, while strictly preserving all existing REST API contracts, security filters, and the standalone Python FastAPI AI microservice integration.

---

## 1. Technology & Compatibility Matrix

| Component | Specification / Version | Role in Architecture |
| :--- | :--- | :--- |
| **Java Platform** | Java 17 LTS | Core JVM runtime |
| **Framework** | Spring Boot 3.2.5 | Application framework & dependency injection |
| **Database Engine** | MySQL 8.0.x / 8.4 LTS | Relational database persistence (`InnoDB`, `utf8mb4`) |
| **Data Access** | Spring Data JPA / Hibernate 6.4 | ORM, repositories, and transactional services |
| **Database Migrations** | Flyway 10.x (`flyway-core`, `flyway-mysql`) | Versioned SQL DDL migrations with baseline support |
| **Connection Pool** | HikariCP (bundled) | High-performance pooled JDBC connections |
| **Schema Validation** | `spring.jpa.hibernate.ddl-auto=validate` | Strict startup validation between JPA entities and MySQL schema |
| **Security & Auth** | Spring Security 6.x + Nimbus OAuth2 Resource Server | JWT authentication (HMAC-SHA256) & RBAC |
| **Password Encoding**| BCrypt (`BCryptPasswordEncoder`, strength 10) | One-way password hashing with salt |
| **AI Integration** | Spring 6 `RestClient` | Communicates with FastAPI microservice on `localhost:8000` |
| **Build Tool** | Maven 3.9+ (`mvnw` / `mvnw.cmd` included) | Project lifecycle management |

---

## 2. MySQL Database Setup & Initialization

### 2.1 Database Creation Script
Before launching the backend, ensure your MySQL 8.x service is running. Connect as an administrative user and execute the following SQL command to provision the database with full UTF-8 Unicode support:

```sql
DROP DATABASE IF EXISTS slopeguard;
CREATE DATABASE slopeguard CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2.2 Dedicated Application User (Recommended for Production)
For production environments, grant restricted least-privilege permissions to a dedicated `slopeguard` database user:

```sql
CREATE USER IF NOT EXISTS 'slopeguard_app'@'localhost' IDENTIFIED BY 'YourStrongPassword123!';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON slopeguard.* TO 'slopeguard_app'@'localhost';
FLUSH PRIVILEGES;
```

---

## 3. Configuration & Environment Variables

All sensitive credentials and environment-specific database parameters are externalized through standard environment variables and Spring properties. A template file is provided at `.env.example`.

### 3.1 Environment Variable Reference

| Environment Variable | Spring Property Mapping | Default Value | Description |
| :--- | :--- | :--- | :--- |
| `SLOPEGUARD_DB_URL` | `spring.datasource.url` | `jdbc:mysql://localhost:3306/slopeguard?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` | JDBC URL for MySQL 8 instance |
| `SLOPEGUARD_DB_USERNAME` | `spring.datasource.username` | `root` | Database user account |
| `SLOPEGUARD_DB_PASSWORD` | `spring.datasource.password` | *(empty string)* | Database user password |
| `ADMIN_ENABLED` | `slopeguard.admin.bootstrap.enabled` | `true` | Enables automatic admin provisioning on startup |
| `ADMIN_EMAIL` | `slopeguard.admin.bootstrap.email` | `admin@slopeguard.gov.in` | Provisioned administrative email |
| `ADMIN_PASSWORD` | `slopeguard.admin.bootstrap.password` | `AdminSecurePass2026!` | Initial password for the admin account |
| `ADMIN_NAME` | `slopeguard.admin.bootstrap.name` | `State Disaster Response Administrator` | Full display name for the admin user |
| `SLOPEGUARD_JWT_SECRET` | `slopeguard.security.jwt.secret` | *(configured in properties)* | Minimum 256-bit secret key for HMAC-SHA256 JWT signing |
| `SLOPEGUARD_UPLOAD_DIR` | `slopeguard.upload.dir` | `./data/uploads` | Local disk directory for incident photos |

### 3.2 Setting Environment Variables on Windows

**In PowerShell:**
```powershell
$env:SLOPEGUARD_DB_URL = "jdbc:mysql://localhost:3306/slopeguard?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:SLOPEGUARD_DB_USERNAME = "root"
$env:SLOPEGUARD_DB_PASSWORD = "YourPasswordHere"
$env:ADMIN_ENABLED = "true"
$env:ADMIN_EMAIL = "admin@slopeguard.gov.in"
$env:ADMIN_PASSWORD = "AdminSecurePass2026!"
```

**In Command Prompt (cmd.exe):**
```cmd
set SLOPEGUARD_DB_URL=jdbc:mysql://localhost:3306/slopeguard?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
set SLOPEGUARD_DB_USERNAME=root
set SLOPEGUARD_DB_PASSWORD=YourPasswordHere
set ADMIN_ENABLED=true
set ADMIN_EMAIL=admin@slopeguard.gov.in
set ADMIN_PASSWORD=AdminSecurePass2026!
```

---

## 4. Flyway Migrations & Schema Architecture

Flyway manages all DDL schema lifecycle operations located in `src/main/resources/db/migration/`.

### 4.1 Strict Hibernate Schema Validation
In production mode, the backend enforces:
```properties
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.type.preferred_enum_jdbc_type=12
```
Hibernate **never** modifies or auto-creates tables (`ddl-auto` is strictly set to `validate`). If any table, column, type, or constraint deviates between Flyway's SQL schema and JPA entity mappings, application startup will fail immediately.

### 4.2 Relational Tables Overview (`V1__init_schema.sql`)

1. **`users`**:
   - Stores citizen and administrator accounts.
   - Primary key `id` (`BIGINT AUTO_INCREMENT`).
   - Case-insensitive unique constraint on normalized `email`.
   - `password_hash` (`VARCHAR(255)`, BCrypt).
   - Account state flags (`enabled` boolean).
   - Role enumeration (`CITIZEN`, `ADMIN`).

2. **`incident_reports`**:
   - Geotagged citizen reports across Northeast India.
   - Foreign key `reporter_user_id` referencing `users(id)`.
   - Status tracking (`PENDING`, `UNDER_REVIEW`, `VERIFIED`, `REJECTED`).
   - Incident category (`ROCKFALL`, `MUD_DEBRIS_FLOW`, `SLOPE_CRACK`, `EROSION`, `ROAD_SUBSIDENCE`).
   - Spatial coordinates (`latitude`, `longitude`) with indexes for regional query speed.

3. **`report_photos`**:
   - Metadata for evidence images attached to incident reports.
   - Primary key `id` (`VARCHAR(64)`).
   - Foreign key `report_id` referencing `incident_reports(id) ON DELETE CASCADE`.
   - Storage filename and internal file path decoupled from client download URLs.
   - Cryptographic SHA-256 `checksum` (`VARCHAR(64)`) for image integrity verification.
   - Compensation cleanup: if metadata insertion fails, the disk file is immediately removed.

4. **`risk_assessments`**:
   - Historical records of environmental risk evaluations.
   - Environmental telemetry: `observed_precipitation`, `forecast_precipitation`, `slope_deg`, `elevation_m`, `lithology`, `soil_type`.
   - Model inference governance: `model_version`, `model_status`, `risk_score`, `risk_category`.
   - Full provenance payload persisted in `provenance_json` (`TEXT`).

5. **`early_warning_alerts`**:
   - Active and adjudicated regional disaster hazard alerts.
   - Primary key `id` (`VARCHAR(64)`).
   - Severity level (`INFO`, `LOW`, `MODERATE`, `HIGH`, `CRITICAL`).
   - Lifecycle state (`ACTIVE`, `ACKNOWLEDGED`, `RESOLVED`, `EXPIRED`, `SUPPRESSED`).
   - Deduplication indexing on spatial proximity and timestamp window.

6. **`alert_status_history`**:
   - **Append-only** audit trail of alert lifecycle changes.
   - Records `previous_status`, `new_status`, `actor_user_id`, `actor_email`, `reason`, and timestamp.
   - Immutable historical record of hazard response adjudication.

7. **`audit_events`**:
   - System-wide security, authentication, and status alteration audit trail.
   - Captures `event_type` (`REPORT_SUBMITTED`, `REPORT_STATUS_CHANGED`, `REPORT_REVIEWED`, `ACCESS_DENIED`, `SECURITY_VIOLATION`), `actor_user_id`, `related_report_id`, `outcome` (`SUCCESS`, `FAILURE`), and chronological timestamp.
   - Independent transaction isolation (`Propagation.REQUIRES_NEW`) ensuring audit entries survive business rollback.

8. **`report_status_audit_logs`**:
   - Chronological administrative report review and transition logs.

---

## 5. Administrator Bootstrapping & Security Rules

### 5.1 Controlled Admin Account Seeding
- On application startup, `JpaUserService` checks if an administrator account exists.
- If `ADMIN_ENABLED=true` and no user with `ADMIN_EMAIL` exists, an administrative account is provisioned with BCrypt password encryption.
- If the administrator already exists in MySQL, existing credentials and review history are preserved without duplication.

### 5.2 Disabled Account Enforcement
- Users with `enabled = false` in MySQL are rejected at authentication time with `AccountDisabledException` (`403 Forbidden`).
- Disabled users cannot obtain JWT tokens or perform authenticated actions.

### 5.3 Fail-Fast on Database Outage
- The application will **never** silently fall back to in-memory mode if MySQL is unavailable.
- If MySQL is down, Spring Boot fails startup immediately with a clear connection exception (`CannotGetJdbcConnectionException`).

---

## 6. Running the Application on Windows

### 6.1 Using Maven Wrapper (CLI)
From the `slopeguard-backend` directory:

```powershell
# Compile and run
.\mvnw.cmd spring-boot:run
```

To run with custom environment variables:
```powershell
$env:SLOPEGUARD_DB_USERNAME="root"
$env:SLOPEGUARD_DB_PASSWORD="your_password"
.\mvnw.cmd spring-boot:run
```

### 6.2 Running in Eclipse IDE
1. Open Eclipse IDE (Eclipse IDE for Enterprise Java and Web Developers).
2. Go to **File -> Import... -> Maven -> Existing Maven Projects**.
3. Select the `slopeguard-backend` directory and click **Finish**.
4. Right-click project -> **Maven -> Update Project...** (ensure Java 17 execution environment is selected).
5. Locate `com.slopeguard.SlopeGuardApplication` in `src/main/java`.
6. Right-click `SlopeGuardApplication.java` -> **Run As -> Java Application** (or **Spring Boot App** if Spring Tools 4 is installed).
7. To configure environment variables in Eclipse:
   - Go to **Run -> Run Configurations...**
   - Select `SlopeGuardApplication` under Java Application.
   - Go to the **Environment** tab.
   - Click **Add...** to define `SLOPEGUARD_DB_URL`, `SLOPEGUARD_DB_USERNAME`, `SLOPEGUARD_DB_PASSWORD`.
   - Click **Apply** and **Run**.

---

## 7. Automated Testing Suite

The backend contains a test suite of **116 automated tests** covering unit logic, security rules, boundary checks, JPA persistence, Flyway migrations, and live MySQL restart recovery.

### 7.1 Running All Tests
```powershell
cd slopeguard-backend
.\mvnw.cmd test
```

### 7.2 Verified Test Breakdown
| Test Category | Test Class | Cases | Status |
| :--- | :--- | :---: | :---: |
| **Live MySQL Persistence & Restart** | `LiveMySQLPersistenceIntegrationTest` | 6 | **PASS** |
| **Admin Review Queue & Auditing** | `AdminReviewAndAuditTest` | 16 | **PASS** |
| **Citizen Report Ownership** | `CitizenReportOwnershipTest` | 8 | **PASS** |
| **Photo Uploads & Validation** | `PhotoUploadTest` | 7 | **PASS** |
| **AI Prediction Integration** | `AiPredictionIntegrationTest` | 6 | **PASS** |
| **Environmental Rainfall & Forecast**| `EnvironmentalRainfallTest` | 7 | **PASS** |
| **Authentication & RBAC** | `AuthControllerTest` & `SecurityIntegrationTest` | 19 | **PASS** |
| **Alerts & Deduplication** | `AlertControllerTest` & `EarlyWarningAlertRepositoryTest` | 8 | **PASS** |
| **Terrain Raster & Elevation** | `TerrainServiceTest`, `SlopeCalculatorTest`, `DemRasterGridTest` | 10 | **PASS** |
| **Live Microservice Probes** | `LiveBackendAiIntegrationTest`, `LiveOpenMeteoConnectivityTest` | 2 | **PASS** |
| **Core Endpoints & Smoke Tests** | `ReportControllerTest`, `RiskControllerTest`, `HealthControllerTest`, `SlopeGuardApplicationTests` | 27 | **PASS** |
| **Total Test Suite** | **116 test cases** | **116** | **ALL PASS (100%)** |

### 7.3 Persistence Across Backend Restart Verification
The test `LiveMySQLPersistenceIntegrationTest.testPersistenceAcrossBackendRestart`:
1. Launches Spring Boot Session 1 against live MySQL 8.x.
2. Writes user accounts, reports, and early-warning alerts.
3. Completely terminates and closes the Spring Boot context.
4. Launches a brand new Spring Boot Session 2 connected to the same MySQL instance.
5. Asserts that all users, BCrypt hashes, reports, coordinates, and alert records remain 100% consistent across server shutdown.

---

## 8. Troubleshooting & Operational Guide

| Symptom | Cause | Resolution |
| :--- | :--- | :--- |
| `CommunicationsException: Communications link failure` | MySQL service is not running on port 3306. | Start MySQL (`net start MySQL80` or start daemon). Ensure MySQL is listening on port 3306. |
| `Schema-validation: missing table [...]` | Flyway migrations were skipped or database is unmigrated. | Verify `spring.flyway.enabled=true` and that `db/migration/V1__init_schema.sql` is present. Run `SHOW TABLES;` in MySQL to inspect. |
| `DuplicateEmailException` (`409 Conflict`) | Citizen registered with an already taken email address. | Emails are unique per user. Use a different email address or retrieve the existing account. |
| `InvalidStatusTransitionException` (`409 Conflict`) | Attempted illegal state change (e.g. reverting to `PENDING`). | Follow valid transition paths: `PENDING` -> `UNDER_REVIEW` -> `VERIFIED` or `REJECTED`. |
| `AccessDeniedException` (`403 Forbidden`) | Non-admin caller attempting admin endpoint or accessing another citizen's private report. | Provide a valid JWT token with `ROLE_ADMIN` in the Authorization header (`Bearer <token>`). |
| `AccountDisabledException` (`403 Forbidden`) | User account has `enabled=false` in MySQL. | Activate the user row in MySQL (`UPDATE users SET enabled = true WHERE id = ?`). |
