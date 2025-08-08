# ✅ TODO - NathBit POS (Sistema Simplificado)

Sistema de punto de venta multi-empresa con arquitectura simplificada.
**Actualizado**: 2025-01-11

---

## 🎯 CAMBIOS ARQUITECTÓNICOS PRINCIPALES

### 📐 Nueva Arquitectura:
- **Un usuario = Un rol global** (eliminar roles múltiples)
- **Token JWT genérico** (contexto manejado por sesión)
- **Dashboards diferenciados** (Sistema vs Empresarial)
- **Navegación en cascada** según nivel de acceso

---

## ✅ FASE 1: SISTEMA BASE (COMPLETADO)

### ✅ Ya implementado:
- Sistema de autenticación básico
- Modelo de datos inicial
- Gestión de usuarios
- Sistema de permisos

---

## 🔄 FASE 2: REFACTORIZACIÓN DEL MODELO (URGENTE)

### ❌ PENDIENTE:

#### 🗄️ 2.1 Modificar Base de Datos
- [ ] Migración para simplificar modelo:
  ```sql
  -- V2__SIMPLIFICAR_MODELO_USUARIOS.sql
  
  -- 1. Agregar rol a usuarios
  ALTER TABLE usuarios ADD COLUMN rol VARCHAR(30);
  
  -- 2. Migrar roles desde usuarios_empresas_roles
  UPDATE usuarios u 
  SET rol = (
    SELECT uer.rol 
    FROM usuarios_empresas_roles uer 
    WHERE uer.usuario_id = u.id 
    AND uer.es_principal = true
    LIMIT 1
  );
  
  -- 3. Hacer rol NOT NULL
  ALTER TABLE usuarios ALTER COLUMN rol SET NOT NULL;
  
  -- 4. Eliminar columna rol de la tabla de relación
  ALTER TABLE usuarios_empresas_roles DROP COLUMN rol;
  
  -- 5. Renombrar tabla
  ALTER TABLE usuarios_empresas_roles RENAME TO usuarios_empresas;
  ```

#### 🔧 2.2 Actualizar Entidades JPA
- [ ] Modificar `Usuario.java`:
  ```java
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private RolNombre rol;  // Agregar este campo
  ```
- [ ] Renombrar y simplificar `UsuarioEmpresaRol.java` → `UsuarioEmpresa.java`
- [ ] Actualizar repositorios y mappers

#### 🔐 2.3 Actualizar Sistema de Auth
- [ ] Modificar `LoginResponse` para nuevo formato:
  - Lista de empresas según rol
  - Flag `requiereSeleccion`
  - Ruta destino
- [ ] Implementar en `AuthServiceImpl`:
  - Lógica diferenciada por rol
  - Construcción de respuesta según tipo usuario

---

## 🆕 FASE 3: IMPLEMENTAR CONTEXTO Y DASHBOARDS

### ❌ PENDIENTE:

#### 🎯 3.1 Sistema de Contexto
- [ ] Crear `ContextoService`:
  ```java
  @Service
  public class ContextoService {
      // Manejar contexto empresa/sucursal
      public ContextoDTO establecerContexto(Long usuarioId, Long empresaId, Long sucursalId);
      public ContextoDTO obtenerContextoActual(Long usuarioId);
      public void limpiarContexto(Long usuarioId);
  }
  ```
- [ ] Implementar endpoint `/api/auth/establecer-contexto`
- [ ] Crear filtro para inyectar contexto en requests

#### 📊 3.2 Dashboard Sistema (ROOT/SOPORTE)
- [ ] Crear `SistemaDashboardController`:
  ```java
  @RestController
  @RequestMapping("/api/sistema")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE')")
  public class SistemaDashboardController {
      // GET /empresas - Todas las empresas con métricas sistema
      // GET /configuracion - Config global
      // GET /pagos - Estados de suscripción
      // PUT /empresas/{id}/estado - Activar/Suspender
  }
  ```
