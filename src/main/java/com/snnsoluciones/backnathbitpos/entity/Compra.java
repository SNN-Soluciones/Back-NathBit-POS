package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.mh.EstadoBitacora;
import com.snnsoluciones.backnathbitpos.enums.mh.EstadoCompra;
import com.snnsoluciones.backnathbitpos.enums.mh.Moneda;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoCompra;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "compras")
@Data
@EqualsAndHashCode(exclude = {"empresa", "sucursal", "proveedor", "usuario", "detalles"})
@ToString(exclude = {"empresa", "sucursal", "proveedor", "usuario", "detalles"})
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relaciones principales
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_compra", nullable = false)
    private TipoCompra tipoCompra;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento_hacienda")
    private TipoDocumento tipoDocumentoHacienda; // FEC = 08

    // Datos del documento
    @Column(name = "numero_documento", length = 50)
    private String numeroDocumento; // Consecutivo o número de factura del proveedor

    @Column(name = "clave_hacienda", length = 50, unique = true)
    private String claveHacienda; // Clave del documento (si es electrónico)

    @Column(name = "consecutivo_hacienda", length = 20)
    private String consecutivoHacienda; // Para FEC generadas

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    @Column(name = "fecha_recepcion")
    private LocalDateTime fechaRecepcion;

    // Condiciones comerciales
    @Column(name = "condicion_venta", length = 2, nullable = false)
    private String condicionVenta; // 01=Contado, 02=Crédito, etc.

    @Column(name = "plazo_credito")
    private Integer plazoCredito; // En días

    @Column(name = "medio_pago", length = 2)
    private String medioPago; // 01=Efectivo, 02=Tarjeta, etc.

    // Moneda
    @Enumerated(EnumType.STRING)
    @JoinColumn(name = "moneda_id", nullable = false)
    private Moneda moneda;

    @Column(name = "tipo_cambio", precision = 18, scale = 5)
    private BigDecimal tipoCambio;

    // Totales
    @Column(name = "total_servicios_gravados", precision = 18, scale = 5)
    private BigDecimal totalServiciosGravados = BigDecimal.ZERO;

    @Column(name = "total_servicios_exentos", precision = 18, scale = 5)
    private BigDecimal totalServiciosExentos = BigDecimal.ZERO;

    @Column(name = "total_servicios_exonerados", precision = 18, scale = 5)
    private BigDecimal totalServiciosExonerados = BigDecimal.ZERO;

    @Column(name = "total_mercancias_gravadas", precision = 18, scale = 5)
    private BigDecimal totalMercanciasGravadas = BigDecimal.ZERO;

    @Column(name = "total_mercancias_exentas", precision = 18, scale = 5)
    private BigDecimal totalMercanciasExentas = BigDecimal.ZERO;

    @Column(name = "total_mercancias_exoneradas", precision = 18, scale = 5)
    private BigDecimal totalMercanciasExoneradas = BigDecimal.ZERO;

    @Column(name = "total_gravado", precision = 18, scale = 5, nullable = false)
    private BigDecimal totalGravado = BigDecimal.ZERO;

    @Column(name = "total_exento", precision = 18, scale = 5, nullable = false)
    private BigDecimal totalExento = BigDecimal.ZERO;

    @Column(name = "total_exonerado", precision = 18, scale = 5, nullable = false)
    private BigDecimal totalExonerado = BigDecimal.ZERO;

    @Column(name = "total_venta", precision = 18, scale = 5, nullable = false)
    private BigDecimal totalVenta;

    @Column(name = "total_descuentos", precision = 18, scale = 5, nullable = false)
    private BigDecimal totalDescuentos = BigDecimal.ZERO;

    @Column(name = "total_venta_neta", precision = 18, scale = 5, nullable = false)
    private BigDecimal totalVentaNeta;

    @Column(name = "total_impuesto", precision = 18, scale = 5, nullable = false)
    private BigDecimal totalImpuesto = BigDecimal.ZERO;

    @Column(name = "total_otros_cargos", precision = 18, scale = 5)
    private BigDecimal totalOtrosCargos = BigDecimal.ZERO;

    @Column(name = "total_comprobante", precision = 18, scale = 5, nullable = false)
    private BigDecimal totalComprobante;

    // Estado y procesamiento
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoCompra estado = EstadoCompra.BORRADOR;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_hacienda")
    private EstadoBitacora estadoBitacora;

    @Column(name = "mensaje_hacienda", columnDefinition = "TEXT")
    private String mensajeHacienda;

    // Archivos
    @Column(name = "xml_original", columnDefinition = "TEXT")
    private String xmlOriginal; // XML cargado o recibido

    @Column(name = "xml_firmado", columnDefinition = "TEXT")
    private String xmlFirmado; // XML firmado para FEC

    @Column(name = "xml_respuesta", columnDefinition = "TEXT")
    private String xmlRespuesta; // Respuesta de Hacienda

    // Observaciones
    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    // Auditoría
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relación con detalles
    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CompraDetalle> detalles = new ArrayList<>();

    // Métodos auxiliares
    public void addDetalle(CompraDetalle detalle) {
        detalles.add(detalle);
        detalle.setCompra(this);
    }

    public void removeDetalle(CompraDetalle detalle) {
        detalles.remove(detalle);
        detalle.setCompra(null);
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}