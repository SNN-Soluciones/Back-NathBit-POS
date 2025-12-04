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

    /**
     * Lista de tablas a migrar en ORDEN de dependencias (FK)
     * Cada grupo se ejecuta secuencialmente
     */
    private static final List<TablaConfig> TABLAS_A_MIGRAR = List.of(
        // ========== NIVEL 0: Base ==========
        new TablaConfig("categorias", "empresa_id = ?", true),
        
        // ========== NIVEL 1: Sucursales y entidades principales ==========
        new TablaConfig("sucursales", "empresa_id = ?", true),
        new TablaConfig("clientes", "empresa_id = ?", true),
        new TablaConfig("proveedores", "empresa_id = ?", true),
        
        // ========== NIVEL 2: Dependientes de sucursal/cliente ==========
        new TablaConfig("terminales", "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?)", true),
        new TablaConfig("productos", "empresa_id = ?", true),
        new TablaConfig("cliente_emails", "cliente_id IN (SELECT id FROM public.clientes WHERE empresa_id = ?)", false),
        new TablaConfig("cliente_actividades", "cliente_id IN (SELECT id FROM public.clientes WHERE empresa_id = ?)", false),
        new TablaConfig("cliente_exoneraciones", "cliente_id IN (SELECT id FROM public.clientes WHERE empresa_id = ?)", false),
        new TablaConfig("consecutivos", "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?)", true),
        
        // ========== NIVEL 3: Productos y usuarios ==========
        new TablaConfig("producto_sucursal", "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?)", false),
        new TablaConfig("producto_compuesto", "producto_id IN (SELECT id FROM public.productos WHERE empresa_id = ?)", false),
        new TablaConfig("usuarios",
            "id IN (SELECT usuario_id FROM public.usuarios_empresas WHERE empresa_id = ?)", true),
        new TablaConfig("usuarios_empresas", "empresa_id = ?", false),
        new TablaConfig("usuario_sucursal",
            "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?)", false),

        // ========== NIVEL 4: Productos compuestos ==========
        new TablaConfig("producto_compuesto_slot", 
            "compuesto_id IN (SELECT pc.id FROM public.producto_compuesto pc " +
            "JOIN public.productos p ON pc.producto_id = p.id WHERE p.empresa_id = ?)", false),
        
        // ========== NIVEL 5: Opciones y configuraciones ==========
        new TablaConfig("producto_compuesto_opcion", 
            "slot_id IN (SELECT pcs.id FROM public.producto_compuesto_slot pcs " +
            "JOIN public.producto_compuesto pc ON pcs.compuesto_id = pc.id " +
            "JOIN public.productos p ON pc.producto_id = p.id WHERE p.empresa_id = ?)", false),
        new TablaConfig("producto_compuesto_configuracion", 
            "compuesto_id IN (SELECT pc.id FROM public.producto_compuesto pc " +
            "JOIN public.productos p ON pc.producto_id = p.id WHERE p.empresa_id = ?)", false),
        
        // ========== NIVEL 6: Slot configuraciones ==========
        new TablaConfig("producto_compuesto_slot_configuracion", 
            "configuracion_id IN (SELECT pcc.id FROM public.producto_compuesto_configuracion pcc " +
            "JOIN public.producto_compuesto pc ON pcc.compuesto_id = pc.id " +
            "JOIN public.productos p ON pc.producto_id = p.id WHERE p.empresa_id = ?)", false),
        
        // ========== NIVEL 7: Cajas y sesiones ==========
        new TablaConfig("sesiones_caja", 
            "terminal_id IN (SELECT t.id FROM public.terminales t " +
            "JOIN public.sucursales s ON t.sucursal_id = s.id WHERE s.empresa_id = ?)", false),
        new TablaConfig("movimientos_caja", 
            "sesion_caja_id IN (SELECT sc.id FROM public.sesiones_caja sc " +
            "JOIN public.terminales t ON sc.terminal_id = t.id " +
            "JOIN public.sucursales s ON t.sucursal_id = s.id WHERE s.empresa_id = ?)", false),
        
        // ========== NIVEL 8: Facturas de venta ==========
        new TablaConfig("facturas", 
            "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?)", false),
        new TablaConfig("factura_detalles", 
            "factura_id IN (SELECT f.id FROM public.facturas f " +
            "JOIN public.sucursales s ON f.sucursal_id = s.id WHERE s.empresa_id = ?)", false),
        new TablaConfig("factura_descuentos", 
            "factura_detalle_id IN (SELECT fd.id FROM public.factura_detalles fd " +
            "JOIN public.facturas f ON fd.factura_id = f.id " +
            "JOIN public.sucursales s ON f.sucursal_id = s.id WHERE s.empresa_id = ?)", false),
        new TablaConfig("factura_impuestos", 
            "factura_detalle_id IN (SELECT fd.id FROM public.factura_detalles fd " +
            "JOIN public.facturas f ON fd.factura_id = f.id " +
            "JOIN public.sucursales s ON f.sucursal_id = s.id WHERE s.empresa_id = ?)", false),
        new TablaConfig("factura_medios_pago", 
            "factura_id IN (SELECT f.id FROM public.facturas f " +
            "JOIN public.sucursales s ON f.sucursal_id = s.id WHERE s.empresa_id = ?)", false),
        new TablaConfig("factura_otros_cargos", 
            "factura_id IN (SELECT f.id FROM public.facturas f " +
            "JOIN public.sucursales s ON f.sucursal_id = s.id WHERE s.empresa_id = ?)", false),
        
        // ========== NIVEL 9: Facturas de recepción (compras) ==========
        new TablaConfig("facturas_recepcion", "empresa_id = ?", false),
        new TablaConfig("facturas_recepcion_detalles", 
            "factura_recepcion_id IN (SELECT id FROM public.facturas_recepcion WHERE empresa_id = ?)", false),
        new TablaConfig("facturas_recepcion_descuentos", 
            "factura_recepcion_detalle_id IN (SELECT frd.id FROM public.facturas_recepcion_detalles frd " +
            "JOIN public.facturas_recepcion fr ON frd.factura_recepcion_id = fr.id WHERE fr.empresa_id = ?)", false),
        new TablaConfig("facturas_recepcion_impuestos", 
            "factura_recepcion_detalle_id IN (SELECT frd.id FROM public.facturas_recepcion_detalles frd " +
            "JOIN public.facturas_recepcion fr ON frd.factura_recepcion_id = fr.id WHERE fr.empresa_id = ?)", false),
        new TablaConfig("facturas_recepcion_medios_pago", 
            "factura_recepcion_id IN (SELECT id FROM public.facturas_recepcion WHERE empresa_id = ?)", false),
        new TablaConfig("facturas_recepcion_otros_cargos", 
            "factura_recepcion_id IN (SELECT id FROM public.facturas_recepcion WHERE empresa_id = ?)", false),
        new TablaConfig("facturas_recepcion_referencias", 
            "factura_recepcion_id IN (SELECT id FROM public.facturas_recepcion WHERE empresa_id = ?)", false),
        
        // ========== NIVEL 10: Compras ==========
        new TablaConfig("compras", "empresa_id = ?", false),
        new TablaConfig("compra_detalles", 
            "compra_id IN (SELECT id FROM public.compras WHERE empresa_id = ?)", false),
        
        // ========== NIVEL 11: Plataformas e impresoras ==========
        new TablaConfig("plataformas_delivery", "empresa_id = ?", false),
        new TablaConfig("impresoras", 
            "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?)", false)
    );

    /**
     * Migra una empresa completa al sistema multi-tenant
     */
    @Transactional
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

            // Migrar datos
            String insertSql = String.format(
                "INSERT INTO %s.%s SELECT * FROM public.%s WHERE %s",
                schemaName, tabla, tabla, config.whereClause
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