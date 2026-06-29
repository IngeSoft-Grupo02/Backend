package pe.edu.pucp.kingstore.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Crea el bean S3Client solo cuando el proveedor de storage es "s3".
 * En storage local este bean no se instancia, por lo que el SDK de AWS
 * nunca intenta buscar credenciales y el arranque es inmediato.
 */
@Configuration
@ConditionalOnProperty(name = "kingstore.storage.provider", havingValue = "s3")
public class S3Config {

    @Value("${aws.s3.region:us-east-1}")
    private String region;

    @Value("${AWS_ACCESS_KEY_ID:}")
    private String accessKeyId;

    @Value("${AWS_SECRET_ACCESS_KEY:}")
    private String secretAccessKey;

    @Value("${AWS_SESSION_TOKEN:}")
    private String sessionToken;

    @Bean
    public S3Client s3Client() {
        var builder = S3Client.builder().region(Region.of(region));

        if (!accessKeyId.isBlank() && !secretAccessKey.isBlank()) {
            var credentials = sessionToken.isBlank()
                    ? AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                    : AwsSessionCredentials.create(accessKeyId, secretAccessKey, sessionToken);
            builder.credentialsProvider(StaticCredentialsProvider.create(credentials));
        }

        return builder.build();
    }
}
