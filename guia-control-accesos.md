# 🔐 Guía de Control de Accesos - NathBit POS

## 📋 Índice
1. [Modelo de Permisos](#modelo-de-permisos)
2. [Flujo de Autenticación](#flujo-de-autenticación)
3. [Estructura de Navegación](#estructura-de-navegación)
4. [Endpoints y Permisos](#endpoints-y-permisos)
5. [Casos de Uso](#casos-de-uso)

## 🎯 Modelo de Permisos

### Principio Base
**Un usuario = Un rol global** en todo el sistema. El rol determina el nivel de acceso y las funcionalidades disponibles.

### Jerarquía de Roles

```
SISTEMA
├── ROOT (acceso total)
├── SOPORTE (acceso total sin config crítica)
│
EMPRESARIAL
└── SUPER_ADMIN (múltiples empresas propias)
    │
    GERENCIAL
    └── ADMIN (una empresa, múltiples sucursales)
        │
        OPERATIVO
        ├── JEFE_CAJAS (supervisión)
        ├── CAJERO (ventas)
        ├── MESERO (mesas)
        └── COCINERO (cocina)
```

## 🔑 Flujo de Autenticación

### 1. Login Inicial

```http
POST /api/auth/login
{
  "email": "usuario@email.com",
  "password": "password123"
}
```

### 2. Respuestas según Rol

#### ROOT/SOPORTE
```json
{
  "success": true,
  "data": {
    "token": "jwt-token",
    "usuario": {...},
    "requiereSeleccion": false,
    "rutaDestino": "/dashboard-sistema"
  }
}
```

#### SUPER_ADMIN
```json
{
  "success": true,
  "data": {
    "token": "jwt-token",
    "usuario": {...},
    "empresas": [
      {"id": 1, "nombre": "Empresa A"},
      {"id": 2, "nombre": "Empresa B"}
    ],
    "requiereSeleccion": true,
    "rutaDestino": "/dashboard-empresarial"
  }
}
```

#### ADMIN
```json
{
  "success": true,
  "data": {
    "token": "jwt-token",
    "usuario": {...},
    "empresa": {"id": 1, "nombre": "Empresa A"},
    "sucursales": [
      {"id": 1, "nombre": "Sucursal Centro"},
      {"id": 2, "nombre": "Sucursal Norte"}
    ],
    "requiereSeleccion": true,
    "rutaDestino": "/dashboard-sucursales/1"
  }
}
```

#### OPERATIVOS
```json
{
  "success": true,
  "data": {
    "token": "jwt-token",
    "usuario": {...},
    "contexto": {
      "empresa": {"id": 1, "nombre": "Empresa A"},
      "sucursal": {"id": 1, "nombre": "Sucursal Centro"}
    },
    "requiereSeleccion": false,
    "rutaDestino": "/sistema"
  }
}
```

### 3. Establecer Contexto (si requiere)

```http
POST /api/auth/contexto
{
  "empresaId": 1,
  "sucursalId": 2  // opcional según rol
}
```

## 🗺️ Estructura de Navegación

### Flujo por Rol

```
ROOT/SOPORTE
└── Dashboard Sistema
    └── Lista todas las empresas
        └── Seleccionar cualquier empresa
            └── Dashboard Sucursales
                └── Sistema Operativo

SUPER_ADMIN
└── Dashboard Empresarial
    └── Mis empresas
        └── Seleccionar empresa
            └── Dashboard Sucursales
                └── Sistema Operativo

ADMIN
└── Dashboard Sucursales
    └── Mis sucursales asignadas
        └── Seleccionar sucursal
            └── Sistema Operativo

OPERATIVOS
└── Sistema Operativo (directo)
```

## 🔒 Endpoints y Permisos

### Matriz de Permisos por Endpoint

| Endpoint | ROOT | SOPORTE | SUPER_ADMIN | ADMIN | OPERATIVOS |
|----------|------|---------|-------------|-------|------------|
| **Auth** |
| POST /api/auth/login | ✅ | ✅ | ✅ | ✅ | ✅ |
| POST /api/auth/contexto | ✅ | ✅ | ✅ | ✅ | ❌ |
| **Empresas** |
| GET /api/empresas | ✅ | ✅ | ✅ | ❌ | ❌ |
| POST /api/empresas | ✅ | ✅ | ✅ | ❌ | ❌ |
| PUT /api/empresas/{id} | ✅ | ✅ | ✅* | ❌ | ❌ |
| DELETE /api/empresas/{id} | ✅ | ✅ | ❌ | ❌ | ❌ |
| **Sucursales** |
| GET /api/sucursales | ✅ | ✅ | ✅ | ✅ | ❌ |
| POST /api/sucursales | ✅ | ✅ | ✅ | ❌ | ❌ |
| PUT /api/sucursales/{id} | ✅ | ✅ | ✅ | ✅* | ❌ |
| DELETE /api/sucursales/{id} | ✅ | ✅ | ❌ | ❌ | ❌ |
| **Usuarios** |
| GET /api/usuarios | ✅ | ✅ | ✅ | ✅ | ❌ |
| POST /api/usuarios | ✅ | ✅ | ✅ | ❌ | ❌ |
| PUT /api/usuarios/{id} | ✅ | ✅ | ✅ | ✅* | ❌ |
| DELETE /api/usuarios/{id} | ✅ | ✅ | ❌ | ❌ | ❌ |
| GET /api/usuarios/perfil | ✅ | ✅ | ✅ | ✅ | ✅ |

*Solo sus propios recursos

### Validaciones Adicionales

1. **SUPER_ADMIN**: Solo puede gestionar SUS empresas
2. **ADMIN**: Solo puede gestionar sucursales ASIGNADAS
3. **OPERATIVOS**: Solo pueden operar en SU sucursal asignada

## 📝 Casos de Uso

### Caso 1: Carlos (ROOT)
1. Login → Dashboard Sistema
2. Ve TODAS las empresas del sistema
3. Selecciona "Restaurante El Sabor"
4. Ve todas las sucursales
5. Puede crear, editar, eliminar cualquier recurso

### Caso 2: María (SUPER_ADMIN)
1. Login → Ve sus 3 empresas
2. Selecciona "Café Luna"
3. Ve las 5 sucursales de Café Luna
4. Puede gestionar TODO de sus empresas
5. NO puede ver empresas de otros

### Caso 3: Juan (ADMIN)
1. Login → Dashboard con 3 sucursales asignadas
2. Selecciona "Sucursal Norte"
3. Puede gestionar usuarios, productos de esa sucursal
4. NO puede crear nuevas sucursales

### Caso 4: Ana (CAJERO)
1. Login → Sistema POS directo
2. Contexto fijo: Empresa "X", Sucursal "Centro"
3. Solo accede a módulos de venta
4. NO puede cambiar de sucursal

## 🔧 Implementación Técnica

### JWT Token Structure
```json
{
  "sub": "usuario@email.com",
  "userId": 123,
  "rol": "ADMIN",
  "iat": 1234567890,
  "exp": 1234567890
}
```

### Contexto en Sesión
El contexto empresa/sucursal se maneja por separado del token para mayor flexibilidad:

```java
// Después de seleccionar contexto
SecurityContext:
- Usuario: ID, Email, Rol
- Empresa: ID (si aplica)
- Sucursal: ID (si aplica)
```

### Validaciones en Backend

1. **Filtro JWT**: Valida token en cada request
2. **@PreAuthorize**: Valida roles por endpoint
3. **Servicio de Seguridad**: Valida acceso a recursos específicos

```java
// Ejemplo de validación
@PreAuthorize("hasAnyRole('ROOT', 'SOPORTE') or 
              (hasRole('SUPER_ADMIN') and 
               @seguridadService.esEmpresaPropia(#empresaId))")
public void editarEmpresa(Long empresaId) {
    // Solo ROOT/SOPORTE o SUPER_ADMIN de su propia empresa
}
```

## 📌 Notas Importantes

1. **ROOT y SOPORTE** pueden "impersonar" cualquier contexto
2. **Tokens no expiran** por cambio de contexto, solo por tiempo
3. **Operativos** no pueden cambiar su contexto asignado
4. **Contexto se pierde** al cerrar sesión

## 🚀 Próximos Pasos

- Implementar auditoría de accesos
- Dashboard específico por rol
- Reportes de actividad por usuario
- Sistema de notificaciones por rol