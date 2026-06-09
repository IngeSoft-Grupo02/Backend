package pe.edu.pucp.kingstore.api;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import pe.edu.pucp.kingstore.api.interceptor.AuditInterceptor;
import pe.edu.pucp.kingstore.api.security.JwtAuthenticationFilter;
import pe.edu.pucp.kingstore.domain.model.audit.AuditLog;
import pe.edu.pucp.kingstore.domain.model.audit.enums.AuditLevel;
import pe.edu.pucp.kingstore.domain.model.user.enums.Role;
import pe.edu.pucp.kingstore.service.audit.AuditLogService;
import pe.edu.pucp.kingstore.service.security.JwtUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SecurityCoverageTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void jwtFilterSetsAuthenticationOnlyForValidBearerToken() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil);

        MockHttpServletRequest noHeaderRequest = new MockHttpServletRequest("GET", "/merchant/profile");
        MockHttpServletResponse noHeaderResponse = new MockHttpServletResponse();
        FilterChain noHeaderChain = mock(FilterChain.class);
        filter.doFilter(noHeaderRequest, noHeaderResponse, noHeaderChain);
        verify(noHeaderChain).doFilter(noHeaderRequest, noHeaderResponse);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        MockHttpServletRequest invalidRequest = new MockHttpServletRequest("GET", "/merchant/profile");
        invalidRequest.addHeader("Authorization", "Bearer invalid");
        MockHttpServletResponse invalidResponse = new MockHttpServletResponse();
        FilterChain invalidChain = mock(FilterChain.class);
        when(jwtUtil.isTokenValid("invalid")).thenReturn(false);
        filter.doFilter(invalidRequest, invalidResponse, invalidChain);
        verify(invalidChain).doFilter(invalidRequest, invalidResponse);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        MockHttpServletRequest validRequest = new MockHttpServletRequest("GET", "/merchant/profile");
        validRequest.addHeader("Authorization", "Bearer valid");
        MockHttpServletResponse validResponse = new MockHttpServletResponse();
        FilterChain validChain = mock(FilterChain.class);
        when(jwtUtil.isTokenValid("valid")).thenReturn(true);
        when(jwtUtil.extractRole("valid")).thenReturn(Role.MERCHANT);
        when(jwtUtil.extractUserId("valid")).thenReturn(77);

        filter.doFilter(validRequest, validResponse, validChain);

        verify(validChain).doFilter(validRequest, validResponse);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo("77");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_MERCHANT");
    }

    @Test
    void auditInterceptorSkipsAuditEndpointAndPersistsTokenContextWithLevels() {
        AuditLogService auditLogService = mock(AuditLogService.class);
        JwtUtil jwtUtil = mock(JwtUtil.class);
        AuditInterceptor interceptor = new AuditInterceptor(auditLogService, jwtUtil);

        MockHttpServletRequest skippedRequest = new MockHttpServletRequest("GET", "/admin/audit");
        interceptor.afterCompletion(skippedRequest, new MockHttpServletResponse(), new Object(), null);
        verify(auditLogService, never()).save(any());

        Claims claims = mock(Claims.class);
        when(claims.get("email", String.class)).thenReturn("merchant@test.com");
        when(jwtUtil.isTokenValid("valid")).thenReturn(true);
        when(jwtUtil.extractClaims("valid")).thenReturn(claims);
        when(jwtUtil.extractRole("valid")).thenReturn(Role.MERCHANT);
        when(jwtUtil.extractStoreSlug("valid")).thenReturn("main-store");

        MockHttpServletRequest okRequest = new MockHttpServletRequest("PUT", "/merchant/products/10");
        okRequest.addHeader("Authorization", "Bearer valid");
        MockHttpServletResponse okResponse = new MockHttpServletResponse();
        okResponse.setStatus(200);
        interceptor.afterCompletion(okRequest, okResponse, new Object(), null);

        MockHttpServletRequest warnRequest = new MockHttpServletRequest("DELETE", "/merchant/products/99");
        MockHttpServletResponse warnResponse = new MockHttpServletResponse();
        warnResponse.setStatus(404);
        interceptor.afterCompletion(warnRequest, warnResponse, new Object(), null);

        MockHttpServletRequest errorRequest = new MockHttpServletRequest("POST", "/merchant/products");
        MockHttpServletResponse errorResponse = new MockHttpServletResponse();
        errorResponse.setStatus(500);
        interceptor.afterCompletion(errorRequest, errorResponse, new Object(), null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, org.mockito.Mockito.times(3)).save(captor.capture());

        assertThat(captor.getAllValues()).extracting(AuditLog::getLevel)
                .containsExactly(AuditLevel.INFO, AuditLevel.WARN, AuditLevel.ERROR);
        AuditLog tokenLog = captor.getAllValues().get(0);
        assertThat(tokenLog.getUserEmail()).isEqualTo("merchant@test.com");
        assertThat(tokenLog.getRole()).isEqualTo("MERCHANT");
        assertThat(tokenLog.getTenantSlug()).isEqualTo("main-store");
        assertThat(tokenLog.getHttpMethod()).isEqualTo("PUT");
        assertThat(tokenLog.getEndpoint()).isEqualTo("/merchant/products/10");
    }
}
