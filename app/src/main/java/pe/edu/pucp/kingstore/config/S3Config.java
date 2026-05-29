package pe.edu.pucp.kingstore.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Crea el bean S3Client SOLO en perfil "prod".
 * En perfil "local" este bean no se instancia, por lo que el SDK de AWS
 * nunca intenta buscar credenciales y el arranque es inmediato.
 */
@Configuration
@Profile("prod")
public class S3Config {

    @Value("${aws.s3.region:us-east-1}")
    private String region;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }
}