- [ ] Crear DTOs específicos:
  - `EmpresaSistemaDTO` (pagos, estado, recursos)
  - `ConfiguracionSistemaDTO`

#### 💼 3.3 Dashboard Empresarial (SUPER_ADMIN)
- [ ] Crear `EmpresarialDashboardController`:
  ```java
  @RestController
  @RequestMapping("/api/empresarial")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public class EmpresarialDashboardController {
      // GET /mis-empresas - Empresas del usuario con métricas
      // GET /metricas - Consolidados
      // POST /empresas - Crear nueva empresa
  }
  ```
- [ ] Crear DTOs:
  - `EmpresaNegocioDTO` (ventas, inventarios)
  - `MetricasConsolidadasDTO`

#### 🏢 3.4 Dashboard Sucursales (Compartido)
- [ ] Actualizar endpoints existentes para manejo por contexto
- [ ] Validar acceso según rol y empresa seleccionada

---

## 📋 FASE 4: SERVICIOS DE NEGOCIO

### ❌ PENDIENTE:

#### 🏭 4.1 Servicio de Empresas
- [ ] Crear `EmpresaGestionService`:
  - Métodos diferenciados por rol
  - `obtenerTodasConMetricasSistema()` para ROOT/SOPORTE
  - `obtenerMisEmpresasConMetricas()` para SUPER_ADMIN
  - Validaciones de acceso

#### 📊 4.2 Servicio de Métricas
- [ ] Crear `MetricasService`:
  - Métricas de sistema (pagos, recursos)
  - Métricas de negocio (ventas, inventarios)
  - Consolidados por empresa/sucursal

#### 🔒 4.3 Actualizar Validaciones
- [ ] Crear `@PreAuthorize` personalizados:
  - `@EsSistema` - Solo ROOT/SOPORTE
  - `@EsEmpresaPropia` - Validar ownership
  - `@TieneContexto` - Requiere empresa/sucursal

---

## 🌐 FASE 5: ENDPOINTS FINALES

### ❌ PENDIENTE:

#### 5.1 Organizar por Módulos
```
/api/sistema/*          → ROOT/SOPORTE only
/api/empresarial/*      → SUPER_ADMIN only  
/api/admin/*            → ADMIN+ con contexto
/api/operaciones/*      → Todos con contexto
```

#### 5.2 Documentar con Swagger
- [ ] Agregar anotaciones @Operation
- [ ] Documentar respuestas por rol
- [ ] Ejemplos de requests/responses

---

## 🧪 FASE 6: TESTING Y VALIDACIÓN

### ❌ PENDIENTE:

#### 6.1 Tests de Integración
- [ ] Test flujo ROOT → Dashboard Sistema
- [ ] Test flujo SUPER_ADMIN → Dashboard Empresarial
- [ ] Test flujo ADMIN → Dashboard Sucursales
- [ ] Test flujo OPERATIVO → Sistema directo

#### 6.2 Datos de Prueba
- [ ] Script con usuarios de cada tipo
- [ ] Empresas y sucursales de ejemplo
- [ ] Métricas simuladas

---

## 🚀 PRÓXIMOS PASOS INMEDIATOS:

1. **Ejecutar migración de BD** para modelo simplificado
2. **Actualizar entidades JPA**
3. **Modificar LoginResponse** con nuevo formato
4. **Implementar contexto básico**
5. **Crear primer dashboard** (Sistema)

---

## 📝 NOTAS DE DECISIONES:

1. **Modelo simplificado**: Un rol por usuario es más claro
2. **Contexto en sesión**: Mayor flexibilidad para cambiar empresa
3. **Dashboards separados**: Mejor UX por tipo de usuario
4. **Token genérico**: Simplifica manejo de JWT

---

## ⚠️ BREAKING CHANGES:

- Usuarios ya no pueden tener múltiples roles
- Tabla `usuarios_empresas_roles` → `usuarios_empresas`
- LoginResponse cambia completamente
- Nuevas rutas para dashboards

---

Última actualización: 2025-01-11
Por: Sistema después de reunión de rediseño