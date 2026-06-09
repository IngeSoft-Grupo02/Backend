package pe.edu.pucp.kingstore.service.storage;

/**
 * Contrato de almacenamiento de archivos.
 * Dos implementaciones:
 *   - LocalStorageService  â†’ perfil "local"  (guarda en disco, sin AWS)
 *   - S3StorageService     â†’ perfil "prod"   (sube a S3)
 *
 * BulkUploadService depende de esta interfaz, nunca de la implementaciÃ³n concreta.
 */
public interface StorageService {

    /**
     * Sube bytes y devuelve la URL pÃºblica (o local) del archivo.
     *
     * @param key         identificador del objeto, ej: "logos/ripley.png"
     * @param bytes       contenido del archivo
     * @param contentType MIME type, ej: "image/png"
     * @return URL accesible para guardar en BD
     */
    String uploadBytes(String key, byte[] bytes, String contentType);
}
