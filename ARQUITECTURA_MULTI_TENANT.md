# 🏗️ NathBit POS — Arquitectura Multi-Tenant

> Documento maestro de arquitectura y estado de implementación
> Versión: 2.0 — Revisión completa
> Fecha: Abril 2026
> Autor: Andrés / SNN Soluciones

---

## 📋 Índice

1. [Resumen Ejecutivo](#1-resumen-ejecutivo)
2. [Decisiones de Arquitectura](#2-decisiones-de-arquitectura)
3. [Estructura de Datos](#3-estructura-de-datos)
4. [Sistema de Autenticación Dual](#4-sistema-de-autenticación-dual)
5. [Flujo de Requests](#5-flujo-de-requests)
6. [Estado Actual del Código](#6-estado-actual-del-código)
7. [Plan de Migración](#7-plan-de-migración)
8. [Fases de Implementación](#8-fases-de-implementación)
9. [API Endpoints](#9-api-endpoints)
10. [Convenciones de Código](#10-convenciones-de-código)
11. [Qué Eliminar](#11-qué-eliminar)

---

## 1. Resumen Ejecutivo

### 1.1 Objetivo

NathBit POS opera con una arquitectura legacy donde todos los clientes comparten el schema `public` de PostgreSQL, diferenciados únicamente por `empresa_id` en cada tabla. El objetivo es migrar a **PostgreSQL Schemas por tenant**, donde cada cliente tiene su propio schema aislado (`tenant_{codigo}`).

### 1.2 Contexto

- **Clientes en producción:** 10+ con datos reales
- **Estrategia de migración:** Rolling — empresa por empresa, comenzando con `empresa_id = 1` (SNN Soluciones, ambiente controlado)
- **Fases paralelas:** Fase A (separación en tenants) y Fase B (auth dual) se desarrollan en el mismo sprint

### 1.3 Beneficios

| Beneficio | Descripción |
|-----------|-------------|
| **Aislamiento total** | Datos completamente separados entre clientes |
| **Backups independientes** | Respaldo por tenant sin afectar otros |
| **Onboarding limpio** | Nuevos clientes van directo al nuevo sistema |
| **Seguridad** | Un tenant jamás accede a datos de otro |
| **Simplicidad futura** | Sin `empresa_id` en cada query |

### 1.4 Stack tecnológico

| Componente | Tecnología |
|------------|------------|
| Backend | Java 17 + Spring Boot 3.x |
| Base de datos | PostgreSQL (Schemas) |
| Auth | JWT (dos tipos: GLOBAL y PDV) |
| Email OTP | Resend |
| Frontend | Angular + Capacitor |

---

## 2. Decisiones de Arquitectura

### 2.1 Separación de datos

| Decisión | Valor | Justificación |
|----------|-------|---------------|
| Estrategia | **Schema por Tenant** | Aislamiento real sin complejidad de BD múltiples |
| Base de datos | Una sola BD PostgreSQL | Simplifica conexiones y mantenimiento |
| Schema master | `public` | Datos globales: tenants, usuarios_globales, dispositivos |
| Schema tenant | `tenant_{codigo}` | Datos de negocio del cliente, completamente aislados |

### 2.2 Dos modos de acceso — decisión central

El sistema tiene **dos modos de acceso completamente separados**, cada uno con su propio flujo de auth y tipo de JWT:

| Modo | Canal | Usuarios | Auth | JWT |
|------|-------|----------|------|-----|
| **Web** | Navegador / Chrome | ROOT, SOPORTE, SUPER_ADMIN | Email + Password | `JWT_GLOBAL` |
| **PDV** | Dispositivo físico | ADMIN, JEFE_CAJAS, CAJERO, MESERO, COCINERO | Device Token + PIN | `JWT_PDV` |

**Regla clave:** El modo se determina por la presencia del header `X-Device-Token`.
- Con `X-Device-Token` → flujo PDV
- Sin `X-Device-Token` → flujo Web

### 2.3 Modelo de usuarios

| Tipo | Schema | Auth | Alcance |
|------|--------|------|---------|
| ROOT | `public.usuarios_globales` | Email + Password | Cross-tenant, todo el sistema |
| SOPORTE | `public.usuarios_globales` | Email + Password | Cross-tenant, sin destructivos |
| SUPER_ADMIN | `public.usuarios_globales` | Email + Password | Sus tenants asignados |
| ADMIN | `{tenant}.usuarios` | PIN | Solo su tenant, todas las sucursales |
| JEFE_CAJAS | `{tenant}.usuarios` | PIN | Solo su sucursal |
| CAJERO | `{tenant}.usuarios` | PIN | Solo su sucursal |
| MESERO | `{tenant}.usuarios` | PIN | Solo su sucursal |
| COCINERO | `{tenant}.usuarios` | PIN | Solo su sucursal |

### 2.4 Resolución de tenant por request

El `TenantInterceptor` resuelve el schema en este orden de prioridad:

```
1. Header X-Device-Token → busca Dispositivo → obtiene Tenant → schema
2. Header X-Tenant-Code  → busca Tenant por código → schema
3. JWT (empresaId legacy) → busca Tenant por empresaLegacyId → schema
4. Default: schema public (endpoints globales)
```

---

## 3. Estructura de Datos

### 3.1 Diagrama general

```
PostgreSQL Database
│
├── SCHEMA: public (Master)
│   ├── tenants                    → Registro de cada tenant
│   ├── usuarios_globales          → ROOT, SOPORTE, SUPER_ADMIN
│   ├── super_admin_tenants        → Relación SUPER_ADMIN ↔ Tenants
│   ├── dispositivos               → Dispositivos físicos registrados
│   ├── codigos_registro           → OTP temporales para registro de dispositivo
│   ├── ─────────────────────────────────
│   ├── provincias, cantones, distritos, barrios  → Catálogos CR
│   ├── actividades_economicas                    → Catálogo Hacienda
│   └── codigos_cabys                             → Catálogo Hacienda
│
├── SCHEMA: tenant_snn_soluciones
│   ├── empresa (1 registro)
│   ├── sucursales
│   ├── usuarios  (ADMIN, CAJERO, etc. — con PIN)
│   ├── productos, categorias_producto
│   ├── clientes, proveedores
│   ├── facturas, facturas_detalle
│   ├── sesiones_caja, terminales
│   └── ... (resto de tablas operativas)
│
└── SCHEMA: tenant_{codigo_cliente}
    └── (misma estructura que arriba)
```

### 3.2 Tablas del schema `public`

#### `tenants`
```sql
CREATE TABLE public.tenants (
                              id              BIGSERIAL PRIMARY KEY,
                              codigo          VARCHAR(50) UNIQUE NOT NULL,   -- "snn_soluciones"
                              nombre          VARCHAR(200) NOT NULL,
                              schema_name     VARCHAR(100) UNIQUE NOT NULL,  -- "tenant_snn_soluciones"
                              empresa_legacy_id BIGINT UNIQUE,               -- para migración desde sistema viejo
                              activo          BOOLEAN NOT NULL DEFAULT true,
                              config          JSONB DEFAULT '{}',
                              created_at      TIMESTAMP DEFAULT NOW(),
                              updated_at      TIMESTAMP DEFAULT NOW()
);
```

#### `usuarios_globales`
```sql
CREATE TABLE public.usuarios_globales (
                                        id              BIGSERIAL PRIMARY KEY,
                                        email           VARCHAR(200) UNIQUE NOT NULL,
                                        password        VARCHAR(255) NOT NULL,         -- BCrypt
                                        nombre          VARCHAR(100) NOT NULL,
                                        apellidos       VARCHAR(100),
                                        rol             VARCHAR(20) NOT NULL,          -- ROOT | SOPORTE | SUPER_ADMIN
                                        activo          BOOLEAN NOT NULL DEFAULT true,
                                        requiere_cambio_password BOOLEAN DEFAULT false,
                                        ultimo_acceso   TIMESTAMP,
                                        created_at      TIMESTAMP DEFAULT NOW(),
                                        updated_at      TIMESTAMP DEFAULT NOW()
);
```

#### `dispositivos`
```sql
CREATE TABLE public.dispositivos (
                                   id              BIGSERIAL PRIMARY KEY,
                                   token           UUID UNIQUE NOT NULL,          -- device token permanente
                                   nombre          VARCHAR(100) NOT NULL,
                                   tenant_id       BIGINT NOT NULL REFERENCES public.tenants(id),
                                   sucursal_id     BIGINT NOT NULL,               -- ID en el schema del tenant
                                   plataforma      VARCHAR(20),                   -- WEB | ANDROID | IOS | DESKTOP
                                   activo          BOOLEAN NOT NULL DEFAULT true,
                                   ip_registro     VARCHAR(45),
                                   ultimo_uso      TIMESTAMP,
                                   created_at      TIMESTAMP DEFAULT NOW()
);
```

> **Nota importante:** `dispositivos.sucursal_id` referencia una sucursal dentro del schema del tenant. No hay FK real porque la sucursal vive en otro schema.

#### `codigos_registro`
```sql
CREATE TABLE public.codigos_registro (
                                       id              BIGSERIAL PRIMARY KEY,
                                       tenant_id       BIGINT NOT NULL REFERENCES public.tenants(id),
                                       nombre_dispositivo VARCHAR(100) NOT NULL,
                                       codigo          VARCHAR(10) NOT NULL,          -- OTP 6 dígitos
                                       usado           BOOLEAN NOT NULL DEFAULT false,
                                       expira_en       TIMESTAMP NOT NULL,            -- 10 minutos
                                       ip_solicitud    VARCHAR(45),
                                       created_at      TIMESTAMP DEFAULT NOW()
);
```

#### `super_admin_tenants`
```sql
CREATE TABLE public.super_admin_tenants (
                                          id              BIGSERIAL PRIMARY KEY,
                                          usuario_id      BIGINT NOT NULL REFERENCES public.usuarios_globales(id),
                                          tenant_id       BIGINT NOT NULL REFERENCES public.tenants(id),
                                          es_propietario  BOOLEAN NOT NULL DEFAULT false,
                                          activo          BOOLEAN NOT NULL DEFAULT true,
                                          UNIQUE(usuario_id, tenant_id)
);
```

### 3.3 Modificaciones requeridas en tablas del tenant

Al crear el schema de un tenant, las tablas NO llevan `empresa_id`. Ese campo es solo del sistema legacy. El aislamiento lo da el schema.

```sql
-- En el schema del tenant, la tabla empresa tiene 1 solo registro
CREATE TABLE {schema}.empresa (
                                id      BIGSERIAL PRIMARY KEY,
  -- todos los campos de empresa, SIN empresa_id externo
                                ...
);

-- usuarios del tenant (con PIN en lugar de password)
CREATE TABLE {schema}.usuarios (
                                 id              BIGSERIAL PRIMARY KEY,
                                 nombre          VARCHAR(100) NOT NULL,
  apellidos       VARCHAR(100),
  email           VARCHAR(200),
  rol             VARCHAR(20) NOT NULL,
  pin             VARCHAR(255),                  -- BCrypt del PIN
  pin_longitud    INTEGER DEFAULT 4,
  requiere_cambio_pin BOOLEAN DEFAULT true,
  activo          BOOLEAN DEFAULT true,
  sucursal_id     BIGINT,                        -- sucursal asignada (para roles operativos)
  created_at      TIMESTAMP DEFAULT NOW()
  );

-- resto de tablas sin empresa_id
```

---

## 4. Sistema de Autenticación Dual

### 4.1 JWT_GLOBAL — Acceso Web

**Quién lo usa:** ROOT, SOPORTE, SUPER_ADMIN

**Cómo se obtiene:**
```
POST /api/auth/global/login
Body: { email, password }
```

**Claims del token:**
```json
{
  "userId": 1,
  "email": "admin@snn.cr",
  "rol": "SUPER_ADMIN",
  "tenantId": 2,
  "type": "GLOBAL"
}
```

**Qué puede hacer:** Acceso a todas las sucursales del tenant. Configuración. Reportes cross-sucursal.

**Expiración:** 8 horas (configurable)

---

### 4.2 JWT_PDV — Acceso Dispositivo

**Quién lo usa:** ADMIN, JEFE_CAJAS, CAJERO, MESERO, COCINERO

**Cómo se obtiene:**

```
Flujo de registro de dispositivo (primera vez):
1. POST /api/auth/empresa           → valida código del tenant
2. POST /api/auth/dispositivo/solicitar → envía OTP al email del SUPER_ADMIN
3. POST /api/auth/dispositivo/verificar → valida OTP → genera device token (UUID permanente)

Login diario:
4. GET /api/auth/dispositivo/usuarios   [X-Device-Token: uuid]  → lista usuarios de la sucursal
5. POST /api/auth/pin                   [X-Device-Token: uuid]  → PIN → JWT_PDV
```

**Claims del token:**
```json
{
  "userId": 5,
  "rol": "CAJERO",
  "tenantId": 2,
  "sucursalId": 3,
  "type": "PDV"
}
```

**Qué puede hacer:** Solo su sucursal. Pedidos, mesas, sesión de caja, reportes de esa sucursal.

**Expiración:** 8 horas (se renueva en cada login con PIN)

---

### 4.3 Validación por tipo de JWT

El `JwtAuthenticationFilter` debe validar el claim `type` contra el endpoint que se accede:

```
Endpoints /api/auth/global/**  → requieren type=GLOBAL
Endpoints /api/pdv/**          → requieren type=PDV o device token válido
Endpoints /api/admin/**        → requieren type=GLOBAL + rol ROOT|SOPORTE|SUPER_ADMIN
Endpoints /api/**              → cualquier JWT válido (sucursalId tomado del token)
```

**Importante:** El `sucursalId` ya NO se envía como header separado. Viene dentro del `JWT_PDV`. El `ContextHeaderFilter` es eliminable.

---

## 5. Flujo de Requests

### 5.1 Request desde Web (SUPER_ADMIN)

```
Browser → GET /api/sucursales
  │
  ├─ JwtAuthenticationFilter
  │    Extrae JWT, valida, pone ContextoUsuario en SecurityContext
  │    ContextoUsuario { userId, rol=SUPER_ADMIN, tenantId, type=GLOBAL }
  │
  ├─ TenantInterceptor
  │    No hay X-Device-Token → intenta por JWT legacy empresaId → busca tenant
  │    TenantContext.setTenant(tenantId, "tenant_snn_soluciones")
  │
  ├─ TenantConnectionProvider
  │    Hibernate llama getConnection("tenant_snn_soluciones")
  │    SET search_path TO tenant_snn_soluciones, public
  │
  └─ SucursalRepository.findAll()
       JPA ejecuta sobre tenant_snn_soluciones.sucursales
       Retorna TODAS las sucursales del tenant ✅
```

### 5.2 Request desde PDV (CAJERO)

```
Tablet → POST /api/pedidos  [X-Device-Token: uuid-abc]  [Authorization: Bearer JWT_PDV]
  │
  ├─ JwtAuthenticationFilter
  │    Extrae JWT_PDV, valida
  │    ContextoUsuario { userId=5, rol=CAJERO, tenantId=2, sucursalId=3, type=PDV }
  │
  ├─ TenantInterceptor
  │    Tiene X-Device-Token → busca Dispositivo → obtiene Tenant
  │    TenantContext.setTenant(2, "tenant_snn_soluciones")
  │
  ├─ TenantConnectionProvider
  │    SET search_path TO tenant_snn_soluciones, public
  │
  └─ PedidoController
       Lee sucursalId del ContextoUsuario (viene del JWT_PDV)
       Filtra pedidos por sucursalId=3
       Sin headers adicionales necesarios ✅
```

---

## 6. Estado Actual del Código

### ✅ IMPLEMENTADO — Listo o casi listo

#### Infraestructura Tenant
- [x] `TenantContext.java` — ThreadLocal para schema y tenantId
- [x] `TenantConnectionProvider.java` — Hibernate MultiTenantConnectionProvider con SET search_path
- [x] `TenantIdentifierResolver.java` — CurrentTenantIdentifierResolver
- [x] `TenantInterceptor.java` — 3 estrategias: device token / X-Tenant-Code header / JWT legacy
- [x] `Tenant.java` — Entidad completa con todos los campos
- [x] `TenantRepository.java` — Queries completas incluyendo búsqueda por empresaLegacyId
- [x] `DispositivoRepository.java` — CRUD completo
- [x] `TenantService.java` — CRUD y lógica de creación de schema (parcial)
- [x] `TenantAdminController.java` — Endpoints CRUD para ROOT/SOPORTE

#### Entidades Global
- [x] `Dispositivo.java`
- [x] `UsuarioGlobal.java`
- [x] `SuperAdminTenant.java`
- [x] `CodigoRegistro.java`

#### Autenticación Web (JWT_GLOBAL)
- [x] `AuthGlobalService.java` — Login con email+password, soporte multi-tenant para SUPER_ADMIN
- [x] `AuthMultitenantController.java` — Endpoints /api/auth/global/login y selección de tenant

#### Autenticación PDV (JWT_PDV)
- [x] `AuthDispositivoService.java` — Flujo OTP de registro de dispositivo
- [x] `AuthPinService.java` — Login con PIN (usa JdbcTemplate directo — ver pendiente)
- [x] `AuthMultitenantController.java` — Endpoints /api/auth/dispositivo/**, /api/auth/pin

#### JWT
- [x] `JwtTokenProvider.java` — `generateToken`, `generateTokenWithContext`, `generateRefreshToken`
- [x] `JwtAuthenticationFilter.java` — Extrae userId, email, rol, empresaId, sucursalId del token

#### Security
- [x] `SecurityConfig.java` — Endpoints públicos multitenant ya declarados

---

### 🔧 MODIFICAR — Existe pero debe cambiar

- [ ] **`JwtTokenProvider.java`** — Agregar claim `type: GLOBAL | PDV` en todos los métodos de generación. Agregar `tenantId` directo (no solo `empresaId`). Agregar getter `getTypeFromToken()`.

- [ ] **`JwtAuthenticationFilter.java`** — Leer claim `type` del token y setearlo en `ContextoUsuario`. Leer `tenantId` del token (no solo empresaId). Validar que el tipo de token sea coherente con el endpoint.

- [ ] **`ContextoUsuario.java`** — Agregar campo `tokenType: String` (GLOBAL | PDV). Agregar campo `tenantId: Long` directo. Eliminar dependencia de `empresaId` para routing de tenant.

- [ ] **`AuthPinService.java`** — Actualmente usa `JdbcTemplate` para buscar usuarios por schema. Debe validarse si el `TenantConnectionProvider` funciona correctamente vía JPA primero; si sí, migrar a JPA. Si no, documentar por qué se mantiene JDBC.

- [ ] **`TenantInterceptor.java`** — En el fallback legacy (estrategia 3), usar `tenantId` del JWT si está disponible, antes de buscar por `empresaLegacyId`. Más eficiente.

- [ ] **`AuthGlobalService.java`** — Al generar el JWT, incluir `type=GLOBAL` y `tenantId` real (no empresaId negativo como workaround actual).

- [ ] **`DispositivoServiceImpl.java`** — `GenerarTokenRequest` todavía usa `empresaId`. Debe migrar a `tenantId` una vez que los clientes estén en el nuevo sistema.

- [ ] **`Usuario.java` (entidad tenant)** — Agregar campos `pin`, `pin_longitud`, `requiere_cambio_pin` si no los tiene. Estos son necesarios para el flujo PDV.

---

### ⬜ CREAR — No existe, hay que construir

#### Fase A — Tenant Split

- [ ] **Script validación post-migración** (`/sql/validate-migration.sql`)
  Compara conteos entre `public.{tabla} WHERE empresa_id = ?` y `{tenant}.{tabla}`.
  Confirma integridad antes de activar el tenant.

- [ ] **Script rollback** (`/sql/rollback-tenant.sql`)
  `DROP SCHEMA {tenant} CASCADE` + limpieza en `public.tenants`.
  Para ejecutar si la migración falla o hay problemas post-activación.

- [ ] **Verificar cobertura de `TABLAS_A_MIGRAR`**
  Comparar lista actual en `TenantMigrationService` contra todas las tablas de producción.
  Puede haber tablas nuevas no incluidas aún.

#### Fase B — Auth Split

- [ ] **Claim `type` en JWT** (modificación a `JwtTokenProvider`)
  Dos métodos nuevos:
  - `generateGlobalToken(userId, email, rol, tenantId)` → incluye `type=GLOBAL`
  - `generatePdvToken(userId, rol, tenantId, sucursalId)` → incluye `type=PDV`, sin email

- [ ] **Validación de tipo de JWT en endpoints sensibles**
  Anotación o interceptor que rechace `JWT_PDV` en endpoints web y viceversa.

#### Utilidades

- [ ] **Script de validación post-migración** (`/sql/validate-migration.sql`)
  Compara conteos entre `public.{tabla} WHERE empresa_id = ?` y `{tenant}.{tabla}`.

- [ ] **Script rollback** (`/sql/rollback-tenant.sql`)
  `DROP SCHEMA {tenant} CASCADE` + limpieza de registros en `public.tenants`.

---

### 🗑️ ELIMINAR — Existe pero sobra

- [ ] **`ContextHeaderFilter.java`** — Lee `X-Empresa-Id` y `X-Sucursal-Id` como headers. Con el nuevo sistema, `sucursalId` viene en el `JWT_PDV` y `tenantId` viene en ambos JWT. Este filtro es redundante y genera confusión. **Eliminar después de completar Fase B.**

- [ ] **`AuthPdvController.java`** — Controller legacy para login con PIN. Reemplazado completamente por `AuthMultitenantController`. **Eliminar después de validar que `AuthPinService` multitenant funciona end-to-end.**

- [ ] **`AuthPdvService.java` + `AuthPdvServiceImpl.java`** — Misma razón. La lógica de PIN ya está en `AuthPinService` multitenant. **Eliminar junto con el controller.**

- [ ] **`DispositivoServiceImpl.generateTokenRegistro()`** — El flujo de registro por token QR (`TokenRegistro`) es el sistema viejo. El nuevo flujo es por código OTP vía email (`CodigoRegistro`). **Evaluar si se mantiene para compatibilidad temporal o se elimina.**

- [ ] **Campos `empresa_id` en todas las tablas de negocio** — Solo después de que **todos** los clientes hayan migrado al nuevo schema. No antes. Durante la transición, `empresa_id` sigue existiendo en el schema `public` legacy.

- [ ] **`validarAccesoEmpresa()` y `validarAccesoSucursal()` en `ContextHeaderFilter`** — Ambos retornan `true` (TODO incompleto). Al eliminar el filtro, estos métodos desaparecen solos.

---

## 7. Plan de Migración

### 7.1 Estrategia: Rolling migration

```
empresa_id = 1  (SNN Soluciones — entorno controlado, podemos romper)
      ↓
Validar que todo funciona en producción
      ↓
empresa_id = X  (cliente real #1 — coordinar ventana de mantenimiento)
      ↓
empresa_id = Y  (cliente real #2)
      ↓
... uno por uno
      ↓
Cuando TODOS migrados → eliminar empresa_id de tablas
```

### 7.2 Proceso por empresa

```
INPUT: empresa_id = 1

PASO 1 — Generar código tenant
  Razón Social "SNN Soluciones S.A." → codigo: "snn_soluciones"
  Schema: "tenant_snn_soluciones"

PASO 2 — Registro en public.tenants
  INSERT INTO public.tenants (codigo, nombre, schema_name, empresa_legacy_id)
  VALUES ('snn_soluciones', 'SNN Soluciones', 'tenant_snn_soluciones', 1)

PASO 3 — Crear schema
  CREATE SCHEMA tenant_snn_soluciones;
  Ejecutar SQL template completo

PASO 4 — Migrar datos de negocio
  INSERT INTO tenant_snn.empresa     SELECT * FROM public.empresas WHERE id = 1
  INSERT INTO tenant_snn.sucursales  SELECT * FROM public.sucursales WHERE empresa_id = 1
  INSERT INTO tenant_snn.productos   SELECT * FROM public.productos WHERE empresa_id = 1
  INSERT INTO tenant_snn.clientes    SELECT * FROM public.clientes WHERE empresa_id = 1
  ... (todas las tablas)

PASO 5 — Migrar usuarios
  ROOT/SOPORTE/SUPER_ADMIN → public.usuarios_globales  (password se mantiene BCrypt)
  ADMIN/CAJERO/etc         → tenant_snn.usuarios        (PIN default: 123456 BCrypt)
                                                          requiere_cambio_pin = true

PASO 6 — Vincular SUPER_ADMIN
  INSERT INTO public.super_admin_tenants (usuario_id, tenant_id, es_propietario)

PASO 7 — Registrar dispositivos existentes
  Si hay dispositivos del sistema viejo, migrar sus tokens a public.dispositivos
  Vincular con tenant y sucursal correspondiente

PASO 8 — Validar integridad
  Ejecutar /sql/validate-migration.sql
  Verificar conteos por tabla
  Verificar que auth web funciona
  Verificar que auth PDV funciona

PASO 9 — Activar
  UPDATE public.tenants SET activo = true WHERE empresa_legacy_id = 1
  (el sistema comienza a rutear requests de empresa_id=1 al nuevo schema)

ROLLBACK (si algo falla):
  DROP SCHEMA tenant_snn_soluciones CASCADE;
  DELETE FROM public.tenants WHERE empresa_legacy_id = 1;
  El sistema legacy sigue funcionando sin cambios.
```

### 7.3 Mapeo de tablas

| Tabla legacy (public) | Destino | Notas |
|----------------------|---------|-------|
| `empresas` WHERE id=N | `{tenant}.empresa` | 1 registro |
| `sucursales` WHERE empresa_id=N | `{tenant}.sucursales` | Sin empresa_id |
| `usuarios` ROOT/SOPORTE/SUPER_ADMIN | `public.usuarios_globales` | Password se mantiene |
| `usuarios` resto | `{tenant}.usuarios` | PIN default + requiere_cambio=true |
| `usuario_empresa` | `public.super_admin_tenants` | Solo SUPER_ADMIN |
| `productos` | `{tenant}.productos` | Sin empresa_id |
| `categorias_producto` | `{tenant}.categorias_producto` | Sin empresa_id |
| `clientes` | `{tenant}.clientes` | Sin empresa_id |
| `proveedores` | `{tenant}.proveedores` | Sin empresa_id |
| `facturas` | `{tenant}.facturas` | Sin empresa_id |
| `sesiones_caja` | `{tenant}.sesiones_caja` | Sin empresa_id |
| Catálogos CR (provincias, etc.) | Permanecen en `public` | Compartidos |
| `codigos_cabys` | Permanece en `public` | Compartido |

---

## 8. Fases de Implementación

### Fase A — Tenant Split (bloqueante)

| # | Tarea | Descripción | Estado |
|---|-------|-------------|--------|
| A1 | Schema copy strategy | `LIKE public.{tabla} INCLUDING ALL` en `copiarEstructuraTablas()` | ✅ |
| A2 | `TenantMigrationService` | Migración completa: schema, datos, sequences, PIN, usuarios globales | ✅ |
| A3 | `TenantMigrationController` | `POST /api/admin/migration/empresa/{empresaId}` + assign-pins | ✅ |
| A4 | Tablas ordenadas por FK | 14 niveles de dependencia definidos en `TABLAS_A_MIGRAR` | ✅ |
| A5 | PIN default + sequences | `asignarPinPorDefecto()` + `resetearSequences()` | ✅ |
| A6 | Migrar SUPER_ADMIN | `migrarUsuariosSuperAdmin()` → `public.usuarios_globales` | ✅ |
| A7 | Verificar tablas faltantes | Confirmar que `TABLAS_A_MIGRAR` incluye TODAS las tablas del sistema | 🔧 |
| A8 | Script validación post-migración | SQL que compara conteos public vs tenant schema | ⬜ |
| A9 | Script rollback | `DROP SCHEMA {tenant} CASCADE` + limpiar `public.tenants` | ⬜ |
| A10 | Probar migración empresa_id=1 | Ejecutar en staging primero, luego producción | ⬜ |
| A11 | Validar auth en producción | JWT_GLOBAL + JWT_PDV funcionando con nuevo schema | ⬜ |

### Fase B — Auth Split (paralela a A)

| # | Tarea | Descripción | Estado |
|---|-------|-------------|--------|
| B1 | Claim `type` en JWT | Agregar GLOBAL/PDV a todos los tokens | ⬜ |
| B2 | `tenantId` en JWT | Reemplazar workaround de empresaId negativo | ⬜ |
| B3 | `getTypeFromToken()` | Getter en `JwtTokenProvider` | ⬜ |
| B4 | `ContextoUsuario` actualizar | Agregar `tokenType`, `tenantId` real | ⬜ |
| B5 | `JwtAuthenticationFilter` | Leer type y tenantId del nuevo JWT | ⬜ |
| B6 | Validar `AuthPinService` con JPA | Confirmar que TenantConnectionProvider rutea bien | ⬜ |
| B7 | Eliminar `ContextHeaderFilter` | Ya no se necesita con sucursalId en JWT | ⬜ |
| B8 | Eliminar `AuthPdvController` | Reemplazado por `AuthMultitenantController` | ⬜ |
| B9 | Eliminar `AuthPdvService` | Reemplazado por `AuthPinService` | ⬜ |

### Fase C — Onboarding nuevos clientes (post-A)

| # | Tarea | Descripción | Estado |
|---|-------|-------------|--------|
| C1 | Crear tenant nuevo directo | Nuevo cliente → schema propio sin pasar por legacy | ⬜ |
| C2 | Migrar clientes reales | Uno por uno, coordinar mantenimiento | ⬜ |

### Fase D — Limpieza final (cuando todos migraron)

| # | Tarea | Descripción | Estado |
|---|-------|-------------|--------|
| D1 | Remover `empresa_id` de repos | Quitar filtros WHERE empresa_id en repositorios | ⬜ |
| D2 | Remover `empresa_id` de tablas | Migrations para quitar la columna | ⬜ |
| D3 | Archivar sistema legacy | Dejar solo por referencia histórica | ⬜ |

---

## 9. API Endpoints

### Auth Web (JWT_GLOBAL)

```
POST /api/auth/global/login
Body: { email, password }
Response: { token(JWT_GLOBAL), refreshToken, usuario, tenants, requiereSeleccionTenant }

POST /api/auth/global/seleccionar-tenant/{tenantId}
Auth: JWT_GLOBAL
Response: nuevo token con tenantId específico

POST /api/auth/global/cambiar-password
Auth: JWT_GLOBAL
Body: { passwordActual, passwordNueva }
```

### Auth PDV (JWT_PDV)

```
POST /api/auth/empresa
Body: { codigo }
Header: X-Device-Token (opcional — si ya está registrado, retorna usuarios directo)
Response: { requiereRegistro, tenant, dispositivo? }

POST /api/auth/dispositivo/solicitar
Body: { tenantCodigo, nombreDispositivo }
Response: { mensaje, expiraEn }  (envía OTP al email del SUPER_ADMIN)

POST /api/auth/dispositivo/verificar
Body: { tenantCodigo, nombreDispositivo, codigo }
Response: { deviceToken(UUID), tenant, usuarios }

POST /api/auth/dispositivo/reenviar
Body: { tenantCodigo, nombreDispositivo }

GET  /api/auth/dispositivo/usuarios
Header: X-Device-Token
Response: { tenant, dispositivo, usuarios[] }

POST /api/auth/pin
Header: X-Device-Token
Body: { usuarioId, pin }
Response: { sessionToken(JWT_PDV), usuario, sucursal, requiereCambioPin }

POST /api/auth/pin/cambiar
Auth: JWT_PDV
Body: { nuevoPin, longitud }

POST /api/auth/cerrar-sesion
Auth: JWT_PDV o JWT_GLOBAL
```

### Administración (solo ROOT/SOPORTE)

```
GET    /api/admin/tenants
POST   /api/admin/tenants
GET    /api/admin/tenants/{id}
PUT    /api/admin/tenants/{id}
DELETE /api/admin/tenants/{id}

GET    /api/admin/tenants/{id}/dispositivos
DELETE /api/admin/dispositivos/{id}

POST   /api/admin/migracion/{empresaId}         ← NUEVO
GET    /api/admin/migracion/{empresaId}/status   ← NUEVO
```

---

## 10. Convenciones de Código

### 10.1 Estructura de paquetes

```
com.snnsoluciones.backnathbitpos/
├── config/
│   └── tenant/
│       ├── TenantContext.java          ✅
│       ├── TenantInterceptor.java      ✅ (modificar)
│       ├── TenantConnectionProvider.java ✅
│       ├── TenantIdentifierResolver.java ✅
│       └── TenantSchemaInitializer.java  ⬜ CREAR
├── entity/
│   └── global/
│       ├── Tenant.java                 ✅
│       ├── UsuarioGlobal.java          ✅
│       ├── SuperAdminTenant.java       ✅
│       ├── Dispositivo.java            ✅
│       └── CodigoRegistro.java         ✅
├── repository/
│   └── global/
│       ├── TenantRepository.java       ✅
│       ├── UsuarioGlobalRepository.java ✅
│       ├── DispositivoRepository.java  ✅
│       ├── SuperAdminTenantRepository.java ✅
│       └── CodigoRegistroRepository.java   ✅
├── service/
│   ├── tenant/
│   │   ├── TenantService.java          ✅
│   │   └── TenantMigrationService.java ⬜ CREAR
│   └── auth/multitenant/
│       ├── AuthGlobalService.java      ✅ (modificar JWT)
│       ├── AuthDispositivoService.java ✅
│       └── AuthPinService.java         ✅ (validar JPA vs JDBC)
├── controller/
│   ├── admin/
│   │   ├── TenantAdminController.java      ✅
│   │   └── TenantMigrationController.java  ⬜ CREAR
│   └── auth/
│       └── AuthMultitenantController.java  ✅
├── security/
│   ├── ContextoUsuario.java            🔧 modificar (agregar tokenType, tenantId)
│   ├── ContextHeaderFilter.java        🗑️ ELIMINAR (Fase B)
│   └── jwt/
│       ├── JwtTokenProvider.java       🔧 modificar (claim type, tenantId)
│       └── JwtAuthenticationFilter.java 🔧 modificar (leer type y tenantId)
└── sql/
    ├── tenant-schema-template.sql      ⬜ CREAR
    ├── validate-migration.sql          ⬜ CREAR
    └── rollback-tenant.sql             ⬜ CREAR
```

### 10.2 Convenciones de nombres

| Tipo | Convención | Ejemplo |
|------|------------|---------|
| Código tenant | snake_case, minúsculas, sin acentos | `snn_soluciones` |
| Schema tenant | `tenant_` + código | `tenant_snn_soluciones` |
| Device token | UUID v4 | `f47ac10b-58cc-4372-a567-0e02b2c3d479` |
| JWT_GLOBAL | Bearer en header Authorization | `Bearer eyJ...` |
| JWT_PDV | Bearer en header Authorization | `Bearer eyJ...` |
| Device header | `X-Device-Token` | UUID |

### 10.3 Headers HTTP

| Header | Quién lo envía | Qué hace |
|--------|---------------|---------- |
| `Authorization: Bearer {jwt}` | Todo cliente | Autenticación de usuario |
| `X-Device-Token: {uuid}` | Solo PDV | Identifica el dispositivo, activa flujo PDV |
| `X-Tenant-Code: {codigo}` | Opcional | Resolución directa de tenant (no requiere JWT) |

### 10.4 Códigos de error

| Código | HTTP | Descripción |
|--------|------|-------------|
| `TENANT_NOT_FOUND` | 404 | Código de empresa no existe |
| `DEVICE_NOT_REGISTERED` | 401 | Dispositivo sin token válido |
| `DEVICE_INACTIVE` | 401 | Dispositivo desconectado por admin |
| `CODE_EXPIRED` | 400 | Código OTP expirado (10 min) |
| `CODE_INVALID` | 400 | Código OTP incorrecto |
| `CODE_ALREADY_USED` | 400 | Código OTP ya usado |
| `PIN_INVALID` | 401 | PIN incorrecto |
| `PIN_CHANGE_REQUIRED` | 403 | Debe cambiar PIN antes de continuar |
| `INVALID_TOKEN_TYPE` | 403 | JWT_PDV usado en endpoint web o viceversa |
| `MIGRATION_FAILED` | 500 | Error durante migración (ver logs) |

---

## 11. Qué Eliminar

Resumen de lo que debe eliminarse, en orden:

| Archivo | Cuándo | Motivo |
|---------|--------|--------|
| `ContextHeaderFilter.java` | Fase B completa | `sucursalId` ya viene en JWT_PDV |
| `AuthPdvController.java` | Fase B completa | Reemplazado por `AuthMultitenantController` |
| `AuthPdvService.java` | Fase B completa | Reemplazado por `AuthPinService` |
| `AuthPdvServiceImpl.java` | Fase B completa | Ídem |
| Métodos `validarAccesoEmpresa/Sucursal` | Junto con el filtro | Retornan `true`, nunca implementados |
| Workaround `empresaId negativo` en `AuthGlobalService` | Fase B | Reemplazar por `tenantId` real en JWT |
| `empresa_id` en tablas de negocio | Fase D (todos migrados) | Ya no se necesita con schema routing |
| Tabla `token_registro` (sistema viejo de QR) | Fase D | Reemplazado por `codigos_registro` OTP |

---

*Fin del documento. Última actualización: Abril 2026.*