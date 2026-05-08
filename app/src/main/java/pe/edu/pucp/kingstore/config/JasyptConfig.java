package pe.edu.pucp.kingstore.config;

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de Jasypt para desencriptación automática de propiedades sensibles.
 * 
 * SEGURIDAD:
 * - La clave de encriptación se obtiene SOLO de la variable de entorno JASYPT_ENCRYPTOR_PASSWORD
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

    @Value("${jasypt.encryptor.password:#{null}}")
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
        
        // Obtener la clave SOLO de la variable de entorno (la forma más segura)
        String password = System.getenv("JASYPT_ENCRYPTOR_PASSWORD");
        
        if (password == null || password.isEmpty()) {
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
        config.setStringOutputType("hex");
        
        encryptor.setConfig(config);
        return encryptor;
    }
}
