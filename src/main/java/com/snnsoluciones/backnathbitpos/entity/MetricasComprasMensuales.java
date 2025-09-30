package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "metricas_compras_mensuales",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_metricas_compras_periodo",
            columnNames = {"empresa_id", "sucursal_id", "anio", "mes"}
        )
    }
)
@Data
@EqualsAndHashCode(exclude = {"empresa", "sucursal", "cerradoPor"})
@ToString(exclude = {"empresa", "sucursal", "cerradoPor"})
public class MetricasComprasMensuales {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relaciones
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @Column(nullable = false)
    private Integer anio;

    @Column(nullable = false)
    private Integer mes;

    // === TOTALES GENERALES ===
    @Column(name = "total_compras_cantidad", nullable = false)
    private Integer totalComprasCantidad = 0;

    @Column(name = "total_compras_monto", precision = 18, scale = 2)
    private BigDecimal totalComprasMonto = BigDecimal.ZERO;

    @Column(name = "total_compras_monto_usd", precision = 18, scale = 2)
    private BigDecimal totalComprasMontoUsd = BigDecimal.ZERO;

    // === POR TIPO DE DOCUMENTO ===
    @Column(name = "compras_fe_cantidad")
    private Integer comprasFeCantidad = 0;

    @Column(name = "compras_fe_monto", precision = 18, scale = 2)
    private BigDecimal comprasFeMonto = BigDecimal.ZERO;

    @Column(name = "compras_te_cantidad")
    private Integer comprasTeCantidad = 0;

    @Column(name = "compras_te_monto", precision = 18, scale = 2)
    private BigDecimal comprasTeMonto = BigDecimal.ZERO;

    @Column(name = "compras_fec_cantidad")
    private Integer comprasFecCantidad = 0;

    @Column(name = "compras_fec_monto", precision = 18, scale = 2)
    private BigDecimal comprasFecMonto = BigDecimal.ZERO;

    @Column(name = "compras_nc_cantidad")
    private Integer comprasNcCantidad = 0;

    @Column(name = "compras_nc_monto", precision = 18, scale = 2)
    private BigDecimal comprasNcMonto = BigDecimal.ZERO;

    @Column(name = "compras_nd_cantidad")
    private Integer comprasNdCantidad = 0;

    @Column(name = "compras_nd_monto", precision = 18, scale = 2)
    private BigDecimal comprasNdMonto = BigDecimal.ZERO;

    @Column(name = "compras_fee_cantidad")
    private Integer comprasFeeCantidad = 0;

    @Column(name = "compras_fee_monto", precision = 18, scale = 2)
    private BigDecimal comprasFeeMonto = BigDecimal.ZERO;

    // === POR CONDICIÓN DE PAGO ===
    @Column(name = "compras_contado_cantidad")
    private Integer comprasContadoCantidad = 0;

    @Column(name = "compras_contado_monto", precision = 18, scale = 2)
    private BigDecimal comprasContadoMonto = BigDecimal.ZERO;

    @Column(name = "compras_credito_cantidad")
    private Integer comprasCreditoCantidad = 0;

    @Column(name = "compras_credito_monto", precision = 18, scale = 2)
    private BigDecimal comprasCreditoMonto = BigDecimal.ZERO;

    @Column(name = "compras_otros_cantidad")
    private Integer comprasOtrosCantidad = 0;

    @Column(name = "compras_otros_monto", precision = 18, scale = 2)
    private BigDecimal comprasOtrosMonto = BigDecimal.ZERO;

    // === ANÁLISIS DE IMPUESTOS ===
    @Column(name = "total_base_imponible", precision = 18, scale = 2)
    private BigDecimal totalBaseImponible = BigDecimal.ZERO;

    @Column(name = "total_iva_pagado", precision = 18, scale = 2)
    private BigDecimal totalIvaPagado = BigDecimal.ZERO;

    @Column(name = "total_iva_acreditable", precision = 18, scale = 2)
    private BigDecimal totalIvaAcreditable = BigDecimal.ZERO;

    @Column(name = "total_iva_no_acreditable", precision = 18, scale = 2)
    private BigDecimal totalIvaNoAcreditable = BigDecimal.ZERO;

