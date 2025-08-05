# ✅ TODO - NathBit POS (Sistema Simplificado)

Sistema de punto de venta para restaurantes con arquitectura simplificada (sin multi-tenant).
**Actualizado**: 2025-01-03

---

## 🎯 FASE 1: SISTEMA BASE DE AUTENTICACIÓN Y PERMISOS (En Progreso)

### 📋 1.1 Modelo de Base de Datos
- [ ] Crear entidades JPA
  ```java
  - [ ] Usuario.java
  - [ ] Empresa.java  
  - [ ] Sucursal.java
  - [ ] UsuarioEmpresaRol.java
  ```

- [ ] Crear migraciones Flyway
  ```sql
  - [ ] V1__crear_tabla_usuarios.sql
  - [ ] V2__crear_tabla_empresas.sql
  - [ ] V3__crear_tabla_sucursales.sql
  - [ ] V4__crear_tabla_usuarios_empresas_roles.sql
  - [ ] V5__datos_iniciales.sql (empresa demo, usuarios de prueba)
  ```

### 🔐 1.2 Sistema de Autenticación

#### DTOs de Autenticación
- [ ] LoginRequest.java
  ```java
  - email: String
  - password: String
  ```

- [ ] LoginResponse.java
  ```java
  - token: String
  - refreshToken: String
  - usuario: UsuarioDTO
  - tipoAcceso: OPERATIVO | ADMINISTRATIVO | MULTIPLE
  - accesoDirecto: AccesoDTO (para operativos)
  - accesosDisponibles: List<AccesoDTO> (para admin)
  - requiereSeleccion: boolean
  ```

- [ ] SeleccionContextoRequest.java
  ```java
  - empresaId: Long
  - sucursalId: Long (opcional)
  ```

#### Servicios de Autenticación
- [ ] AuthService.java
  ```java
  - login(LoginRequest): LoginResponse
  - seleccionarContexto(SeleccionContextoRequest): TokenResponse
  - refresh(String refreshToken): TokenResponse
  - logout(String token): void
  ```

- [ ] UsuarioService.java
  ```java
  - findByEmail(String email): Usuario
  - obtenerAccesos(Long usuarioId): List<AccesoDTO>
  - validarAcceso(Long usuarioId, Long empresaId, Long sucursalId): boolean
  ```

#### Controladores
- [ ] AuthController.java
  ```java
  POST /api/auth/login
  POST /api/auth/seleccionar-contexto
  POST /api/auth/refresh
  POST /api/auth/logout
  ```

### 🛡️ 1.3 Sistema de Permisos

#### Componentes de Seguridad
- [ ] JwtTokenProvider.java (actualizar para nuevo modelo)
  ```java
  - Incluir empresaId, sucursalId, rol en claims
  - Validación de contexto
  ```

- [ ] SecurityConfig.java
  ```java
  - Rutas públicas: /api/auth/**
  - Rutas protegidas: resto
  ```

- [ ] PermisoService.java
  ```java
  - tienePermiso(usuario, empresa, sucursal, accion): boolean
  - obtenerPermisos(usuarioEmpresaRol): PermisoDTO
  ```

- [ ] @PreAuthorize personalizado
  ```java
  @RequierePermiso("productos.crear")
  @RequiereRol("ADMIN", "JEFE_CAJAS")
  ```

### 👤 1.4 Gestión de Usuarios

#### DTOs
- [ ] UsuarioDTO.java
- [ ] CrearUsuarioRequest.java
- [ ] ActualizarUsuarioRequest.java
- [ ] AsignarRolRequest.java

#### Servicios
- [ ] UsuarioGestionService.java
  ```java
  - crearUsuario(CrearUsuarioRequest): UsuarioDTO
  - actualizarUsuario(Long id, ActualizarUsuarioRequest): UsuarioDTO
  - asignarRol(Long usuarioId, AsignarRolRequest): void
  - removerRol(Long usuarioId, Long empresaId, Long sucursalId): void
  - listarUsuarios(Long empresaId, Long sucursalId): Page<UsuarioDTO>
  ```

