package pe.edu.pucp.kingstore.service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import pe.edu.pucp.kingstore.domain.model.user.enums.Role;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {
    //private static final String SECRET = System.getenv("JWT_SECRET"); //revisar
    private static final String SECRET = "kingstore-local-test-secret-32-bytes-minimum";
    private static final long EXPIRATION_ADMIN_MS = 4 * 60 * 60 * 1000L;
    private static final long EXPIRATION_MERCHANT_MS = 2 * 60 * 60 * 1000L;
    private static final long EXPIRATION_CUSTOMER_MS = 60 * 60 * 1000L;

    private SecretKey getSigningKey(){

        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.isBlank()) {
            secret = SECRET;
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

    }

    public String generateToken(Integer userId, String email, Role role, String storeslug){
        long expiration = switch (role){
            case CUSTOMER -> EXPIRATION_CUSTOMER_MS;
            case MERCHANT -> EXPIRATION_MERCHANT_MS;
            case SYSTEM_ADMIN -> EXPIRATION_ADMIN_MS;
        };

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email",email)
                .claim("role",role.name())
                .claim("storeSlug",storeslug != null ? storeslug: "")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims extractClaims(String token){
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Integer extractUserId(String token) {
        return Integer.parseInt(extractClaims(token).getSubject());
    }

    public Role extractRole(String token) {
        return Role.valueOf(extractClaims(token).get("role", String.class));
    }

    public String extractStoreSlug(String token) {
        return extractClaims(token).get("storeSlug", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
