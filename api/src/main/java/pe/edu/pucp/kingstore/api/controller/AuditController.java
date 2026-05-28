
package pe.edu.pucp.kingstore.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.kingstore.domain.model.audit.enums.AuditLevel;
import pe.edu.pucp.kingstore.domain.model.audit.AuditLog;
import pe.edu.pucp.kingstore.service.audit.AuditLogService;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;

import java.util.List;

@RestController
@RequestMapping("/admin/audit")
public class AuditController {

    private final AuditLogService auditLogService;

    public AuditController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<List<AuditLog>> findAll(
            @RequestParam(required = false) AuditLevel level,
            @RequestParam(required = false) String userEmail,
            @RequestParam(required = false) String tenantSlug,
            @RequestParam(required = false) String range) {
        try {
            return ResponseEntity.ok(
                    auditLogService.findAll(level, userEmail, tenantSlug, range)
            );
        } catch (BusinessRuleException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}