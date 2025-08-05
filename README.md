# 🍽️ NathBit POS - Sistema de Punto de Venta para Restaurantes

Sistema POS moderno y eficiente diseñado para restaurantes con soporte multi-empresa y multi-sucursal, sin la complejidad de multi-tenancy.

## 📋 Descripción

NathBit POS es una solución integral que permite:
- 🏪 Gestión multi-empresa y multi-sucursal con base de datos única
- 📊 Control de inventarios por sucursal
- 💰 Sistema de punto de venta ágil
- 🧾 Integración con facturación electrónica (Hacienda CR)
- 👥 Gestión flexible de usuarios y permisos
- 📈 Reportes consolidados y por sucursal
- 🪑 Control de mesas y zonas
- 📱 API REST para integraciones

## 🏗️ Arquitectura Simplificada

### **Conceptos Clave**
- **Base de Datos Única**: Todo en PostgreSQL sin schemas separados
- **Usuarios Flexibles**: Un usuario puede tener diferentes roles en diferentes empresas/sucursales
- **Permisos Granulares**: Control detallado de qué puede hacer cada usuario

### **Modelo de Datos Principal**

```sql
usuarios (tabla única)
├── id
├── email (único)
├── password (bcrypt)
├── nombre
├── apellidos
├── telefono
├── identificacion
├── activo
└── ultimo_acceso

usuarios_empresas_roles
├── id
├── usuario_id (FK)
├── empresa_id (FK)
├── sucursal_id (FK, nullable - null = todas las sucursales)
├── rol (ENUM)
├── permisos (JSON)
├── es_principal (boolean)
└── activo

empresas
├── id
├── codigo (único)
├── nombre
├── nombre_comercial
├── cedula_juridica
├── configuracion (JSON)
└── activa

sucursales
├── id
├── empresa_id (FK)
├── codigo (único por empresa)
├── nombre
├── direccion
├── telefono
├── configuracion (JSON)
└── activa
```

## 👥 Roles y Permisos

### **Roles del Sistema**

| Rol | Descripción | Alcance |
|-----|-------------|---------|
| **ROOT** | Desarrollador del sistema | Acceso total |
| **SUPER_ADMIN** | Administrador SNN | Todas las empresas |
| **ADMIN** | Dueño/Administrador | Su empresa completa |
| **JEFE_CAJAS** | Supervisor de cajas | Sucursales asignadas |
| **CAJERO** | Operador de POS | Su sucursal |
| **MESERO** | Toma pedidos | Su sucursal |
| **COCINA** | Vista de cocina | Su sucursal |

### **Sistema de Permisos JSON**

```json
{
  "productos": { "ver": true, "crear": true, "editar": true, "eliminar": false },
  "ordenes": { "ver": true, "crear": true, "editar": true, "eliminar": false },
  "caja": { "abrir": true, "cerrar": true, "arqueo": true },
  "reportes": { "ventas": true, "inventario": false, "empleados": false },
  "clientes": { "ver": true, "crear": true, "editar": false },
  "descuentos": { "aplicar": true, "autorizar": false }
}
```

## 🔐 Flujo de Autenticación

### **1. Login Unificado**
```
POST /api/auth/login
{
  "email": "usuario@email.com",
  "password": "********"
}
```

### **2. Respuesta según tipo de usuario**

**Para Operativos (Cajero/Mesero):**
```json
{
  "token": "jwt...",
  "usuario": { ... },
  "acceso": {
    "tipo": "OPERATIVO",
    "empresa": { "id": 1, "nombre": "Restaurante XYZ" },
    "sucursal": { "id": 1, "nombre": "Sucursal Centro" },
    "rol": "CAJERO",
    "permisos": { ... }
  }
}
```

**Para Administrativos (Admin/Jefe):**
```json
{
  "token": "jwt...",
  "usuario": { ... },
  "accesos": [
    {
      "empresa": { "id": 1, "nombre": "Restaurante XYZ" },
      "sucursales": [
        { "id": 1, "nombre": "Centro" },
        { "id": 2, "nombre": "Norte" }
      ],
      "rol": "ADMIN",
      "permisos": { ... }
    }
  ],
  "requiereSeleccion": true
}
```

### **3. Selección de Contexto (si aplica)**
```
POST /api/auth/seleccionar-contexto
{
  "empresaId": 1,
  "sucursalId": 2  // opcional
}
```

## 🚀 Tecnologías

- **Backend**: Java 17 + Spring Boot 3.5.4
- **Base de Datos**: PostgreSQL 14+ (base única)
- **Seguridad**: Spring Security + JWT
- **Migraciones**: Flyway
- **Mapeo**: MapStruct + JPA/Hibernate
- **Documentación**: OpenAPI/Swagger
- **Cache**: Spring Cache
- **Build**: Gradle

