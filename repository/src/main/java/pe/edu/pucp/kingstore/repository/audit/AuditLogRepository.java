package pe.edu.pucp.kingstore.repository.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.kingstore.domain.model.audit.AuditLog;
import pe.edu.pucp.kingstore.domain.model.audit.enums.AuditLevel;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {

    List<AuditLog> findByLevel(AuditLevel level);

    List<AuditLog> findByUserEmailContainingIgnoreCase(String userEmail);

    List<AuditLog> findByTenantSlugContainingIgnoreCase(String tenantSlug);

    List<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    List<AuditLog> findByLevelAndTimestampBetween(AuditLevel level, LocalDateTime start, LocalDateTime end);

    List<AuditLog> findByUserEmailContainingIgnoreCaseAndTimestampBetween(String userEmail, LocalDateTime start, LocalDateTime end);

    List<AuditLog> findByTenantSlugContainingIgnoreCaseAndTimestampBetween(String tenantSlug, LocalDateTime start, LocalDateTime end);
}