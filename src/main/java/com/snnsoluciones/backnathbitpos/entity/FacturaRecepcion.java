package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.factura.EstadoFacturaRecepcion;
import com.snnsoluciones.backnathbitpos.enums.factura.TipoMensajeReceptor;
import com.snnsoluciones.backnathbitpos.enums.mh.Moneda;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoIdentificacion;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que almacena facturas electrónicas RECIBIDAS de proveedores
 * Estructura ESPEJO de la entidad Factura (ventas)
 * Representa el XML completo parseado según estructura de Hacienda v4.4
 */
@Entity
@Table(name = "facturas_recepcion",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_factura_recepcion_clave", columnNames = "clave")
    },
    indexes = {
        @Index(name = "idx_factura_recepcion_empresa", columnList = "empresa_id"),
        @Index(name = "idx_factura_recepcion_sucursal", columnList = "sucursal_id"),
        @Index(name = "idx_factura_recepcion_clave", columnList = "clave"),
        @Index(name = "idx_factura_recepcion_estado", columnList = "estado_interno"),
        @Index(name = "idx_factura_recepcion_proveedor", columnList = "proveedor_identificacion"),
        @Index(name = "idx_factura_recepcion_fecha_emision", columnList = "fecha_emision"),
        @Index(name = "idx_factura_recepcion_compra", columnList = "compra_id")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"empresa", "sucursal", "compra", "detalles", "otrosCargos", "mediosPago", "referencias"})