    @Column(name = "total_otros_impuestos", precision = 18, scale = 2)
    private BigDecimal totalOtrosImpuestos = BigDecimal.ZERO;

    @Column(name = "total_exento", precision = 18, scale = 2)
    private BigDecimal totalExento = BigDecimal.ZERO;

    @Column(name = "total_exonerado", precision = 18, scale = 2)
    private BigDecimal totalExonerado = BigDecimal.ZERO;

    @Column(name = "total_gravado", precision = 18, scale = 2)
    private BigDecimal totalGravado = BigDecimal.ZERO;

    @Column(name = "total_no_sujeto", precision = 18, scale = 2)
    private BigDecimal totalNoSujeto = BigDecimal.ZERO;

    // === ANÁLISIS DE PROVEEDORES ===
    @Column(name = "cantidad_proveedores_unicos")
    private Integer cantidadProveedoresUnicos = 0;

    @Column(name = "cantidad_proveedores_nuevos")
    private Integer cantidadProveedoresNuevos = 0;

    // Top 1 Proveedor
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_top1_id")
    private Proveedor proveedorTop1;

    @Column(name = "proveedor_top1_identificacion", length = 20)
    private String proveedorTop1Identificacion;

    @Column(name = "proveedor_top1_nombre", length = 160)
    private String proveedorTop1Nombre;

    @Column(name = "proveedor_top1_monto", precision = 18, scale = 2)
    private BigDecimal proveedorTop1Monto = BigDecimal.ZERO;

    @Column(name = "proveedor_top1_cantidad")
    private Integer proveedorTop1Cantidad = 0;

    // Top 2 Proveedor
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_top2_id")
    private Proveedor proveedorTop2;

    @Column(name = "proveedor_top2_identificacion", length = 20)
    private String proveedorTop2Identificacion;

    @Column(name = "proveedor_top2_nombre", length = 160)
    private String proveedorTop2Nombre;

    @Column(name = "proveedor_top2_monto", precision = 18, scale = 2)
    private BigDecimal proveedorTop2Monto = BigDecimal.ZERO;

    @Column(name = "proveedor_top2_cantidad")
    private Integer proveedorTop2Cantidad = 0;

    // Top 3 Proveedor
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_top3_id")
    private Proveedor proveedorTop3;

    @Column(name = "proveedor_top3_identificacion", length = 20)
    private String proveedorTop3Identificacion;

    @Column(name = "proveedor_top3_nombre", length = 160)
    private String proveedorTop3Nombre;

    @Column(name = "proveedor_top3_monto", precision = 18, scale = 2)
    private BigDecimal proveedorTop3Monto = BigDecimal.ZERO;

    @Column(name = "proveedor_top3_cantidad")
    private Integer proveedorTop3Cantidad = 0;

    // === ANÁLISIS POR CATEGORÍAS ===
    @Column(name = "gasto_mercaderia", precision = 18, scale = 2)
    private BigDecimal gastoMercaderia = BigDecimal.ZERO;

    @Column(name = "gasto_servicios", precision = 18, scale = 2)
    private BigDecimal gastoServicios = BigDecimal.ZERO;

    @Column(name = "gasto_activos_fijos", precision = 18, scale = 2)
    private BigDecimal gastoActivosFijos = BigDecimal.ZERO;

