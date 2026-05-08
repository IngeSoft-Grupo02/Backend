package pe.edu.pucp.kingstore.util;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;

/**
 * Utilidad para cifrar y descifrar propiedades sensibles usando Jasypt
 * Uso: java -cp ... pe.edu.pucp.kingstore.util.JasyptEncryptorUtil "ingesoft26"
 */
public class JasyptEncryptorUtil {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Uso: java JasyptEncryptorUtil <texto_a_cifrar>");
            System.out.println("Variable de entorno requerida: JASYPT_ENCRYPTOR_PASSWORD");
            System.exit(1);
        }

        String textToCipher = args[0];
        String encryptorPassword = System.getenv("JASYPT_ENCRYPTOR_PASSWORD");

        if (encryptorPassword == null || encryptorPassword.isEmpty()) {
            System.out.println("Error: La variable de entorno JASYPT_ENCRYPTOR_PASSWORD no está establecida");
            System.exit(1);
        }

        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword(encryptorPassword);
        encryptor.setAlgorithm("PBEWithMD5AndTripleDES");

        String encryptedText = encryptor.encrypt(textToCipher);
        System.out.println("Texto original: " + textToCipher);
        System.out.println("Texto cifrado: " + encryptedText);
        System.out.println("\nUsa en application.properties: ENC(" + encryptedText + ")");
    }
}
