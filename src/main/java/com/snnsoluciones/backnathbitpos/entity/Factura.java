package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.facturacion.EstadoFactura;
import com.snnsoluciones.backnathbitpos.enums.mh.CondicionVenta;
import com.snnsoluciones.backnathbitpos.enums.mh.Moneda;
import com.snnsoluciones.backnathbitpos.enums.mh.SituacionDocumento;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Entidad LIMPIA para Factura
 * Solo almacena datos, NO hace cálculos
 * Arquitectura La Jachuda 🚀
 */
@Data
@Entity
@Table(name = "facturas")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"detalles", "mediosPago", "otrosCargos", "resumenImpuestos"})
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========== IDENTIFICACIÓN ==========
    @Column(length = 50, unique = true, updatable = false)
    private String clave;

    @Column(length = 20, nullable = false, unique = true)
    private String consecutivo;

    @Column(name = "codigo_seguridad", length = 8)
    private String codigoSeguridad;

    @Column(name = "tipo_documento", nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoDocumento tipoDocumento;

    @Column(name = "fecha_emision", nullable = false)
    private String fechaEmision;

    @Column(name = "estado", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoFactura estado = EstadoFactura.GENERADA;

    @Column(name = "situacion_comprobante", nullable = false)
    @Enumerated(EnumType.STRING)
    private SituacionDocumento situacion = SituacionDocumento.NORMAL;

    // ========== REFERENCIAS ==========
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terminal_id", nullable = false)
    private Terminal terminal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sesion_caja_id", nullable = false)
    private SesionCaja sesionCaja;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cajero_id", nullable = false)
    private Usuario cajero;

    // Para NC/ND
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_referencia_id")
    private Factura facturaReferencia;

    @Column(name = "codigo_referencia", length = 2)
    private String codigoReferencia;

    @Column(name = "razon_referencia", length = 180)
    private String razonReferencia;

    // ========== DATOS COMERCIALES ==========
    @Column(name = "condicion_venta", nullable = false)
    @Enumerated(EnumType.STRING)
    private CondicionVenta condicionVenta = CondicionVenta.CONTADO;

    @Column(name = "plazo_credito")
    private Integer plazoCredito;

    @Enumerated(EnumType.STRING)
    @Column(name = "codigo_moneda", length = 3, nullable = false)
    private Moneda moneda = Moneda.CRC;

    @Column(name = "tipo_cambio", nullable = false, precision = 18, scale = 5)
    private BigDecimal tipoCambio = BigDecimal.ONE;

    @Column(name = "observaciones", length = 500)
    private String observaciones;

    // ========== DESCUENTO GLOBAL ==========
    @Column(name = "descuento_global_porcentaje", precision = 5, scale = 2)
    private BigDecimal descuentoGlobalPorcentaje = BigDecimal.ZERO;

    @Column(name = "monto_descuento_global", precision = 18, scale = 5)
    private BigDecimal montoDescuentoGlobal = BigDecimal.ZERO;

    @Column(name = "motivo_descuento_global", length = 200)
    private String motivoDescuentoGlobal;

    // ========== TOTALES HACIENDA (Frontend los calcula) ==========
    @Column(name = "total_servicios_gravados", precision = 18, scale = 5)
    private BigDecimal totalServiciosGravados = BigDecimal.ZERO;

    @Column(name = "total_servicios_exentos", precision = 18, scale = 5)
    private BigDecimal totalServiciosExentos = BigDecimal.ZERO;

    @Column(name = "total_servicios_exonerados", precision = 18, scale = 5)
    private BigDecimal totalServiciosExonerados = BigDecimal.ZERO;

    @Column(name = "total_servicios_no_sujetos", precision = 18, scale = 5)
    private BigDecimal totalServiciosNoSujetos = BigDecimal.ZERO;

    @Column(name = "total_mercancias_gravadas", precision = 18, scale = 5)
    private BigDecimal totalMercanciasGravadas = BigDecimal.ZERO;

    @Column(name = "total_mercancias_exentas", precision = 18, scale = 5)
    private BigDecimal totalMercanciasExentas = BigDecimal.ZERO;

    @Column(name = "total_mercancias_exoneradas", precision = 18, scale = 5)
    private BigDecimal totalMercanciasExoneradas = BigDecimal.ZERO;

    @Column(name = "total_mercancias_no_sujetas", precision = 18, scale = 5)
    private BigDecimal totalMercanciasNoSujetas = BigDecimal.ZERO;

    @Column(name = "total_gravado", precision = 18, scale = 5)
    private BigDecimal totalGravado = BigDecimal.ZERO;

    @Column(name = "total_exento", precision = 18, scale = 5)
    private BigDecimal totalExento = BigDecimal.ZERO;

    @Column(name = "total_exonerado", precision = 18, scale = 5)
    private BigDecimal totalExonerado = BigDecimal.ZERO;

    @Column(name = "total_venta", precision = 18, scale = 5)
    private BigDecimal totalVenta = BigDecimal.ZERO;

    @Column(name = "total_descuentos", precision = 18, scale = 5)
    private BigDecimal totalDescuentos = BigDecimal.ZERO;

    @Column(name = "total_venta_neta", precision = 18, scale = 5)
    private BigDecimal totalVentaNeta = BigDecimal.ZERO;

    @Column(name = "total_impuesto", precision = 18, scale = 5)
    private BigDecimal totalImpuesto = BigDecimal.ZERO;

    @Column(name = "total_iva_devuelto", precision = 18, scale = 5)
    private BigDecimal totalIVADevuelto = BigDecimal.ZERO;

    @Column(name = "total_otros_cargos", precision = 18, scale = 5)
    private BigDecimal totalOtrosCargos = BigDecimal.ZERO;

    @Column(name = "total_comprobante", precision = 18, scale = 5)
    private BigDecimal totalComprobante = BigDecimal.ZERO;

    // ========== RELACIONES ==========
    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("numeroLinea ASC")
    private List<FacturaDetalle> detalles = new ArrayList<>();

    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FacturaMedioPago> mediosPago = new ArrayList<>();

    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("numeroLinea ASC")
    private List<OtroCargo> otrosCargos = new ArrayList<>();

    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FacturaResumenImpuesto> resumenImpuestos = new ArrayList<>();

    // ========== AUDITORÍA ==========
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "total_no_sujeto", precision = 19, scale = 5, nullable = false)
    private BigDecimal totalNoSujeto = BigDecimal.ZERO;

    @Column(name = "version_catalogos", length = 64, nullable = false)
    private String versionCatalogos;

    // ========== MÉTODOS HELPER (Solo relaciones) ==========

    public void agregarDetalle(FacturaDetalle detalle) {
        detalles.add(detalle);
        detalle.setFactura(this);
    }

    public void agregarMedioPago(FacturaMedioPago medioPago) {
        mediosPago.add(medioPago);
        medioPago.setFactura(this);
    }

    public void agregarOtroCargo(OtroCargo otroCargo) {
        otrosCargos.add(otroCargo);
        otroCargo.setFactura(this);
        otroCargo.setNumeroLinea(otrosCargos.size());
    }

    public void agregarResumenImpuesto(FacturaResumenImpuesto resumen) {
        resumenImpuestos.add(resumen);
        resumen.setFactura(this);
    }

    public boolean esElectronica() {
        return tipoDocumento != null && tipoDocumento.isElectronico();
    }

    public boolean esNotaCreditoDebito() {
        return tipoDocumento == TipoDocumento.NOTA_CREDITO ||
            tipoDocumento == TipoDocumento.NOTA_DEBITO;
    }

    public void generarCodigoSeguridad() {
        this.codigoSeguridad = String.format("%08d", new Random().nextInt(100000000));
    }
}