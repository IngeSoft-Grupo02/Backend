package pe.edu.pucp.kingstore.service.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * ImplementaciÃ³n LOCAL de StorageService.
 * Activa cuando kingstore.storage.provider=local.
 *
 * Guarda el archivo en disco dentro de upload-local/ y devuelve
 * una URL ficticia del tipo: http://localhost:8080/uploads/logos/ripley.png
 *
 * Esto permite que BulkUploadService funcione exactamente igual
 * sin necesidad de credenciales AWS.
 *
 * ACTIVACIÃ“N:
 *   Por defecto kingstore.storage.provider=local.
 *   Para S3 usar kingstore.storage.provider=s3.
 */
@Service
@ConditionalOnProperty(name = "kingstore.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    @Value("${storage.local.base-dir:upload-local}")
    private String baseDir;

    @Value("${storage.local.base-url:http://localhost:8080/uploads}")
    private String baseUrl;

    @Override
    public String uploadBytes(String key, byte[] bytes, String contentType) {
        try {
            Path target = Paths.get(baseDir, key);
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);

            String url = baseUrl + "/" + key;
            log.info("[LOCAL-STORAGE] Archivo guardado: {} â†’ {}", target.toAbsolutePath(), url);
            return url;

        } catch (IOException e) {
            throw new RuntimeException("Error guardando archivo en disco local: " + key, e);
        }
    }
}
