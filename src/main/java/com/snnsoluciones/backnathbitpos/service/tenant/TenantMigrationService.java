package com.snnsoluciones.backnathbitpos.service.tenant;

import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.entity.global.*;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import com.snnsoluciones.backnathbitpos.repository.EmpresaRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import com.snnsoluciones.backnathbitpos.repository.global.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantMigrationService {

    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final TenantRepository tenantRepository;
    private final UsuarioGlobalRepository usuarioGlobalRepository;
    private final SuperAdminTenantRepository superAdminTenantRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;


    /**
     * Lista de tablas a migrar en ORDEN de dependencias (FK)
     * Cada grupo se ejecuta secuencialmente
     */
    private static final List<TablaConfig> TABLAS_A_MIGRAR = List.of(
        // ========== NIVEL 0: Sucursales (base) ==========
        new TablaConfig("sucursales", "empresa_id = ?", true),

        // ========== NIVEL 1: Entidades principales ==========
        new TablaConfig("categorias_producto", "empresa_id = ?", false),
        new TablaConfig("familia_producto", "empresa_id = ?", false),
        new TablaConfig("clientes_ubicaciones",
            "id IN (SELECT ubicacion_id FROM public.clientes WHERE empresa_id = ? AND ubicacion_id IS NOT NULL)", false),
        new TablaConfig("clientes", "empresa_id = ?", true),
        new TablaConfig("proveedores", "empresa_id = ?", false),
        new TablaConfig("empresa_actividades", "empresa_id = ?", false),
        new TablaConfig("empresa_cabys", "empresa_id = ?", false),
        new TablaConfig("empresa_config_hacienda", "empresa_id = ?", false),
        new TablaConfig("plataforma_digital_config", "empresa_id = ?", false),

        // ========== NIVEL 2: Dependientes de sucursal ==========
        new TablaConfig("terminales",
            "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?)", true),
        new TablaConfig("sucursal_receptor_smtp",
            "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?)", false),
        new TablaConfig("impresoras_android",
            "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?)", false),
        new TablaConfig("consecutivos",
            "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?)", false),

        // ========== NIVEL 3: Mesas y zonas ==========
        new TablaConfig("zona_mesa",
            "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?)", false),
        new TablaConfig("zona_layout",
            "zona_id IN (SELECT zm.id FROM public.zona_mesa zm " +
                "JOIN public.sucursales s ON zm.sucursal_id = s.id WHERE s.empresa_id = ?)", false),
        new TablaConfig("mesa",
            "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?)", false),
        new TablaConfig("mesa_estado_hist",
            "mesa_id IN (SELECT m.id FROM public.mesa m " +
                "JOIN public.sucursales s ON m.sucursal_id = s.id WHERE s.empresa_id = ?)", false),

        // ========== NIVEL 4: Clientes relacionados ==========
        new TablaConfig("cliente_emails",
            "cliente_id IN (SELECT id FROM public.clientes WHERE empresa_id = ?)", false),
        new TablaConfig("cliente_actividades",
            "cliente_id IN (SELECT id FROM public.clientes WHERE empresa_id = ?)", false),
        new TablaConfig("clientes_exoneraciones",
            "cliente_id IN (SELECT id FROM public.clientes WHERE empresa_id = ?)", false),
        new TablaConfig("cliente_exoneracion_cabys",
            "exoneracion_id IN (SELECT ce.id FROM public.clientes_exoneraciones ce " +
                "JOIN public.clientes c ON ce.cliente_id = c.id WHERE c.empresa_id = ?)", false),

        // ========== NIVEL 5: Productos ==========
        new TablaConfig("productos", "empresa_id = ?", true),
        new TablaConfig("producto_sucursal",
            "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?)", false),
        new TablaConfig("producto_categoria",
            "producto_id IN (SELECT id FROM public.productos WHERE empresa_id = ?)", false),
        new TablaConfig("producto_impuestos",
            "producto_id IN (SELECT id FROM public.productos WHERE empresa_id = ?)", false),
        new TablaConfig("producto_codigo_proveedor",
            "producto_id IN (SELECT id FROM public.productos WHERE empresa_id = ?)", false),
        new TablaConfig("productos_inventarios",
            "producto_id IN (SELECT id FROM public.productos WHERE empresa_id = ?)", false),
        new TablaConfig("producto_movimientos",
            "producto_id IN (SELECT id FROM public.productos WHERE empresa_id = ?)", false),

        // ========== NIVEL 6: Productos compuestos ==========
        new TablaConfig("producto_compuesto",
            "producto_id IN (SELECT id FROM public.productos WHERE empresa_id = ?)", false),
        new TablaConfig("producto_compuesto_slot",
            "compuesto_id IN (SELECT pc.id FROM public.producto_compuesto pc " +
                "JOIN public.productos p ON pc.producto_id = p.id WHERE p.empresa_id = ?)", false),
        new TablaConfig("producto_compuesto_opcion",
            "slot_id IN (SELECT pcs.id FROM public.producto_compuesto_slot pcs " +
                "JOIN public.producto_compuesto pc ON pcs.compuesto_id = pc.id " +
                "JOIN public.productos p ON pc.producto_id = p.id WHERE p.empresa_id = ?)", false),
        new TablaConfig("producto_compuesto_configuracion",
            "compuesto_id IN (SELECT pc.id FROM public.producto_compuesto pc " +
                "JOIN public.productos p ON pc.producto_id = p.id WHERE p.empresa_id = ?)", false),
        new TablaConfig("producto_compuesto_slot_configuracion",
            "configuracion_id IN (SELECT pcc.id FROM public.producto_compuesto_configuracion pcc " +
                "JOIN public.producto_compuesto pc ON pcc.compuesto_id = pc.id " +
                "JOIN public.productos p ON pc.producto_id = p.id WHERE p.empresa_id = ?)", false),

        // ========== NIVEL 7: Combos ==========
        new TablaConfig("producto_combo",
            "producto_id IN (SELECT id FROM public.productos WHERE empresa_id = ?)", false),
        new TablaConfig("producto_combo_item",
            "combo_id IN (SELECT pc.id FROM public.producto_combo pc " +
                "JOIN public.productos p ON pc.producto_id = p.id WHERE p.empresa_id = ?)", false),

        // ========== NIVEL 8: Recetas ==========
        new TablaConfig("productos_recetas",
            "producto_id IN (SELECT id FROM public.productos WHERE empresa_id = ?)", false),
        new TablaConfig("receta_ingredientes",
            "receta_id IN (SELECT pr.id FROM public.productos_recetas pr " +
                "JOIN public.productos p ON pr.producto_id = p.id WHERE p.empresa_id = ?)", false),

        // ========== NIVEL 9: Usuarios ==========
        new TablaConfig("usuarios",
            "id IN (SELECT usuario_id FROM public.usuarios_empresas WHERE empresa_id = ?)", true),
        new TablaConfig("usuarios_empresas", "empresa_id = ?", false),
        new TablaConfig("usuarios_sucursales",
            "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?)", false),

        // ========== NIVEL 10: Sesiones de caja ==========
        new TablaConfig("sesiones_caja",
            "terminal_id IN (SELECT t.id FROM public.terminales t " +
                "JOIN public.sucursales s ON t.sucursal_id = s.id WHERE s.empresa_id = ?)", false),
        new TablaConfig("sesion_caja_denominacion",
            "sesion_caja_id IN (SELECT sc.id FROM public.sesiones_caja sc " +
                "JOIN public.terminales t ON sc.terminal_id = t.id " +
                "JOIN public.sucursales s ON t.sucursal_id = s.id WHERE s.empresa_id = ?)", false),
        new TablaConfig("movimientos_caja",
            "sesion_caja_id IN (SELECT sc.id FROM public.sesiones_caja sc " +
                "JOIN public.terminales t ON sc.terminal_id = t.id " +
                "JOIN public.sucursales s ON t.sucursal_id = s.id WHERE s.empresa_id = ?)", false),
        new TablaConfig("cierre_datafono",
            "sesion_caja_id IN (SELECT sc.id FROM public.sesiones_caja sc " +
                "JOIN public.terminales t ON sc.terminal_id = t.id " +
                "JOIN public.sucursales s ON t.sucursal_id = s.id WHERE s.empresa_id = ?)", false),

        // ========== NIVEL 11: Órdenes ==========
        new TablaConfig("ordenes",
            "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?)", false),
        new TablaConfig("orden_items",
            "orden_id IN (SELECT o.id FROM public.ordenes o " +
                "JOIN public.sucursales s ON o.sucursal_id = s.id WHERE s.empresa_id = ?)", false),
        new TablaConfig("orden_item_opciones",
            "orden_item_id IN (SELECT oi.id FROM public.orden_items oi " +
                "JOIN public.ordenes o ON oi.orden_id = o.id " +
                "JOIN public.sucursales s ON o.sucursal_id = s.id WHERE s.empresa_id = ?)", false),

        // ========== NIVEL 12: Facturas electrónicas ==========
        new TablaConfig("facturas",
            "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?)", false),
        new TablaConfig("factura_detalles",
            "factura_id IN (SELECT f.id FROM public.facturas f " +
                "JOIN public.sucursales s ON f.sucursal_id = s.id WHERE s.empresa_id = ?)", false),
        new TablaConfig("factura_descuentos",
            "factura_detalle_id IN (SELECT fd.id FROM public.factura_detalles fd " +
                "JOIN public.facturas f ON fd.factura_id = f.id " +
                "JOIN public.sucursales s ON f.sucursal_id = s.id WHERE s.empresa_id = ?)", false),
        new TablaConfig("factura_detalle_impuesto",
            "detalle_id IN (SELECT fd.id FROM public.factura_detalles fd " +
                "JOIN public.facturas f ON fd.factura_id = f.id " +
                "JOIN public.sucursales s ON f.sucursal_id = s.id WHERE s.empresa_id = ?)", false),
        new TablaConfig("factura_medios_pago",
            "factura_id IN (SELECT f.id FROM public.facturas f " +
                "JOIN public.sucursales s ON f.sucursal_id = s.id WHERE s.empresa_id = ?)", false),
        new TablaConfig("factura_otros_cargos",
            "factura_id IN (SELECT f.id FROM public.facturas f " +
                "JOIN public.sucursales s ON f.sucursal_id = s.id WHERE s.empresa_id = ?)", false),
        new TablaConfig("factura_resumen_impuesto",
            "factura_id IN (SELECT f.id FROM public.facturas f " +
                "JOIN public.sucursales s ON f.sucursal_id = s.id WHERE s.empresa_id = ?)", false),
        new TablaConfig("factura_bitacora",
            "factura_id IN (SELECT f.id FROM public.facturas f " +
                "JOIN public.sucursales s ON f.sucursal_id = s.id WHERE s.empresa_id = ?)", false),
        new TablaConfig("orden_facturas",
            "factura_id IN (SELECT f.id FROM public.facturas f " +
                "JOIN public.sucursales s ON f.sucursal_id = s.id WHERE s.empresa_id = ?)", false),

        // ========== NIVEL 13: Facturas internas ==========
        new TablaConfig("factura_interna",
            "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?)", false),
        new TablaConfig("factura_interna_detalle",
            "factura_interna_id IN (SELECT fi.id FROM public.factura_interna fi " +
                "JOIN public.sucursales s ON fi.sucursal_id = s.id WHERE s.empresa_id = ?)", false),
        new TablaConfig("factura_interna_medios_pago",
            "factura_interna_id IN (SELECT fi.id FROM public.factura_interna fi " +
                "JOIN public.sucursales s ON fi.sucursal_id = s.id WHERE s.empresa_id = ?)", false),
        new TablaConfig("orden_facturas_internas",
            "factura_interna_id IN (SELECT fi.id FROM public.factura_interna fi " +
                "JOIN public.sucursales s ON fi.sucursal_id = s.id WHERE s.empresa_id = ?)", false),

        // ========== NIVEL 14: Facturas de recepción (compras) ==========
        new TablaConfig("facturas_recepcion", "empresa_id = ?", false),
        new TablaConfig("facturas_recepcion_detalles",
            "factura_recepcion_id IN (SELECT id FROM public.facturas_recepcion WHERE empresa_id = ?)", false),
        new TablaConfig("facturas_recepcion_descuentos",
            "factura_recepcion_detalle_id IN (SELECT frd.id FROM public.facturas_recepcion_detalles frd " +
                "JOIN public.facturas_recepcion fr ON frd.factura_recepcion_id = fr.id WHERE fr.empresa_id = ?)", false),
        new TablaConfig("facturas_recepcion_detalles_impuestos",
            "factura_recepcion_detalle_id IN (SELECT frd.id FROM public.facturas_recepcion_detalles frd " +
                "JOIN public.facturas_recepcion fr ON frd.factura_recepcion_id = fr.id WHERE fr.empresa_id = ?)", false),
        new TablaConfig("facturas_recepcion_medios_pago",
            "factura_recepcion_id IN (SELECT id FROM public.facturas_recepcion WHERE empresa_id = ?)", false),
        new TablaConfig("facturas_recepcion_otros_cargos",
            "factura_recepcion_id IN (SELECT id FROM public.facturas_recepcion WHERE empresa_id = ?)", false),
        new TablaConfig("facturas_recepcion_referencias",
            "factura_recepcion_id IN (SELECT id FROM public.facturas_recepcion WHERE empresa_id = ?)", false),

        // ========== NIVEL 15: Compras ==========
        new TablaConfig("compras", "empresa_id = ?", false),
        new TablaConfig("compra_detalles",
            "compra_id IN (SELECT id FROM public.compras WHERE empresa_id = ?)", false),
        new TablaConfig("mensaje_receptor_bitacora",
            "compra_id IN (SELECT id FROM public.compras WHERE empresa_id = ?)", false),

        // ========== NIVEL 16: Pagos y cuentas ==========
        new TablaConfig("cuentas_por_cobrar", "empresa_id = ?", false),
        new TablaConfig("pagos",
                "cliente_id IN (SELECT id FROM public.clientes WHERE empresa_id = ?)", false),
        new TablaConfig("historial_pagos", "empresa_id = ?", false),
        new TablaConfig("planes_pago", "empresa_id = ?", false),

        // ========== NIVEL 17: Ventas pausadas ==========
        new TablaConfig("ventas_pausadas",
            "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?)", false),

        // ========== NIVEL 18: Métricas ==========
        new TablaConfig("metricas_ventas_diarias",
            "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?)", false),
        new TablaConfig("metricas_ventas_mensuales", "empresa_id = ?", false),

        new TablaConfig("metricas_productos_vendidos",
            "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?)", false),
        new TablaConfig("metricas_compras_mensuales", "empresa_id = ?", false),

        // ========== NIVEL 19: Auditoría ==========
        new TablaConfig("email_audit_log",
            "factura_id IN (SELECT f.id FROM public.facturas f " +
                "JOIN public.sucursales s ON f.sucursal_id = s.id WHERE s.empresa_id = ?)", false)
    );

    /**
     * Migra una empresa completa al sistema multi-tenant
     */
    @Transactional(noRollbackFor = Exception.class)
    public MigrationResult migrarEmpresa(Long empresaId) {
        log.info("══════════════════════════════════════════════════════════");
        log.info("   INICIANDO MIGRACIÓN DE EMPRESA ID: {}", empresaId);
        log.info("══════════════════════════════════════════════════════════");
        
        MigrationResult result = new MigrationResult();
        result.setEmpresaId(empresaId);
        result.setTablasExitosas(new ArrayList<>());
        result.setTablasConError(new ArrayList<>());

        try {
            // 1. Validar empresa existe
            Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada: " + empresaId));
            
            log.info("✓ Empresa: {} ({})", empresa.getNombreComercial(), empresa.getIdentificacion());

            // 2. Verificar si ya está migrada
            if (tenantRepository.findByEmpresaLegacyId(empresaId).isPresent()) {
                throw new RuntimeException("La empresa ya está migrada a tenant");
            }

            // 3. Generar código de tenant
            String codigo = generarCodigoTenant(empresa.getNombreComercial());
            String schemaName = "tenant_" + codigo;
            log.info("✓ Código tenant: {} (schema: {})", codigo, schemaName);

            // 4. Crear tenant en tabla global
            Tenant tenant = Tenant.builder()
                .codigo(codigo)
                .nombre(empresa.getNombreComercial())
                .schemaName(schemaName)
                .empresaLegacyId(empresaId)
                .activo(true)
                .build();
            tenant = tenantRepository.save(tenant);
            result.setTenantId(tenant.getId());
            result.setTenantCodigo(codigo);
            log.info("✓ Tenant creado con ID: {}", tenant.getId());

            // 5. Crear schema en PostgreSQL
            crearSchema(schemaName);
            log.info("✓ Schema {} creado en PostgreSQL", schemaName);

            // 6. Copiar estructura de TODAS las tablas
            copiarEstructuraTablas(schemaName);
            log.info("✓ Estructura de tablas copiada");

            // 7. Migrar datos tabla por tabla
            migrarDatos(empresaId, schemaName, result);

            // 7.1 Asignar PIN por defecto a usuarios sin PIN
            asignarPinPorDefecto(schemaName);


            // 8. Migrar usuarios SUPER_ADMIN a usuarios_globales
            int usuariosMigrados = migrarUsuariosSuperAdmin(empresa, tenant);
            result.setUsuariosMigrados(usuariosMigrados);
            log.info("✓ {} usuarios SUPER_ADMIN migrados a global", usuariosMigrados);

            // 9. Marcar empresa como migrada
            empresa.setMigradoATenant(true);
            empresa.setTenantId(tenant.getId());
            empresaRepository.save(empresa);

            result.setSuccess(true);
            result.setMensaje("Migración completada exitosamente");
            
            log.info("══════════════════════════════════════════════════════════");
            log.info("   ✅ MIGRACIÓN COMPLETADA EXITOSAMENTE");
            log.info("   Tablas exitosas: {}", result.getTablasExitosas().size());
            log.info("   Tablas con error: {}", result.getTablasConError().size());
            log.info("══════════════════════════════════════════════════════════");

        } catch (Exception e) {
            log.error("❌ Error en migración: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMensaje("Error: " + e.getMessage());
        }

        return result;
    }

    /**
     * Crea el schema en PostgreSQL
     */
    private void crearSchema(String schemaName) {
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);
    }

    /**
     * Copia la estructura de todas las tablas al nuevo schema
     */
    private void copiarEstructuraTablas(String schemaName) {
        Set<String> tablasCreadas = new HashSet<>();
        
        for (TablaConfig config : TABLAS_A_MIGRAR) {
            String tabla = config.nombre;
            if (tablasCreadas.contains(tabla)) continue;
            
            try {
                // Verificar si la tabla existe en public
                String checkSql = "SELECT EXISTS (SELECT 1 FROM information_schema.tables " +
                                  "WHERE table_schema = 'public' AND table_name = ?)";
                Boolean exists = jdbcTemplate.queryForObject(checkSql, Boolean.class, tabla);
                
                if (Boolean.TRUE.equals(exists)) {
                    String sql = String.format(
                        "CREATE TABLE IF NOT EXISTS %s.%s (LIKE public.%s INCLUDING ALL)",
                        schemaName, tabla, tabla
                    );
                    jdbcTemplate.execute(sql);
                    tablasCreadas.add(tabla);
                    log.debug("  Tabla {}.{} creada", schemaName, tabla);
                }
            } catch (Exception e) {
                log.warn("  ⚠ No se pudo crear tabla {}: {}", tabla, e.getMessage());
            }
        }
        log.info("  {} tablas estructuradas", tablasCreadas.size());
    }

    /**
     * Migra los datos de todas las tablas
     */
    private void migrarDatos(Long empresaId, String schemaName, MigrationResult result) {
        log.info("─────────────────────────────────────────────────────────");
        log.info("   Migrando datos...");
        log.info("─────────────────────────────────────────────────────────");
        
        for (TablaConfig config : TABLAS_A_MIGRAR) {
            migrarTabla(config, empresaId, schemaName, result);
        }
    }

    /**
     * Migra una tabla individual
     */
    private void migrarTabla(TablaConfig config, Long empresaId, String schemaName, MigrationResult result) {
        String tabla = config.nombre;

        try {
            // Verificar si la tabla existe
            String checkSql = "SELECT EXISTS (SELECT 1 FROM information_schema.tables " +
                "WHERE table_schema = 'public' AND table_name = ?)";
            Boolean exists = jdbcTemplate.queryForObject(checkSql, Boolean.class, tabla);

            if (!Boolean.TRUE.equals(exists)) {
                log.debug("  ⊘ {} (no existe)", tabla);
                return;
            }

            // Contar registros a migrar
            String countSql = String.format("SELECT COUNT(*) FROM public.%s WHERE %s",
                tabla, config.whereClause);
            Integer count = jdbcTemplate.queryForObject(countSql, Integer.class, empresaId);

            if (count == null || count == 0) {
                log.debug("  ⊘ {} (0 registros)", tabla);
                return;
            }

            // Obtener columnas NO generadas
            String columnsSql = """
            SELECT column_name FROM information_schema.columns 
            WHERE table_schema = 'public' AND table_name = ? 
            AND is_generated = 'NEVER' AND generation_expression IS NULL
            ORDER BY ordinal_position
            """;
            List<String> columns = jdbcTemplate.queryForList(columnsSql, String.class, tabla);
            String columnList = String.join(", ", columns);

            // Migrar datos (solo columnas no generadas)
            String insertSql = String.format(
                "INSERT INTO %s.%s (%s) SELECT %s FROM public.%s WHERE %s",
                schemaName, tabla, columnList, columnList, tabla, config.whereClause
            );
            jdbcTemplate.update(insertSql, empresaId);

            result.getTablasExitosas().add(tabla + " (" + count + ")");
            log.info("  ✓ {} → {} registros", tabla, count);

        } catch (Exception e) {
            if (config.requerida) {
                throw new RuntimeException("Error migrando tabla requerida " + tabla + ": " + e.getMessage(), e);
            }
            result.getTablasConError().add(tabla + ": " + e.getMessage());
            log.warn("  ✗ {} → {}", tabla, e.getMessage());
        }
    }

    /**
     * Migra usuarios SUPER_ADMIN a la tabla global
     */
    private int migrarUsuariosSuperAdmin(Empresa empresa, Tenant tenant) {
        List<Usuario> superAdmins = usuarioRepository.findByEmpresaId(empresa.getId())
            .stream()
            .filter(u -> u.getRol() == RolNombre.SUPER_ADMIN)
            .toList();

        int migrados = 0;
        for (Usuario usuario : superAdmins) {
            try {
                if (usuarioGlobalRepository.findByEmailIgnoreCase(usuario.getEmail()).isEmpty()) {
                    UsuarioGlobal usuarioGlobal = UsuarioGlobal.builder()
                        .email(usuario.getEmail())
                        .password(usuario.getPassword())
                        .nombre(usuario.getNombre())
                        .apellidos(usuario.getApellidos())
                        .telefono(usuario.getTelefono())
                        .rol(UsuarioGlobal.RolGlobal.SUPER_ADMIN)
                        .activo(usuario.getActivo())
                        .usuarioLegacyId(usuario.getId())
                        .build();
                    usuarioGlobal = usuarioGlobalRepository.save(usuarioGlobal);

                    SuperAdminTenant relacion = SuperAdminTenant.crearPropietario(usuarioGlobal, tenant);
                    superAdminTenantRepository.save(relacion);

                    usuario.setMigradoAGlobal(true);
                    usuario.setUsuarioGlobalId(usuarioGlobal.getId());
                    usuarioRepository.save(usuario);

                    migrados++;
                }
            } catch (Exception e) {
                log.error("Error migrando usuario {}: {}", usuario.getEmail(), e.getMessage());
            }
        }
        return migrados;
    }

    /**
     * Genera código de tenant desde nombre comercial
     */
    private String generarCodigoTenant(String nombre) {
        String normalizado = Normalizer.normalize(nombre, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        normalizado = pattern.matcher(normalizado).replaceAll("");

        normalizado = normalizado.toLowerCase()
            .replaceAll("[^a-z0-9\\s]", "")
            .replaceAll("\\s+", "_")
            .replaceAll("_+", "_")
            .replaceAll("^_|_$", "");

        if (normalizado.length() > 30) {
            normalizado = normalizado.substring(0, 30);
        }

        String codigoFinal = normalizado;
        int contador = 1;
        while (tenantRepository.existsByCodigo(codigoFinal)) {
            codigoFinal = normalizado + "_" + contador;
            contador++;
        }

        return codigoFinal;
    }

    /**
     * Asigna PIN por defecto (1234) encriptado a usuarios sin PIN
     */
    private void asignarPinPorDefecto(String schemaName) {
        String pinEncriptado = passwordEncoder.encode("1234");
        String sql = String.format(
            "UPDATE %s.usuarios SET pin = ?, pin_longitud = 4, requiere_cambio_pin = true WHERE pin IS NULL",
            schemaName
        );
        int updated = jdbcTemplate.update(sql, pinEncriptado);
        log.info("  ✓ {} usuarios actualizados con PIN por defecto", updated);
    }

    /**
     * Asigna PINs a un tenant ya migrado
     */
    public String asignarPinsATenant(Long empresaId) {
        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        if (empresa.getTenantId() == null) {
            throw new RuntimeException("La empresa no está migrada");
        }

        Tenant tenant = tenantRepository.findById(empresa.getTenantId())
            .orElseThrow(() -> new RuntimeException("Tenant no encontrado"));

        asignarPinPorDefecto(tenant.getSchemaName());
        return "PINs asignados en " + tenant.getSchemaName();
    }

    // ==================== Clases auxiliares ====================

    private record TablaConfig(String nombre, String whereClause, boolean requerida) {}

    @lombok.Data
    public static class MigrationResult {
        private boolean success;
        private String mensaje;
        private Long empresaId;
        private Long tenantId;
        private String tenantCodigo;
        private int usuariosMigrados;
        private List<String> tablasExitosas;
        private List<String> tablasConError;
    }
}