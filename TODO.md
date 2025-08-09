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

## ✅ COMPLETADO (Fase 2 - Facturación Electrónica)

### Entidades - Facturación
- [x] Actualización Empresa (campos adicionales)
- [x] Actualización Sucursal (numeroSucursal, modoFacturacion)
- [x] EmpresaConfigHacienda
- [x] Terminal (cajas/puntos de venta)
- [x] ActividadEconomica
- [x] EmpresaActividad
- [x] SesionCaja
- [x] Moneda
- [x] TipoCambio
- [x] Provincia, Canton, Distrito, Barrio

### Enums - Facturación
- [x] RegimenTributario
- [x] ModoFacturacion
- [x] AmbienteHacienda
- [x] TipoAutenticacionHacienda
- [x] EstadoSesion
- [x] TipoDocumento

### Repositorios - Facturación
- [x] EmpresaRepository (actualizado)
- [x] SucursalRepository (actualizado)
- [x] TerminalRepository
- [x] EmpresaConfigHaciendaRepository
- [x] ActividadEconomicaRepository
- [x] EmpresaActividadRepository
- [x] SesionCajaRepository
- [x] MonedaRepository
- [x] TipoCambioRepository
- [x] Ubicación CR (4 repositorios)

### Servicios - Facturación
- [x] EmpresaService (actualizado con config Hacienda)
- [x] SucursalService (actualizado con terminales)
- [x] TerminalService + Implementación
- [x] SesionCajaService + Implementación
- [x] UbicacionService + Implementación
- [x] TipoCambioService + Implementación

### Documentación
- [x] guia-control-accesos.md
- [x] guia-control-empresa-factura.md
- [x] guia-control-facturacion.md
- [x] Documentación sistema facturación

## 🔄 EN PROGRESO (Fase 3)

### DTOs - Facturación
- [ ] DTOs para Terminal
- [ ] DTOs para SesionCaja
- [ ] DTOs para ConfigHacienda
- [ ] DTOs para Ubicación
- [ ] DTOs para TipoCambio

### Controladores - Facturación
- [ ] TerminalController
- [ ] SesionCajaController
- [ ] ConfigHaciendaController
- [ ] UbicacionController
- [ ] TipoCambioController

### Testing
- [ ] Tests unitarios para servicios
- [ ] Tests de integración para controladores
- [ ] Tests de seguridad

## 📅 PENDIENTE (Fases Futuras)

### Fase 4 - Módulos de Negocio
- [ ] Entidad Producto
- [ ] Entidad Categoría
- [ ] Entidad Cliente
- [ ] Entidad Inventario
- [ ] Servicios y controladores correspondientes

### Fase 5 - Sistema de Ventas
- [ ] Entidad Orden/Venta
- [ ] Entidad DetalleOrden
- [ ] Entidad DocumentoEmitido
- [ ] Generación de consecutivos
- [ ] Generación de claves

### Fase 6 - API Facturación Externa
- [ ] API separada para facturación
- [ ] Conversión JSON a XML
- [ ] Firma digital XAdES
- [ ] Integración con Hacienda
- [ ] Generación de PDF
- [ ] Envío por correo

### Fase 7 - Reportes y Analytics
- [ ] Dashboard por rol
- [ ] Reportes de ventas
- [ ] Reportes de cierres de caja
- [ ] Reportes fiscales
- [ ] Métricas por empresa/sucursal

### Fase 8 - Optimizaciones
- [ ] Caché con Redis
- [ ] Paginación optimizada
- [ ] Búsquedas con filtros
- [ ] Auditoría de cambios
- [ ] Scheduled tasks (tipos cambio)

### Fase 9 - Integraciones
- [ ] API BCCR (tipos de cambio)
- [ ] Pasarelas de pago
- [ ] Notificaciones push
- [ ] WebSockets para tiempo real

## 🐛 BUGS CONOCIDOS
- Ninguno reportado

## 💡 MEJORAS SUGERIDAS
- [ ] Implementar refresh token en tabla separada
- [ ] Agregar logs estructurados
- [ ] Mejorar mensajes de error
- [ ] Sistema de notificaciones para consecutivos por agotar
- [ ] Dashboard de monitoreo de facturación

## 📝 NOTAS IMPORTANTES

### Modelo de Facturación
- **Empresa** puede tener múltiples sucursales
- **Sucursal** puede ser ELECTRONICO o SOLO_INTERNO
- **Terminal** maneja sus propios consecutivos
- **Consecutivo**: 20 dígitos (sucursal + terminal + tipo + numero)
- **Clave**: 50 dígitos (incluye fecha, identificación, situación)

### Configuración Hacienda
- Soporta Llave ATV (por ahora)
- Ambiente: SANDBOX o PRODUCCION
- Proveedor sistemas: Tu cédula personal

### Límites del Sistema
- Máximo 2 terminales por sucursal
- Consecutivos hasta 9,999,999,999
- Alerta en 9,999,999,000

### Próximos Pasos Críticos
1. Crear DTOs para las nuevas entidades
2. Implementar controladores REST
3. Crear API facturación separada
4. Implementar carga de datos maestros (ubicaciones, actividades)
5. Crear frontend para configuración

---

*Última actualización: Incluye sistema completo de facturación electrónica y control interno*