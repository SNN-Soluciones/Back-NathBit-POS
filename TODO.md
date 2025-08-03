# ✅ TODO.md — NathBit POS (Actualizado al 2025-01-03)

Sistema backend multi-tenant para punto de venta en restaurantes. Java 17 + Spring Boot 3.
**IMPORTANTE**: Tenant = Sucursal (no empresa). Usuarios globales en schema público.

---

## ✅ FASE 1 – CONFIGURACIÓN BASE MULTI-TENANT ✅ (COMPLETADA)

- [x] `TenantContext` (ThreadLocal)
- [x] `TenantFilter`
- [x] `TenantIdentifierResolver`
- [x] `SchemaBasedMultiTenantConnectionProvider`
- [x] `HibernateConfig`
- [x] `FlywayConfig` + `TenantSchemaInitializer`
- [x] `application.yml` multitenant
- [x] Migraciones SQL base

---

## ✅ FASE 2 – SEGURIDAD BÁSICA ✅ (COMPLETADA)

- [x] Entidades: `Usuario`, `Rol`, `Permiso`
- [x] JWT: `JwtTokenProvider`, `JwtAuthenticationFilter`
- [x] `SecurityConfig` con Spring Security
- [x] Rate limiting básico
- [x] Auditoría de eventos

---

## 🔴 FASE 3 – REDEFINICIÓN ARQUITECTURA (EN PROGRESO - CRÍTICO)

### 📋 Redefinir Modelo de Usuarios y Accesos

#### 1️⃣ **Nuevas Tablas en Schema PUBLIC**
- [ ] Crear tabla `usuarios_global`
  ```sql
  CREATE TABLE public.usuarios_global (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      email VARCHAR(255) UNIQUE NOT NULL,
      password VARCHAR(255) NOT NULL,
      nombre VARCHAR(100) NOT NULL,
      apellidos VARCHAR(100),
      telefono VARCHAR(20),
      identificacion VARCHAR(50),
      tipo_identificacion VARCHAR(20),
      activo BOOLEAN DEFAULT true,
      bloqueado BOOLEAN DEFAULT false,
      intentos_fallidos INTEGER DEFAULT 0,
      ultimo_acceso TIMESTAMP,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  );
  ```

- [ ] Crear tabla `empresas`
  ```sql
  CREATE TABLE public.empresas (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      codigo VARCHAR(50) UNIQUE NOT NULL,
      nombre VARCHAR(200) NOT NULL,
      nombre_comercial VARCHAR(200),
      cedula_juridica VARCHAR(50),
      telefono VARCHAR(20),
      email VARCHAR(100),
      direccion TEXT,
      tipo VARCHAR(50), -- RESTAURANTE, CAFETERIA, BAR, COMIDA_RAPIDA
      activa BOOLEAN DEFAULT true,
      configuracion JSONB,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  );
  ```

- [ ] Crear tabla `empresas_sucursales`
  ```sql
  CREATE TABLE public.empresas_sucursales (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      empresa_id UUID NOT NULL REFERENCES empresas(id),
      codigo_sucursal VARCHAR(50) NOT NULL,
      nombre_sucursal VARCHAR(200) NOT NULL,
      schema_name VARCHAR(50) UNIQUE NOT NULL, -- nombre del tenant
      direccion TEXT,
      telefono VARCHAR(20),
      activa BOOLEAN DEFAULT true,
      es_principal BOOLEAN DEFAULT false,
      configuracion JSONB,
      UNIQUE(empresa_id, codigo_sucursal)
  );
  ```

- [ ] Crear tabla `usuario_empresas`
  ```sql
  CREATE TABLE public.usuario_empresas (
      usuario_id UUID NOT NULL REFERENCES usuarios_global(id),
      empresa_id UUID NOT NULL REFERENCES empresas(id),
      rol VARCHAR(50) NOT NULL, -- SUPER_ADMIN, ADMIN, JEFE_CAJAS, CAJERO, MESERO
      fecha_asignacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      asignado_por UUID,
      activo BOOLEAN DEFAULT true,
      PRIMARY KEY (usuario_id, empresa_id)
  );
  ```

- [ ] Crear tabla `usuario_sucursales`
  ```sql
  CREATE TABLE public.usuario_sucursales (
      usuario_id UUID NOT NULL,
      empresa_id UUID NOT NULL,
      sucursal_id UUID NOT NULL REFERENCES empresas_sucursales(id),
      puede_leer BOOLEAN DEFAULT true,
      puede_escribir BOOLEAN DEFAULT true,
      es_principal BOOLEAN DEFAULT false,
      activo BOOLEAN DEFAULT true,
      PRIMARY KEY (usuario_id, sucursal_id),
      FOREIGN KEY (usuario_id, empresa_id) REFERENCES usuario_empresas(usuario_id, empresa_id)
  );
  ```

