# 📚 Documentación Sistema de Permisos y Navegación
## NathBit POS - Sistema Multi-Empresa Multi-Sucursal

---

## 📋 Tabla de Contenidos
1. [Visión General](#visión-general)
2. [Arquitectura del Sistema](#arquitectura-del-sistema)
3. [Roles y Permisos](#roles-y-permisos)
4. [Flujo de Autenticación](#flujo-de-autenticación)
5. [Estructura de Navegación](#estructura-de-navegación)
6. [Modelo de Base de Datos](#modelo-de-base-de-datos)
7. [API Endpoints](#api-endpoints)
8. [Implementación Frontend](#implementación-frontend)
9. [Casos de Uso](#casos-de-uso)
10. [Seguridad y Validaciones](#seguridad-y-validaciones)

---

## 🎯 Visión General

NathBit POS es un sistema de punto de venta diseñado para manejar múltiples empresas y sucursales con un modelo de permisos jerárquico. El sistema permite que diferentes tipos de usuarios accedan a recursos específicos según su rol y contexto.

### Principios Clave:
- **Un usuario = Un rol global** (simplificación del modelo)
- **Contexto dinámico** por empresa/sucursal
- **Navegación en cascada** según nivel de acceso
- **Token JWT genérico** con contexto manejado en sesión

---

## 🏗️ Arquitectura del Sistema

### Backend (Spring Boot)
```
┌─────────────────────────────────────────┐
│            API Gateway                   │
├─────────────────────────────────────────┤
│         Spring Security + JWT           │
├─────────────────────────────────────────┤
│          Controladores REST             │
├─────────────────────────────────────────┤
│         Servicios de Negocio            │
├─────────────────────────────────────────┤
│          Repositorios JPA               │
├─────────────────────────────────────────┤
│         PostgreSQL Database             │
└─────────────────────────────────────────┘
```

### Frontend (Angular)
```
┌─────────────────────────────────────────┐
│          Guards & Interceptors          │
├─────────────────────────────────────────┤
│         Servicios de Contexto           │
├─────────────────────────────────────────┤
│            Componentes                  │
├─────────────────────────────────────────┤
│         Rutas y Navegación              │
└─────────────────────────────────────────┘
```

---

## 🔐 Roles y Permisos

### Jerarquía de Roles

| Rol | Nivel | Descripción | Acceso |
|-----|-------|-------------|--------|
| **ROOT** | Sistema | Superusuario del sistema | Acceso total a todas las empresas y configuraciones |
| **SOPORTE** | Sistema | Soporte técnico | Similar a ROOT pero sin configuraciones críticas |
| **SUPER_ADMIN** | Empresarial | Dueño de empresas | Gestiona sus propias empresas y todas sus sucursales |
| **ADMIN** | Gerencial | Administrador de empresa | Gestiona sucursales asignadas de una empresa |
| **JEFE_CAJAS** | Operativo | Supervisor de cajas | Opera en una sucursal con permisos elevados |
| **CAJERO** | Operativo | Operador de caja | Funciones de venta en una sucursal |
| **MESERO** | Operativo | Atención a mesas | Gestión de órdenes y mesas |
| **COCINERO** | Operativo | Personal de cocina | Vista de órdenes de cocina |

### Matriz de Permisos

```
┌─────────────────┬─────────┬───────────┬────────────┬───────────┐
│      ROL        │ Empresas│ Sucursales│ Requiere   │ Dashboard │
│                 │         │           │ Selección  │           │
├─────────────────┼─────────┼───────────┼────────────┼───────────┤
│ ROOT            │  Todas  │   Todas   │     No     │  Sistema  │
│ SOPORTE         │  Todas  │   Todas   │     No     │  Sistema  │
│ SUPER_ADMIN     │Múltiples│   Todas   │     Sí     │Empresarial│
│ ADMIN           │   Una   │ Asignadas │     Sí     │Sucursales │
│ JEFE_CAJAS      │   Una   │    Una    │     No     │ Operativo │
│ CAJERO          │   Una   │    Una    │     No     │ Operativo │
│ MESERO          │   Una   │    Una    │     No     │ Operativo │
│ COCINERO        │   Una   │    Una    │     No     │ Operativo │
└─────────────────┴─────────┴───────────┴────────────┴───────────┘
```

---

## 🔑 Flujo de Autenticación

### 1. Login Inicial
```json
POST /api/auth/login
{
  "email": "usuario@email.com",
  "password": "********"
}
```

### 2. Respuesta según Rol

#### Para ROOT/SOPORTE:
```json
{
  "success": true,
  "data": {
    "token": "jwt-token...",
    "refreshToken": "refresh-token...",
    "usuario": {
      "id": 1,
      "email": "root@sistema.com",
      "nombre": "Usuario Root",
      "rol": "ROOT"
    },
    "requiereSeleccion": false,
    "rutaDestino": "/dashboard-sistema"
  }
}
```

#### Para SUPER_ADMIN:
```json
{
  "success": true,
  "data": {
    "token": "jwt-token...",
    "refreshToken": "refresh-token...",
    "usuario": {
      "id": 10,
      "email": "superadmin@empresa.com",
      "nombre": "Super Admin",
      "rol": "SUPER_ADMIN"
    },
    "empresas": [
      {
        "id": 1,
        "nombre": "Restaurante El Sabor",
        "logo": "url...",
        "activa": true
      },
      {
        "id": 2,
        "nombre": "Café Luna",
        "logo": "url...",
        "activa": true
      }
    ],
    "requiereSeleccion": true,
    "rutaDestino": "/dashboard-empresarial"
  }
}
```

#### Para ADMIN:
```json
{
  "success": true,
  "data": {
    "token": "jwt-token...",
    "refreshToken": "refresh-token...",
    "usuario": {
      "id": 20,
      "email": "admin@sucursal.com",
      "nombre": "Administrador",
      "rol": "ADMIN"
    },
    "empresa": {
      "id": 1,
      "nombre": "Restaurante El Sabor"
    },
    "sucursales": [
      {"id": 1, "nombre": "Sucursal Centro"},
      {"id": 2, "nombre": "Sucursal Norte"}
    ],
    "requiereSeleccion": true,
    "rutaDestino": "/dashboard-sucursales/1"
  }
}
```

#### Para OPERATIVOS:
```json
{
  "success": true,
  "data": {
    "token": "jwt-token...",
    "refreshToken": "refresh-token...",
    "usuario": {
      "id": 30,
      "email": "cajero@sucursal.com",
      "nombre": "Juan Cajero",
      "rol": "CAJERO"
    },
    "contexto": {
      "empresa": {
        "id": 1,
        "nombre": "Restaurante El Sabor"
      },
      "sucursal": {
        "id": 1,
        "nombre": "Sucursal Centro"
      }
    },
    "requiereSeleccion": false,
    "rutaDestino": "/sistema"
  }
}
```

### 3. Establecer Contexto (cuando requiereSeleccion = true)
```json
POST /api/auth/establecer-contexto
{
  "empresaId": 1,
  "sucursalId": 2  // opcional según el rol
}

Response:
{
  "success": true,
  "data": {
    "contextoId": "ctx_123",
    "empresa": {...},
    "sucursal": {...},
    "permisos": {...}
  }
}
```

---

## 🗺️ Estructura de Navegación

### Flujo de Navegación por Rol

```
ROOT/SOPORTE
    └── Dashboard Sistema
         └── Lista Empresas (todas)
              └── Dashboard Sucursales
                   └── Sistema Operativo

SUPER_ADMIN
    └── Dashboard Empresarial
         └── Mis Empresas
              └── Dashboard Sucursales
                   └── Sistema Operativo

ADMIN
    └── Dashboard Sucursales (empresa fija)
         └── Sistema Operativo

OPERATIVOS
    └── Sistema Operativo (directo)
```

### Componentes de Cada Nivel

#### 1. Dashboard Sistema (ROOT/SOPORTE)
- Lista de todas las empresas del sistema
- Estado de suscripciones y pagos
- Métricas del sistema
- Configuraciones globales
- Gestión de recursos

#### 2. Dashboard Empresarial (SUPER_ADMIN)
- Lista de empresas propias
- Ventas consolidadas
- Inventarios globales
- Comparativas entre empresas
- Reportes financieros

#### 3. Dashboard Sucursales
- Lista de sucursales de la empresa
- Métricas por sucursal
- Personal activo
- Ventas del día/mes
- Estado de inventarios

#### 4. Sistema Operativo
- Botones según rol:
    - **CAJERO**: [Clientes] [Productos] [Facturar]
    - **MESERO**: [Mesas] [Órdenes] [Clientes]
    - **COCINERO**: [Órdenes] [Cocina]
    - **JEFE_CAJAS**: [Caja] [Reportes] [Cierre] [Supervisión]

---

## 💾 Modelo de Base de Datos

### Estructura Simplificada

```sql
-- Tabla de usuarios con rol único
CREATE TABLE usuarios (
    id BIGINT PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    nombre VARCHAR(100),
    apellidos VARCHAR(100),
    rol VARCHAR(30) NOT NULL, -- UN SOLO ROL POR USUARIO
    activo BOOLEAN DEFAULT true,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Relación usuarios-empresas (sin rol)
CREATE TABLE usuarios_empresas (
    id BIGINT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    fecha_asignacion TIMESTAMP,
    activo BOOLEAN DEFAULT true,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    FOREIGN KEY (empresa_id) REFERENCES empresas(id),
    UNIQUE(usuario_id, empresa_id)
);

-- Relación usuarios-sucursales (para ADMIN con múltiples sucursales)
CREATE TABLE usuarios_sucursales (
    usuario_id BIGINT NOT NULL,
    sucursal_id BIGINT NOT NULL,
    activo BOOLEAN DEFAULT true,
    PRIMARY KEY (usuario_id, sucursal_id),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    FOREIGN KEY (sucursal_id) REFERENCES sucursales(id)
);
```

### Reglas de Negocio en BD

1. **ROOT/SOPORTE**: No requieren registros en `usuarios_empresas`
2. **SUPER_ADMIN**: Múltiples registros en `usuarios_empresas`
3. **ADMIN**: Un registro en `usuarios_empresas`, múltiples en `usuarios_sucursales`
4. **OPERATIVOS**: Un registro en `usuarios_empresas` y uno en `usuarios_sucursales`

---

## 🌐 API Endpoints

### Autenticación
```
POST   /api/auth/login                    # Login inicial
POST   /api/auth/refresh                  # Renovar token
POST   /api/auth/logout                   # Cerrar sesión
POST   /api/auth/establecer-contexto      # Seleccionar empresa/sucursal
GET    /api/auth/contexto-actual          # Obtener contexto activo
```

### Dashboard Sistema (ROOT/SOPORTE)
```
GET    /api/sistema/empresas              # Todas las empresas con métricas
GET    /api/sistema/empresas/{id}         # Detalle empresa
PUT    /api/sistema/empresas/{id}/estado  # Activar/Suspender empresa
GET    /api/sistema/metricas              # Métricas globales del sistema
GET    /api/sistema/configuracion         # Configuraciones globales
```

### Dashboard Empresarial (SUPER_ADMIN)
```
GET    /api/empresarial/mis-empresas      # Empresas del usuario
GET    /api/empresarial/metricas          # Métricas consolidadas
GET    /api/empresarial/comparativa       # Comparativa entre empresas
POST   /api/empresarial/empresas          # Crear nueva empresa
PUT    /api/empresarial/empresas/{id}     # Editar empresa propia
```

### Gestión de Sucursales
```
GET    /api/sucursales/por-empresa/{id}   # Lista sucursales de empresa
GET    /api/sucursales/{id}               # Detalle sucursal
GET    /api/sucursales/{id}/metricas      # Métricas de sucursal
POST   /api/sucursales                    # Crear sucursal (SUPER_ADMIN)
PUT    /api/sucursales/{id}               # Editar sucursal
```

### Sistema Operativo
```
GET    /api/sistema/mi-contexto           # Contexto del usuario actual
GET    /api/sistema/mis-permisos          # Permisos según rol
GET    /api/sistema/modulos-disponibles   # Módulos habilitados
```

### Validaciones por Rol en Spring Security

```java
// Configuración de seguridad
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                // Sistema - Solo ROOT y SOPORTE
                .requestMatchers("/api/sistema/**")
                    .hasAnyRole("ROOT", "SOPORTE")
                
                // Empresarial - Solo SUPER_ADMIN
                .requestMatchers("/api/empresarial/**")
                    .hasRole("SUPER_ADMIN")
                
                // Sucursales - Validación por contexto
                .requestMatchers("/api/sucursales/**")
                    .access(new SucursalAccessExpression())
                
                // Operaciones - Todos con contexto
                .requestMatchers("/api/pos/**", "/api/ordenes/**")
                    .access(new ContextoValidoExpression())
            );
    }
}
```

---

## 🎨 Implementación Frontend

### Guards de Angular

```typescript
// auth.guard.ts - Verifica autenticación básica
export class AuthGuard implements CanActivate {
  canActivate(): boolean {
    const token = this.authService.getToken();
    if (!token || this.authService.isTokenExpired()) {
      this.router.navigate(['/login']);
      return false;
    }
    return true;
  }
}

// role.guard.ts - Verifica roles permitidos
export class RoleGuard implements CanActivate {
  canActivate(route: ActivatedRouteSnapshot): boolean {
    const rolesPermitidos = route.data['roles'] as string[];
    const rolUsuario = this.authService.getUserRole();
    
    if (!rolesPermitidos.includes(rolUsuario)) {
      this.router.navigate(['/unauthorized']);
      return false;
    }
    return true;
  }
}

// contexto.guard.ts - Verifica contexto empresa/sucursal
export class ContextoGuard implements CanActivate {
  canActivate(): boolean {
    const contexto = this.contextoService.getContexto();
    const rol = this.authService.getUserRole();
    
    // ROOT y SOPORTE no necesitan contexto
    if (['ROOT', 'SOPORTE'].includes(rol)) {
      return true;
    }
    
    // Otros roles necesitan al menos empresa
    if (!contexto.empresaId) {
      this.router.navigate(['/seleccionar-contexto']);
      return false;
    }
    
    // Operativos necesitan sucursal
    if (this.esRolOperativo(rol) && !contexto.sucursalId) {
      this.router.navigate(['/error-sin-sucursal']);
      return false;
    }
    
    return true;
  }
}
```

### Rutas de Angular

```typescript
export const routes: Routes = [
  // Públicas
  {
    path: 'login',
    component: LoginComponent
  },
  
  // Dashboard Sistema - ROOT/SOPORTE
  {
    path: 'dashboard-sistema',
    component: DashboardSistemaComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ROOT', 'SOPORTE'] }
  },
  
  // Dashboard Empresarial - SUPER_ADMIN
  {
    path: 'dashboard-empresarial',
    component: DashboardEmpresarialComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['SUPER_ADMIN'] }
  },
  
  // Dashboard Sucursales
  {
    path: 'dashboard-sucursales/:empresaId',
    component: DashboardSucursalesComponent,
    canActivate: [AuthGuard, RoleGuard, ContextoGuard],
    data: { roles: ['ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN'] }
  },
  
  // Sistema Operativo
  {
    path: 'sistema',
    component: SistemaComponent,
    canActivate: [AuthGuard, ContextoGuard],
    children: [
      { path: 'pos', component: PosComponent },
      { path: 'ordenes', component: OrdenesComponent },
      { path: 'cocina', component: CocinaComponent },
      { path: 'reportes', component: ReportesComponent }
    ]
  },
  
  // Redirección por defecto
  {
    path: '',
    redirectTo: '/login',
    pathMatch: 'full'
  }
];
```

### Servicio de Contexto

```typescript
@Injectable({
  providedIn: 'root'
})
export class ContextoService {
  private contexto$ = new BehaviorSubject<Contexto | null>(null);
  
  establecerContexto(empresaId: number, sucursalId?: number): Observable<any> {
    return this.http.post('/api/auth/establecer-contexto', {
      empresaId,
      sucursalId
    }).pipe(
      tap(response => {
        this.contexto$.next(response.data);
        localStorage.setItem('contexto', JSON.stringify(response.data));
      })
    );
  }
  
  obtenerContexto(): Contexto | null {
    return this.contexto$.value;
  }
  
  limpiarContexto(): void {
    this.contexto$.next(null);
    localStorage.removeItem('contexto');
  }
}
```

---

## 📝 Casos de Uso

### Caso 1: Roberto - ROOT del Sistema
1. Login con credenciales ROOT
2. Accede directamente a Dashboard Sistema
3. Ve todas las empresas con estados de pago
4. Selecciona "Restaurante El Sabor"
5. Ve dashboard de sucursales
6. Puede configurar, suspender o modificar cualquier recurso

### Caso 2: María - SUPER_ADMIN con 3 empresas
1. Login → Dashboard Empresarial
2. Ve sus 3 empresas con métricas de ventas
3. Selecciona "Café Luna"
4. Ve las 2 sucursales de Café Luna
5. Entra a sucursal "Principal"
6. Accede al sistema operativo con permisos totales

### Caso 3: Juan - ADMIN de una empresa
1. Login → Directo a Dashboard Sucursales
2. Ve las 3 sucursales que tiene asignadas
3. Selecciona "Sucursal Norte"
4. Accede al sistema con permisos administrativos
5. Puede gestionar usuarios, productos, reportes de su sucursal

### Caso 4: Pedro - CAJERO
1. Login → Directo al Sistema Operativo
2. Ve solo botones de [Clientes] [Productos] [Facturar]
3. Comienza a trabajar inmediatamente
4. No puede cambiar de sucursal ni empresa

---

## 🔒 Seguridad y Validaciones

### Validaciones en Backend

1. **Nivel de Controlador**
   ```java
   @PreAuthorize("hasRole('SUPER_ADMIN') and @seguridadService.esEmpresaPropia(#empresaId)")
   public ResponseEntity<?> editarEmpresa(@PathVariable Long empresaId) {
       // Solo puede editar sus propias empresas
   }
   ```

2. **Nivel de Servicio**
   ```java
   public void validarAccesoEmpresa(Long usuarioId, Long empresaId) {
       Usuario usuario = obtenerUsuario(usuarioId);
       
       // ROOT y SOPORTE tienen acceso total
       if (usuario.esRolSistema()) return;
       
       // Otros deben tener relación con la empresa
       if (!usuarioEmpresaRepository.existsByUsuarioAndEmpresa(usuarioId, empresaId)) {
           throw new AccesoDenegadoException("No tienes acceso a esta empresa");
       }
   }
   ```

3. **Nivel de Repositorio**
   ```java
   @Query("SELECT e FROM Empresa e WHERE " +
          "(:rol IN ('ROOT', 'SOPORTE') OR " +
          "e.id IN (SELECT ue.empresa.id FROM UsuarioEmpresa ue WHERE ue.usuario.id = :usuarioId))")
   List<Empresa> findEmpresasAccesibles(@Param("usuarioId") Long usuarioId, @Param("rol") String rol);
   ```

### Validaciones en Frontend

1. **Interceptor HTTP**
   ```typescript
   intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
     const token = this.authService.getToken();
     const contexto = this.contextoService.getContexto();
     
     let headers = req.headers.set('Authorization', `Bearer ${token}`);
     
     if (contexto) {
       headers = headers
         .set('X-Empresa-Id', contexto.empresaId.toString())
         .set('X-Sucursal-Id', contexto.sucursalId?.toString() || '');
     }
     
     const authReq = req.clone({ headers });
     return next.handle(authReq);
   }
   ```

2. **Directiva de Permisos**
   ```typescript
   @Directive({
     selector: '[appTienePermiso]'
   })
   export class TienePermisoDirective {
     @Input() set appTienePermiso(permiso: string) {
       const tienePermiso = this.permisoService.validarPermiso(permiso);
       this.viewContainer.clear();
       
       if (tienePermiso) {
         this.viewContainer.createEmbeddedView(this.templateRef);
       }
     }
   }
   ```

### Auditoría

Todas las acciones importantes se registran:

```sql
CREATE TABLE auditoria (
    id BIGINT PRIMARY KEY,
    usuario_id BIGINT,
    empresa_id BIGINT,
    sucursal_id BIGINT,
    accion VARCHAR(100),
    entidad VARCHAR(50),
    entidad_id BIGINT,
    datos_anteriores JSONB,
    datos_nuevos JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP
);
```

---

## 🚀 Próximos Pasos

1. **Fase 1**: Implementar modelo de usuarios y autenticación
2. **Fase 2**: Crear dashboards diferenciados
3. **Fase 3**: Implementar guards y validaciones
4. **Fase 4**: Desarrollar sistema operativo con módulos
5. **Fase 5**: Agregar auditoría y reportes

---

## 📞 Contacto y Soporte

Para dudas sobre la implementación, contactar al equipo de desarrollo.

**Última actualización**: Enero 2025  
**Versión**: 1.0.0