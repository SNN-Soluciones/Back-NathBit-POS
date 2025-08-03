# ✅ TODO.md — Back-NathBit-POS (Actualizado al 2025-08-02)

Sistema backend multi-tenant para punto de venta en restaurantes. Construido con Java 17 y Spring Boot 3. Utiliza PostgreSQL, separación por schema, JWT, MapStruct, Flyway y arquitectura modular.

---

## ✅ FASE 1 – MULTI-TENANT ✅ (COMPLETADA)

- [x] `TenantContext` (ThreadLocal)
- [x] `TenantFilter` (extrae `X-Tenant-ID`)
- [x] `TenantIdentifierResolver`
- [x] `SchemaBasedMultiTenantConnectionProvider`
- [x] `HibernateConfig` (con corrección: `MULTI_TENANT_MODE`)
- [x] `FlywayConfig` + `TenantSchemaInitializer`
- [x] `application.yml` multitenant
- [x] Migraciones SQL:
    - `V1__create_tenant_table.sql`
    - `V2__insert_demo_tenant.sql`
    - `V1__create_base_tables.sql` (tenant)
    - `V2__create_order_tables.sql`
    - `V3__insert_seed_data.sql`

---

## 🟡 FASE 2 – SEGURIDAD Y CONTROL DE ACCESO (EN PROGRESO)

### ✅ Completado
- [x] Entidades: `Usuario`, `Rol`, `Permiso`
- [x] JWT: `JwtTokenProvider`, `JwtAuthenticationFilter`
- [x] Servicios y repositorios: `UsuarioRepository`, `UsuarioService`, `UsuarioServiceImpl`
- [x] DTOs: `UsuarioCreateRequest`, `UsuarioUpdateRequest`, `UsuarioResponse`, `CambioPasswordRequest`
- [x] `UsuarioMapper` con MapStruct
- [x] Excepciones y `GlobalExceptionHandler` (sin builder)

### 🔴 Pendientes
- [ ] `UsuarioController` con endpoints REST:
    - POST `/api/usuarios`
    - PUT `/api/usuarios/{id}`
    - GET `/api/usuarios/{id}`
    - GET `/api/usuarios`
    - DELETE `/api/usuarios/{id}`
    - PUT `/api/usuarios/{id}/password`
    - PUT `/api/usuarios/{id}/bloquear`
    - PUT `/api/usuarios/{id}/desbloquear`
- [ ] Endpoint `GET /api/me`
- [ ] Asociar `Usuario ↔ Sucursal ↔ Caja` correctamente
- [ ] Validar permisos por sucursal y caja

---

## 🟠 FASE 3 – CRUDS BASE (A INICIAR)

- [ ] Controladores + servicios: Clientes, Proveedores, Categorías, Productos
- [ ] Validación, paginación, DTOs, mappers

---

## 🟣 FASE 4 – FUNCIONALIDAD RESTAURANTE

- [x] Entidades: `Mesa`, `Zona`
- [ ] Estados de mesa: Libre, Ocupada, Cerrada
- [ ] Apertura y cierre de mesa
- [ ] Asignación de meseros
- [ ] Relación con `Sucursal`

---

## 🔵 FASE 5 – PEDIDOS PARA LLEVAR

- [ ] CRUD pedidos externos
- [ ] Estados: Pendiente, En cocina, Listo, Entregado
- [ ] Asociación opcional con cliente
- [ ] Relación con caja/sucursal

---

## 🧾 FASE 6 – FACTURACIÓN ELECTRÓNICA (INTEGRACIÓN API_FE)

- [ ] Endpoint `POST /factura/rapida`
- [ ] Construcción de datos y lógica de negocio
- [ ] Envío asíncrono a `API_FE` (no esperar respuesta)
- [ ] Registro local del envío
- [ ] Reintentos manuales o seguimiento (opcional)

---

## 📊 FASE 7 – REPORTES

- [ ] Reporte ventas por rango de fechas
- [ ] Productos más vendidos
- [ ] Auditoría de acciones por usuario
- [ ] Cierre de caja

---

## 🧪 TESTING

- [ ] Tests unitarios (servicios)
- [ ] Tests de integración (controladores)
- [ ] Seguridad y multitenancy en pruebas

---

## 🔧 NOTAS DE CONFIGURACIÓN

```properties
# Base de datos
DB_NAME=nathbitpos
DB_USER=postgres
DB_PASSWORD=tu_password
JWT_SECRET=clave-segura-de-256-bits

# Header obligatorio en peticiones autenticadas
X-Tenant-ID: demo 
```
---

## ⚙️ COMPONENTES A REVISAR
	•	OrdenServiceImpl (incompleto)
	•	UsuarioServiceImpl (requiere ajustar lógica con sucursal y caja)

> Última actualización: `2025-08-02`  
> Autor: Jorge Andrés Mayorga  
> Acompañamiento por Claude + ChatGPT
