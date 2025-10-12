# 📚 DOCUMENTACIÓN - SISTEMA DE FACTURACIÓN NATHBIT POS

## 🎯 RESUMEN EJECUTIVO

NathBit POS es un sistema de punto de venta multi-empresa y multi-sucursal diseñado para Costa Rica, que soporta tanto **facturación electrónica** (Ministerio de Hacienda) como **control interno** (sin conexión a Hacienda).

## 🏗️ ARQUITECTURA DEL SISTEMA

### 1. **MODELO DE NEGOCIO**

```
┌─────────────┐
│   EMPRESA   │ (Puede tener múltiples sucursales)
└──────┬──────┘
       │
       ├─── requiereHacienda: true/false
       ├─── regimenTributario: TRADICIONAL/SIMPLIFICADO
       └─── configHacienda (si requiereHacienda = true)
       
┌─────────────┐
│  SUCURSAL   │ (001, 002, 003...)
└──────┬──────┘
       │
       ├─── modoFacturacion: ELECTRONICO/SOLO_INTERNO
       └─── terminales (máximo 2)
       
┌─────────────┐
│  TERMINAL   │ (00001, 00002)
└──────┬──────┘
       │
       └─── consecutivos por tipo de documento
```

### 2. **GESTIÓN DE CONSECUTIVOS**

#### Estructura del Consecutivo (20 dígitos):
```
001  |  00001  |  01  |  0000000001
 ↓        ↓        ↓         ↓
Sucursal Terminal Tipo  Consecutivo
```

#### Estructura de la Clave (50 dígitos):
```
506 | 09 | 08 | 25 | 123456789012 | [CONSECUTIVO 20 dig] | 1 | 12345678
 ↓     ↓    ↓    ↓        ↓              ↓                 ↓       ↓
País  Día  Mes  Año  Identificación  Consecutivo      Situación  Seguridad
```

### 3. **TIPOS DE DOCUMENTOS**

#### Electrónicos (van a Hacienda):
- 01: Factura Electrónica
- 02: Nota de Débito
- 03: Nota de Crédito
- 04: Tiquete Electrónico
- 08: Factura de Compra
- 09: Factura de Exportación
- 10: Recibo de Pago

#### Internos (control interno):
- TI: Tiquete Interno
- FI: Factura Interna
- PF: Proforma
- OP: Orden de Pedido

## 🔧 CONFIGURACIÓN DEL SISTEMA

### 1. **CONFIGURACIÓN DE EMPRESA**

```java
// Empresa básica (sin facturación electrónica)
Empresa empresa = new Empresa();
empresa.setRequiereHacienda(false);
empresa.setRegimenTributario(RegimenTributario.REGIMEN_TRADICIONAL);

// Empresa con facturación electrónica
Empresa empresa = new Empresa();
empresa.setRequiereHacienda(true);
empresa.setRegimenTributario(RegimenTributario.REGIMEN_SIMPLIFICADO);
// Se debe crear EmpresaConfigHacienda
```

### 2. **CONFIGURACIÓN HACIENDA**

```java
EmpresaConfigHacienda config = new EmpresaConfigHacienda();
config.setAmbiente(AmbienteHacienda.SANDBOX); // o PRODUCCION
config.setTipoAutenticacion(TipoAutenticacionHacienda.LLAVE_CRIPTOGRAFICA);
config.setUsuarioHacienda("usuario@hacienda");
config.setClaveHacienda("clave_encriptada");
config.setProveedorSistemas("TU_CEDULA"); // Identificación del desarrollador
```

### 3. **MODOS DE FACTURACIÓN POR SUCURSAL**

```java
// Sucursal solo control interno
Sucursal sucursal = new Sucursal();
sucursal.setNumeroSucursal("001");
sucursal.setModoFacturacion(ModoFacturacion.SOLO_INTERNO);

// Sucursal con facturación electrónica
Sucursal sucursal = new Sucursal();
sucursal.setNumeroSucursal("002");
sucursal.setModoFacturacion(ModoFacturacion.ELECTRONICO);
```

## 💼 CASOS DE USO

### 1. **PULPERÍA PEQUEÑA** (Sin Facturación Electrónica)
```
- Empresa: requiereHacienda = false
- Sucursal 001: modoFacturacion = SOLO_INTERNO
- Terminal 00001: Solo emite TI (Tiquetes Internos)
- No requiere certificados ni configuración Hacienda
```

### 2. **RESTAURANTE** (Mixto)
```
- Empresa: requiereHacienda = true
- Sucursal 001 (Restaurante): modoFacturacion = ELECTRONICO
- Sucursal 002 (Bar): modoFacturacion = SOLO_INTERNO
- Puede emitir facturas electrónicas o tiquetes internos según sucursal
```

### 3. **SUPERMERCADO** (Todo Electrónico)
```
- Empresa: requiereHacienda = true
- Todas las sucursales: modoFacturacion = ELECTRONICO
- Todas las cajas emiten documentos electrónicos
```

## 🔐 CONTROL DE SESIONES

### Flujo de Trabajo:
1. **Apertura de Caja**
   ```
   Usuario → Login → Selecciona Terminal → Abre Sesión con Monto Inicial
   ```

2. **Durante el Día**
   ```
   Terminal bloqueada para ese usuario
   Todos los documentos se registran en la sesión
   Se actualizan totales automáticamente
   ```

3. **Cierre de Caja**
   ```
   Cuenta efectivo → Ingresa monto final → Sistema calcula diferencia
   Genera reporte Z → Terminal queda libre
   ```

## 📊 REGLAS DE NEGOCIO

### 1. **Límites del Sistema**
- Máximo 2 terminales por sucursal
- Consecutivos hasta 9,999,999,999
- Alerta en 9,999,999,000

### 2. **Validaciones**
- Una terminal = Una sesión activa
- Un usuario = Una sesión activa
- Número sucursal único por empresa
- Número terminal único por sucursal

### 3. **Régimen Simplificado CR**
- SÍ puede emitir facturas electrónicas
- Tiene límites de facturación anual
- Cálculo de impuestos simplificado

## 🌐 INTEGRACIÓN CON API EXTERNA

### Arquitectura:
```
POS → API Middleware → Hacienda

API Middleware:
1. Recibe JSON del POS
2. Convierte a XML
3. Firma digitalmente
4. Envía a Hacienda
5. Recibe respuesta
6. Genera PDF
7. Envía por correo
```

## 🗄️ ESTRUCTURA DE DATOS

### Tablas Principales:
- `empresas` - Datos de la empresa
- `empresa_config_hacienda` - Configuración fiscal
- `sucursales` - Sucursales con su modo de facturación
- `terminales` - Cajas con sus consecutivos
- `sesiones_caja` - Control de apertura/cierre
- `monedas` y `tipos_cambio` - Multi-moneda

### Tablas de Ubicación CR:
- `provincia` (7 registros)
- `canton` (82 registros)
- `distrito` (488 registros)
- `barrio` (miles)

## 🚀 PRÓXIMOS PASOS

1. **Crear DTOs** para cada entidad
2. **Implementar Controllers** REST
3. **Crear API de facturación** separada
4. **Implementar seguridad** por contexto
5. **Crear frontend** Angular

## 📝 NOTAS IMPORTANTES

- **Código de Seguridad**: Se genera con timestamp + hash del ID
- **Proveedor de Sistemas**: Debes registrarte en Hacienda
- **Ambientes**: Sandbox para pruebas, Producción para real
- **Monedas**: CRC (local), USD, EUR con tipos de cambio BCCR

---

**Esta documentación cubre todo el diseño del sistema de facturación que hemos creado.**