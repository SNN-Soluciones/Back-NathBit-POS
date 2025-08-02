# 🍽️ Restaurant ERP - Backend API

Sistema de gestión integral para restaurantes con arquitectura multi-tenant, diseñado para manejar múltiples establecimientos desde una única plataforma.

## 📋 Descripción

Restaurant ERP es una solución completa para la gestión de restaurantes que incluye:
- 🏪 Gestión multi-restaurante (Multi-tenant)
- 📊 Control de inventarios y productos
- 💰 Sistema de punto de venta (POS)
- 🧾 Facturación electrónica (integración con Hacienda CR)
- 👥 Gestión de empleados y roles
- 📈 Reportes y análisis de ventas
- 🪑 Control de mesas y zonas
- 📱 API REST para integración con aplicaciones móviles

## 🚀 Tecnologías

- **Java 17**
- **Spring Boot 3.5.4**
- **Spring Security + JWT**
- **PostgreSQL**
- **Flyway** (Migraciones de BD)
- **MapStruct** (Mapeo de DTOs)
- **Lombok**
- **OpenAPI/Swagger**

## 🏗️ Arquitectura

- **Patrón Multi-tenant**: Separación por schema de PostgreSQL
- **Arquitectura REST**: API RESTful con documentación OpenAPI
- **Seguridad JWT**: Autenticación stateless con tokens
- **Auditoría**: Registro automático de cambios
- **Cache**: Optimización con Spring Cache

## 🛠️ Instalación

### Prerequisitos
- Java 17+
- PostgreSQL 14+
- Maven o Gradle

### Configuración de Base de Datos
```sql
CREATE DATABASE restaurant_erp_dev;
```

### Variables de Entorno
```bash
DB_USER=postgres
DB_PASSWORD=tu_password
JWT_SECRET=tu_secret_key
```

### Ejecutar el proyecto
```bash
# Con Gradle
./gradlew bootRun

# O con el wrapper
gradle bootRun
```

## 📚 Documentación API

Una vez ejecutado el proyecto, accede a:
- Swagger UI: `http://localhost:8080/api/swagger-ui.html`
- OpenAPI Docs: `http://localhost:8080/api/v3/api-docs`

## 🔧 Estructura del Proyecto

```
src/main/java/com/snnsoluciones/backnathbitpos/
├── config/         # Configuraciones (Seguridad, Tenant, Cache)
├── controller/     # Controladores REST
├── service/        # Lógica de negocio
├── repository/     # Acceso a datos
├── entity/         # Entidades JPA
├── dto/           # Objetos de transferencia
├── mapper/        # Mappers de entidades a DTOs
├── exception/     # Manejo de excepciones
└── util/          # Utilidades
```

## 👥 Módulos Principales

- **Autenticación**: Login, registro, refresh token
- **Gestión de Usuarios**: CRUD usuarios, roles y permisos
- **Catálogo**: Productos, categorías, precios
- **Operaciones**: Órdenes, caja, facturación
- **Reportes**: Ventas, inventario, análisis

## 🤝 Contribuir

1. Fork el proyecto
2. Crea tu rama de feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## 📄 Licencia

Propiedad de SNN Soluciones - Todos los derechos reservados

## 📞 Contacto

SNN Soluciones - [@snnsoluciones](https://snnsoluciones.com)

---
Desarrollado con ❤️ por SNN Soluciones