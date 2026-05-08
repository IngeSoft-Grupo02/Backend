# Cifrado de Propiedades Sensibles con Jasypt

## Descripción
Este proyecto utiliza **Jasypt (Java Simplified Encryption)** para cifrar propiedades sensibles como contraseñas de base de datos.

## Configuración

### 1. Variable de Entorno
Se requiere establecer la variable de entorno `JASYPT_ENCRYPTOR_PASSWORD` antes de ejecutar la aplicación:

```bash
export JASYPT_ENCRYPTOR_PASSWORD=kingstore-secret-key-2024
```

O en Windows:
```bash
set JASYPT_ENCRYPTOR_PASSWORD=kingstore-secret-key-2024
```

### 2. Uso en application.properties
Las propiedades cifradas se envuelven con `ENC(...)`:

```properties
spring.datasource.password=ENC(1ZWVwsgdP3MMf6aAg4WRkeoKwW6hG0eB)
```

## Cifrar una Nueva Propiedad

Para cifrar una nueva contraseña o propiedad sensible:

1. Asegurate que el proyecto está compilado:
   ```bash
   ./mvnw clean compile -DskipTests
   ```

2. Ejecuta la utilidad de Jasypt:
   ```bash
   cd ingesoft-grupo02-backend
   export JASYPT_ENCRYPTOR_PASSWORD=kingstore-secret-key-2024
   java -cp "app/target/classes:$(find ~/.m2/repository -name '*.jar' | tr '\n' ':' | head -c -1)" \
     pe.edu.pucp.kingstore.util.JasyptEncryptorUtil "tu_contraseña_aqui"
   ```

3. Copia el resultado (sin "Usa en application.properties:") y úsalo en el archivo de propiedades

## Configuración Actual

### Contraseña de Base de Datos
- **Texto original**: `ingesoft26`
- **Texto cifrado**: `1ZWVwsgdP3MMf6aAg4WRkeoKwW6hG0eB`
- **Clave de encriptación**: `kingstore-secret-key-2024`

## Ejecución de la Aplicación

Siempre debes establecer la variable de entorno antes de ejecutar:

```bash
export JASYPT_ENCRYPTOR_PASSWORD=kingstore-secret-key-2024
./mvnw spring-boot:run
```

O:
```bash
export JASYPT_ENCRYPTOR_PASSWORD=kingstore-secret-key-2024
java -jar app/target/app-1.0.0.jar
```

## Archivos Modificados

- `app/pom.xml` - Agregada dependencia de jasypt-spring-boot-starter
- `app/src/main/resources/application.properties` - Contraseña cifrada
- `app/src/main/java/pe/edu/pucp/kingstore/config/JasyptConfig.java` - Configuración de Jasypt
- `app/src/main/java/pe/edu/pucp/kingstore/util/JasyptEncryptorUtil.java` - Utilidad para cifrar/descifrar

## Seguridad

⚠️ **IMPORTANTE**: 
- La clave de encriptación (`JASYPT_ENCRYPTOR_PASSWORD`) debe almacenarse de forma segura (variables de entorno, secretos en CI/CD, etc.)
- Nunca la incluyas en el control de versiones
- Considera usar un servicio de gestión de secretos en producción (AWS Secrets Manager, Azure Key Vault, etc.)
