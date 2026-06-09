package pe.edu.pucp.kingstore.service.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.kingstore.domain.model.audit.enums.AuditLevel;
import pe.edu.pucp.kingstore.domain.model.audit.AuditLog;
import pe.edu.pucp.kingstore.repository.audit.AuditLogRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void save(AuditLog log) {
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> findAll(AuditLevel level, String userEmail,
                                  String tenantSlug, String range) {
        LocalDateTime start = resolveStartDate(range);
        LocalDateTime end = LocalDateTime.now();

        // Filtrar por combinaciÃ³n de parÃ¡metros
        if (level != null && start != null) {
            return auditLogRepository.findByLevelAndTimestampBetween(level, start, end);
        }
        if (level != null) {
            return auditLogRepository.findByLevel(level);
        }
        if (userEmail != null && !userEmail.isBlank() && start != null) {
            return auditLogRepository.findByUserEmailContainingIgnoreCaseAndTimestampBetween(
                    userEmail, start, end);
        }
        if (userEmail != null && !userEmail.isBlank()) {
            return auditLogRepository.findByUserEmailContainingIgnoreCase(userEmail);
        }
        if (tenantSlug != null && !tenantSlug.isBlank() && start != null) {
            return auditLogRepository.findByTenantSlugContainingIgnoreCaseAndTimestampBetween(
                    tenantSlug, start, end);
        }
        if (tenantSlug != null && !tenantSlug.isBlank()) {
            return auditLogRepository.findByTenantSlugContainingIgnoreCase(tenantSlug);
        }
        if (start != null) {
            return auditLogRepository.findByTimestampBetween(start, end);
        }
        return auditLogRepository.findAll();
    }

    private LocalDateTime resolveStartDate(String range) {
        if (range == null) return null;
        return switch (range.toUpperCase()) {
            case "TODAY" -> LocalDateTime.now().toLocalDate().atStartOfDay();
            case "LAST_7_DAYS" -> LocalDateTime.now().minusDays(7);
            case "LAST_30_DAYS" -> LocalDateTime.now().minusDays(30);
            default -> null;
        };
    }
}