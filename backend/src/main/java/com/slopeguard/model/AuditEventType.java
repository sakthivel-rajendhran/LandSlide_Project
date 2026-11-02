package com.slopeguard.model;

/**
 * Categorization of security-relevant and administrative actions captured in audit records.
 */
public enum AuditEventType {
    REPORT_SUBMITTED,
    REPORT_STATUS_CHANGED,
    REPORT_REVIEWED,
    REPORT_ACCESSED,
    PHOTO_UPLOADED,
    ACCESS_DENIED,
    SECURITY_VIOLATION
}
