# ✅ TODO - NathBit POS (Sistema Simplificado)

Sistema de punto de venta multi-empresa con arquitectura simplificada.
**Actualizado**: 2025-01-11

---

## 🎯 CAMBIOS ARQUITECTÓNICOS PRINCIPALES

### 📐 Nueva Arquitectura:
- **Un usuario = Un rol global** (eliminar roles múltiples) ✅
- **Token JWT genérico** (contexto manejado por sesión) ✅
- **Dashboards diferenciados** (Sistema vs Empresarial) ✅
- **Navegación en cascada** según nivel de acceso ✅

---

## ✅ FASE 1: SISTEMA BASE (COMPLETADO)

### ✅ Ya implementado:
- Sistema de autenticación básico
- Modelo de datos inicial
- Gestión de usuarios
- Sistema de permisos

---

## ✅ FASE 2: REFACTORIZACIÓN DEL MODELO (COMPLETADO - 2025-01-11)

### ✅ COMPLETADO:

#### ✅ 2.1 Base de Datos
- [x] Migración SQL para nuevo modelo (V2__SIMPLIFICAR_MODELO_USUARIOS.sql)
- [x] Tabla usuarios con rol global
- [x] Tabla usuarios_empresas sin rol
- [x] Scripts de creación desde cero
- [x] Datos de prueba actualizados

#### ✅ 2.2 Entidades JPA
- [x] Usuario.java con campo rol y métodos de utilidad
- [x] UsuarioEmpresa.java (renombrada desde UsuarioEmpresaRol)
- [x] Repositorios actualizados
- [x] Mappers actualizados

#### ✅ 2.3 Sistema de Auth
- [x] LoginResponse con nuevo formato
- [x] AuthServiceImpl con lógica diferenciada por rol
- [x] JwtTokenProvider con soporte para contexto
- [x] ContextoService implementado
- [x] AuthController completo

#### ✅ 2.4 Seguridad
- [x] CustomUserDetailsService actualizado
- [x] ContextoFilter para inyectar contexto
- [x] SecurityConfig con nuevo filtro
- [x] SeguridadService para validaciones @PreAuthorize

#### ✅ 2.5 DTOs y Modelos
- [x] UsuarioDTO actualizado
- [x] ContextoDTO creado
- [x] LoginResponse con estructura por rol
- [x] DTOs de requests (SeleccionContextoRequest, etc.)

---

## 🔄 FASE 3: IMPLEMENTAR DASHBOARDS (PARCIALMENTE COMPLETADO)

### ✅ COMPLETADO:
- [x] SistemaDashboardController (ROOT/SOPORTE)
- [x] DTOs para dashboard del sistema

### ❌ PENDIENTE:
- [ ] Dashboard Empresarial (SUPER_ADMIN)
- [ ] Dashboard Sucursales (ADMIN)
- [ ] Dashboard Operativo (CAJERO/MESERO/etc)

---

## 🆕 FASE 4: GESTIÓN DE USUARIOS (PENDIENTE)

### ❌ PENDIENTE:
- [ ] UsuarioController con CRUD completo
- [ ] Asignación de empresas/sucursales
- [ ] Cambio de contraseña
- [ ] Recuperación de contraseña
- [ ] Gestión de permisos

---

## 🏢 FASE 5: GESTIÓN EMPRESARIAL (PENDIENTE)

### ❌ PENDIENTE:
- [ ] EmpresaController
- [ ] EmpresaService
- [ ] SucursalController
- [ ] SucursalService
- [ ] ConfiguracionController

---

## 🛒 FASE 6: MÓDULOS OPERATIVOS (PENDIENTE)

### ❌ PENDIENTE:
- [ ] Productos
- [ ] Categorías
- [ ] Clientes
- [ ] Órdenes
- [ ] Facturación
- [ ] Reportes

---

## 📝 NOTAS DE IMPLEMENTACIÓN

### Archivos clave creados/modificados:
1. **Entidades**: Usuario, UsuarioEmpresa
2. **Repositorios**: UsuarioRepository, UsuarioEmpresaRepository
3. **Servicios**: AuthService, ContextoService, SeguridadService
4. **Controladores**: AuthController, SistemaDashboardController
5. **Seguridad**: JwtTokenProvider, ContextoFilter, CustomUserDetailsService
6. **DTOs**: Múltiples DTOs para auth y usuarios

### Decisiones técnicas:
- JWT contiene contexto empresa/sucursal después de selección
- Filtro de contexto inyecta información en cada request
- Authorities basadas en rol global + permisos específicos
- Sin manejo de archivos/logos (fuera del scope)