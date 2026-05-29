package pe.edu.pucp.kingstore.service.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Implementación LOCAL de StorageService.
 * Activa solo con el perfil Spring "local" (desarrollo sin AWS).
 *
 * Guarda el archivo en disco dentro de upload-local/ y devuelve
 * una URL ficticia del tipo: http://localhost:8080/uploads/logos/ripley.png
 *
 * Esto permite que BulkUploadService funcione exactamente igual
 * sin necesidad de credenciales AWS.
 *
 * ACTIVACIÓN:
 *   En application-local.properties ya está configurado:
 *     spring.profiles.active=local
 *   O al arrancar IntelliJ: Edit Configurations → Active profiles: local
 */
@Service
@Profile("local")
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
            log.info("[LOCAL-STORAGE] Archivo guardado: {} → {}", target.toAbsolutePath(), url);
            return url;

        } catch (IOException e) {
            throw new RuntimeException("Error guardando archivo en disco local: " + key, e);
        }
    }
}
