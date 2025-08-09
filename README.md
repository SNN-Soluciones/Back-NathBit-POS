# 🏪 NathBit POS - Backend

Sistema de punto de venta multi-empresa y multi-sucursal desarrollado con Spring Boot.

## 📋 Descripción

NathBit POS es un sistema integral de punto de venta diseñado para manejar múltiples empresas y sucursales con un modelo de permisos jerárquico simplificado. Cada usuario tiene un rol único en el sistema que determina su nivel de acceso.

## 🚀 Tecnologías

- **Java 17**
- **Spring Boot 3.x**
- **Spring Security + JWT**
- **PostgreSQL**
- **JPA/Hibernate**
- **Gradle**

## 🏗️ Arquitectura

### Modelo de Permisos
- **Un usuario = Un rol global** (simplificado)
- **Contexto dinámico** por empresa/sucursal
- **Token JWT** para autenticación
- **Navegación en cascada** según nivel de acceso

### Roles del Sistema

| Rol | Nivel | Descripción |
|-----|-------|-------------|
| ROOT | Sistema | Acceso total a todas las empresas |
| SOPORTE | Sistema | Similar a ROOT sin configuraciones críticas |
| SUPER_ADMIN | Empresarial | Gestiona sus propias empresas |
| ADMIN | Gerencial | Gestiona sucursales asignadas |
| CAJERO | Operativo | Funciones de venta |
| MESERO | Operativo | Gestión de mesas |
| COCINERO | Operativo | Vista de cocina |
| JEFE_CAJAS | Operativo | Supervisión de cajas |

## 🛠️ Instalación

### Prerequisitos
- Java 17+
- PostgreSQL 14+
- Gradle 7+

### Configuración

1. **Clonar el repositorio**
```bash
git clone https://github.com/tuusuario/back-nathbit-pos.git
cd back-nathbit-pos
```

2. **Configurar base de datos**
```sql
CREATE DATABASE nathbitpos;
```

3. **Configurar variables de entorno**
```bash
# Crear archivo .env o configurar en application.yml
DB_USER=postgres
DB_PASSWORD=tu_password
JWT_SECRET=tu_clave_secreta_segura
```

4. **Ejecutar la aplicación**
```bash
./gradlew bootRun
```

## 📚 API Endpoints

### Autenticación
- `POST /api/auth/login` - Login de usuario
- `POST /api/auth/contexto` - Establecer contexto empresa/sucursal
- `POST /api/auth/refresh` - Refrescar token

### Empresas
- `GET /api/empresas` - Listar empresas
- `GET /api/empresas/{id}` - Obtener empresa
- `POST /api/empresas` - Crear empresa
- `PUT /api/empresas/{id}` - Actualizar empresa
- `DELETE /api/empresas/{id}` - Eliminar empresa

### Sucursales
- `GET /api/sucursales/empresa/{empresaId}` - Listar sucursales por empresa
- `GET /api/sucursales/{id}` - Obtener sucursal
- `POST /api/sucursales` - Crear sucursal
- `PUT /api/sucursales/{id}` - Actualizar sucursal
- `DELETE /api/sucursales/{id}` - Eliminar sucursal

### Usuarios
- `GET /api/usuarios` - Listar usuarios
- `GET /api/usuarios/{id}` - Obtener usuario
- `POST /api/usuarios` - Crear usuario
- `PUT /api/usuarios/{id}` - Actualizar usuario
- `DELETE /api/usuarios/{id}` - Eliminar usuario
- `POST /api/usuarios/asignar` - Asignar a empresa/sucursal
- `GET /api/usuarios/perfil` - Mi perfil

## 🔒 Seguridad

- Autenticación mediante JWT
- Roles y permisos por endpoint
- Validación de contexto empresa/sucursal
- Encriptación de contraseñas con BCrypt

## 👥 Contribuidores

- Equipo de desarrollo NathBit

## 📄 Licencia

Proprietary - Todos los derechos reservados