    @Column(name = "gasto_otros", precision = 18, scale = 2)
    private BigDecimal gastoOtros = BigDecimal.ZERO;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "top_categorias_cabys", columnDefinition = "jsonb")
    private List<Map<String, Object>> topCategoriasCabys;

    // === ANÁLISIS DE PRODUCTOS ===
    @Column(name = "cantidad_productos_unicos")
    private Integer cantidadProductosUnicos = 0;

    @Column(name = "cantidad_lineas_detalle_total")
    private Integer cantidadLineasDetalleTotal = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "top_productos_comprados", columnDefinition = "jsonb")
    private List<Map<String, Object>> topProductosComprados;

    // === ANÁLISIS TEMPORAL ===
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "distribucion_diaria", columnDefinition = "jsonb")
    private Map<String, Object> distribucionDiaria = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "distribucion_dia_semana", columnDefinition = "jsonb")
    private Map<String, Object> distribucionDiaSemana = new HashMap<>();

    @Column(name = "mejor_dia_numero")
    private Integer mejorDiaNumero;

    @Column(name = "mejor_dia_monto", precision = 18, scale = 2)
    private BigDecimal mejorDiaMonto = BigDecimal.ZERO;

    @Column(name = "peor_dia_numero")
    private Integer peorDiaNumero;

    @Column(name = "peor_dia_monto", precision = 18, scale = 2)
    private BigDecimal peorDiaMonto = BigDecimal.ZERO;

    // === COMPARACIONES ===
    @Column(name = "variacion_monto_mes_anterior", precision = 18, scale = 2)
    private BigDecimal variacionMontoMesAnterior;

    @Column(name = "variacion_porcentaje_mes_anterior", precision = 5, scale = 2)
    private BigDecimal variacionPorcentajeMesAnterior;

    @Column(name = "variacion_monto_anio_anterior", precision = 18, scale = 2)
    private BigDecimal variacionMontoAnioAnterior;

    @Column(name = "variacion_porcentaje_anio_anterior", precision = 5, scale = 2)
    private BigDecimal variacionPorcentajeAnioAnterior;

    @Column(name = "promedio_compras_diario", precision = 18, scale = 2)
    private BigDecimal promedioComprasDiario = BigDecimal.ZERO;

    @Column(name = "promedio_facturas_diario", precision = 5, scale = 2)
    private BigDecimal promedioFacturasDiario = BigDecimal.ZERO;

    // === MÉTRICAS DE EFICIENCIA ===
    @Column(name = "promedio_dias_credito", precision = 5, scale = 2)
    private BigDecimal promedioDiasCredito = BigDecimal.ZERO;

    @Column(name = "monto_pendiente_pago", precision = 18, scale = 2)
    private BigDecimal montoPendientePago = BigDecimal.ZERO;

    @Column(name = "monto_vencido", precision = 18, scale = 2)
    private BigDecimal montoVencido = BigDecimal.ZERO;

    @Column(name = "total_descuentos_obtenidos", precision = 18, scale = 2)
    private BigDecimal totalDescuentosObtenidos = BigDecimal.ZERO;

    @Column(name = "porcentaje_descuento_promedio", precision = 5, scale = 2)
    private BigDecimal porcentajeDescuentoPromedio = BigDecimal.ZERO;

    // === FACTURACIÓN ELECTRÓNICA ===
    @Column(name = "cantidad_rechazos_hacienda")
    private Integer cantidadRechazosHacienda = 0;

    @Column(name = "cantidad_pendientes_hacienda")
    private Integer cantidadPendientesHacienda = 0;

    @Column(name = "porcentaje_aceptacion", precision = 5, scale = 2)
    private BigDecimal porcentajeAceptacion = new BigDecimal("100.00");

    @Column(name = "facturas_recepcion_automatica")
    private Integer facturasRecepcionAutomatica = 0;

    @Column(name = "facturas_ingreso_manual")
    private Integer facturasIngresoManual = 0;

    @Column(name = "facturas_api_hacienda")
    private Integer facturasApiHacienda = 0;

    // === AUDITORÍA Y CONTROL ===
    @Column(name = "fecha_primera_compra")
    private LocalDateTime fechaPrimeraCompra;

    @Column(name = "fecha_ultima_compra")
    private LocalDateTime fechaUltimaCompra;

    @Column(name = "ultima_actualizacion")
    private LocalDateTime ultimaActualizacion = LocalDateTime.now();

    @Column(name = "cantidad_actualizaciones")
    private Integer cantidadActualizaciones = 0;

    @Column(name = "mes_cerrado")
    private Boolean mesCerrado = false;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cerrado_por")
    private Usuario cerradoPor;

    @Column(columnDefinition = "TEXT")
    private String notas;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ajustes_manuales", columnDefinition = "jsonb")
    private Map<String, Object> ajustesManuales = new HashMap<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        ultimaActualizacion = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        ultimaActualizacion = LocalDateTime.now();
        cantidadActualizaciones++;
    }
}