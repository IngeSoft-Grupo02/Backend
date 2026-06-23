package pe.edu.pucp.kingstore.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // Endpoints publicos - no requieren token
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/stores/*/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/stores/*/customers/register").permitAll()
                        .requestMatchers(HttpMethod.GET, "/stores/public", "/stores/public/**").permitAll() //nuevo
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()

                        // Solo administradores
                        .requestMatchers("/admin/**").hasRole("SYSTEM_ADMIN")
                        .requestMatchers("/merchant/**").hasRole("MERCHANT")
                        .requestMatchers("/stores/*/cart/**").hasRole("CUSTOMER") //new
                        // Perfil del cliente autenticado, restringido a su propia tienda *****NUEVO
                        .requestMatchers(HttpMethod.GET, "/stores/*/customers/me").hasRole("CUSTOMER")
                        .requestMatchers("/stores/*/cart/**").hasRole("CUSTOMER")
                        .requestMatchers("/stores/*/quotations/**").hasRole("CUSTOMER")
                        .requestMatchers("/stores/*/orders/**").hasRole("CUSTOMER")
                        // Todo lo demÃ¡s requiere autenticaciÃ³n
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "http://52.205.138.95",
                "http://52.205.138.95:*",
                "https://ec2-100-27-222-84.compute-1.amazonaws.com",
                "http://localhost:3000",
                "http://localhost:3001",
                "http://localhost:3002",
                "http://localhost:3003"));
//        config.setAllowedOriginPatterns(List.of(
//            "http://52.205.138.95:3000",
//            "http://localhost:3000"));//ANTIGUO
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

}
