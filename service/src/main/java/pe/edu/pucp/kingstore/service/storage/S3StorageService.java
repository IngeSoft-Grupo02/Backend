package pe.edu.pucp.kingstore.service.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * ImplementaciÃ³n PRODUCCIÃ“N de StorageService.
 * Activa cuando kingstore.storage.provider=s3.
 * Sube archivos al bucket S3 configurado en application-prod.properties.
 */
@Service
@ConditionalOnProperty(name = "kingstore.storage.provider", havingValue = "s3")
public class S3StorageService implements StorageService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.region:us-east-1}")
    private String region;

    public S3StorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public String uploadBytes(String key, byte[] bytes, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(bytes));

        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);
    }
}
