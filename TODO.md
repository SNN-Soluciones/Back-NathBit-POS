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
- [x] Excepciones y `GlobalExceptionHandler`
- [x] `UsuarioController` con endpoints REST básicos
- [x] Endpoint `GET /api/me` (perfil usuario actual)
- [x] Sistema de logout con blacklist de tokens
- [x] Auditoría de eventos (login, logout, etc.)
- [x] Scheduled tasks para limpieza de tokens

### 🔧 En Proceso
- [x] Migración a `@SuperBuilder` en entidades que extienden BaseEntity
- [ ] Corrección de errores de compilación MapStruct

### 🔴 Pendientes
- [ ] Validar permisos por sucursal y caja en endpoints
- [ ] Endpoints administrativos adicionales:
  - PUT `/api/usuarios/{id}/rol`
  - POST `/api/usuarios/{id}/sucursales`
  - POST `/api/usuarios/{id}/cajas`
- [ ] Refresh token implementation
- [ ] Rate limiting para login attempts
- [ ] 2FA (Two-Factor Authentication) - opcional

---

## 🟠 FASE 3 – CRUDS BASE (A INICIAR)

### 📦 Catálogo de Productos
- [ ] `CategoriaProductoController` + Service
- [ ] `ProductoController` + Service
- [ ] DTOs y Mappers correspondientes
- [ ] Endpoints:
  - CRUD Categorías (con jerarquía)
  - CRUD Productos
  - Búsqueda y filtros
  - Importación/Exportación (CSV/Excel)
  - Control de precios por sucursal

### 👥 Gestión de Clientes
- [ ] `ClienteController` + Service
- [ ] DTOs: `ClienteRequest`, `ClienteResponse`
- [ ] Validación de cédula/RUC
- [ ] Historial de compras
- [ ] Programa de fidelidad/puntos

### 🏢 Proveedores
- [ ] CRUD completo
- [ ] Gestión de contactos
- [ ] Historial de compras

---

## 🟣 FASE 4 – FUNCIONALIDAD RESTAURANTE

### 🪑 Gestión de Mesas
- [x] Entidades: `Mesa`, `Zona`
- [ ] `MesaController` + Service
- [ ] `ZonaController` + Service
- [ ] Estados de mesa: Libre, Ocupada, Reservada, En limpieza
- [ ] Endpoints:
  - GET `/api/mesas/disponibles`
  - POST `/api/mesas/{id}/abrir`
  - POST `/api/mesas/{id}/cerrar`
  - POST `/api/mesas/{id}/unir`
  - PUT `/api/mesas/{id}/mesero`

### 📋 Gestión de Órdenes
- [x] Entidades: `Orden`, `OrdenDetalle`
- [x] `OrdenMapper` (pendiente corrección)
- [ ] `OrdenController` + Service completo
- [ ] Estados: Pendiente, En preparación, Listo, Entregado, Pagado
- [ ] Endpoints principales:
  - POST `/api/ordenes` (crear orden)
  - GET `/api/ordenes/mesa/{mesaId}`
  - POST `/api/ordenes/{id}/items`
  - DELETE `/api/ordenes/{id}/items/{itemId}`
  - PUT `/api/ordenes/{id}/estado`
  - POST `/api/ordenes/{id}/dividir`

---

## 🔵 FASE 5 – PUNTO DE VENTA (POS)

### 💰 Gestión de Caja
- [x] Entidad: `Caja`
- [ ] `CajaController` + Service
- [ ] Apertura y cierre de caja
- [ ] Arqueo de caja
- [ ] Movimientos de efectivo
- [ ] Endpoints:
  - POST `/api/cajas/{id}/abrir`
  - POST `/api/cajas/{id}/cerrar`
  - GET `/api/cajas/{id}/movimientos`
  - POST `/api/cajas/{id}/arqueo`

