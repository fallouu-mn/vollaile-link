-- Flyway V1__init.sql
-- Initial database schema for Vollaile Link
-- Tables: administrator, audit_log

-- Administrator table
CREATE TABLE administrator (
    id BIGSERIAL PRIMARY KEY,
    phone VARCHAR(20) UNIQUE NOT NULL CHECK (phone ~ '^\+\d{1,3}\d{9,15}$'), -- E.164 format
    recovery_email VARCHAR(255),
    password_hash VARCHAR(255) NOT NULL, -- BCrypt or Argon2 hash
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED')),
    last_login TIMESTAMP WITH TIME ZONE,
    must_change_password BOOLEAN NOT NULL DEFAULT TRUE,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Audit log table
CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    administrator_id BIGINT REFERENCES administrator(id) ON DELETE SET NULL,
    event_type VARCHAR(50) NOT NULL, -- LOGIN_SUCCESS, LOGIN_FAILED, LOGOUT, PASSWORD_CHANGE, etc.
    event_description TEXT,
    ip_address INET,
    user_agent TEXT,
    event_timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB -- For storing additional context-specific data
);

-- Indexes for performance
CREATE INDEX idx_audit_log_administrator_id ON audit_log(administrator_id);
CREATE INDEX idx_audit_log_event_timestamp ON audit_log(event_timestamp);
CREATE INDEX idx_audit_log_event_type ON audit_log(event_type);

-- Comment on columns for documentation
COMMENT ON COLUMN administrator.phone IS 'Administrator phone number in E.164 format (+221...)';
COMMENT ON COLUMN administrator.password_hash IS 'Hashed password using BCrypt or Argon2';
COMMENT ON COLUMN administrator.must_change_password IS 'Flag indicating if password must be changed on next login';
COMMENT ON COLUMN administrator.failed_login_attempts IS 'Number of consecutive failed login attempts';
COMMENT ON COLUMN administrator.locked_until IS 'Timestamp until which the account is locked due to failed attempts';
COMMENT ON COLUMN audit_log.event_type IS 'Type of audited event (LOGIN_SUCCESS, LOGIN_FAILED, etc.)';
COMMENT ON COLUMN audit_log.metadata IS 'Additional JSON data for the event';