package pe.edu.pucp.kingstore.config;

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Configuración de Jasypt para desencriptación automática de propiedades sensibles.
 * 
 * SEGURIDAD:
 * - La clave de encriptación se puede obtener de la variable de entorno JASYPT_ENCRYPTOR_PASSWORD
 * - En pruebas locales también puede cargarse desde el archivo .env
 * - Esta clave NUNCA debe estar en el código fuente ni en Git
 * - Ver .env.example para estructura del archivo .env
 * 
 * USO:
 * - Ejecutar con: JASYPT_ENCRYPTOR_PASSWORD=tu_clave ./mvnw spring-boot:run
 * - O cargar desde archivo: export $(cat .env | xargs)
 * 
 * PROPIEDADES ENCRIPTADAS:
 * - spring.datasource.password=ENC(...)
 */
@Configuration
@EnableEncryptableProperties
public class JasyptConfig {

    @Value("${jasypt.encryptor.password:}")
    private String encryptorPassword;

    /**
     * Configura el encryptor de Jasypt con la clave de la variable de entorno.
     * La desencriptación ocurre automáticamente al inyectar propiedades con ENC(...).
     * 
     * @return PooledPBEStringEncryptor configurado
     * @throws IllegalArgumentException si JASYPT_ENCRYPTOR_PASSWORD no está definida
     */
    @Bean("jasyptStringEncryptor")
    public PooledPBEStringEncryptor jasyptStringEncryptor() {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        
        String password = resolveEncryptorPassword();
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException(
                    "ERROR: Variable de entorno JASYPT_ENCRYPTOR_PASSWORD no definida.\n" +
                    "Soluciones:\n" +
                    "1. Crear archivo .env con: JASYPT_ENCRYPTOR_PASSWORD=tu_clave\n" +
                    "2. Ejecutar: export $(cat .env | xargs)\n" +
                    "3. O en línea de comandos: JASYPT_ENCRYPTOR_PASSWORD=tu_clave ./mvnw spring-boot:run"
            );
        }

        config.setPassword(password);
        config.setAlgorithm("PBEWithMD5AndTripleDES");
        config.setKeyObtentionIterations("1000");
        config.setPoolSize("1");
        config.setProviderName("SunJCE");
        config.setStringOutputType("base64");
        
        encryptor.setConfig(config);
        return encryptor;
    }

    private String resolveEncryptorPassword() {
        if (encryptorPassword != null && !encryptorPassword.isBlank()) {
            return encryptorPassword;
        }

        String envPassword = System.getenv("JASYPT_ENCRYPTOR_PASSWORD");
        if (envPassword != null && !envPassword.isBlank()) {
            return envPassword;
        }

        String sysPassword = System.getProperty("JASYPT_ENCRYPTOR_PASSWORD");
        if (sysPassword != null && !sysPassword.isBlank()) {
            return sysPassword;
        }

        return readPasswordFromDotEnv();
    }

    private String readPasswordFromDotEnv() {
        Path envPath = findDotEnvPath();
        if (envPath == null || !Files.exists(envPath)) {
            return null;
        }

        try {
            List<String> lines = Files.readAllLines(envPath);
            for (String line : lines) {
                String clean = line.trim();
                if (clean.startsWith("#") || clean.isEmpty()) {
                    continue;
                }
                String[] parts = clean.split("=", 2);
                if (parts.length == 2 && "JASYPT_ENCRYPTOR_PASSWORD".equals(parts[0].trim())) {
                    return parts[1].trim();
                }
            }
        } catch (IOException ignored) {
            // Ignorar: si no se puede leer el archivo, seguimos intentando otras fuentes.
        }
        return null;
    }

    private Path findDotEnvPath() {
        Path current = Path.of(System.getProperty("user.dir"));
        for (int i = 0; i < 5; i++) {
            Path candidate = current.resolve(".env");
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
            if (current == null) {
                break;
            }
        }
        return null;
    }
}
