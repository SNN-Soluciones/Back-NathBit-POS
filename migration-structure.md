# Estructura de Migraciones Flyway

Para que las migraciones funcionen correctamente con el sistema multi-tenant, debes organizar los archivos de migración de la siguiente manera:

## Estructura de Directorios

```
src/main/resources/
└── db/
    └── migration/
        ├── public/
        │   ├── V1__create_tenant_table.sql
        │   └── V2__insert_demo_tenant.sql
        └── tenant/
            ├── V1__create_base_tables.sql
            ├── V2__create_order_tables.sql
            └── V3__insert_seed_data.sql
```

## Ubicación de los archivos SQL

1. **Migraciones del schema `public`** (para la tabla de tenants):
    - Copiar `V1__create_tenant_table.sql` a `src/main/resources/db/migration/public/`
    - Copiar `V2__insert_demo_tenant.sql` a `src/main/resources/db/migration/public/`

2. **Migraciones para schemas de tenant** (se ejecutarán en cada tenant):
    - Copiar `V1__create_base_tables.sql` a `src/main/resources/db/migration/tenant/`
    - Copiar `V2__create_order_tables.sql` a `src/main/resources/db/migration/tenant/`
    - Copiar `V3__insert_seed_data.sql` a `src/main/resources/db/migration/tenant/`

## Notas Importantes

- Las migraciones en `/public/` se ejecutan solo en el schema `public`
- Las migraciones en `/tenant/` se ejecutan en cada schema de tenant (`demo`, `tenant1`, etc.)
- Flyway mantendrá un historial separado para cada schema
- El orden de ejecución es importante: primero `public`, luego cada tenant