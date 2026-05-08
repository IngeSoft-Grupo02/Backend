package pe.edu.pucp.kingstore.config;

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableEncryptableProperties
public class JasyptConfig {

    @Value("${jasypt.encryptor.password:#{null}}")
    private String encryptorPassword;

    @Bean("jasyptStringEncryptor")
    public PooledPBEStringEncryptor jasyptStringEncryptor() {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        
        String password = encryptorPassword != null ? 
                encryptorPassword : 
                System.getenv("JASYPT_ENCRYPTOR_PASSWORD");
        
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException(
                    "Se debe proporcionar JASYPT_ENCRYPTOR_PASSWORD como variable de entorno " +
                    "o en application.properties (jasypt.encryptor.password)"
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
