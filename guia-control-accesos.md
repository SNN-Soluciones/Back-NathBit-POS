# 🧭 Redefinición del Alcance - Lógica Multiempresa y Roles de Usuario

## 🎯 Objetivo

Evitar desviarnos del alcance original del sistema, manteniendo una estructura clara de responsabilidades por rol y una lógica coherente en el acceso multiempresa, multiroles y multisucursal.

---

## 🧠 Lógica Central

### Usuarios

* Un usuario puede tener acceso a **múltiples empresas**.
* Puede tener **roles distintos por empresa**:

    * Ej: Andrés puede ser `Admin` en Empresa A y `Jefe de Cajas` en Empresa B.
* Un usuario al iniciar sesión:

    1. Hace login → recibe lista de empresas asociadas.
    2. Selecciona una empresa → se activa el tenant y el rol correspondiente.
    3. Puede salir de una empresa y entrar a otra sin desloguearse.

### Empresas

* Cada empresa:

    * Tiene uno o más **roles permitidos**.
    * Contiene una o más **sucursales**.

### Sucursales

* Cada sucursal maneja:

    * Clientes, productos, inventario, proveedores, órdenes, mesas, reportes, caja, etc.
* La visibilidad depende del **rol y permisos** del usuario dentro de esa empresa.

---

## 🔐 Jerarquía de Roles

| Rol             | Nivel Acceso                                            |
| --------------- |---------------------------------------------------------|
| `Root`          | Acceso total a todo el sistema                          |
| `Super Admin`   | Puede acceder a todas las empresas creadas por el mismo |
| `Admin`         | Accede a sucursales de su empresa                       |
| `Jefe de Cajas` | Manejo de cajas y cierre                                |
| `Cajero`        | Operaciones POS en caja                                 |
| `Mesero`        | Solo manejo de órdenes en mesas asignadas               |

> Un mismo usuario puede tener múltiples combinaciones. Ej: Admin en una empresa y mesero en otra.

---

## 🧪 Ejemplos de Casos de Uso

### 1. Andrés es usuario con los siguientes accesos:

* Empresa: **Samyx Personal** → Rol: `Admin`
* Empresa: **Restaurante NathBit** → Rol: `Jefe de Cajas`

Al iniciar sesión, Andrés ve:

* [ ] Samyx Personal
* [ ] Restaurante NathBit

Al entrar a Samyx Personal:

* Puede administrar usuarios, productos, etc.

Al cambiar a Restaurante NathBit:

* Solo tiene permisos sobre cajas y cierre.

---

## 🧱 Restricciones y Validaciones

1. Toda operación debe verificar:

    * Contexto del tenant activo.
    * Permisos del usuario sobre esa empresa/sucursal.

2. El `SuperAdmin` define:

    * Qué admins pueden ver/modificar otras sucursales.
    * Si un admin puede acceder a otras empresas.

3. Por defecto:

    * Los admins **no pueden ver otras empresas**.
    * Los usuarios solo ven **las sucursales asignadas**.

---

## 📌 Tareas inmediatas sugeridas

1. Documentar claramente el modelo `UsuarioTenant` con sus roles y sucursales asignadas.
2. Implementar pantalla de selección de empresa (backend ya avanza en eso).
3. Auditar lógica de `AuthServiceImpl` para login/logout por empresa.
4. Crear utilidad centralizada para obtener permisos por empresa y sucursal.
5. Validar cada endpoint de negocio con `@PreAuthorize` o `SecurityUtils` revisando rol y tenant.

---

## 🧭 Recomendación Final

💡 Mantener la lógica **modular y extensible**, pero simple. Evitar que cada feature tenga lógica condicional compleja por empresa/rol. En su lugar:

* Centralizar lógica de permisos
* Usar claims en el JWT
* Y documentar los casos de uso clave (como el de Andrés)

Esto nos ayudará a escalar sin perdernos en el scope.