### 💳 Procesamiento de Pagos
- [ ] Múltiples formas de pago
- [ ] División de cuenta
- [ ] Propinas
- [ ] Descuentos y promociones

---

## 🧾 FASE 6 – FACTURACIÓN ELECTRÓNICA (HACIENDA CR)

> Esta funcionalidad será gestionada por un microservicio externo ya existente.
> El backend actual se comunicará con este sistema de manera asíncrona.

- [ ] Conexión segura al API externo de facturación
- [ ] Publicación de comprobantes para facturación vía cola o REST
- [ ] Consulta periódica del estado de comprobantes
- [ ] Manejo de respuestas de aceptación/rechazo (asíncronas)
- [ ] Persistencia de resultados (XML respuesta, PDF, estado)
- [ ] Gestión de reintentos en caso de errores transitorios

---

## 📊 FASE 7 – REPORTES Y ANÁLISIS

- [ ] Ventas por período
- [ ] Productos más vendidos
- [ ] Performance por mesero
- [ ] Análisis de horas pico
- [ ] Reportes de inventario
- [ ] Dashboard ejecutivo

---

## 🔧 MEJORAS TÉCNICAS PENDIENTES

### 🏗️ Arquitectura
- [ ] Implementar CQRS para operaciones complejas
- [ ] Event Sourcing para auditoría completa
- [ ] WebSockets para actualizaciones en tiempo real
- [ ] Cache distribuido con Redis
- [ ] Message Queue (RabbitMQ/Kafka) para operaciones asíncronas

### 🔐 Seguridad
- [ ] API Rate Limiting
- [ ] Encriptación de datos sensibles
- [ ] Auditoría completa de cambios
- [ ] Backup automático de BD

### 📱 Integraciones
- [ ] API para aplicación móvil
- [ ] Webhooks para integraciones externas
- [ ] SDK para terceros

### 🧪 Testing
- [ ] Tests unitarios (mínimo 80% cobertura)
- [ ] Tests de integración
- [ ] Tests de carga/performance
- [ ] Tests E2E

### 📚 Documentación
- [ ] Guía de instalación completa
- [ ] Manual de API con ejemplos
- [ ] Documentación de arquitectura
- [ ] Guías de troubleshooting

---

## 🚀 PRÓXIMOS PASOS INMEDIATOS

1. **Resolver errores de compilación MapStruct**
  - Completar migración a `@SuperBuilder`
  - Verificar todos los mappers
  - Asegurar build exitoso

2. **Completar módulo de seguridad**
  - Implementar refresh tokens
  - Agregar rate limiting
  - Completar endpoints de gestión de usuarios

3. **Iniciar CRUD de Productos**
  - Crear estructura base
  - Implementar categorías
  - Agregar productos con variantes

4. **Implementar gestión de Mesas**
  - Completar servicios
  - Crear controladores
  - Implementar lógica de estados

---

## 📝 NOTAS IMPORTANTES

- **Multi-tenant**: Cada operación debe validar el tenant context
- **Auditoría**: Todas las operaciones críticas deben ser auditadas
- **Performance**: Considerar índices en BD para consultas frecuentes
- **Seguridad**: Validar permisos en cada endpoint
- **Transacciones**: Usar `@Transactional` apropiadamente

---

## 🐛 BUGS CONOCIDOS

1. MapStruct no compila con `@Builder` en entidades que extienden `BaseEntity`
  - **Solución**: Migrar a `@SuperBuilder` ✅ (en proceso)

2. Logout no invalida tokens inmediatamente
  - **Solución**: Implementar blacklist ✅ (completado)

---

## 💡 IDEAS FUTURAS

- Sistema de reservaciones
- Programa de lealtad avanzado
- IA para predicción de demanda
- Integración con delivery apps
- Sistema de feedback/reviews
- Gestión de eventos especiales

---

**Última actualización**: 2025-08-02 por Sistema
**Próxima revisión**: Al completar Fase 2
