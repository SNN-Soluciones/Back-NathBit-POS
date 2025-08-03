# ✅ TODO.md — Plan de Implementación para `backnathbitpos` (Actualizado)

Este documento organiza el trabajo pendiente para el backend multi-tenant POS de restaurantes. Incluye lo que ya está hecho y lo que sigue, en orden estratégico por fase.

---

## 🟢 Estado Actual

- Ya están implementadas las principales entidades: `Tenant`, `Sucursal`, `Caja`, `Usuario`, `Cliente`, `Producto`, `Orden`, `Mesa`, `Zona`.
- Ya existe estructura de repositorios y servicios para varias entidades.
- Seguridad con JWT está configurada (login/logout básico funcionando).
- Se han creado enums de estado (`EstadoOrden`, `EstadoCaja`, etc.).
- Falta completar lógica multi-tenant a nivel de conexión y separación por esquema.
- Se estaba empezando la parte de orden (te preguntaron por los DTOs y el `OrdenMapper`).
- Ya existe estructura `TenantContext` y `TenantFilter`.

---

## 🔴 Fase 1 - Multi-Tenant (pendiente completar)

- [x] Entidad `Tenant` creada
- [x] `TenantContext` con ThreadLocal
- [x] Filtro `TenantFilter` implementado para leer `X-Tenant-ID`
- [ ] Implementar `TenantIdentifierResolver`
- [ ] Implementar `MultiTenantConnectionProvider`
- [ ] Configurar Hibernate multi-tenancy (`SCHEMA` mode)
- [ ] Ajustar `application.yml` con ejemplo de schema `demo`
- [ ] Crear migraciones Flyway multi-schema
- [ ] Crear un seed inicial con tenant `DEMO`, usuario admin y estructura básica

---

## 🟠 Fase 2 - Seguridad y Control de Acceso

- [x] Usuario, Rol y Permiso ya creados
- [x] `JwtTokenProvider`, `JwtAuthenticationFilter`, `SecurityConfig` listos
- [x] `AuthService` y `AuthController` funcionando
- [ ] Relación de Usuario ↔ Sucursal ↔ Caja
- [ ] Endpoint `/api/me` con info del usuario logueado, roles, sucursales y cajas
- [ ] Validación por sucursal/caja al consumir endpoints

---

## 🟡 Fase 3 - CRUDs Base

- [x] Entidades: `Cliente`, `Producto`, `Categoría`, `Proveedor` creadas
- [ ] Repositorios funcionando
- [ ] Servicios + Controladores para cada uno
- [ ] Validaciones + Paginación
- [ ] DTOs y Mappers (`ClienteDTO`, `ProductoDTO`, etc.)

---

## 🟣 Fase 4 - Restaurante (Mesas y Zonas)

- [x] Entidades `Mesa`, `Zona` implementadas
- [ ] Servicios para apertura/cierre de mesas
- [ ] Relación con órdenes y sucursales
- [ ] Estados de mesa: Libre, Ocupada, Cerrada

---

## 🔵 Fase 5 - Pedidos para Llevar

- [ ] CRUD de pedidos tipo Uber
- [ ] Estados de pedido: Pendiente → Cocina → Listo → Entregado
- [ ] Integración con caja/sucursal y usuario
- [ ] Asociación opcional con cliente

---

## 🧾 Fase 6 - Facturación Electrónica (via API_FE)

- [ ] Endpoint `POST /factura/rapida`
- [ ] Procesamiento de orden y factura
- [ ] Envío del JSON a `API_FE` (asíncrono, sin esperar respuesta)
- [ ] Guardado del estado local ("ENVIADO")
- [ ] Endpoint de auditoría o reintento manual (opcional)

---

## 📊 Fase 7 - Reportes

- [ ] Ventas por día, semana, mes
- [ ] Productos más vendidos
- [ ] Cierre de caja
- [ ] Historial por usuario (auditoría)

---

## 🎁 Extras

- [ ] Importar catálogo CABYS de Hacienda (CSV/XML)
- [ ] Generar PDF de facturas o tiquetes (Jasper u otro)
- [ ] Exportación de reportes (Excel, CSV)

---

## 🧪 Testing

- [ ] Pruebas unitarias (JUnit 5 + Mockito)
- [ ] Pruebas de integración
- [ ] Tests para seguridad y control de acceso

---

> Última actualización: `2025-08-02`  
> Autor: Jorge Andrés Mayorga  
> Acompañamiento por Claude + ChatGPT