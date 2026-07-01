package pe.edu.pucp.kingstore.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.util.Locale;

@Component
public class CachedJsonRequestFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String contentType = request.getContentType();
        boolean isJson = contentType != null
                && contentType.toLowerCase(Locale.ROOT).contains("application/json");

        if (!isJson || request instanceof ContentCachingRequestWrapper) {
            filterChain.doFilter(request, response);
            return;
        }

        filterChain.doFilter(new ContentCachingRequestWrapper(request), response);
    }
}
