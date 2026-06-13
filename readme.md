# GUIA DE TRABAJO

## Overview
Esta guia es para poder orientar a los desarrolladores el flujo de trabajo para la integración del proyecto.
Principalmente contamos con 2 ramas base
1. Development
2. Main

## Development
Esta rama está orientada para el desarrollo de tareas y pruebas antes de agregarlo al producto final.
Los desarrolladores tendrán que crear sus ramas basadas en esta para poder realizar el proceso de **integración**

## Main
Está es la rama produción y se usará unicamente para las presentaciones del proyecto. Todo lo que se integre aca tendrá que estar debidamente probado en **Development**

![Flujo de trabajo en repositorio](diagramas/git_workflow_springboot.svg)

## Flujo del Proceso

### 1. Creación de la rama de trabajo
```bash
git checkout Development
git pull origin Development
git checkout -b KS-feature-description
```

### 2. Desarrollo y regular Rebasing

* El desarrollador debe trabajar en su rama creada para el respectivo task que se le asignó
* Ejecuten un rebase cada día o cuando se le informé que la rama **Development** fue actualizada

```bash
git fetch origin
git rebase -i origin/Development
```

* Cree commits constantemente con mensajes claros para tener conocimiento de lo desarrollado
```bash
git commit -m "KS-feature-description: implement user authentication"
```

### 3. Push & Create Pull Request

```bash
git push origin feature/JIRA-123-feature-description
```

* Crear un Pull Request hacia **Development**
* Agregue detalles sobre lo que se hizo para ese ticket

---

## Despliegue del Backend

El backend es una aplicación Spring Boot multi-módulo (Maven). Se despliega como contenedor Docker en EC2 y se conecta a una base de datos MySQL en AWS RDS.

### Arquitectura

```
Internet → EC2:8080
              │
         Spring Boot (Docker)
              │
         AWS RDS MySQL
```

### Requisitos previos

- Java 21
- Maven (o usar el wrapper `./mvnw` incluido en el repo)
- Docker instalado y sesión iniciada en Docker Hub (`docker login`)

### Datos del servidor

| Campo | Valor |
|-------|-------|
| IP EC2 | `52.205.138.95` |
| Puerto backend | `8080` |
| Imagen Docker Hub | `bryanpisco/kingstore-backend:latest` |
| Conexión SSH | `ssh -i "ingesoft_key.pem" ubuntu@ec2-52-205-138-95.compute-1.amazonaws.com` |

---

### Primer despliegue en el servidor (solo una vez)

**1. Conectarse al servidor:**
```bash
ssh -i "ingesoft_key.pem" ubuntu@ec2-52-205-138-95.compute-1.amazonaws.com
```

**2. Crear el archivo `.env` con las credenciales del backend:**

> Este archivo nunca se sube al repositorio. Contiene los secretos necesarios para que Spring Boot arranque correctamente.

```bash
cat > .env << 'EOF'
JASYPT_ENCRYPTOR_PASSWORD=kingstore-secret-key-2024
JWT_SECRET=kingstore-secret-key-ingesoft-2026
SPRING_DATASOURCE_PASSWORD=<password_de_rds>
EOF
```

**3. Copiar el `docker-compose.yml` al servidor (desde tu máquina local):**
```bash
scp -i "ingesoft_key.pem" \
    ../Frontend/docker-compose.yml \
    ubuntu@ec2-52-205-138-95.compute-1.amazonaws.com:~/docker-compose.yml
```

**4. Levantar los contenedores:**
```bash
docker compose pull
docker compose up -d
docker compose ps   # verificar que el backend esté "Up"
```

---

### Publicar nueva versión (tras cada cambio de código)

Ejecutar desde la raíz del proyecto backend (`/Backend`):

**1. Compilar el JAR:**
```bash
./mvnw clean package -DskipTests
```
> El JAR generado queda en `app/target/*.jar`. El flag `-DskipTests` omite los tests para agilizar el build.

**2. Construir la imagen Docker:**
```bash
docker build -t bryanpisco/kingstore-backend:latest .
```

**3. Subir la imagen a Docker Hub:**
```bash
docker push bryanpisco/kingstore-backend:latest
```

**4. Actualizar el servidor:**
```bash
ssh -i "ingesoft_key.pem" ubuntu@ec2-52-205-138-95.compute-1.amazonaws.com

docker compose pull backend      # descarga la imagen nueva
docker compose up -d backend     # reinicia solo el contenedor del backend
docker compose logs backend -f   # ver logs en tiempo real para confirmar que arrancó
```

---

### Ver logs del backend

```bash
# Últimas 50 líneas
docker compose logs backend --tail=50

# En tiempo real (Ctrl+C para salir)
docker compose logs backend -f

# Logs de todos los servicios en tiempo real
docker compose logs -f
```

---

### Apagar el backend

```bash
ssh -i "ingesoft_key.pem" ubuntu@ec2-52-205-138-95.compute-1.amazonaws.com
```

**Opción 1 — Detener solo el backend (el resto sigue corriendo):**
```bash
docker compose stop backend
```

**Opción 2 — Detener y eliminar el contenedor del backend:**
```bash
docker compose down backend
```

**Opción 3 — Apagar todos los servicios (frontend + backend):**
```bash
docker compose down
```
> Los datos en RDS y las imágenes Docker no se eliminan. Solo se detienen los contenedores.

**Opción 4 — Apagar todo y limpiar imágenes (fuerza re-descarga en el próximo despliegue):**
```bash
docker compose down --rmi all
```

---

### Variables de entorno del backend

| Variable | Descripción |
|----------|-------------|
| `SPRING_PROFILES_ACTIVE` | Perfil activo. Usar `prod` en el servidor |
| `JASYPT_ENCRYPTOR_PASSWORD` | Clave para desencriptar propiedades sensibles en `application.properties` |
| `JWT_SECRET` | Clave para firmar y verificar tokens JWT |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de la base de datos MySQL en RDS |

> Las credenciales AWS (S3) las provee automáticamente el IAM Role asociado a la instancia EC2. No se configuran manualmente.
