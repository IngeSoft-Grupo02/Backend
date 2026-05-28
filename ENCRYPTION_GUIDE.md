# Cifrado de Propiedades Sensibles con Jasypt

## Descripción
Este proyecto utiliza **Jasypt (Java Simplified Encryption)** para cifrar propiedades sensibles como contraseñas de base de datos.

## ⚠️ Seguridad: Clave de Encriptación

### IMPORTANTE
**La clave de encriptación NUNCA debe estar en el código fuente ni en Git.**

### Configuración Segura

#### 1. Crear archivo `.env` (ignorado por Git)

```bash
cp .env.example .env
```

Editar `.env` con tu clave segura:
```
JASYPT_ENCRYPTOR_PASSWORD=tu-clave-segura-aqui
```

#### 2. Asegurarse que `.env` está en `.gitignore`
```bash
cat .gitignore | grep "\.env"
```

Debe mostrar:
```
.env
.env.*
```

## Ejecución de la Aplicación

### Opción 1: Cargar variables desde `.env` (Recomendado)

```bash
# Una sola vez en la sesión
export $(cat .env | xargs)

# Ejecutar la aplicación
./mvnw spring-boot:run
```

### Opción 2: Pasar directamente por línea de comandos

```bash
JASYPT_ENCRYPTOR_PASSWORD=tu-clave ./mvnw spring-boot:run
```

### Opción 3: En producción (por variables de entorno del servidor)

```bash
export JASYPT_ENCRYPTOR_PASSWORD=tu-clave-secreta
java -jar app/target/app-1.0.0.jar
```

## Cifrar una Nueva Propiedad

Si necesitas cifrar una nueva contraseña o propiedad sensible:

1. **Cargar la variable de entorno:**
   ```bash
   export $(cat .env | xargs)
   ```

2. **Compilar el proyecto:**
   ```bash
   ./mvnw clean compile -DskipTests
   ```

3. **Ejecutar la utilidad de cifrado:**
   ```bash
   java -cp "app/target/classes:$(find ~/.m2/repository -name '*.jar' | tr '\n' ':' | head -c -1)" \
     pe.edu.pucp.kingstore.util.JasyptEncryptorUtil "tu_contraseña_aqui"
   ```

4. **Copiar el resultado (solo la parte cifrada):**
   ```
   Texto cifrado: ABC123DEF456...
   
   Usar en application.properties:
   spring.property=ENC(ABC123DEF456...)
   ```

## Configuración Actual

### Contraseña de Base de Datos
- **Texto original**: `ingesoft26`
- **Texto cifrado**: `1ZWVwsgdP3MMf6aAg4WRkeoKwW6hG0eB`
- **Ubicación**: `app/src/main/resources/application.properties`

## Flujo de Desencriptación

```
1. Spring Boot inicia
   ↓
2. @EnableEncryptableProperties detecta propiedades con ENC(...)
   ↓
3. Lee variable de entorno JASYPT_ENCRYPTOR_PASSWORD
   ↓
4. Desencripta automáticamente los valores
   ↓
5. Inyecta valores desencriptados en la aplicación
```

## Archivos Modificados/Creados

- `.env.example` - Plantilla para variables de entorno
- `app/pom.xml` - Dependencia `jasypt-spring-boot-starter`
- `app/src/main/resources/application.properties` - Contraseña cifrada
- `app/src/main/java/pe/edu/pucp/kingstore/config/JasyptConfig.java` - Configuración segura
- `app/src/main/java/pe/edu/pucp/kingstore/util/JasyptEncryptorUtil.java` - Utilidad de cifrado

## Control de Versiones

### Lo que SÍ va a Git:
- `.gitignore` (con `.env` incluido)
- `.env.example` (sin valores reales)
- Código fuente con propiedades cifradas
- `ENCRYPTION_GUIDE.md`

### Lo que NO va a Git:
- `.env` (contiene la clave de encriptación)
- Archivo compilado con claves en memoria

### Verificar que `.env` no está en Git:
```bash
git status | grep ".env"
# No debería mostrar nada

git ls-files | grep ".env"
# Debería mostrar solo .env.example, no .env
```

## Troubleshooting

### Error: "JASYPT_ENCRYPTOR_PASSWORD no está definida"
```bash
# Solución:
export $(cat .env | xargs)
./mvnw spring-boot:run
```

### Error: "Cannot decrypt property"
- Verificar que estés usando la misma clave para descifrar que la usada para cifrar
- Cambiar la clave requiere re-cifrar TODAS las propiedades

### ¿Qué pasa si alguien obtiene mi `.env`?
- Cambiar inmediatamente la clave
- Re-cifrar todas las propiedades sensibles
- Actualizar la nueva clave en producción

## Consideraciones de Seguridad

✅ **Seguro:**
- Clave en variable de entorno (no en código)
- Propiedades cifradas en el repositorio
- Diferentes claves por ambiente

❌ **Inseguro:**
- Clave hardcodeada en `application.properties`
- Clave en el repositorio Git
- Usar la misma clave en todos los ambientes

## Para Producción

1. Generar una clave segura (mínimo 32 caracteres):
   ```bash
   openssl rand -hex 16  # Genera 32 caracteres hexadecimales
   ```

2. Almacenar en gestor de secretos (AWS Secrets Manager, Azure Key Vault, etc.)
   O como variable de entorno en el servidor/contenedor

3. Re-cifrar todas las propiedades con la nueva clave

4. Verificar que `.env` nunca se copie a producción

