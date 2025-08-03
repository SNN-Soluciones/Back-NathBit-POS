# ✅ TODO.md — Plan de Implementación para `backnathbitpos`

Este documento organiza el trabajo pendiente para el desarrollo del backend multi-tenant del sistema POS para restaurantes. El objetivo es construir una API REST robusta, modular, escalable y lista para producción.

---

## 🟠 Fase 1 - Configuración Multi-Tenant (TenantResolver)

- [ ] Crear entidad `Tenant` y sus relaciones con sucursales y cajas
- [ ] Implementar `TenantIdentifierResolver`
- [ ] Implementar `MultiTenantConnectionProvider`
- [ ] Configurar Hibernate y Spring para multi-tenancy
- [ ] Interceptor para extraer `X-Tenant-ID` desde headers HTTP
- [ ] Seed de datos iniciales por tenant (Flyway)

> Claude: ayudame a configurar un backend multi-tenant en Spring Boot usando PostgreSQL y separación por schema o base de datos (por definir), usando el header `X-Tenant-ID`.

---

## 🔐 Fase 2 - Seguridad y Control de Acceso (JWT)

- [ ] Crear entidad `Usuario`, `Rol`, `Permiso`, `Sucursal`, `Caja`
- [ ] Relación: usuario puede estar en muchas sucursales, y manejar varias cajas
- [ ] Autenticación vía `/auth/login` con JWT
- [ ] Middleware que verifique permisos de usuario según endpoint
- [ ] Endpoint `/me` para obtener info del usuario logueado y sus roles/permisos

> Claude: ayúdame a implementar autenticación JWT multi-sucursal con control de roles y permisos. El token debe incluir sucursales permitidas y cajas disponibles.

---

## 📦 Fase 3 - CRUDs Base de Negocio

- [ ] Clientes
- [ ] Proveedores
- [ ] Categorías
- [ ] Productos (asociados a CABYS)
- [ ] Compras
- [ ] Inventarios (ajustes, ingresos, egresos)
- [ ] MapStruct + DTOs para cada módulo

> Claude: generá controladores, servicios, DTOs y repositorios para estos CRUDs. Aplicá buenas prácticas de validación y paginación.

---

## 🍽️ Fase 4 - Estructura de Restaurante

- [ ] Entidades `Zona`, `Mesa`, `Mesero`
- [ ] Estado de mesa (libre, ocupada, cerrada)
- [ ] Abrir/cerrar mesa
- [ ] Relación con sucursal y caja
- [ ] Control de pedidos en mesa

---

## 🛍️ Fase 5 - Pedidos para Llevar (Takeaway/UberEats-style)

- [ ] CRUD de pedidos externos
- [ ] Estados: Pendiente → En cocina → Listo → Entregado
- [ ] Relación opcional con cliente
- [ ] Asociación con caja/sucursal

---

## 🧾 Fase 6 - Facturación Electrónica (Asíncrona vía API_FE)

- [ ] Endpoint `POST /factura/rapida` para emitir facturas
- [ ] Procesar internamente la lógica de negocio del POS
- [ ] Enviar JSON a `API_FE` por `RestTemplate` o `WebClient`
- [ ] No esperar respuesta (no bloqueante)
- [ ] Guardar estado local como "ENVIADO"

> Claude: generá un servicio que procese una factura rápida y la envíe a un endpoint externo sin esperar respuesta.

---

## 📊 Fase 7 - Reportes

- [ ] Ventas por día, semana, mes, año
- [ ] Top 5 productos más vendidos
- [ ] Reporte de cierre de caja
- [ ] Historial por usuario (auditoría)

---

## 📚 Fase 8 - Extras

- [ ] Importador del catálogo CABYS desde Hacienda
- [ ] Servicio para generar PDF de factura (opcional con JasperReports)
- [ ] Exportar reportes a Excel o CSV

---

## 🧪 Fase 9 - Testing

- [ ] Pruebas unitarias (Junit 5 + Mockito)
- [ ] Pruebas de integración de endpoints
- [ ] Tests de seguridad y autenticación

---

## 🚀 Fase 10 - Deploy & Productividad

- [ ] Configuración de perfiles: `dev`, `test`, `prod`
- [ ] Configuración en Spring Boot Admin / Actuator
- [ ] Soporte para JAR y WAR en Tomcat
- [ ] Documentación Swagger / OpenAPI

---

### ✅ Tips para trabajar con Claude:
- Compartí clases clave o estructuras para que pueda analizarlas.
- Pedile ayuda por fase: `"Ayúdame con Fase 3: Productos + Categorías."`
- Usá prompts como:
  > "Genera una clase `ProductoController` con endpoints CRUD usando MapStruct, validaciones y paginación."

---

> Última actualización: `2025-08-02`  
> Mantené este archivo actualizado conforme completes funcionalidades ✔️