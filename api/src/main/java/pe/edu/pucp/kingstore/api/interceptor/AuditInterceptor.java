package pe.edu.pucp.kingstore.api.interceptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import pe.edu.pucp.kingstore.domain.model.audit.enums.AuditLevel;
import pe.edu.pucp.kingstore.domain.model.audit.AuditLog;
import pe.edu.pucp.kingstore.service.audit.AuditLogService;
import pe.edu.pucp.kingstore.service.security.JwtUtil;
import pe.edu.pucp.kingstore.service.user.UserAccountService;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
public class AuditInterceptor implements HandlerInterceptor {

    private final AuditLogService auditLogService;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final UserAccountService userAccountService;

    public AuditInterceptor(AuditLogService auditLogService,
                            JwtUtil jwtUtil,
                            ObjectMapper objectMapper,
                            UserAccountService userAccountService) {
        this.auditLogService = auditLogService;
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
        this.userAccountService = userAccountService;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {

        // No registrar el endpoint de auditorÃ­a mismo
        if (request.getRequestURI().contains("/admin/audit")) return;
        // Solo registrar acciones del usuario, no cargas automáticas
        if (request.getMethod().equals("GET")) return;

        AuditLog log = new AuditLog();
        log.setHttpMethod(request.getMethod());
        log.setEndpoint(request.getRequestURI());
        log.setStatusCode(response.getStatus());
        String requestEmail = extractRequestEmail(request);

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
        if (log.getUserEmail() == null) {
            log.setUserEmail(requestEmail != null ? requestEmail : "no-autenticado");
        }
        if (log.getRole() == null) {
            log.setRole(resolveFallbackRole(requestEmail, request.getRequestURI()));
        }
        if (log.getTenantSlug() == null) {
            log.setTenantSlug(extractTenantSlugFromPath(request.getRequestURI()));
        }

        // Determinar nivel segÃºn status code
        if (response.getStatus() >= 500) {
            log.setLevel(AuditLevel.ERROR);
        } else if (response.getStatus() >= 400) {
            log.setLevel(AuditLevel.WARN);
        } else {
            log.setLevel(AuditLevel.INFO);
        }

        // DescripciÃ³n basada en mÃ©todo y endpoint
        log.setDescription(request.getMethod() + " " + request.getRequestURI()
                + " â†’ " + response.getStatus());

        auditLogService.save(log);
    }

    private String extractRequestEmail(HttpServletRequest request) {
        if (!(request instanceof ContentCachingRequestWrapper wrapper)) {
            return null;
        }

        byte[] content = wrapper.getContentAsByteArray();
        if (content.length == 0) return null;

        try {
            String body = new String(content, StandardCharsets.UTF_8);
            JsonNode payload = objectMapper.readTree(body);
            JsonNode emailNode = payload.get("email");
            if (emailNode == null) emailNode = payload.get("userEmail");
            if (emailNode == null || emailNode.asText().isBlank()) return null;
            return emailNode.asText().trim().toLowerCase(Locale.ROOT);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String resolveFallbackRole(String requestEmail, String uri) {
        if (requestEmail != null) {
            return userAccountService.findRoleByEmail(requestEmail)
                    .map(Enum::name)
                    .orElse(isLoginPath(uri) ? "NO_REGISTRADO" : "PUBLICO");
        }
        return isLoginPath(uri) ? "NO_REGISTRADO" : "PUBLICO";
    }

    private boolean isLoginPath(String uri) {
        return uri != null && uri.toLowerCase(Locale.ROOT).endsWith("/auth/login");
    }

    private String extractTenantSlugFromPath(String uri) {
        String marker = "/stores/";
        int start = uri.indexOf(marker);
        if (start < 0) return null;

        int slugStart = start + marker.length();
        int slugEnd = uri.indexOf('/', slugStart);
        if (slugEnd <= slugStart) return null;

        return uri.substring(slugStart, slugEnd);
    }
}
