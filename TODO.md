## 📝 **TODO.md**

```markdown
# 📋 TODO - NathBit POS Backend

## ✅ COMPLETADO (Fase 1 - Base del Sistema)

### Entidades y Modelo de Datos
- [x] Entidad Usuario con rol único
- [x] Entidad Empresa
- [x] Entidad Sucursal  
- [x] Entidad UsuarioEmpresa (relación sin rol)
- [x] Enums: RolNombre, TipoIdentificacion

### Repositorios
- [x] UsuarioRepository
- [x] EmpresaRepository
- [x] SucursalRepository
- [x] UsuarioEmpresaRepository

### Servicios
- [x] UsuarioService + Implementación
- [x] EmpresaService + Implementación
- [x] SucursalService + Implementación
- [x] UsuarioEmpresaService + Implementación
- [x] AuthService + Implementación

### Seguridad
- [x] JWT Token Provider
- [x] JWT Authentication Filter
- [x] JWT Authentication Entry Point
- [x] Custom UserDetailsService
- [x] Security Configuration
- [x] CORS Configuration

### Controladores
- [x] AuthController (login, contexto, refresh)
- [x] EmpresaController (CRUD)
- [x] SucursalController (CRUD)
- [x] UsuarioController (CRUD + asignaciones)

### DTOs
- [x] ApiResponse (common)
- [x] DTOs de Usuario
- [x] DTOs de Empresa
- [x] DTOs de Sucursal
- [x] DTOs de Auth

### Configuraciones
- [x] application.yml
- [x] Manejo global de excepciones
- [x] Configuración de base de datos

## 🔄 EN PROGRESO (Fase 2)

### Testing
- [ ] Tests unitarios para servicios
- [ ] Tests de integración para controladores
- [ ] Tests de seguridad

### Documentación
- [ ] Swagger/OpenAPI completo
- [ ] Postman collection
- [ ] Javadoc en servicios

## 📅 PENDIENTE (Fases Futuras)

### Fase 3 - Módulos de Negocio
- [ ] Entidad Producto
- [ ] Entidad Categoría
- [ ] Entidad Cliente
- [ ] Entidad Inventario
- [ ] Servicios y controladores correspondientes

### Fase 4 - Sistema de Ventas
- [ ] Entidad Orden
- [ ] Entidad DetalleOrden
- [ ] Entidad Mesa
- [ ] Entidad Caja
- [ ] Sistema de facturación

### Fase 5 - Reportes y Analytics
- [ ] Dashboard por rol
- [ ] Reportes de ventas
- [ ] Reportes de inventario
- [ ] Métricas por empresa/sucursal

### Fase 6 - Optimizaciones
- [ ] Caché con Redis
- [ ] Paginación optimizada
- [ ] Búsquedas con filtros
- [ ] Auditoría de cambios

### Fase 7 - Integraciones
- [ ] Facturación electrónica
- [ ] Pasarelas de pago
- [ ] Notificaciones push
- [ ] WebSockets para tiempo real

## 🐛 BUGS CONOCIDOS
- Ninguno reportado

## 💡 MEJORAS SUGERIDAS
- [ ] Implementar refresh token en tabla separada
- [ ] Agregar logs estructurados
- [ ] Mejorar mensajes de error
- [ ] Agregar validaciones de negocio más complejas

## 📝 NOTAS
- Mantener el modelo simple: un usuario = un rol
- El contexto empresa/sucursal se maneja por sesión
- ROOT y SOPORTE pueden acceder a cualquier empresa
- Los operativos tienen contexto fijo