## 🛠️ Instalación

### **Prerequisitos**
- Java 17+
- PostgreSQL 14+
- Git

### **1. Clonar Repositorio**
```bash
git clone https://github.com/snnsoluciones/nathbit-pos.git
cd nathbit-pos
```

### **2. Configurar Base de Datos**
```sql
-- Crear base de datos
CREATE DATABASE nathbitpos;
```

### **3. Variables de Entorno**
```bash
# Copiar archivo de ejemplo
cp .env.example .env

# Editar con tus valores
DB_HOST=localhost
DB_PORT=5432
DB_NAME=nathbitpos
DB_USER=postgres
DB_PASSWORD=tu_password
JWT_SECRET=genera_una_clave_segura_aqui
```

### **4. Ejecutar**
```bash
# Desarrollo
./gradlew bootRun

# Producción
./gradlew build
java -jar build/libs/nathbitpos-0.0.1-SNAPSHOT.jar
```

## 📚 API Endpoints

### **Autenticación**
```
POST   /api/auth/login                    # Login
POST   /api/auth/refresh                  # Renovar token
POST   /api/auth/logout                   # Cerrar sesión
POST   /api/auth/seleccionar-contexto     # Seleccionar empresa/sucursal
```

### **Gestión de Usuarios**
```
GET    /api/usuarios/perfil               # Mi perfil
PUT    /api/usuarios/perfil               # Actualizar perfil
GET    /api/usuarios                      # Listar usuarios (admin)
POST   /api/usuarios                      # Crear usuario
PUT    /api/usuarios/{id}                 # Actualizar usuario
DELETE /api/usuarios/{id}                 # Eliminar usuario
POST   /api/usuarios/{id}/asignar-rol     # Asignar rol a empresa/sucursal
```

### **Empresas y Sucursales**
```
GET    /api/empresas                      # Mis empresas
GET    /api/empresas/{id}/sucursales      # Sucursales de una empresa
GET    /api/sucursales/{id}               # Detalle sucursal
```

## 🎯 Casos de Uso

### **Caso 1: Juan - Dueño de Restaurante**
- Tiene rol ADMIN en "Restaurante El Sabor"
- Ve todas las sucursales (Centro, Norte, Sur)
- Puede gestionar todo: usuarios, productos, reportes

### **Caso 2: María - Cajera y Jefa de Cajas**
- Rol CAJERO en Sucursal Centro
- Rol JEFE_CAJAS en Sucursal Norte
- Al login, elige dónde trabajar hoy

### **Caso 3: Pedro - Mesero Simple**
- Rol MESERO en Sucursal Centro
- Login directo al POS
- Solo ve mesas y puede tomar órdenes

## 🔧 Estructura del Proyecto

```
src/main/java/com/snnsoluciones/backnathbitpos/
├── config/
│   ├── security/          # JWT, Spring Security
│   └── web/              # CORS, etc
├── controller/
│   ├── auth/            # Login, permisos
│   ├── usuario/         # Gestión usuarios
│   └── empresa/         # Empresas/Sucursales
├── service/
│   ├── auth/            # Lógica autenticación
│   ├── usuario/         # Lógica usuarios
│   └── permiso/         # Validación permisos
├── repository/
│   ├── UsuarioRepository
│   ├── EmpresaRepository
│   └── SucursalRepository
├── entity/
│   ├── Usuario
│   ├── Empresa
│   ├── Sucursal
│   └── UsuarioEmpresaRol
├── dto/                 # Request/Response
├── exception/           # Manejo errores
└── util/               # Utilidades
```

## 🚧 Roadmap

### **Fase 1: Base del Sistema** (En progreso)
- [x] Estructura inicial Spring Boot
- [ ] Modelo de usuarios/empresas/roles
- [ ] Sistema de autenticación JWT
- [ ] Gestión de permisos
- [ ] CRUD usuarios
- [ ] Asignación de roles

### **Fase 2: Gestión Empresarial**
- [ ] CRUD Empresas
- [ ] CRUD Sucursales
- [ ] Dashboard por rol
- [ ] Cambio de contexto

### **Fase 3: Módulos de Negocio**
- [ ] Productos
- [ ] Categorías
- [ ] Inventario
- [ ] Clientes

### **Fase 4: Punto de Venta**
- [ ] Órdenes
- [ ] Mesas
- [ ] Caja
- [ ] Facturación

## 📄 Licencia

Copyright © 2025 SNN Soluciones. Todos los derechos reservados.

---

Desarrollado con ❤️ por SNN Soluciones en Costa Rica 🇨🇷