#### 2️⃣ **Crear Vistas en Cada Tenant**
- [ ] Vista `usuarios` en cada schema tenant
  ```sql
  CREATE OR REPLACE VIEW usuarios AS
  SELECT 
      ug.id,
      ug.email,
      ug.password,
      ug.nombre,
      ug.apellidos,
      ug.telefono,
      ug.identificacion,
      ug.tipo_identificacion,
      ug.activo,
      ug.bloqueado,
      ug.ultimo_acceso,
      ue.rol,
      current_schema() as tenant_id,
      es.empresa_id,
      e.nombre as empresa_nombre
  FROM public.usuarios_global ug
  JOIN public.usuario_empresas ue ON ug.id = ue.usuario_id
  JOIN public.empresas e ON ue.empresa_id = e.id
  JOIN public.empresas_sucursales es ON es.empresa_id = e.id
  JOIN public.usuario_sucursales us ON us.usuario_id = ug.id AND us.sucursal_id = es.id
  WHERE es.schema_name = current_schema()
    AND ue.activo = true
    AND us.activo = true;
  ```

#### 3️⃣ **Migrar Datos Existentes**
- [ ] Script para migrar usuarios actuales a `usuarios_global`
- [ ] Crear empresa y sucursal por defecto para datos existentes
- [ ] Establecer relaciones en tablas de acceso

---

### 🔧 Refactorizar Sistema de Autenticación

#### 1️⃣ **Nuevas Entidades Java**
- [ ] Crear `UsuarioGlobal.java` (schema public)
- [ ] Crear `Empresa.java`
- [ ] Crear `EmpresaSucursal.java`
- [ ] Crear `UsuarioEmpresa.java`
- [ ] Crear `UsuarioSucursal.java`

#### 2️⃣ **Nuevos DTOs**
- [ ] `LoginResponse` con soporte multi-empresa
  ```java
  public class LoginResponse {
      private String accessToken;
      private String refreshToken;
      private String tipoUsuario; // OPERATIVO o ADMINISTRATIVO
      private List<EmpresaAcceso> empresasDisponibles;
      private boolean requiereSeleccion;
      private SucursalInfo sucursalDirecta; // Para cajeros
  }
  ```

- [ ] `EmpresaAcceso.java`
- [ ] `SucursalInfo.java`
- [ ] `SeleccionContextoRequest.java`

#### 3️⃣ **Modificar Servicios**
- [ ] Refactorizar `AuthServiceImpl` completamente
  - [ ] Login sin tenant obligatorio
  - [ ] Detección de tipo de usuario
  - [ ] Flujo diferenciado operativo/administrativo

- [ ] Crear `UsuarioGlobalService`
- [ ] Crear `EmpresaService`
- [ ] Crear `AccesoService` para gestionar permisos

#### 4️⃣ **Actualizar Controladores**
- [ ] Modificar `AuthController`
  - [ ] Endpoint `/login` sin tenant
  - [ ] Nuevo endpoint `/select-context`
  - [ ] Endpoint `/switch-sucursal` para admins

#### 5️⃣ **Seguridad y Filtros**
- [ ] Modificar `TenantFilter`
  - [ ] No requerir tenant para rutas públicas
  - [ ] Obtener tenant del JWT, no del header
  - [ ] Validar acceso al tenant

- [ ] Actualizar `JwtTokenProvider`
  - [ ] Incluir empresa_id y sucursal_id en claims
  - [ ] Tokens temporales para selección

- [ ] Crear `AccessControlService`
  - [ ] Validar permisos usuario-empresa-sucursal
  - [ ] Cache de permisos

---

## 🟡 FASE 4 – DETECCIÓN INTELIGENTE DE SUCURSAL

### 1️⃣ **Sistema de Detección por IP**
- [ ] Tabla `sucursal_redes`
  ```sql
  CREATE TABLE public.sucursal_redes (
      id UUID PRIMARY KEY,
      sucursal_id UUID NOT NULL,
      ip_inicio INET,
      ip_fin INET,
      descripcion VARCHAR(200),
      activa BOOLEAN DEFAULT true
  );
  ```

- [ ] Servicio `SucursalDetectionService`
- [ ] Configuración de rangos IP por sucursal

### 2️⃣ **Configuración por Terminal**
- [ ] Tabla `terminal_config` para POS fijos
- [ ] API para registrar terminales
- [ ] Validación de MAC address (opcional)

---

