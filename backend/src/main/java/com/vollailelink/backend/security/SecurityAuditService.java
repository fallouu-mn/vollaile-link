package com.vollailelink.backend.security;

import com.vollailelink.backend.model.Administrator;
import com.vollailelink.backend.model.AuditLog;
import com.vollailelink.backend.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SecurityAuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void recordSuccess(Administrator administrator, String eventType, String description, String ipAddress) {
        auditLogRepository.save(AuditLog.builder()
                .administrator(administrator)
                .eventType(eventType)
                .eventDescription(description)
                .ipAddress(ipAddress)
                .build());
    }

    @Transactional
    public void recordFailure(Administrator administrator, String eventType, String description, String ipAddress) {
        auditLogRepository.save(AuditLog.builder()
                .administrator(administrator)
                .eventType(eventType)
                .eventDescription(description)
                .ipAddress(ipAddress)
                .build());
    }

    @Transactional
    public void recordFailure(String eventType, String description, String ipAddress) {
        auditLogRepository.save(AuditLog.builder()
                .eventType(eventType)
                .eventDescription(description)
                .ipAddress(ipAddress)
                .build());
    }
}
