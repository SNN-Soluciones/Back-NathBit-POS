# 🏗️ NathBit POS - Arquitectura Multi-Tenant

> Documento maestro para la migración a arquitectura multi-tenant
> Versión: 1.0
> Fecha: Diciembre 2025
> Autor: Equipo NathBit

---

## 📋 Índice

1. [Resumen Ejecutivo](#1-resumen-ejecutivo)
2. [Decisiones de Arquitectura](#2-decisiones-de-arquitectura)
3. [Estructura de Datos](#3-estructura-de-datos)
4. [Flujos de Autenticación](#4-flujos-de-autenticación)
5. [Plan de Implementación](#5-plan-de-implementación)
6. [Migración de Datos](#6-migración-de-datos)
7. [Convenciones de Código](#7-convenciones-de-código)
8. [API Endpoints](#8-api-endpoints)
9. [Checklist por Fase](#9-checklist-por-fase)

---

## 1. Resumen Ejecutivo

### 1.1 Objetivo

Transformar NathBit POS de una arquitectura multi-empresa (con separación lógica por `empresa_id`) a una arquitectura multi-tenant real usando **PostgreSQL Schemas**, donde cada cliente/empresa tiene su propio schema aislado.

### 1.2 Beneficios

| Beneficio | Descripción |
|-----------|-------------|
| **Aislamiento total** | Datos completamente separados entre clientes |
| **Backups independientes** | Respaldo por tenant sin afectar otros |
| **Escalabilidad** | Horizontal, agregar tenants sin impacto |
| **Seguridad mejorada** | Un tenant jamás accede a datos de otro |
| **Mantenimiento simplificado** | Migraciones controladas por schema |
| **Preparación offline** | Sync bidireccional por tenant |

### 1.3 Alcance MVP

- ✅ Infraestructura multi-schema funcional
- ✅ Sistema de autenticación híbrido (global + dispositivo + PIN)
- ✅ Migración del tenant piloto (empresa_id = 1)
- ✅ Portal de administración básico
- ✅ Compatibilidad con sistema legacy durante transición

### 1.4 Tecnologías

| Componente | Tecnología |
|------------|------------|
| Backend | Java 17 + Spring Boot 3.x |
| Base de datos | PostgreSQL 14+ (Schemas) |
| Auth | JWT + BCrypt |
| Email | Resend |
| Frontend | Angular + Capacitor |
| Futuro Desktop | Electron o Tauri |

---

## 2. Decisiones de Arquitectura

### 2.1 Separación de Datos

| Decisión | Valor | Justificación |
|----------|-------|---------------|
| Estrategia | **Schema por Tenant** | Balance entre aislamiento y eficiencia |
| Base de datos | Una sola BD PostgreSQL | Simplifica conexiones y mantenimiento |
| Schema master | `public` | Datos globales y catálogos compartidos |
| Schema tenant | `tenant_{codigo}` | Datos de negocio aislados |

### 2.2 Modelo de Usuarios

| Tipo | Ubicación | Auth | Acceso |
|------|-----------|------|--------|
| ROOT | `public.usuarios_globales` | Email + Password | Cross-tenant |
| SOPORTE | `public.usuarios_globales` | Email + Password | Cross-tenant |
| SUPER_ADMIN | `public.usuarios_globales` | Email + Password | Sus tenants asignados |
| ADMIN | `{tenant}.usuarios` | PIN (4-6 dígitos) | Solo su tenant |
| JEFE_CAJAS | `{tenant}.usuarios` | PIN | Solo su tenant |
| CAJERO | `{tenant}.usuarios` | PIN | Solo su tenant |
| MESERO | `{tenant}.usuarios` | PIN | Solo su tenant |
| COCINERO | `{tenant}.usuarios` | PIN | Solo su tenant |

### 2.3 Autenticación por Capas

```
┌─────────────────────────────────────────────────────────────┐
│                    CAPA 1: USUARIO GLOBAL                   │
│         (ROOT, SOPORTE, SUPER_ADMIN)                        │
│         Auth: Email + Password → JWT                        │
│         Acceso: Desde cualquier dispositivo                 │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    CAPA 2: DISPOSITIVO                      │
│         Auth: Código empresa + OTP → Device Token           │
│         Token permanente guardado en dispositivo            │
│         Vinculado a UN solo tenant                          │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    CAPA 3: USUARIO LOCAL                    │
│         (ADMIN, CAJERO, MESERO, etc.)                       │
│         Auth: Seleccionar usuario + PIN → Session Token     │
│         Session: 8 horas o cierre manual                    │
│         Solo desde dispositivo registrado                   │
└─────────────────────────────────────────────────────────────┘
```

### 2.4 Almacenamiento de Device Token

| Plataforma | Almacenamiento | Notas |
|------------|----------------|-------|
| Web (Angular) | localStorage → IndexedDB | Migrar a IndexedDB para offline |
| Android (Capacitor) | Preferences → SQLite | Para modo offline futuro |
| Windows (Electron/Tauri) | Secure Storage | Por definir tecnología |

### 2.5 PIN de Usuario

| Aspecto | Valor |
|---------|-------|
| Longitud | 4 a 6 dígitos (usuario elige) |
| Encriptación | BCrypt (igual que passwords) |
| Cambio obligatorio | En primer uso después de migración |
| PIN por defecto | `123456` |

### 2.6 Convención de Nombres para Tenants

| Campo | Formato | Ejemplo |
|-------|---------|---------|
| Razón Social | Original | "Inversiones JR de Zagala Vieja S.A." |
| Código | snake_case, 3-4 palabras | `inversiones_jr_zagala` |
| Schema | `tenant_` + código | `tenant_inversiones_jr_zagala` |

**Reglas para generar código:**
1. Tomar primeras 3-4 palabras significativas
2. Eliminar artículos (de, la, el, etc.)
3. Eliminar sufijos legales (S.A., Ltda, etc.)
4. Convertir a minúsculas
5. Reemplazar espacios por guiones bajos
6. Solo caracteres: `a-z`, `0-9`, `_`

---

## 3. Estructura de Datos

### 3.1 Diagrama General

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         PostgreSQL Database                              │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │                    SCHEMA: public (Master)                       │    │
│  ├─────────────────────────────────────────────────────────────────┤    │
│  │  tenants                    → Registro de cada tenant            │    │
│  │  usuarios_globales          → ROOT, SOPORTE, SUPER_ADMIN         │    │
│  │  super_admin_tenants        → Relación SUPER_ADMIN ↔ Tenants     │    │
│  │  dispositivos               → Dispositivos registrados           │    │
│  │  codigos_registro           → OTP temporales (10 min)            │    │
│  │  planes                     → Planes de suscripción (futuro)     │    │
│  │  suscripciones              → Estado por tenant (futuro)         │    │
│  │  ─────────────────────────────────────────────────────────────   │    │
│  │  provincias, cantones, distritos, barrios  → Catálogos CR        │    │
│  │  actividades_economicas                    → Catálogo Hacienda   │    │
│  │  codigos_cabys                             → Catálogo Hacienda   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  ┌──────────────────────┐  ┌──────────────────────┐                     │
│  │ SCHEMA: tenant_snn   │  │ SCHEMA: tenant_birria│  ... más tenants    │
│  ├──────────────────────┤  ├──────────────────────┤                     │
│  │  empresa (1 registro)│  │  empresa (1 registro)│                     │
│  │  sucursales          │  │  sucursales          │                     │
│  │  terminales          │  │  terminales          │                     │
│  │  usuarios (PIN)      │  │  usuarios (PIN)      │                     │
│  │  productos           │  │  productos           │                     │
│  │  categorias          │  │  categorias          │                     │
│  │  clientes            │  │  clientes            │                     │
│  │  proveedores         │  │  proveedores         │                     │
│  │  facturas            │  │  facturas            │                     │
│  │  inventario          │  │  inventario          │                     │
│  │  ...                 │  │  ...                 │                     │
│  └──────────────────────┘  └──────────────────────┘                     │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 3.2 Schema `public` - Tablas Nuevas

#### 3.2.1 Tabla: tenants

```sql
CREATE TABLE public.tenants (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(50) UNIQUE NOT NULL,
    nombre VARCHAR(200) NOT NULL,
    schema_name VARCHAR(100) UNIQUE NOT NULL,
    activo BOOLEAN DEFAULT true,
    config JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP,
    
    CONSTRAINT chk_codigo_format CHECK (codigo ~ '^[a-z0-9_]+$'),
    CONSTRAINT chk_schema_format CHECK (schema_name ~ '^tenant_[a-z0-9_]+$')
);

CREATE INDEX idx_tenants_codigo ON public.tenants(codigo);
CREATE INDEX idx_tenants_activo ON public.tenants(activo);

COMMENT ON TABLE public.tenants IS 'Registro maestro de tenants del sistema';
COMMENT ON COLUMN public.tenants.codigo IS 'Código único, ej: snn_soluciones';
COMMENT ON COLUMN public.tenants.schema_name IS 'Nombre del schema, ej: tenant_snn_soluciones';
COMMENT ON COLUMN public.tenants.config IS 'Configuraciones extra en JSON';
```

#### 3.2.2 Tabla: usuarios_globales

```sql
CREATE TABLE public.usuarios_globales (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100),
    telefono VARCHAR(20),
    rol VARCHAR(20) NOT NULL,
    activo BOOLEAN DEFAULT true,
    ultimo_acceso TIMESTAMP,
    requiere_cambio_password BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP,
    
    CONSTRAINT chk_rol_global CHECK (rol IN ('ROOT', 'SOPORTE', 'SUPER_ADMIN'))
);

CREATE INDEX idx_usuarios_globales_email ON public.usuarios_globales(email);
CREATE INDEX idx_usuarios_globales_rol ON public.usuarios_globales(rol);
CREATE INDEX idx_usuarios_globales_activo ON public.usuarios_globales(activo);

COMMENT ON TABLE public.usuarios_globales IS 'Usuarios con acceso cross-tenant';
```

#### 3.2.3 Tabla: super_admin_tenants

```sql
CREATE TABLE public.super_admin_tenants (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES public.usuarios_globales(id) ON DELETE CASCADE,
    tenant_id BIGINT NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    es_propietario BOOLEAN DEFAULT false,
    activo BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    
    CONSTRAINT uk_usuario_tenant UNIQUE (usuario_id, tenant_id)
);

CREATE INDEX idx_super_admin_tenants_usuario ON public.super_admin_tenants(usuario_id);
CREATE INDEX idx_super_admin_tenants_tenant ON public.super_admin_tenants(tenant_id);

COMMENT ON TABLE public.super_admin_tenants IS 'Relación N:M entre SUPER_ADMIN y Tenants';
COMMENT ON COLUMN public.super_admin_tenants.es_propietario IS 'true=dueño principal, false=colaborador';
```

#### 3.2.4 Tabla: dispositivos

```sql
CREATE TABLE public.dispositivos (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    nombre VARCHAR(100) NOT NULL,
    token VARCHAR(255) UNIQUE NOT NULL,
    user_agent TEXT,
    ip_registro VARCHAR(45),
    plataforma VARCHAR(50),
    activo BOOLEAN DEFAULT true,
    ultimo_uso TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE INDEX idx_dispositivos_tenant ON public.dispositivos(tenant_id);
CREATE INDEX idx_dispositivos_token ON public.dispositivos(token);
CREATE INDEX idx_dispositivos_activo ON public.dispositivos(activo);

COMMENT ON TABLE public.dispositivos IS 'Dispositivos registrados por tenant';
COMMENT ON COLUMN public.dispositivos.plataforma IS 'WEB, ANDROID, WINDOWS, IOS';
COMMENT ON COLUMN public.dispositivos.token IS 'UUID largo, no expira';
```

#### 3.2.5 Tabla: codigos_registro

```sql
CREATE TABLE public.codigos_registro (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    dispositivo_nombre VARCHAR(100) NOT NULL,
    codigo VARCHAR(6) NOT NULL,
    ip_solicitante VARCHAR(45),
    user_agent TEXT,
    expira_at TIMESTAMP NOT NULL,
    usado BOOLEAN DEFAULT false,
    usado_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_codigos_registro_tenant ON public.codigos_registro(tenant_id);
CREATE INDEX idx_codigos_registro_codigo ON public.codigos_registro(codigo);
CREATE INDEX idx_codigos_registro_expira ON public.codigos_registro(expira_at);

COMMENT ON TABLE public.codigos_registro IS 'Códigos OTP para registro de dispositivos';
COMMENT ON COLUMN public.codigos_registro.expira_at IS 'created_at + 10 minutos';
```

#### 3.2.6 Tablas Futuras: planes y suscripciones

```sql
-- FUTURO: Sistema de facturación/suscripciones

CREATE TABLE public.planes (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    codigo VARCHAR(50) UNIQUE NOT NULL,
    precio_mensual DECIMAL(10,2),
    precio_anual DECIMAL(10,2),
    max_sucursales INTEGER,
    max_usuarios INTEGER,
    max_productos INTEGER,
    features JSONB DEFAULT '{}',
    activo BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE public.suscripciones (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES public.tenants(id),
    plan_id BIGINT NOT NULL REFERENCES public.planes(id),
    estado VARCHAR(20) DEFAULT 'ACTIVA',
    fecha_inicio DATE NOT NULL,
    fecha_vencimiento DATE,
    created_at TIMESTAMP DEFAULT NOW(),
    
    CONSTRAINT chk_estado CHECK (estado IN ('ACTIVA', 'SUSPENDIDA', 'CANCELADA', 'PRUEBA'))
);
```

### 3.3 Schema `public` - Catálogos Compartidos

Estas tablas **YA EXISTEN** y se comparten entre todos los tenants (solo lectura):

| Tabla | Registros | Descripción |
|-------|-----------|-------------|
| `provincias` | 7 | Provincias de Costa Rica |
| `cantones` | 82 | Cantones de Costa Rica |
| `distritos` | 488 | Distritos de Costa Rica |
| `barrios` | ~4,500 | Barrios de Costa Rica |
| `actividades_economicas` | ~600 | Catálogo Hacienda |
| `codigos_cabys` | ~29,000 | Catálogo Hacienda |

**Acceso desde tenant:** `SELECT * FROM public.provincias`

### 3.4 Schema del Tenant - Template

#### 3.4.1 Tabla: empresa (1 registro por tenant)

```sql
CREATE TABLE {SCHEMA}.empresa (
    id BIGSERIAL PRIMARY KEY,
    tipo_identificacion VARCHAR(20),
    identificacion VARCHAR(20) UNIQUE,
    nombre_comercial VARCHAR(80),
    nombre_razon_social VARCHAR(100) NOT NULL,
    telefono VARCHAR(20),
    fax VARCHAR(20),
    email VARCHAR(100),
    email_notificacion VARCHAR(100),
    
    -- Ubicación (referencias a public.*)
    provincia_id INTEGER,
    canton_id INTEGER,
    distrito_id INTEGER,
    barrio_id INTEGER,
    otras_senas VARCHAR(500),
    
    -- Configuración fiscal
    requiere_hacienda BOOLEAN DEFAULT false,
    regimen_tributario VARCHAR(30) DEFAULT 'REGIMEN_TRADICIONAL',
    limite_anual_simplificado DECIMAL(18,2),
    
    -- Configuración de entidades
    productos_por_sucursal BOOLEAN DEFAULT false,
    categorias_por_sucursal BOOLEAN DEFAULT false,
    clientes_por_sucursal BOOLEAN DEFAULT false,
    proveedores_por_sucursal BOOLEAN DEFAULT false,
    
    -- Otros
    logo_url VARCHAR(500),
    activa BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP
);
```

#### 3.4.2 Tabla: usuarios (locales con PIN)

```sql
CREATE TABLE {SCHEMA}.usuarios (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100),
    email VARCHAR(100),
    username VARCHAR(50),
    telefono VARCHAR(20),
    pin VARCHAR(255) NOT NULL,
    pin_longitud INTEGER DEFAULT 4,
    rol VARCHAR(30) NOT NULL,
    activo BOOLEAN DEFAULT true,
    requiere_cambio_pin BOOLEAN DEFAULT true,
    ultimo_acceso TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP,
    
    CONSTRAINT chk_rol_local CHECK (rol IN ('ADMIN', 'JEFE_CAJAS', 'CAJERO', 'MESERO', 'COCINERO')),
    CONSTRAINT chk_pin_longitud CHECK (pin_longitud IN (4, 6))
);

CREATE INDEX idx_usuarios_rol ON {SCHEMA}.usuarios(rol);
CREATE INDEX idx_usuarios_activo ON {SCHEMA}.usuarios(activo);
```

#### 3.4.3 Tabla: sucursales

```sql
CREATE TABLE {SCHEMA}.sucursales (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    numero_sucursal VARCHAR(3) NOT NULL,
    modo_facturacion VARCHAR(20) DEFAULT 'ELECTRONICO',
    
    -- Ubicación
    provincia_id INTEGER,
    canton_id INTEGER,
    distrito_id INTEGER,
    barrio_id INTEGER,
    otras_senas VARCHAR(500),
    
    -- Configuración
    maneja_inventario BOOLEAN DEFAULT true,
    aplica_recetas BOOLEAN DEFAULT true,
    permite_negativos BOOLEAN DEFAULT false,
    
    logo_sucursal_path VARCHAR(500),
    activa BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP,
    
    CONSTRAINT uk_numero_sucursal UNIQUE (numero_sucursal)
);
```

#### 3.4.4 Tabla: usuario_sucursal

```sql
CREATE TABLE {SCHEMA}.usuario_sucursal (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES {SCHEMA}.usuarios(id) ON DELETE CASCADE,
    sucursal_id BIGINT NOT NULL REFERENCES {SCHEMA}.sucursales(id) ON DELETE CASCADE,
    activo BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    
    CONSTRAINT uk_usuario_sucursal UNIQUE (usuario_id, sucursal_id)
);
```

#### 3.4.5 Resto de Tablas

Las siguientes tablas mantienen su estructura actual, eliminando la columna `empresa_id` donde ya no sea necesaria:

- `empresa_config_hacienda`
- `terminales`
- `sesiones_caja`
- `movimientos_caja`
- `productos`
- `categorias_producto`
- `familia_producto`
- `producto_impuesto`
- `producto_receta`
- `producto_compuesto_slot`
- `producto_compuesto_opcion`
- `clientes`
- `clientes_ubicaciones`
- `clientes_emails`
- `clientes_exoneracion`
- `proveedores`
- `producto_codigo_proveedor`
- `compras`
- `compra_detalles`
- `facturas`
- `facturas_detalles`
- `facturas_internas`
- `facturas_recepcion`
- `empresa_cabys`
- `empresa_actividad`
- ... (todas las demás tablas de negocio)

---

## 4. Flujos de Autenticación

### 4.1 Pantalla Inicial

```
┌──────────────────────────────────────────────────────────────┐
│                                                              │
│                      🏪 NathBit POS                          │
│                                                              │
│                 ¿Cómo desea ingresar?                        │
│                                                              │
│         ┌─────────────┐      ┌─────────────┐                │
│         │             │      │             │                │
│         │  👤 Usuario │      │  🏢 Empresa │                │
│         │             │      │             │                │
│         └─────────────┘      └─────────────┘                │
│                                                              │
│         Para administradores   Para dispositivos             │
│         del sistema            de punto de venta             │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

### 4.2 Flujo: Login Usuario Global

```
[Click "👤 Usuario"]
        │
        ▼
┌──────────────────────────────────────────────────────────────┐
│                     LOGIN GLOBAL                              │
│                                                              │
│     Email:    ┌────────────────────────────┐                 │
│               │ admin@empresa.com          │                 │
│               └────────────────────────────┘                 │
│                                                              │
│     Password: ┌────────────────────────────┐                 │
│               │ ••••••••••                 │                 │
│               └────────────────────────────┘                 │
│                                                              │
│               [        Ingresar        ]                     │
│                                                              │
└──────────────────────────────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────────────────────────────┐
│  POST /api/auth/login                                        │
│  Body: { email, password }                                   │
│                                                              │
│  Backend:                                                    │
│  1. Buscar en public.usuarios_globales                       │
│  2. Validar password con BCrypt                              │
│  3. Generar JWT con claims:                                  │
│     - userId, email, rol                                     │
│     - tenants[] (si es SUPER_ADMIN)                          │
│  4. Retornar token + info usuario                            │
└──────────────────────────────────────────────────────────────┘
        │
        ├─────────────────────┬─────────────────────┐
        │                     │                     │
   [ROOT/SOPORTE]       [SUPER_ADMIN]         [ERROR]
        │                     │                     │
        ▼                     ▼                     ▼
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│ Dashboard     │    │ Seleccionar   │    │ Credenciales  │
│ Admin Global  │    │ Tenant        │    │ inválidas     │
└───────────────┘    └───────────────┘    └───────────────┘
```

### 4.3 Flujo: Login por Empresa (Dispositivo)

```
[Click "🏢 Empresa"]
        │
        ▼
┌──────────────────────────────────────────────────────────────┐
│                   CÓDIGO DE EMPRESA                          │
│                                                              │
│     Código:   ┌────────────────────────────┐                 │
│               │ SNN_SOLUCIONES             │                 │
│               └────────────────────────────┘                 │
│                                                              │
│               [        Continuar       ]                     │
│                                                              │
└──────────────────────────────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────────────────────────────┐
│  POST /api/auth/empresa                                      │
│  Body: { codigo: "SNN_SOLUCIONES" }                          │
│  Headers: { X-Device-Token: null | "abc123..." }             │
│                                                              │
│  Backend:                                                    │
│  1. Buscar tenant por código                                 │
│  2. Verificar device_token en header                         │
│  3. Si no hay token → requiere_registro: true                │
│  4. Si hay token válido → retornar lista usuarios            │
└──────────────────────────────────────────────────────────────┘
        │
        ├─────────────────────┬─────────────────────┐
        │                     │                     │
   [SIN TOKEN]           [CON TOKEN]           [NO EXISTE]
        │                     │                     │
        ▼                     ▼                     ▼
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│ Registrar     │    │ Seleccionar   │    │ Empresa no    │
│ Dispositivo   │    │ Usuario + PIN │    │ encontrada    │
└───────────────┘    └───────────────┘    └───────────────┘
```

### 4.4 Flujo: Registro de Dispositivo

```
┌──────────────────────────────────────────────────────────────┐
│              DISPOSITIVO NO REGISTRADO                        │
│                                                              │
│  ⚠️  Este dispositivo no está autorizado                     │
│                                                              │
│  Nombre del dispositivo:                                     │
│  ┌────────────────────────────────────────┐                  │
│  │ Caja Principal                         │                  │
│  └────────────────────────────────────────┘                  │
│                                                              │
│  Se enviará un código de verificación a los                  │
│  administradores de la empresa.                              │
│                                                              │
│              [   Solicitar Código   ]                        │
│                                                              │
└──────────────────────────────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────────────────────────────┐
│  POST /api/auth/dispositivo/solicitar                        │
│  Body: {                                                     │
│    tenant_codigo: "SNN_SOLUCIONES",                          │
│    nombre_dispositivo: "Caja Principal",                     │
│    plataforma: "WEB"                                         │
│  }                                                           │
│                                                              │
│  Backend:                                                    │
│  1. Generar código 6 dígitos aleatorio                       │
│  2. Guardar en public.codigos_registro (expira 10 min)       │
│  3. Buscar SUPER_ADMIN(s) del tenant                         │
│  4. Enviar email con código vía Resend                       │
│  5. Retornar { mensaje: "Código enviado" }                   │
└──────────────────────────────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────────────────────────────┐
│                     📧 EMAIL ENVIADO                          │
│                                                              │
│  Asunto: Nuevo dispositivo solicita acceso                   │
│                                                              │
│  ─────────────────────────────────────────────────────────   │
│                                                              │
│  Un nuevo dispositivo quiere registrarse:                    │
│                                                              │
│  • Nombre: Caja Principal                                    │
│  • Plataforma: Web (Chrome)                                  │
│  • IP: 190.113.45.123                                        │
│  • Fecha: 02/12/2025 14:30                                   │
│                                                              │
│  ┌─────────────────────────────────────┐                     │
│  │     Código: 8 4 7 2 9 1             │                     │
│  │     Válido por 10 minutos           │                     │
│  └─────────────────────────────────────┘                     │
│                                                              │
│  Si no reconoce esta solicitud, ignórela.                    │
└──────────────────────────────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────────────────────────────┐
│              INGRESAR CÓDIGO DE VERIFICACIÓN                  │
│                                                              │
│  Ingrese el código de 6 dígitos:                             │
│                                                              │
│       ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐                   │
│       │ 8 │ │ 4 │ │ 7 │ │ 2 │ │ 9 │ │ 1 │                   │
│       └───┘ └───┘ └───┘ └───┘ └───┘ └───┘                   │
│                                                              │
│       ⏱️ Expira en: 8:45                                     │
│                                                              │
│       ¿No recibió el código? [Reenviar]                      │
│                                                              │
│              [      Verificar      ]                         │
│                                                              │
└──────────────────────────────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────────────────────────────┐
│  POST /api/auth/dispositivo/verificar                        │
│  Body: {                                                     │
│    tenant_codigo: "SNN_SOLUCIONES",                          │
│    nombre_dispositivo: "Caja Principal",                     │
│    codigo: "847291"                                          │
│  }                                                           │
│                                                              │
│  Backend:                                                    │
│  1. Buscar código en public.codigos_registro                 │
│  2. Validar: no expirado, no usado, mismo tenant             │
│  3. Marcar como usado                                        │
│  4. Generar device_token (UUID largo)                        │
│  5. Guardar en public.dispositivos                           │
│  6. Retornar { device_token, tenant_info }                   │
└──────────────────────────────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────────────────────────────┐
│              ✅ DISPOSITIVO REGISTRADO                        │
│                                                              │
│  "Caja Principal" ahora tiene acceso                         │
│                                                              │
│              [       Continuar       ]                       │
│                                                              │
└──────────────────────────────────────────────────────────────┘
        │
        ▼
    (Guardar device_token en localStorage/SecureStorage)
    (Ir a pantalla de selección de usuario)
```

### 4.5 Flujo: Login con PIN

```
┌──────────────────────────────────────────────────────────────┐
│                   SNN SOLUCIONES                              │
│                   📱 Caja Principal                           │
│                                                              │
│  Seleccione su usuario:                                      │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  👤  Juan Pérez                              CAJERO    │  │
│  ├────────────────────────────────────────────────────────┤  │
│  │  👤  María González                          MESERA    │  │
│  ├────────────────────────────────────────────────────────┤  │
│  │  👤  Carlos Rodríguez                        ADMIN     │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                              │
└──────────────────────────────────────────────────────────────┘
        │
        ▼ (Click en usuario)
┌──────────────────────────────────────────────────────────────┐
│                   SNN SOLUCIONES                              │
│                                                              │
│                      👤                                       │
│                   Juan Pérez                                  │
│                     CAJERO                                    │
│                                                              │
│  Ingrese su PIN:                                             │
│                                                              │
│            ┌───┐ ┌───┐ ┌───┐ ┌───┐                          │
│            │ • │ │ • │ │ • │ │ • │                          │
│            └───┘ └───┘ └───┘ └───┘                          │
│                                                              │
│       ┌───┐ ┌───┐ ┌───┐                                     │
│       │ 1 │ │ 2 │ │ 3 │                                     │
│       ├───┤ ├───┤ ├───┤                                     │
│       │ 4 │ │ 5 │ │ 6 │                                     │
│       ├───┤ ├───┤ ├───┤                                     │
│       │ 7 │ │ 8 │ │ 9 │                                     │
│       ├───┤ ├───┤ ├───┤                                     │
│       │ ← │ │ 0 │ │ ✓ │                                     │
│       └───┘ └───┘ └───┘                                     │
│                                                              │
│            [← Cambiar usuario]                               │
│                                                              │
└──────────────────────────────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────────────────────────────┐
│  POST /api/auth/pin                                          │
│  Headers: { X-Device-Token: "abc123..." }                    │
│  Body: { usuario_id: 5, pin: "1234" }                        │
│                                                              │
│  Backend:                                                    │
│  1. Validar device_token → obtener tenant                    │
│  2. SET search_path TO tenant_schema                         │
│  3. Buscar usuario en {tenant}.usuarios                      │
│  4. Validar PIN con BCrypt                                   │
│  5. Verificar requiere_cambio_pin                            │
│  6. Generar session_token (JWT 8h)                           │
│  7. Actualizar ultimo_acceso                                 │
│  8. Retornar token + info + requiere_cambio_pin              │
└──────────────────────────────────────────────────────────────┘
        │
        ├─────────────────────┬─────────────────────┐
        │                     │                     │
[requiere_cambio=true]  [requiere_cambio=false]  [ERROR]
        │                     │                     │
        ▼                     ▼                     ▼
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│ Cambiar PIN   │    │ Dashboard/POS │    │ PIN incorrecto│
│               │    │               │    │ Reintente     │
│ Nuevo: ____   │    │ ¡Bienvenido!  │    │               │
│ Confirmar: _  │    │               │    │               │
└───────────────┘    └───────────────┘    └───────────────┘
```

### 4.6 Flujo: Cambiar PIN

```
┌──────────────────────────────────────────────────────────────┐
│                   CAMBIAR PIN                                 │
│                                                              │
│  Debe cambiar su PIN antes de continuar                      │
│                                                              │
│  Longitud del PIN:                                           │
│  ┌─────────────┐  ┌─────────────┐                            │
│  │ ○ 4 dígitos │  │ ● 6 dígitos │                            │
│  └─────────────┘  └─────────────┘                            │
│                                                              │
│  Nuevo PIN:      ┌───┐┌───┐┌───┐┌───┐┌───┐┌───┐             │
│                  │   ││   ││   ││   ││   ││   │             │
│                  └───┘└───┘└───┘└───┘└───┘└───┘             │
│                                                              │
│  Confirmar PIN:  ┌───┐┌───┐┌───┐┌───┐┌───┐┌───┐             │
│                  │   ││   ││   ││   ││   ││   │             │
│                  └───┘└───┘└───┘└───┘└───┘└───┘             │
│                                                              │
│              [      Guardar      ]                           │
│                                                              │
└──────────────────────────────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────────────────────────────┐
│  POST /api/auth/pin/cambiar                                  │
│  Headers: { Authorization: "Bearer {session_token}" }        │
│  Body: { nuevo_pin: "582947", longitud: 6 }                  │
│                                                              │
│  Backend:                                                    │
│  1. Validar session_token                                    │
│  2. Validar que nuevo_pin tenga la longitud correcta         │
│  3. Encriptar con BCrypt                                     │
│  4. Actualizar usuario: pin, pin_longitud, requiere_cambio   │
│  5. Retornar { success: true }                               │
└──────────────────────────────────────────────────────────────┘
```

### 4.7 Flujo: Cerrar Sesión Usuario

```
┌──────────────────────────────────────────────────────────────┐
│  El usuario cierra sesión desde el POS                       │
│                                                              │
│  POST /api/auth/cerrar-sesion                                │
│  Headers: { Authorization: "Bearer {session_token}" }        │
│                                                              │
│  Backend:                                                    │
│  1. Invalidar session_token (opcional: blacklist)            │
│  2. NO tocar el device_token                                 │
│  3. Retornar a pantalla de selección de usuario              │
│                                                              │
│  NOTA: El dispositivo sigue autenticado                      │
│        Solo el usuario cierra su sesión                      │
└──────────────────────────────────────────────────────────────┘
```

### 4.8 Flujo: Desconectar Dispositivo (Solo SUPER_ADMIN)

```
┌──────────────────────────────────────────────────────────────┐
│  Desde el Portal de Administración                           │
│                                                              │
│  DELETE /api/admin/dispositivos/{id}                         │
│  Headers: { Authorization: "Bearer {jwt_global}" }           │
│                                                              │
│  Backend:                                                    │
│  1. Validar que el usuario es SUPER_ADMIN del tenant         │
│  2. Marcar dispositivo como activo = false                   │
│  3. El dispositivo deberá re-registrarse                     │
│                                                              │
│  En el dispositivo:                                          │
│  - Siguiente request con ese token → 401 Unauthorized        │
│  - Eliminar device_token del storage                         │
│  - Mostrar pantalla de registro nuevamente                   │
└──────────────────────────────────────────────────────────────┘
```

---

## 5. Plan de Implementación

### 5.1 Fase 0: Preparación (1-2 días)

| ID | Tarea | Descripción | Estado |
|----|-------|-------------|--------|
| 0.1 | Crear branch | `git checkout -b feature/multi-tenant` | ⬜ |
| 0.2 | Documentación | Crear `/docs/multi-tenant/ARQUITECTURA.md` | ⬜ |
| 0.3 | Backup | Backup completo de BD producción | ⬜ |
| 0.4 | Definir piloto | Confirmar empresa_id = 1 | ⬜ |

### 5.2 Fase 1: Infraestructura Multi-Schema (5-7 días)

| ID | Tarea | Descripción | Estado |
|----|-------|-------------|--------|
| 1.1 | Tablas master | Crear tablas en schema `public` | ⬜ |
| 1.2 | Entidades JPA | `Tenant`, `UsuarioGlobal`, `Dispositivo`, etc. | ⬜ |
| 1.3 | TenantContext | ThreadLocal para contexto actual | ⬜ |
| 1.4 | TenantInterceptor | Interceptor HTTP para extraer tenant | ⬜ |
| 1.5 | TenantConnectionProvider | Implementar cambio de schema | ⬜ |
| 1.6 | TenantService | CRUD de tenants + crear schema | ⬜ |
| 1.7 | Schema template | SQL para crear schema completo | ⬜ |
| 1.8 | Tests unitarios | Tests de infraestructura | ⬜ |

### 5.3 Fase 2: Autenticación (5-7 días)

| ID | Tarea | Descripción | Estado |
|----|-------|-------------|--------|
| 2.1 | Auth global | Adaptar login para `usuarios_globales` | ⬜ |
| 2.2 | Auth empresa | Endpoint validar código empresa | ⬜ |
| 2.3 | Solicitar código | Endpoint + generación OTP | ⬜ |
| 2.4 | Email OTP | Template + envío con Resend | ⬜ |
| 2.5 | Verificar código | Validar + crear device_token | ⬜ |
| 2.6 | Auth PIN | Login con PIN | ⬜ |
| 2.7 | Cambio PIN | Endpoint cambiar PIN | ⬜ |
| 2.8 | Session tokens | JWT diferenciados | ⬜ |
| 2.9 | Tests | Tests de flujos auth | ⬜ |

### 5.4 Fase 3: Migración Piloto (3-5 días)

| ID | Tarea | Descripción | Estado |
|----|-------|-------------|--------|
| 3.1 | Script migración | Herramienta de migración | ⬜ |
| 3.2 | Migrar estructura | Crear schema tenant_snn | ⬜ |
| 3.3 | Migrar datos | Copiar datos empresa_id = 1 | ⬜ |
| 3.4 | Migrar usuarios | Separar globales vs locales | ⬜ |
| 3.5 | Validación | Verificar integridad | ⬜ |
| 3.6 | Tests E2E | Probar flujos completos | ⬜ |

### 5.5 Fase 4: Portal Admin (4-6 días)

| ID | Tarea | Descripción | Estado |
|----|-------|-------------|--------|
| 4.1 | API tenants | CRUD de tenants | ⬜ |
| 4.2 | API dispositivos | Listar, desconectar | ⬜ |
| 4.3 | API usuarios tenant | CRUD usuarios locales | ⬜ |
| 4.4 | Dashboard ROOT | Vista global | ⬜ |
| 4.5 | Dashboard SUPER_ADMIN | Vista por tenant | ⬜ |
| 4.6 | Frontend admin | Implementar UI | ⬜ |

### 5.6 Fase 5: Compatibilidad (3-4 días)

| ID | Tarea | Descripción | Estado |
|----|-------|-------------|--------|
| 5.1 | Detector migración | Flag `migrado_a_tenant` | ⬜ |
| 5.2 | Rutas legacy | Mantener funcionando | ⬜ |
| 5.3 | Nuevos clientes | Crear en nuevo schema | ⬜ |
| 5.4 | UI indicador | Visual Legacy vs Multi-Tenant | ⬜ |

### 5.7 Estimación Total

| Fase | Duración |
|------|----------|
| Fase 0: Preparación | 1-2 días |
| Fase 1: Infraestructura | 5-7 días |
| Fase 2: Autenticación | 5-7 días |
| Fase 3: Migración Piloto | 3-5 días |
| Fase 4: Portal Admin | 4-6 días |
| Fase 5: Compatibilidad | 3-4 días |
| **TOTAL MVP** | **~4-5 semanas** |

---

## 6. Migración de Datos

### 6.1 Proceso de Migración por Empresa

```
┌─────────────────────────────────────────────────────────────┐
│                 PROCESO DE MIGRACIÓN                         │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  INPUT: empresa_id a migrar (ej: 1)                         │
│                                                              │
│  PASO 1: Generar código tenant                              │
│  ─────────────────────────────────────                      │
│  Razón Social: "SNN Soluciones S.A."                        │
│  Código: "snn_soluciones"                                   │
│  Schema: "tenant_snn_soluciones"                            │
│                                                              │
│  PASO 2: Crear registro en public.tenants                   │
│  ─────────────────────────────────────                      │
│  INSERT INTO public.tenants (codigo, nombre, schema_name)   │
│  VALUES ('snn_soluciones', 'SNN Soluciones', 'tenant_...')  │
│                                                              │
│  PASO 3: Crear schema                                       │
│  ─────────────────────────────────────                      │
│  CREATE SCHEMA tenant_snn_soluciones;                       │
│  (Ejecutar template SQL con todas las tablas)               │
│                                                              │
│  PASO 4: Migrar datos                                       │
│  ─────────────────────────────────────                      │
│  - empresa → tenant.empresa (1 registro)                    │
│  - sucursales WHERE empresa_id = 1                          │
│  - productos WHERE empresa_id = 1                           │
│  - (todas las tablas con empresa_id = 1)                    │
│                                                              │
│  PASO 5: Migrar usuarios                                    │
│  ─────────────────────────────────────                      │
│  - ROOT/SOPORTE/SUPER_ADMIN → public.usuarios_globales      │
│  - ADMIN/CAJERO/etc → tenant.usuarios                       │
│  - Asignar PIN: "123456" (BCrypt)                           │
│  - requiere_cambio_pin = true                               │
│                                                              │
│  PASO 6: Crear relaciones                                   │
│  ─────────────────────────────────────                      │
│  INSERT INTO public.super_admin_tenants                     │
│  (vincular SUPER_ADMIN con el nuevo tenant)                 │
│                                                              │
│  PASO 7: Marcar empresa original                            │
│  ─────────────────────────────────────                      │
│  UPDATE empresas SET migrado_a_tenant = true                │
│  WHERE id = 1;                                              │
│                                                              │
│  PASO 8: Generar reporte                                    │
│  ─────────────────────────────────────                      │
│  - Conteo de registros migrados por tabla                   │
│  - Usuarios globales creados                                │
│  - Usuarios locales creados                                 │
│  - Advertencias/errores                                     │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 6.2 Mapeo de Tablas

| Tabla Original | Destino | Notas |
|----------------|---------|-------|
| `empresas` | `{tenant}.empresa` | 1 registro por tenant |
| `sucursales` | `{tenant}.sucursales` | Eliminar empresa_id |
| `usuarios` (ROOT/SOPORTE/SUPER_ADMIN) | `public.usuarios_globales` | Migrar password |
| `usuarios` (otros) | `{tenant}.usuarios` | Convertir password a PIN |
| `usuario_empresa` | `public.super_admin_tenants` | Solo para SUPER_ADMIN |
| `usuario_sucursal` | `{tenant}.usuario_sucursal` | Mantener estructura |
| `productos` | `{tenant}.productos` | Eliminar empresa_id |
| `categorias_producto` | `{tenant}.categorias_producto` | Eliminar empresa_id |
| `clientes` | `{tenant}.clientes` | Eliminar empresa_id |
| `proveedores` | `{tenant}.proveedores` | Eliminar empresa_id |
| `facturas` | `{tenant}.facturas` | Eliminar empresa_id |
| ... | ... | ... |

### 6.3 Validación Post-Migración

```sql
-- Script de validación
-- Ejecutar después de cada migración

-- 1. Verificar conteo de registros
SELECT 'sucursales' as tabla, 
       (SELECT COUNT(*) FROM public.sucursales WHERE empresa_id = {ID}) as original,
       (SELECT COUNT(*) FROM tenant_{codigo}.sucursales) as migrado;

SELECT 'productos' as tabla,
       (SELECT COUNT(*) FROM public.productos WHERE empresa_id = {ID}) as original,
       (SELECT COUNT(*) FROM tenant_{codigo}.productos) as migrado;

-- ... más tablas

-- 2. Verificar usuarios
SELECT 'usuarios_globales' as tipo, COUNT(*) as total
FROM public.usuarios_globales
WHERE id IN (SELECT usuario_id FROM public.super_admin_tenants WHERE tenant_id = {TENANT_ID});

SELECT 'usuarios_locales' as tipo, COUNT(*) as total
FROM tenant_{codigo}.usuarios;

-- 3. Verificar integridad referencial
-- (queries específicos por tabla)
```

---

## 7. Convenciones de Código

### 7.1 Estructura de Paquetes

```
com.snnsoluciones.backnathbitpos/
├── config/
│   └── tenant/
│       ├── TenantContext.java
│       ├── TenantInterceptor.java
│       ├── TenantConnectionProvider.java
│       └── TenantConfig.java
├── entity/
│   ├── global/                    # Entidades de public.*
│   │   ├── Tenant.java
│   │   ├── UsuarioGlobal.java
│   │   ├── SuperAdminTenant.java
│   │   ├── Dispositivo.java
│   │   └── CodigoRegistro.java
│   └── tenant/                    # Entidades de {tenant}.*
│       ├── Empresa.java           # (modificada)
│       ├── Usuario.java           # (modificada, con PIN)
│       └── ... (resto igual)
├── repository/
│   ├── global/
│   │   ├── TenantRepository.java
│   │   ├── UsuarioGlobalRepository.java
│   │   └── DispositivoRepository.java
│   └── tenant/
│       └── ... (repositorios existentes)
├── service/
│   ├── tenant/
│   │   ├── TenantService.java
│   │   └── TenantMigrationService.java
│   └── auth/
│       ├── AuthGlobalService.java
│       ├── AuthDispositivoService.java
│       └── AuthPinService.java
├── controller/
│   ├── admin/
│   │   ├── TenantController.java
│   │   └── DispositivoAdminController.java
│   └── auth/
│       ├── AuthController.java      # (modificado)
│       └── AuthDispositivoController.java
└── dto/
    ├── tenant/
    │   ├── TenantDTO.java
    │   └── CreateTenantRequest.java
    └── auth/
        ├── LoginGlobalRequest.java
        ├── LoginEmpresaRequest.java
        ├── LoginPinRequest.java
        ├── SolicitarCodigoRequest.java
        └── VerificarCodigoRequest.java
```

### 7.2 Convenciones de Nombres

| Tipo | Convención | Ejemplo |
|------|------------|---------|
| Entidad global | `*Global` o en paquete `global` | `UsuarioGlobal` |
| Tabla master | `public.{tabla}` | `public.tenants` |
| Tabla tenant | `{schema}.{tabla}` | `tenant_snn.usuarios` |
| Código tenant | snake_case, minúsculas | `snn_soluciones` |
| Schema tenant | `tenant_` + código | `tenant_snn_soluciones` |
| Device token | UUID v4 | `f47ac10b-58cc-4372-a567-0e02b2c3d479` |
| Session token | JWT con exp 8h | Standard JWT |

### 7.3 Headers HTTP

| Header | Uso | Ejemplo |
|--------|-----|---------|
| `Authorization` | JWT global o session | `Bearer eyJ...` |
| `X-Device-Token` | Token de dispositivo | `f47ac10b-58cc-...` |
| `X-Tenant-Code` | Código del tenant (opcional) | `snn_soluciones` |

### 7.4 Códigos de Error

| Código | Descripción |
|--------|-------------|
| `TENANT_NOT_FOUND` | Código de empresa no existe |
| `DEVICE_NOT_REGISTERED` | Dispositivo sin token válido |
| `DEVICE_INACTIVE` | Dispositivo desconectado por admin |
| `CODE_EXPIRED` | Código OTP expirado |
| `CODE_INVALID` | Código OTP incorrecto |
| `CODE_ALREADY_USED` | Código OTP ya usado |
| `PIN_INVALID` | PIN incorrecto |
| `PIN_CHANGE_REQUIRED` | Debe cambiar PIN |
| `USER_INACTIVE` | Usuario desactivado |
| `SESSION_EXPIRED` | Sesión expirada |

---

## 8. API Endpoints

### 8.1 Autenticación Global

```
POST /api/auth/login
────────────────────
Request:
{
  "email": "admin@empresa.com",
  "password": "secreto123"
}

Response (200):
{
  "success": true,
  "data": {
    "token": "eyJ...",
    "refreshToken": "eyJ...",
    "usuario": {
      "id": 1,
      "email": "admin@empresa.com",
      "nombre": "Admin",
      "rol": "SUPER_ADMIN"
    },
    "tenants": [
      { "id": 1, "codigo": "snn_soluciones", "nombre": "SNN Soluciones" }
    ],
    "requiereSeleccion": true
  }
}
```

### 8.2 Autenticación por Empresa

```
POST /api/auth/empresa
──────────────────────
Request:
{
  "codigo": "SNN_SOLUCIONES"
}
Headers:
  X-Device-Token: "abc123..." (opcional)

Response (200) - Dispositivo registrado:
{
  "success": true,
  "data": {
    "tenant": {
      "id": 1,
      "codigo": "snn_soluciones",
      "nombre": "SNN Soluciones"
    },
    "dispositivo": {
      "id": 5,
      "nombre": "Caja Principal"
    },
    "usuarios": [
      { "id": 1, "nombre": "Juan Pérez", "rol": "CAJERO" },
      { "id": 2, "nombre": "María González", "rol": "MESERA" }
    ],
    "requiereRegistro": false
  }
}

Response (200) - Dispositivo nuevo:
{
  "success": true,
  "data": {
    "tenant": {
      "id": 1,
      "codigo": "snn_soluciones",
      "nombre": "SNN Soluciones"
    },
    "requiereRegistro": true
  }
}
```

### 8.3 Registro de Dispositivo

```
POST /api/auth/dispositivo/solicitar
────────────────────────────────────
Request:
{
  "tenantCodigo": "snn_soluciones",
  "nombreDispositivo": "Caja Principal",
  "plataforma": "WEB"
}

Response (200):
{
  "success": true,
  "message": "Código enviado a los administradores",
  "data": {
    "expiraEn": 600
  }
}

─────────────────────────────────────

POST /api/auth/dispositivo/verificar
────────────────────────────────────
Request:
{
  "tenantCodigo": "snn_soluciones",
  "nombreDispositivo": "Caja Principal",
  "codigo": "847291"
}

Response (200):
{
  "success": true,
  "data": {
    "deviceToken": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "tenant": {
      "id": 1,
      "codigo": "snn_soluciones",
      "nombre": "SNN Soluciones"
    },
    "usuarios": [...]
  }
}

─────────────────────────────────────

POST /api/auth/dispositivo/reenviar
───────────────────────────────────
Request:
{
  "tenantCodigo": "snn_soluciones",
  "nombreDispositivo": "Caja Principal"
}

Response (200):
{
  "success": true,
  "message": "Código reenviado"
}
```

### 8.4 Autenticación PIN

```
GET /api/auth/dispositivo/usuarios
──────────────────────────────────
Headers:
  X-Device-Token: "f47ac10b-..."

Response (200):
{
  "success": true,
  "data": {
    "tenant": {
      "codigo": "snn_soluciones",
      "nombre": "SNN Soluciones"
    },
    "dispositivo": {
      "nombre": "Caja Principal"
    },
    "usuarios": [
      { "id": 1, "nombre": "Juan", "apellidos": "Pérez", "rol": "CAJERO" },
      { "id": 2, "nombre": "María", "apellidos": "González", "rol": "MESERA" }
    ]
  }
}

─────────────────────────────────────

POST /api/auth/pin
──────────────────
Headers:
  X-Device-Token: "f47ac10b-..."
Request:
{
  "usuarioId": 1,
  "pin": "1234"
}

Response (200):
{
  "success": true,
  "data": {
    "sessionToken": "eyJ...",
    "usuario": {
      "id": 1,
      "nombre": "Juan Pérez",
      "rol": "CAJERO"
    },
    "sucursales": [...],
    "requiereCambioPin": true
  }
}

─────────────────────────────────────

POST /api/auth/pin/cambiar
──────────────────────────
Headers:
  Authorization: Bearer {sessionToken}
Request:
{
  "nuevoPin": "582947",
  "longitud": 6
}

Response (200):
{
  "success": true,
  "message": "PIN actualizado correctamente"
}

─────────────────────────────────────

POST /api/auth/cerrar-sesion
────────────────────────────
Headers:
  Authorization: Bearer {sessionToken}

Response (200):
{
  "success": true,
  "message": "Sesión cerrada"
}
```

### 8.5 Administración de Tenants (ROOT/SOPORTE)

```
GET /api/admin/tenants
──────────────────────
Headers:
  Authorization: Bearer {jwtGlobal}

Response (200):
{
  "success": true,
  "data": [
    {
      "id": 1,
      "codigo": "snn_soluciones",
      "nombre": "SNN Soluciones",
      "schemaName": "tenant_snn_soluciones",
      "activo": true,
      "dispositivosActivos": 3,
      "usuariosActivos": 12,
      "createdAt": "2025-12-01T10:00:00"
    }
  ]
}

─────────────────────────────────────

POST /api/admin/tenants
───────────────────────
Headers:
  Authorization: Bearer {jwtGlobal}
Request:
{
  "razonSocial": "Nueva Empresa S.A.",
  "superAdminEmail": "admin@nuevaempresa.com",
  "superAdminNombre": "Admin",
  "superAdminPassword": "temp123"
}

Response (201):
{
  "success": true,
  "data": {
    "tenant": {
      "id": 2,
      "codigo": "nueva_empresa",
      "schemaName": "tenant_nueva_empresa"
    },
    "superAdmin": {
      "id": 5,
      "email": "admin@nuevaempresa.com"
    }
  }
}
```

### 8.6 Administración de Dispositivos

```
GET /api/admin/tenants/{tenantId}/dispositivos
──────────────────────────────────────────────
Headers:
  Authorization: Bearer {jwtGlobal}

Response (200):
{
  "success": true,
  "data": [
    {
      "id": 1,
      "nombre": "Caja Principal",
      "plataforma": "WEB",
      "activo": true,
      "ultimoUso": "2025-12-02T14:30:00",
      "ipRegistro": "190.113.45.123"
    }
  ]
}

─────────────────────────────────────

DELETE /api/admin/dispositivos/{id}
───────────────────────────────────
Headers:
  Authorization: Bearer {jwtGlobal}

Response (200):
{
  "success": true,
  "message": "Dispositivo desconectado"
}
```

---

## 9. Checklist por Fase

### ✅ Fase 0: Preparación

- [ ] Branch `feature/multi-tenant` creado
- [ ] Documento `ARQUITECTURA.md` en `/docs/multi-tenant/`
- [ ] Backup de BD producción realizado
- [ ] Empresa piloto identificada (ID: 1)
- [ ] Código tenant piloto definido: `snn_soluciones`

### ✅ Fase 1: Infraestructura

- [ ] Tabla `public.tenants` creada
- [ ] Tabla `public.usuarios_globales` creada
- [ ] Tabla `public.super_admin_tenants` creada
- [ ] Tabla `public.dispositivos` creada
- [ ] Tabla `public.codigos_registro` creada
- [ ] Entidad `Tenant.java` creada
- [ ] Entidad `UsuarioGlobal.java` creada
- [ ] Entidad `Dispositivo.java` creada
- [ ] Entidad `CodigoRegistro.java` creada
- [ ] `TenantContext.java` implementado
- [ ] `TenantInterceptor.java` implementado
- [ ] `TenantConnectionProvider.java` implementado
- [ ] `TenantService.java` implementado
- [ ] Script SQL template para crear schema
- [ ] Tests unitarios pasando

### ✅ Fase 2: Autenticación

- [ ] `AuthGlobalService` implementado
- [ ] `POST /api/auth/login` adaptado
- [ ] `POST /api/auth/empresa` implementado
- [ ] `POST /api/auth/dispositivo/solicitar` implementado
- [ ] Template email OTP creado
- [ ] Integración con Resend funcionando
- [ ] `POST /api/auth/dispositivo/verificar` implementado
- [ ] `POST /api/auth/dispositivo/reenviar` implementado
- [ ] `GET /api/auth/dispositivo/usuarios` implementado
- [ ] `POST /api/auth/pin` implementado
- [ ] `POST /api/auth/pin/cambiar` implementado
- [ ] `POST /api/auth/cerrar-sesion` implementado
- [ ] Tests de todos los flujos auth

### ✅ Fase 3: Migración Piloto

- [ ] Script de migración creado
- [ ] Schema `tenant_snn_soluciones` creado
- [ ] Datos de empresa ID 1 migrados
- [ ] Usuarios globales migrados
- [ ] Usuarios locales migrados con PIN genérico
- [ ] Relación super_admin_tenants creada
- [ ] Validación de integridad ejecutada
- [ ] Tests E2E del piloto pasando

### ✅ Fase 4: Portal Admin

- [ ] `GET /api/admin/tenants` implementado
- [ ] `POST /api/admin/tenants` implementado
- [ ] `GET /api/admin/tenants/{id}/dispositivos` implementado
- [ ] `DELETE /api/admin/dispositivos/{id}` implementado
- [ ] Dashboard ROOT en frontend
- [ ] Dashboard SUPER_ADMIN en frontend

### ✅ Fase 5: Compatibilidad

- [ ] Flag `migrado_a_tenant` en tabla empresas
- [ ] Detector de sistema legacy vs multi-tenant
- [ ] Rutas legacy funcionando
- [ ] Nuevos clientes se crean en nuevo schema
- [ ] Indicador visual en UI

---

## 📚 Referencias

- [PostgreSQL Schemas](https://www.postgresql.org/docs/current/ddl-schemas.html)
- [Spring Multi-Tenancy](https://docs.spring.io/spring-framework/reference/data-access/multi-tenancy.html)
- [Hibernate Multi-Tenancy](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#multitenacy)
- [JWT Best Practices](https://datatracker.ietf.org/doc/html/rfc8725)
- [Resend API](https://resend.com/docs)

---

## 📝 Historial de Cambios

| Versión | Fecha | Autor | Cambios |
|---------|-------|-------|---------|
| 1.0 | 2025-12-02 | Equipo NathBit | Documento inicial |

---

*Documento generado para el proyecto NathBit POS*
*© 2025 SNN Soluciones*