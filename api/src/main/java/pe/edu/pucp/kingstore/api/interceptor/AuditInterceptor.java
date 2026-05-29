package pe.edu.pucp.kingstore.api.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import pe.edu.pucp.kingstore.domain.model.audit.enums.AuditLevel;
import pe.edu.pucp.kingstore.domain.model.audit.AuditLog;
import pe.edu.pucp.kingstore.service.audit.AuditLogService;
import pe.edu.pucp.kingstore.service.security.JwtUtil;

@Component
public class AuditInterceptor implements HandlerInterceptor {

    private final AuditLogService auditLogService;
    private final JwtUtil jwtUtil;

    public AuditInterceptor(AuditLogService auditLogService, JwtUtil jwtUtil) {
        this.auditLogService = auditLogService;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {

        // No registrar el endpoint de auditoría mismo
        if (request.getRequestURI().contains("/admin/audit")) return;

        AuditLog log = new AuditLog();
        log.setHttpMethod(request.getMethod());
        log.setEndpoint(request.getRequestURI());
        log.setStatusCode(response.getStatus());

        // Extraer info del token si existe
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.isTokenValid(token)) {
                log.setUserEmail(jwtUtil.extractClaims(token).get("email", String.class));
                log.setRole(jwtUtil.extractRole(token).name());
                log.setTenantSlug(jwtUtil.extractStoreSlug(token));
            }
        }

        // Determinar nivel según status code
        if (response.getStatus() >= 500) {
            log.setLevel(AuditLevel.ERROR);
        } else if (response.getStatus() >= 400) {
            log.setLevel(AuditLevel.WARN);
        } else {
            log.setLevel(AuditLevel.INFO);
        }

        // Descripción basada en método y endpoint
        log.setDescription(request.getMethod() + " " + request.getRequestURI()
                + " → " + response.getStatus());

        auditLogService.save(log);
    }
}