package pe.edu.pucp.kingstore.util;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;

/**
 * Utilidad para cifrar propiedades sensibles usando Jasypt.
 * 
 * SEGURIDAD:
 * - Se utiliza para cifrar valores nuevos antes de ponerlos en application.properties
 * - Requiere la clave de encriptación como variable de entorno
 * - Nunca incluir claves en el código fuente
 * 
 * USO:
 * 1. Cargar la variable de entorno: export $(cat .env | xargs)
 * 2. Compilar el proyecto: ./mvnw clean compile
 * 3. Ejecutar esta utilidad:
 *    java -cp "app/target/classes:$(find ~/.m2/repository -name '*.jar' | tr '\n' ':' | head -c -1)" \
 *      pe.edu.pucp.kingstore.util.JasyptEncryptorUtil "valor_a_cifrar"
 * 
 * EJEMPLO:
 *    java ... JasyptEncryptorUtil "mi_contraseña_nueva"
 *    Output: Texto cifrado: ABC123DEF456...
 *    Usar en application.properties: spring.datasource.password=ENC(ABC123DEF456...)
 */
public class JasyptEncryptorUtil {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("=== Jasypt Encryptor Utility ===");
            System.out.println("Uso: java JasyptEncryptorUtil <texto_a_cifrar>");
            System.out.println("\nVariable de entorno requerida: JASYPT_ENCRYPTOR_PASSWORD");
            System.out.println("\nEjemplo:");
            System.out.println("  export $(cat .env | xargs)");
            System.out.println("  java JasyptEncryptorUtil 'mi_contraseña'");
            System.exit(1);
        }

        String textToCipher = args[0];
        String encryptorPassword = System.getenv("JASYPT_ENCRYPTOR_PASSWORD");

        if (encryptorPassword == null || encryptorPassword.isEmpty()) {
            System.err.println("❌ ERROR: La variable de entorno JASYPT_ENCRYPTOR_PASSWORD no está establecida");
            System.err.println("\nSoluciones:");
            System.err.println("1. Crear archivo .env: cp .env.example .env");
            System.err.println("2. Cargar variables: export $(cat .env | xargs)");
            System.err.println("3. O ejecutar directamente: JASYPT_ENCRYPTOR_PASSWORD=tu_clave java ...");
            System.exit(1);
        }

        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword(encryptorPassword);
        encryptor.setAlgorithm("PBEWithMD5AndTripleDES");

        String encryptedText = encryptor.encrypt(textToCipher);
        System.out.println("\n✓ Encriptación exitosa");
        System.out.println("Texto original: " + textToCipher);
        System.out.println("Texto cifrado: " + encryptedText);
        System.out.println("\n📝 Usar en application.properties:");
        System.out.println("spring.datasource.password=ENC(" + encryptedText + ")");
    }
}