@ToString(exclude = {"empresa", "sucursal", "compra", "detalles", "otrosCargos", "mediosPago", "referencias"})
public class FacturaRecepcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== RELACIONES ====================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compra_id")
    private Compra compra; // Nullable hasta que se convierta

    @OneToMany(mappedBy = "facturaRecepcion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numeroLinea ASC")
    @Builder.Default
    private List<FacturaRecepcionDetalle> detalles = new ArrayList<>();

    @OneToMany(mappedBy = "facturaRecepcion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numeroLinea ASC")
    @Builder.Default
    private List<FacturaRecepcionOtroCargo> otrosCargos = new ArrayList<>();

    @OneToMany(mappedBy = "facturaRecepcion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FacturaRecepcionMedioPago> mediosPago = new ArrayList<>();

    @OneToMany(mappedBy = "facturaRecepcion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numeroLinea ASC")
    @Builder.Default
    private List<FacturaRecepcionReferencia> referencias = new ArrayList<>();

    // ==================== DATOS DEL DOCUMENTO ====================

    /**
     * Clave numérica de 50 dígitos del comprobante
     */
    @Column(name = "clave", length = 50, nullable = false, unique = true)
    private String clave;

    /**
     * Tipo de documento (01=FE, 02=ND, 03=NC, 04=TE, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 20)
    private TipoDocumento tipoDocumento;

    /**
     * Consecutivo de 20 dígitos del documento
     */
    @Column(name = "numero_consecutivo", length = 20, nullable = false)
    private String numeroConsecutivo;

    /**
     * Código de seguridad de 8 dígitos
     */
    @Column(name = "codigo_seguridad", length = 8)
    private String codigoSeguridad;

    /**
     * Fecha de emisión del documento (del XML)
     */
    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    /**
     * Fecha cuando se recibió/subió el XML a nuestro sistema
     */
    @Column(name = "fecha_recepcion", nullable = false)
    private LocalDateTime fechaRecepcion;

    // ==================== DATOS DEL PROVEEDOR (EMISOR) ====================

    @Column(name = "proveedor_nombre", length = 200, nullable = false)
    private String proveedorNombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "proveedor_tipo_identificacion", nullable = false, length = 20)
    private TipoIdentificacion proveedorTipoIdentificacion;

    @Column(name = "proveedor_identificacion", length = 12, nullable = false)
    private String proveedorIdentificacion;

    @Column(name = "proveedor_nombre_comercial", length = 200)
    private String proveedorNombreComercial;

    @Column(name = "proveedor_email", length = 100)
    private String proveedorEmail;

    @Column(name = "proveedor_telefono", length = 20)
    private String proveedorTelefono;

    // Ubicación del proveedor
    @Column(name = "proveedor_provincia", length = 100)
    private String proveedorProvincia;

    @Column(name = "proveedor_canton", length = 100)
    private String proveedorCanton;

    @Column(name = "proveedor_distrito", length = 100)
    private String proveedorDistrito;

    @Column(name = "proveedor_barrio", length = 100)
    private String proveedorBarrio;

    @Column(name = "proveedor_otras_senas", length = 500)
    private String proveedorOtrasSenas;

    // ==================== CONDICIONES COMERCIALES ====================

    /**
     * Código de condición de venta (01=Contado, 02=Crédito, etc.)
     */
    @Column(name = "condicion_venta", length = 2, nullable = false)
    private String condicionVenta;

    /**
     * Plazo de crédito en días (si aplica)
     */
    @Column(name = "plazo_credito")
    private Integer plazoCredito;

    // ==================== MONEDA Y TIPO DE CAMBIO ====================

    @Enumerated(EnumType.STRING)
    @Column(name = "moneda", nullable = false, length = 10)
    @Builder.Default
    private Moneda moneda = Moneda.CRC;

    @Column(name = "tipo_cambio", precision = 18, scale = 5)
    @Builder.Default
    private BigDecimal tipoCambio = BigDecimal.ONE;

    // ==================== TOTALES (según ResumenFactura) ====================

    @Column(name = "total_serv_gravados", precision = 18, scale = 5)
    @Builder.Default
    private BigDecimal totalServGravados = BigDecimal.ZERO;

    @Column(name = "total_serv_exentos", precision = 18, scale = 5)
    @Builder.Default
    private BigDecimal totalServExentos = BigDecimal.ZERO;

    @Column(name = "total_serv_exonerado", precision = 18, scale = 5)
    @Builder.Default
    private BigDecimal totalServExonerado = BigDecimal.ZERO;

    @Column(name = "total_serv_no_sujeto", precision = 18, scale = 5)
    @Builder.Default
    private BigDecimal totalServNoSujeto = BigDecimal.ZERO;

    @Column(name = "total_merc_gravada", precision = 18, scale = 5)
    @Builder.Default
    private BigDecimal totalMercGravada = BigDecimal.ZERO;

    @Column(name = "total_merc_exenta", precision = 18, scale = 5)
    @Builder.Default
    private BigDecimal totalMercExenta = BigDecimal.ZERO;

    @Column(name = "total_merc_exonerada", precision = 18, scale = 5)
    @Builder.Default
    private BigDecimal totalMercExonerada = BigDecimal.ZERO;

    @Column(name = "total_merc_no_sujeta", precision = 18, scale = 5)
    @Builder.Default
    private BigDecimal totalMercNoSujeta = BigDecimal.ZERO;

    @Column(name = "total_gravado", precision = 18, scale = 5)
    @Builder.Default
    private BigDecimal totalGravado = BigDecimal.ZERO;

    @Column(name = "total_exento", precision = 18, scale = 5)
    @Builder.Default
    private BigDecimal totalExento = BigDecimal.ZERO;

    @Column(name = "total_exonerado", precision = 18, scale = 5)
    @Builder.Default
    private BigDecimal totalExonerado = BigDecimal.ZERO;

    @Column(name = "total_no_sujeto", precision = 18, scale = 5)
    @Builder.Default
    private BigDecimal totalNoSujeto = BigDecimal.ZERO;

    @Column(name = "total_venta", precision = 18, scale = 5, nullable = false)
    private BigDecimal totalVenta;

    @Column(name = "total_descuentos", precision = 18, scale = 5)
    @Builder.Default
    private BigDecimal totalDescuentos = BigDecimal.ZERO;

    @Column(name = "total_venta_neta", precision = 18, scale = 5, nullable = false)
    private BigDecimal totalVentaNeta;

    @Column(name = "total_impuesto", precision = 18, scale = 5, nullable = false)
    private BigDecimal totalImpuesto;

    @Column(name = "total_iva_devuelto", precision = 18, scale = 5)
    @Builder.Default
    private BigDecimal totalIVADevuelto = BigDecimal.ZERO;

    @Column(name = "total_otros_cargos", precision = 18, scale = 5)
    @Builder.Default
    private BigDecimal totalOtrosCargos = BigDecimal.ZERO;

    @Column(name = "total_comprobante", precision = 18, scale = 5, nullable = false)
    private BigDecimal totalComprobante;

    // ==================== CONTROL DE ESTADOS ====================

    /**
     * Estado del comprobante en Hacienda (aceptado, rechazado, procesando)
     */
    @Column(name = "estado_hacienda", length = 50)
    private String estadoHacienda;

    /**
     * Mensaje de respuesta de Hacienda al consultar estado
     */
    @Column(name = "mensaje_hacienda", columnDefinition = "TEXT")
    private String mensajeHacienda;

    /**
     * Estado interno del proceso de aceptación
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_interno", nullable = false, length = 30)
    @Builder.Default
    private EstadoFacturaRecepcion estadoInterno = EstadoFacturaRecepcion.PENDIENTE_DECISION;

    /**
     * Indica si ya se envió el mensaje receptor a Hacienda
     */
    @Column(name = "mensaje_receptor_enviado", nullable = false)
    @Builder.Default
    private Boolean mensajeReceptorEnviado = false;

    /**
     * Tipo de mensaje receptor enviado (1=Aceptado, 2=Parcial, 3=Rechazado)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_mensaje_receptor", length = 20)
    private TipoMensajeReceptor tipoMensajeReceptor;

    /**
     * Motivo de rechazo o aceptación parcial
     */
    @Column(name = "motivo_respuesta", columnDefinition = "TEXT")
    private String motivoRespuesta;

    /**
     * Monto de impuesto aceptado (solo para aceptación parcial)
     */
    @Column(name = "monto_impuesto_aceptado", precision = 18, scale = 5)
    private BigDecimal montoImpuestoAceptado;

    /**
     * Indica si ya se convirtió a registro de Compra
     */
    @Column(name = "convertida_compra", nullable = false)
    @Builder.Default
    private Boolean convertidaCompra = false;

    // ==================== ARCHIVOS EN S3 ====================

    /**
     * Ruta del XML original en S3
     */
    @Column(name = "xml_original_path", length = 500, nullable = false)
    private String xmlOriginalPath;

    /**
     * Ruta del XML del mensaje receptor firmado en S3
     */
    @Column(name = "xml_mensaje_receptor_path", length = 500)
    private String xmlMensajeReceptorPath;

    /**
     * Ruta del PDF (si viene adjunto)
     */
    @Column(name = "pdf_path", length = 500)
    private String pdfPath;

    // ==================== OBSERVACIONES ====================

    /**
     * Notas u observaciones internas
     */
    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    // ==================== VERSIÓN ====================

    /**
     * Versión de catálogos de Hacienda utilizada
     */
    @Column(name = "version_catalogos", length = 64)
    private String versionCatalogos;

    // ==================== TIMESTAMPS ====================

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ==================== HELPER METHODS ====================

    public void addDetalle(FacturaRecepcionDetalle detalle) {
        detalles.add(detalle);
        detalle.setFacturaRecepcion(this);
    }

    public void removeDetalle(FacturaRecepcionDetalle detalle) {
        detalles.remove(detalle);
        detalle.setFacturaRecepcion(null);
    }

    public void addOtroCargo(FacturaRecepcionOtroCargo cargo) {
        otrosCargos.add(cargo);
        cargo.setFacturaRecepcion(this);
        cargo.setNumeroLinea(otrosCargos.size());
    }

    public void removeOtroCargo(FacturaRecepcionOtroCargo cargo) {
        otrosCargos.remove(cargo);
        cargo.setFacturaRecepcion(null);
    }

    public void addMedioPago(FacturaRecepcionMedioPago medioPago) {
        mediosPago.add(medioPago);
        medioPago.setFacturaRecepcion(this);
    }

    public void removeMedioPago(FacturaRecepcionMedioPago medioPago) {
        mediosPago.remove(medioPago);
        medioPago.setFacturaRecepcion(null);
    }

    public void addReferencia(FacturaRecepcionReferencia referencia) {
        referencias.add(referencia);
        referencia.setFacturaRecepcion(this);
        referencia.setNumeroLinea(referencias.size() + 1);
    }

    public void removeReferencia(FacturaRecepcionReferencia referencia) {
        referencias.remove(referencia);
        referencia.setFacturaRecepcion(null);
    }

    /**
     * Verifica si la factura puede ser convertida a compra
     */
    public boolean puedeConvertirseACompra() {
        return estadoInterno == EstadoFacturaRecepcion.ACEPTADA
            || estadoInterno == EstadoFacturaRecepcion.ACEPTADA_PARCIAL;
    }

    /**
     * Verifica si ya tiene decisión tomada
     */
    public boolean tieneDecisionTomada() {
        return estadoInterno != EstadoFacturaRecepcion.PENDIENTE_DECISION
            && estadoInterno != EstadoFacturaRecepcion.ERROR_HACIENDA
            && estadoInterno != EstadoFacturaRecepcion.FACTURA_RECHAZADA_MH;
    }
}