#### Controladores
- [ ] UsuarioController.java
  ```java
  GET    /api/usuarios/perfil
  PUT    /api/usuarios/perfil
  GET    /api/usuarios (paginado, filtros)
  POST   /api/usuarios
  PUT    /api/usuarios/{id}
  DELETE /api/usuarios/{id}
  POST   /api/usuarios/{id}/roles
  DELETE /api/usuarios/{id}/roles/{rolId}
  ```

### 🏢 1.5 Gestión de Empresas y Sucursales

#### DTOs
- [ ] EmpresaDTO.java
- [ ] SucursalDTO.java
- [ ] CrearEmpresaRequest.java
- [ ] CrearSucursalRequest.java

#### Servicios
- [ ] EmpresaService.java
- [ ] SucursalService.java

#### Controladores
- [ ] EmpresaController.java
- [ ] SucursalController.java

---

## 🧪 TESTS FASE 1

### Tests Unitarios
- [ ] AuthServiceTest
- [ ] UsuarioServiceTest
- [ ] PermisoServiceTest
- [ ] JwtTokenProviderTest

### Tests de Integración
- [ ] AuthControllerIntegrationTest
- [ ] UsuarioControllerIntegrationTest
- [ ] PermisoIntegrationTest

### Postman Collection
- [ ] Crear colección con todos los endpoints
- [ ] Ejemplos para cada tipo de usuario
- [ ] Tests automatizados

---

## 📝 DOCUMENTACIÓN FASE 1

- [ ] Swagger/OpenAPI configurado
- [ ] README actualizado con ejemplos
- [ ] Guía de instalación
- [ ] Ejemplos de uso de la API

---

## ✅ CRITERIOS DE ÉXITO FASE 1

1. **Login funcional**
  - [ ] Usuario puede hacer login con email/password
  - [ ] Recibe JWT válido
  - [ ] Token incluye información de contexto

2. **Selección de contexto**
  - [ ] Usuarios admin ven lista de empresas/sucursales
  - [ ] Pueden seleccionar dónde trabajar
  - [ ] Usuarios operativos van directo al POS

3. **Permisos funcionando**
  - [ ] Validación por rol
  - [ ] Validación por permisos JSON
  - [ ] Bloqueo de acciones no autorizadas

4. **CRUD usuarios completo**
  - [ ] Crear usuarios
  - [ ] Asignar roles por empresa/sucursal
  - [ ] Modificar permisos
  - [ ] Activar/desactivar usuarios

---

## 🚀 COMANDOS ÚTILES

```bash
# Limpiar y construir
./gradlew clean build

# Ejecutar tests
./gradlew test

# Ejecutar aplicación
./gradlew bootRun

# Generar reporte de cobertura
./gradlew jacocoTestReport

# Ver logs SQL
./gradlew bootRun -Dspring.jpa.show-sql=true
```

---

## 🎯 SIGUIENTES FASES (Después de completar Fase 1)

### FASE 2: Módulos de Negocio Básicos
- Productos y Categorías
- Clientes
- Proveedores

### FASE 3: Punto de Venta
- Órdenes
- Mesas
- Caja y pagos

### FASE 4: Inventario y Reportes
- Control de inventario
- Movimientos
- Reportes básicos

### FASE 5: Facturación Electrónica
- Integración con API externa
- Gestión de comprobantes

---

## 💡 NOTAS IMPORTANTES

1. **Sin Multi-tenant**: Todo en una sola base de datos
2. **Filtrado por contexto**: Siempre filtrar por empresa_id/sucursal_id
3. **Permisos flexibles**: Usar JSON para permisos granulares
4. **JWT con contexto**: Token incluye empresa y sucursal actual
5. **Validación doble**: Por rol Y por permisos específicos

---

Última actualización: 2025-01-03