## 🟢 FASE 5 – MÓDULOS DE NEGOCIO

### 📦 Gestión de Productos
- [ ] Migrar `Producto` y `CategoriaProducto` al nuevo modelo
- [ ] `ProductoController` + `ProductoService`
- [ ] CRUD completo con paginación
- [ ] Búsqueda y filtros
- [ ] Gestión de modificadores
- [ ] Precios por sucursal (opcional)

### 👥 Gestión de Clientes
- [ ] `ClienteController` + `ClienteService`
- [ ] Validación de identificación (cédula CR)
- [ ] Historial de compras
- [ ] Programa de puntos (fase 2)

### 🪑 Gestión de Mesas y Zonas
- [ ] `MesaController` + `MesaService`
- [ ] `ZonaController` + `ZonaService`
- [ ] Estados de mesa en tiempo real
- [ ] Asignación de meseros
- [ ] Transferencia entre mesas

### 📋 Sistema de Órdenes
- [ ] `OrdenController` + `OrdenService`
- [ ] Crear y modificar órdenes
- [ ] Estados de orden
- [ ] División de cuentas
- [ ] Integración con cocina

### 💰 Gestión de Caja
- [ ] `CajaController` + `CajaService`
- [ ] Apertura y cierre de caja
- [ ] Arqueo de caja
- [ ] Movimientos de efectivo
- [ ] Reportes de cierre

---

## 🔵 FASE 6 – INTEGRACIONES

### 🧾 Facturación Electrónica
- [ ] Cliente HTTP para API externa
- [ ] Colas de procesamiento asíncrono
- [ ] Manejo de reintentos
- [ ] Almacenamiento de respuestas

### 📊 Reportes
- [ ] Servicio de reportes básicos
- [ ] Ventas por período
- [ ] Productos más vendidos
- [ ] Performance por empleado

---

## 🟣 FASE 7 – OPTIMIZACIONES

### Performance
- [ ] Implementar caché Redis
- [ ] Índices de base de datos optimizados
- [ ] Lazy loading inteligente
- [ ] Connection pooling ajustado

### Monitoreo
- [ ] Integración con Actuator
- [ ] Métricas personalizadas
- [ ] Health checks por tenant
- [ ] Alertas de sistema

---

## 📝 DOCUMENTACIÓN

- [ ] Actualizar README.md ✅
- [ ] Guía de instalación detallada
- [ ] Manual de API con ejemplos
- [ ] Documentación de arquitectura
- [ ] Guías de troubleshooting

---

## 🐛 BUGS CONOCIDOS

1. ~~MapStruct no compila con `@Builder`~~ ✅ RESUELTO
2. TenantFilter requiere tenant en todas las rutas (debe ser opcional)
3. Login actual requiere tenant_id (debe eliminarse)

---

## 📊 PRIORIDADES INMEDIATAS

### 🔴 Crítico (Esta semana)
1. Implementar modelo de usuarios globales
2. Refactorizar login para no requerir tenant
3. Crear vistas en tenants

### 🟡 Importante (Próximas 2 semanas)
1. Sistema de detección de sucursal
2. CRUD de productos
3. Gestión básica de órdenes

### 🟢 Normal (Próximo mes)
1. Reportes
2. Optimizaciones
3. Documentación completa

---

## 🎯 CRITERIOS DE ÉXITO

- [ ] Cajeros pueden hacer login sin ver selectores de empresa
- [ ] Admins pueden cambiar entre sucursales sin relogin
- [ ] Un usuario puede tener diferentes roles en diferentes empresas
- [ ] El sistema detecta automáticamente la sucursal por IP
- [ ] Cero duplicación de datos de usuarios
- [ ] Performance < 200ms en operaciones críticas

---

## 💡 NOTAS IMPORTANTES

- **Multi-tenant**: Cada sucursal = schema PostgreSQL independiente
- **Usuarios globales**: Todos en schema `public`, acceden según permisos
- **Seguridad**: Validar SIEMPRE tenant + rol + permisos en cada operación
- **Performance**: Usar vistas materializadas si las vistas normales son lentas
- **Simplicidad**: Evitar sobre-ingeniería, empezar simple y evolucionar

---

## 🚀 COMANDOS ÚTILES

```bash
# Ejecutar migraciones
./gradlew flywayMigrate

# Limpiar y reconstruir
./gradlew clean build

# Ejecutar tests
./gradlew test

# Generar reporte de cobertura
./gradlew jacocoTestReport

# Ejecutar con perfil de desarrollo
./gradlew bootRun --args='--spring.profiles.active=dev'
```

---

Última actualización: 2025-01-03 por @equipo-backend