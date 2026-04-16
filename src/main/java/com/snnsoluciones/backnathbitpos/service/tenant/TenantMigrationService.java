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

    /**
     * ══════════════════════════════════════════════════════════════════════════
     * PATCH: TABLAS_A_MIGRAR — Reemplazar la lista completa en TenantMigrationService
     *
     * AUDIT RESULT: Se encontraron ~15 tablas en entidades JPA que NO estaban
     * en la lista original. Ejecutar la migración con la lista vieja dejaría
     * datos huérfanos en: ordenes, inventario, facturas internas, recetas,
     * promociones y tablas hijas de compuestos.
     *
     * INSTRUCCIÓN: Reemplazar el bloque `TABLAS_A_MIGRAR` completo en
     * TenantMigrationService.java con la lista de abajo.
     *
     * CAMBIOS vs lista original:
     * [+] empresa (copia el registro de la empresa al schema del tenant)
     * [+] producto_compuesto          (antes solo estaba producto_compuesto_v2)
     * [+] producto_compuesto_slot     (antes solo estaba slot_v2)
     * [+] producto_compuesto_slot_opcion (antes solo estaba opcion_v2)
     * [+] producto_compuesto_configuracion
     * [+] producto_compuesto_slot_configuracion
     * [+] producto_receta
     * [+] receta_ingrediente
     * [+] producto_inventario
     * [+] producto_movimiento
     * [+] promociones
     * [+] promocion_items
     * [+] factura_interna
     * [+] factura_interna_detalles
     * [+] facturas_recepcion_detalles
     * [+] facturas_recepcion_referencias
     * [+] facturas_recepcion_otros_cargos
     * [+] facturas_recepcion_medios_pago
     * [+] ordenes
     * [+] orden_items
     * [+] orden_item_opcion
     * [+] orden_personas
     * [+] orden_promocion_estado
     * ══════════════════════════════════════════════════════════════════════════
     */

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

        // ══════════════════════════════════════════════════════════════════
        // NIVEL 0: La empresa misma (WHERE id = ?, no empresa_id = ?)
        // ══════════════════════════════════════════════════════════════════
        new TablaConfig("empresas", "id = ?", true),

        // ══════════════════════════════════════════════════════════════════
        // NIVEL 1: Dependen directamente de empresa
        // ══════════════════════════════════════════════════════════════════
        new TablaConfig("sucursales",               "empresa_id = ?", true),
        new TablaConfig("categorias_producto",      "empresa_id = ?", false),
        new TablaConfig("familia_producto",         "empresa_id = ?", false),
        new TablaConfig("empresa_actividades",      "empresa_id = ?", false),
        new TablaConfig("empresa_cabys",            "empresa_id = ?", false),
        new TablaConfig("empresa_config_hacienda",  "empresa_id = ?", false),
        new TablaConfig("plataforma_digital_config","empresa_id = ?", false),
        new TablaConfig("usuarios_empresas",        "empresa_id = ?", false),

        // ══════════════════════════════════════════════════════════════════
        // NIVEL 2: Dependen de sucursal (via sucursal_id directo)
        // ══════════════════════════════════════════════════════════════════
        new TablaConfig("terminales",
            "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?)", true),
        new TablaConfig("sucursal_receptor_smtp",
            "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?)", false),
        new TablaConfig("impresoras_android",
            "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?)", false),
        new TablaConfig("codigos_verificacion",
            "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?)", false),
        new TablaConfig("tokens_registro", "empresa_id = ?", false),

        // ══════════════════════════════════════════════════════════════════
        // NIVEL 3: Zonas, mesas y barras
        // ══════════════════════════════════════════════════════════════════
        new TablaConfig("zona_mesa",
            "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?)", false),
        new TablaConfig("zona_layout",
            "zona_id IN (SELECT id FROM public.zona_mesa WHERE sucursal_id IN " +
                "(SELECT id FROM public.sucursales WHERE empresa_id = ?))", false),
        new TablaConfig("mesa",
            "zona_id IN (SELECT id FROM public.zona_mesa WHERE sucursal_id IN " +
                "(SELECT id FROM public.sucursales WHERE empresa_id = ?))", false),
        new TablaConfig("barra",
            "zona_id IN (SELECT id FROM public.zona_mesa WHERE sucursal_id IN " +
                "(SELECT id FROM public.sucursales WHERE empresa_id = ?))", false),
        new TablaConfig("silla_barra",
            "barra_id IN (SELECT b.id FROM public.barra b JOIN public.zona_mesa zm ON b.zona_id = zm.id " +
                "JOIN public.sucursales s ON zm.sucursal_id = s.id WHERE s.empresa_id = ?)", false),

        // ══════════════════════════════════════════════════════════════════
        // NIVEL 4: Clientes y proveedores
        // ══════════════════════════════════════════════════════════════════
        new TablaConfig("clientes_ubicaciones",
            "id IN (SELECT ubicacion_id FROM public.clientes WHERE empresa_id = ? AND ubicacion_id IS NOT NULL)", false),
        new TablaConfig("clientes",    "empresa_id = ?", true),
        new TablaConfig("proveedores", "empresa_id = ?", false),
        new TablaConfig("cliente_emails",
            "cliente_id IN (SELECT id FROM public.clientes WHERE empresa_id = ?)", false),
        new TablaConfig("cliente_actividades",
            "cliente_id IN (SELECT id FROM public.clientes WHERE empresa_id = ?)", false),
        new TablaConfig("clientes_exoneraciones",
            "cliente_id IN (SELECT id FROM public.clientes WHERE empresa_id = ?)", false),
        new TablaConfig("cliente_exoneracion_cabys",
            "exoneracion_id IN (SELECT ce.id FROM public.clientes_exoneraciones ce " +
                "JOIN public.clientes c ON ce.cliente_id = c.id WHERE c.empresa_id = ?)", false),

        // ══════════════════════════════════════════════════════════════════
        // NIVEL 5: Productos base
        // ══════════════════════════════════════════════════════════════════
        new TablaConfig("productos",        "empresa_id = ?", true),
        new TablaConfig("producto_categoria",
            "producto_id IN (SELECT id FROM public.productos WHERE empresa_id = ?)", false),
        new TablaConfig("producto_impuestos",
            "producto_id IN (SELECT id FROM public.productos WHERE empresa_id = ?)", false),
        new TablaConfig("producto_codigo_proveedor",
            "producto_id IN (SELECT id FROM public.productos WHERE empresa_id = ?)", false),

        // ══════════════════════════════════════════════════════════════════
        // NIVEL 5.1: Combos
        // ══════════════════════════════════════════════════════════════════
        new TablaConfig("producto_combo",
            "producto_id IN (SELECT id FROM public.productos WHERE empresa_id = ?)", false),
        new TablaConfig("producto_combo_item",
            "combo_id IN (SELECT pc.id FROM public.producto_combo pc " +
                "JOIN public.productos p ON pc.producto_id = p.id WHERE p.empresa_id = ?)", false),

        // ══════════════════════════════════════════════════════════════════
        // NIVEL 5.2: Compuestos v1
        // ══════════════════════════════════════════════════════════════════
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

        // ══════════════════════════════════════════════════════════════════
        // NIVEL 5.3: Compuestos v2
        // ══════════════════════════════════════════════════════════════════
        new TablaConfig("producto_compuesto_v2",
            "producto_id IN (SELECT id FROM public.productos WHERE empresa_id = ?)", false),
        new TablaConfig("slot_v2",
            "compuesto_id IN (SELECT id FROM public.producto_compuesto_v2 WHERE " +
                "producto_id IN (SELECT id FROM public.productos WHERE empresa_id = ?))", false),
        new TablaConfig("opcion_v2",
            "slot_id IN (SELECT sv.id FROM public.slot_v2 sv " +
                "JOIN public.producto_compuesto_v2 pc ON sv.compuesto_id = pc.id " +
                "JOIN public.productos p ON pc.producto_id = p.id WHERE p.empresa_id = ?)", false),

        // ══════════════════════════════════════════════════════════════════
        // NIVEL 5.4: Recetas e inventario
        // ══════════════════════════════════════════════════════════════════
        new TablaConfig("productos_recetas",
            "producto_id IN (SELECT id FROM public.productos WHERE empresa_id = ?)", false),
        new TablaConfig("receta_ingredientes",
            "receta_id IN (SELECT pr.id FROM public.productos_recetas pr " +
                "JOIN public.productos p ON pr.producto_id = p.id WHERE p.empresa_id = ?)", false),
        new TablaConfig("productos_inventarios",
            "producto_id IN (SELECT id FROM public.productos WHERE empresa_id = ?)", false),
        new TablaConfig("producto_movimientos",
            "producto_id IN (SELECT id FROM public.productos WHERE empresa_id = ?)", false),

        // ══════════════════════════════════════════════════════════════════
        // NIVEL 6: Usuarios del tenant
        // ══════════════════════════════════════════════════════════════════
        new TablaConfig("usuarios",
            "id IN (SELECT usuario_id FROM public.usuarios_sucursales WHERE " +
                "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?))", true),
        new TablaConfig("usuarios_sucursales",
            "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?)", false),
        new TablaConfig("usuario_registro",
            "usuario_id IN (SELECT usuario_id FROM public.usuarios_sucursales WHERE " +
                "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?))", false),
        new TablaConfig("asistencias", "empresa_id = ?", false),

        // ══════════════════════════════════════════════════════════════════
        // NIVEL 7: Sesiones de caja v1
        // NOTA: sesiones_caja usa terminal_id, NO sucursal_id directo
        //       La cadena es: sesiones_caja → terminal_id → terminales → sucursal_id
        // ══════════════════════════════════════════════════════════════════
        new TablaConfig("sesiones_caja",
            "terminal_id IN (SELECT id FROM public.terminales WHERE " +
                "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?))", false),
        new TablaConfig("sesion_caja_usuario",
            "sesion_caja_id IN (SELECT id FROM public.sesiones_caja WHERE " +
                "terminal_id IN (SELECT id FROM public.terminales WHERE " +
                "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?)))", false),
        new TablaConfig("sesion_caja_denominacion",
            "sesion_caja_id IN (SELECT id FROM public.sesiones_caja WHERE " +
                "terminal_id IN (SELECT id FROM public.terminales WHERE " +
                "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?)))", false),
        new TablaConfig("cierre_datafono",
            "sesion_caja_id IN (SELECT id FROM public.sesiones_caja WHERE " +
                "terminal_id IN (SELECT id FROM public.terminales WHERE " +
                "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?)))", false),
        new TablaConfig("movimientos_caja",
            "sesion_caja_id IN (SELECT id FROM public.sesiones_caja WHERE " +
                "terminal_id IN (SELECT id FROM public.terminales WHERE " +
                "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?)))", false),

        // ══════════════════════════════════════════════════════════════════
        // NIVEL 7.1: Sesiones de caja v2
        // v2_sesion_caja tiene sucursal_id directo Y terminal_id
        // ══════════════════════════════════════════════════════════════════
        new TablaConfig("v2_sesion_caja",
            "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?)", false),
        new TablaConfig("v2_turno_cajero",
            "sesion_id IN (SELECT id FROM public.v2_sesion_caja WHERE " +
                "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?))", false),
        new TablaConfig("v2_turno_denominacion",
            "turno_id IN (SELECT id FROM public.v2_turno_cajero WHERE " +
                "sesion_id IN (SELECT id FROM public.v2_sesion_caja WHERE " +
                "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?)))", false),
        new TablaConfig("v2_movimiento_caja",
            "sesion_id IN (SELECT id FROM public.v2_sesion_caja WHERE " +
                "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?))", false),
        new TablaConfig("v2_cierre_datafono",
            "sesion_id IN (SELECT id FROM public.v2_sesion_caja WHERE " +
                "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?))", false),
        new TablaConfig("v2_sesion_plataforma",
            "sesion_id IN (SELECT id FROM public.v2_sesion_caja WHERE " +
                "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?))", false),

        // ══════════════════════════════════════════════════════════════════
        // NIVEL 8: Promociones
        // ══════════════════════════════════════════════════════════════════
        new TablaConfig("promociones",          "empresa_id = ?", false),
        new TablaConfig("promocion_items",
            "promocion_id IN (SELECT id FROM public.promociones WHERE empresa_id = ?)", false),
        new TablaConfig("promocion_productos",
            "promocion_id IN (SELECT id FROM public.promociones WHERE empresa_id = ?)", false),
        new TablaConfig("promocion_categorias",
            "promocion_id IN (SELECT id FROM public.promociones WHERE empresa_id = ?)", false),
        new TablaConfig("promocion_familias",
            "promocion_id IN (SELECT id FROM public.promociones WHERE empresa_id = ?)", false),

        // ══════════════════════════════════════════════════════════════════
        // NIVEL 9: Facturas electrónicas Hacienda
        // ══════════════════════════════════════════════════════════════════
        new TablaConfig("facturas",
            "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?)", true),
        new TablaConfig("factura_detalles",
            "factura_id IN (SELECT f.id FROM public.facturas f " +
                "JOIN public.sucursales s ON f.sucursal_id = s.id WHERE s.empresa_id = ?)", false),
        new TablaConfig("factura_detalle_impuesto",
            "detalle_id IN (SELECT fd.id FROM public.factura_detalles fd " +
                "JOIN public.facturas f ON fd.factura_id = f.id " +
                "JOIN public.sucursales s ON f.sucursal_id = s.id WHERE s.empresa_id = ?)", false),
        new TablaConfig("factura_descuentos",
            "factura_detalle_id IN (SELECT fd.id FROM public.factura_detalles fd " +
                "JOIN public.facturas f ON fd.factura_id = f.id " +
                "JOIN public.sucursales s ON f.sucursal_id = s.id WHERE s.empresa_id = ?)", false),
        new TablaConfig("factura_otros_cargos",
            "factura_id IN (SELECT f.id FROM public.facturas f " +
                "JOIN public.sucursales s ON f.sucursal_id = s.id WHERE s.empresa_id = ?)", false),
        new TablaConfig("factura_medios_pago",
            "factura_id IN (SELECT f.id FROM public.facturas f " +
                "JOIN public.sucursales s ON f.sucursal_id = s.id WHERE s.empresa_id = ?)", false),
        new TablaConfig("factura_resumen_impuesto",
            "factura_id IN (SELECT f.id FROM public.facturas f " +
                "JOIN public.sucursales s ON f.sucursal_id = s.id WHERE s.empresa_id = ?)", false),
        new TablaConfig("factura_bitacora",
            "factura_id IN (SELECT f.id FROM public.facturas f " +
                "JOIN public.sucursales s ON f.sucursal_id = s.id WHERE s.empresa_id = ?)", false),
        new TablaConfig("mensaje_receptor_bitacora",
            "compra_id IN (SELECT id FROM public.compras WHERE empresa_id = ?)", false),

        // ══════════════════════════════════════════════════════════════════
        // NIVEL 9.1: Facturas internas
        // ══════════════════════════════════════════════════════════════════
        new TablaConfig("factura_interna",
            "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?)", false),
        new TablaConfig("factura_interna_detalle",  // singular, verificado en producción
            "factura_interna_id IN (SELECT fi.id FROM public.factura_interna fi " +
                "JOIN public.sucursales s ON fi.sucursal_id = s.id WHERE s.empresa_id = ?)", false),
        new TablaConfig("factura_interna_medios_pago",
            "factura_interna_id IN (SELECT fi.id FROM public.factura_interna fi " +
                "JOIN public.sucursales s ON fi.sucursal_id = s.id WHERE s.empresa_id = ?)", false),

        // ══════════════════════════════════════════════════════════════════
        // NIVEL 9.2: Facturas de recepción (compras de proveedores)
        // ══════════════════════════════════════════════════════════════════
        new TablaConfig("facturas_recepcion",
            "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?)", false),
        new TablaConfig("facturas_recepcion_detalles",
            "factura_recepcion_id IN (SELECT fr.id FROM public.facturas_recepcion fr " +
                "JOIN public.sucursales s ON fr.sucursal_id = s.id WHERE s.empresa_id = ?)", false),
        new TablaConfig("facturas_recepcion_detalles_impuestos",
            "factura_recepcion_detalle_id IN (SELECT frd.id FROM public.facturas_recepcion_detalles frd " +
                "JOIN public.facturas_recepcion fr ON frd.factura_recepcion_id = fr.id " +
                "JOIN public.sucursales s ON fr.sucursal_id = s.id WHERE s.empresa_id = ?)", false),
        new TablaConfig("facturas_recepcion_descuentos",
            "factura_recepcion_detalle_id IN (SELECT frd.id FROM public.facturas_recepcion_detalles frd " +
                "JOIN public.facturas_recepcion fr ON frd.factura_recepcion_id = fr.id " +
                "JOIN public.sucursales s ON fr.sucursal_id = s.id WHERE s.empresa_id = ?)", false),
        new TablaConfig("facturas_recepcion_referencias",
            "factura_recepcion_id IN (SELECT fr.id FROM public.facturas_recepcion fr " +
                "JOIN public.sucursales s ON fr.sucursal_id = s.id WHERE s.empresa_id = ?)", false),
        new TablaConfig("facturas_recepcion_otros_cargos",
            "factura_recepcion_id IN (SELECT fr.id FROM public.facturas_recepcion fr " +
                "JOIN public.sucursales s ON fr.sucursal_id = s.id WHERE s.empresa_id = ?)", false),
        new TablaConfig("facturas_recepcion_medios_pago",
            "factura_recepcion_id IN (SELECT fr.id FROM public.facturas_recepcion fr " +
                "JOIN public.sucursales s ON fr.sucursal_id = s.id WHERE s.empresa_id = ?)", false),

        // ══════════════════════════════════════════════════════════════════
        // NIVEL 10: Compras
        // ══════════════════════════════════════════════════════════════════
        new TablaConfig("compras",        "empresa_id = ?", false),
        new TablaConfig("compra_detalles",
            "compra_id IN (SELECT id FROM public.compras WHERE empresa_id = ?)", false),

        // ══════════════════════════════════════════════════════════════════
        // NIVEL 11: Órdenes de mesa
        // orden_items → factura_id y factura_interna_id (FK opcionales)
        // Va después de facturas para evitar FK violations
        // ══════════════════════════════════════════════════════════════════
        new TablaConfig("ordenes",
            "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?)", false),
        new TablaConfig("orden_items",
            "orden_id IN (SELECT o.id FROM public.ordenes o " +
                "JOIN public.sucursales s ON o.sucursal_id = s.id WHERE s.empresa_id = ?)", false),
        new TablaConfig("orden_item_opciones",  // plural, verificado en producción
            "orden_item_id IN (SELECT oi.id FROM public.orden_items oi " +
                "JOIN public.ordenes o ON oi.orden_id = o.id " +
                "JOIN public.sucursales s ON o.sucursal_id = s.id WHERE s.empresa_id = ?)", false),
        new TablaConfig("orden_personas",
            "orden_id IN (SELECT o.id FROM public.ordenes o " +
                "JOIN public.sucursales s ON o.sucursal_id = s.id WHERE s.empresa_id = ?)", false),
        new TablaConfig("orden_promocion_estado",
            "orden_id IN (SELECT o.id FROM public.ordenes o " +
                "JOIN public.sucursales s ON o.sucursal_id = s.id WHERE s.empresa_id = ?)", false),
        new TablaConfig("orden_promocion_estados",  // tabla duplicada en prod, migrar también
            "orden_id IN (SELECT o.id FROM public.ordenes o " +
                "JOIN public.sucursales s ON o.sucursal_id = s.id WHERE s.empresa_id = ?)", false),
        new TablaConfig("orden_facturas",
            "orden_id IN (SELECT o.id FROM public.ordenes o " +
                "JOIN public.sucursales s ON o.sucursal_id = s.id WHERE s.empresa_id = ?)", false),
        new TablaConfig("orden_facturas_internas",
            "orden_id IN (SELECT o.id FROM public.ordenes o " +
                "JOIN public.sucursales s ON o.sucursal_id = s.id WHERE s.empresa_id = ?)", false),

        // ══════════════════════════════════════════════════════════════════
        // NIVEL 12: Cobros y pagos
        // ══════════════════════════════════════════════════════════════════
        new TablaConfig("cuentas_por_cobrar", "empresa_id = ?", false),
        new TablaConfig("pagos",
            "cliente_id IN (SELECT id FROM public.clientes WHERE empresa_id = ?)", false),
        new TablaConfig("historial_pagos",    "empresa_id = ?", false),
        new TablaConfig("planes_pago",        "empresa_id = ?", false),

        // ══════════════════════════════════════════════════════════════════
        // NIVEL 13: Historial mesas
        // ══════════════════════════════════════════════════════════════════
        new TablaConfig("mesa_estado_hist",
            "mesa_id IN (SELECT m.id FROM public.mesa m " +
                "JOIN public.zona_mesa zm ON m.zona_id = zm.id " +
                "JOIN public.sucursales s ON zm.sucursal_id = s.id WHERE s.empresa_id = ?)", false),

        // ══════════════════════════════════════════════════════════════════
        // NIVEL 14: Métricas
        // ══════════════════════════════════════════════════════════════════
        new TablaConfig("metricas_ventas_diarias",
            "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?)", false),
        new TablaConfig("metricas_ventas_mensuales",     "empresa_id = ?", false),
        new TablaConfig("metricas_productos_vendidos",
            "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?)", false),
        new TablaConfig("metricas_compras_mensuales",    "empresa_id = ?", false),

        // ══════════════════════════════════════════════════════════════════
        // NIVEL 15: Cierre y auditoría
        // ══════════════════════════════════════════════════════════════════
        new TablaConfig("ventas_pausadas",
            "sucursal_id IN (SELECT id FROM public.sucursales WHERE empresa_id = ?)", false),
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

            // 7.2 Resetear sequences  ← NUEVO
            resetearSequences(schemaName);

            // 7.3 Asignar PIN por defecto
            asignarPinPorDefecto(schemaName);

            // 8. Migrar usuarios SUPER_ADMIN a usuarios_globales
            int usuariosMigrados = migrarUsuariosSuperAdmin(empresa, tenant);
            result.setUsuariosMigrados(usuariosMigrados);
            log.info("✓ {} usuarios SUPER_ADMIN migrados a global", usuariosMigrados);

            // 9. Marcar empresa como migrada
            jdbcTemplate.update(
                "UPDATE public.empresas SET migrado_a_tenant = true, tenant_id = ? WHERE id = ?",
                tenant.getId(), empresaId
            );
            log.info("✓ Empresa marcada como migrada (tenant_id={})", tenant.getId());
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

        jdbcTemplate.execute("SET search_path TO " + schemaName + ", public");

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
            if (!config.requerida) {
                jdbcTemplate.execute("SAVEPOINT sp_tabla_opcional");
            }
            migrarTabla(config, empresaId, schemaName, result);
        }
    }

    private void resetearSequences(String schemaName) {
        log.info("─────────────────────────────────────────────────────────");
        log.info("   Reseteando sequences...");

        for (TablaConfig config : TABLAS_A_MIGRAR) {
            jdbcTemplate.execute("SAVEPOINT sp_seq");
            try {
                String checkSql = "SELECT EXISTS (SELECT 1 FROM information_schema.tables " +
                    "WHERE table_schema = ? AND table_name = ?)";
                Boolean exists = jdbcTemplate.queryForObject(checkSql, Boolean.class, schemaName, config.nombre);
                if (!Boolean.TRUE.equals(exists)) continue;

                String seqSql = "SELECT pg_get_serial_sequence(?, 'id')";
                String seqName = jdbcTemplate.queryForObject(
                    seqSql, String.class, schemaName + "." + config.nombre
                );
                if (seqName == null) continue;

                String maxSql = String.format("SELECT COALESCE(MAX(id), 0) FROM %s.%s", schemaName, config.nombre);
                Long maxId = jdbcTemplate.queryForObject(maxSql, Long.class);

                jdbcTemplate.execute(String.format("SELECT setval('%s', %d)", seqName, Math.max(maxId, 1)));
                log.debug("  ✓ sequence {} reseteada a {}", config.nombre, maxId);

            } catch (Exception e) {
                jdbcTemplate.execute("ROLLBACK TO SAVEPOINT sp_seq");
                log.warn("  ⚠ No se pudo resetear sequence de {}: {}", config.nombre, e.getMessage());
            }
        }
        log.info("  ✓ Sequences reseteadas");
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
            // ← SAVEPOINT: recuperar la transacción para que las tablas siguientes no fallen
            jdbcTemplate.execute("ROLLBACK TO SAVEPOINT sp_tabla_opcional");
            result.getTablasConError().add(tabla + ": " + e.getMessage());
            log.warn("  ✗ {} → {}", tabla, e.getMessage());
        }
    }

    /**
     * Migra usuarios SUPER_ADMIN a la tabla global
     */
    /**
     * Migra usuarios ROOT, SOPORTE y SUPER_ADMIN a public.usuarios_globales
     */
    private int migrarUsuariosSuperAdmin(Empresa empresa, Tenant tenant) {
        // Usar JDBC directo para evitar filtros de activo y problemas de join
        String sql = """
            SELECT DISTINCT u.id, u.nombre, u.apellidos, u.email, u.password,
                           u.telefono, u.activo, u.rol
            FROM public.usuarios u
            JOIN public.usuarios_empresas ue ON ue.usuario_id = u.id
            WHERE ue.empresa_id = ?
              AND u.rol IN ('SUPER_ADMIN')
            ORDER BY u.rol, u.nombre
            """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, empresa.getId());
        log.info("  Usuarios globales a migrar: {}", rows.size());

        int migrados = 0;
        for (Map<String, Object> row : rows) {
            try {
                String email    = (String) row.get("email");
                String rolStr   = (String) row.get("rol");
                Long   legacyId = ((Number) row.get("id")).longValue();

                if (email == null || email.isBlank()) {
                    log.warn("  ⊘ Usuario id={} sin email, omitido", legacyId);
                    continue;
                }

                // Si ya existe en usuarios_globales, solo vinculamos si es SUPER_ADMIN
                if (usuarioGlobalRepository.findByEmailIgnoreCase(email).isPresent()) {
                    log.debug("  ⊘ {} ya existe en usuarios_globales", email);
                    // Si es SUPER_ADMIN, asegurar que tenga relación con este tenant
                    if ("SUPER_ADMIN".equals(rolStr)) {
                        usuarioGlobalRepository.findByEmailIgnoreCase(email).ifPresent(ug -> {
                            if (superAdminTenantRepository
                                .findByUsuarioIdAndTenantId(ug.getId(), tenant.getId())
                                .isEmpty()) {
                                SuperAdminTenant relacion = SuperAdminTenant.crearPropietario(ug, tenant);
                                superAdminTenantRepository.save(relacion);
                                log.info("  ✓ Relación SuperAdminTenant creada para {}", email);
                            }
                        });
                    }
                    continue;
                }

                // Mapear rol legacy → RolGlobal
                UsuarioGlobal.RolGlobal rolGlobal = UsuarioGlobal.RolGlobal.SUPER_ADMIN;

                // Crear usuario global
                UsuarioGlobal usuarioGlobal = UsuarioGlobal.builder()
                    .email(email)
                    .password((String) row.get("password"))
                    .nombre((String) row.get("nombre"))
                    .apellidos((String) row.get("apellidos"))
                    .telefono((String) row.get("telefono"))
                    .rol(rolGlobal)
                    .activo(row.get("activo") != null ? (Boolean) row.get("activo") : true)
                    .usuarioLegacyId(legacyId)
                    .requiereCambioPassword(false)
                    .build();

                usuarioGlobal = usuarioGlobalRepository.save(usuarioGlobal);
                log.info("  ✓ {} → usuarios_globales ({})", email, rolGlobal);

                // Solo SUPER_ADMIN se vincula al tenant
                // ROOT y SOPORTE tienen acceso cross-tenant por rol, no necesitan relación
                if (rolGlobal == UsuarioGlobal.RolGlobal.SUPER_ADMIN) {
                    SuperAdminTenant relacion = SuperAdminTenant.crearPropietario(usuarioGlobal, tenant);
                    superAdminTenantRepository.save(relacion);
                    log.info("  ✓ SuperAdminTenant creado para {}", email);
                }

                migrados++;

            } catch (Exception e) {
                log.error("  ✗ Error migrando usuario {}: {}", row.get("email"), e.getMessage());
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