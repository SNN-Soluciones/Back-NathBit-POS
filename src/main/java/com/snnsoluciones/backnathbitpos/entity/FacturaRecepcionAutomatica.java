package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.mh.EstadoBitacora;
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
@Table(name = "factura_recepcion_automatica")
@Data
@EqualsAndHashCode(exclude = {"empresa", "sucursal", "compra", "detalles", "impuestosTotales", 
                               "mediosPago", "referencias", "otrosCargos"})
@ToString(exclude = {"empresa", "sucursal", "compra", "detalles", "impuestosTotales", 
                     "mediosPago", "referencias", "otrosCargos"})
public class FacturaRecepcionAutomatica {
    
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
    
    // Datos del documento
    @Column(name = "clave_hacienda", length = 50, unique = true, nullable = false)
    private String claveHacienda;
    
    @Column(name = "proveedor_sistemas", length = 20)
    private String proveedorSistemas;
    
    @Column(name = "codigo_actividad_emisor", length = 10)
    private String codigoActividadEmisor;
    
    @Column(name = "numero_consecutivo", length = 20)
    private String numeroConsecutivo;
    
    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", length = 3, nullable = false)
    private TipoDocumento tipoDocumento;
    
    // Datos del emisor (proveedor)
    @Column(name = "emisor_nombre", length = 160, nullable = false)
    private String emisorNombre;
    
    @Column(name = "emisor_tipo_identificacion", length = 2, nullable = false)
    private String emisorTipoIdentificacion;
    
    @Column(name = "emisor_numero_identificacion", length = 20, nullable = false)
    private String emisorNumeroIdentificacion;
    
    @Column(name = "emisor_nombre_comercial", length = 80)
    private String emisorNombreComercial;
    
    @Column(name = "emisor_provincia", length = 2)
    private String emisorProvincia;
    
    @Column(name = "emisor_canton", length = 3)
    private String emisorCanton;
    
    @Column(name = "emisor_distrito", length = 3)
    private String emisorDistrito;
    
    @Column(name = "emisor_barrio", length = 3)
    private String emisorBarrio;
    
    @Column(name = "emisor_otras_senas", columnDefinition = "TEXT")
    private String emisorOtrasSenas;
    
    @Column(name = "emisor_telefono_codigo", length = 3)
    private String emisorTelefonoCodigo;
    
    @Column(name = "emisor_telefono_numero", length = 20)
    private String emisorTelefonoNumero;
    
    @Column(name = "emisor_fax_codigo", length = 3)
    private String emisorFaxCodigo;
    
    @Column(name = "emisor_fax_numero", length = 20)
    private String emisorFaxNumero;
    
    @Column(name = "emisor_correo", length = 160)
    private String emisorCorreo;
    
    // Datos del receptor (nuestra empresa)
    @Column(name = "receptor_nombre", length = 160)
    private String receptorNombre;
    
    @Column(name = "receptor_tipo_identificacion", length = 2)
    private String receptorTipoIdentificacion;
    
    @Column(name = "receptor_numero_identificacion", length = 20)
    private String receptorNumeroIdentificacion;
    
    @Column(name = "receptor_nombre_comercial", length = 80)
    private String receptorNombreComercial;
    
    @Column(name = "receptor_provincia", length = 2)
    private String receptorProvincia;
    
    @Column(name = "receptor_canton", length = 3)
    private String receptorCanton;
    
    @Column(name = "receptor_distrito", length = 3)
    private String receptorDistrito;
    
    @Column(name = "receptor_barrio", length = 3)
    private String receptorBarrio;
    
    @Column(name = "receptor_otras_senas", columnDefinition = "TEXT")
    private String receptorOtrasSenas;
    
    @Column(name = "receptor_telefono_codigo", length = 3)
    private String receptorTelefonoCodigo;
    
    @Column(name = "receptor_telefono_numero", length = 20)
    private String receptorTelefonoNumero;
    
    @Column(name = "receptor_fax_codigo", length = 3)
    private String receptorFaxCodigo;
    
    @Column(name = "receptor_fax_numero", length = 20)
    private String receptorFaxNumero;
    
    @Column(name = "receptor_correo", length = 160)
    private String receptorCorreo;
    
    // Condiciones comerciales
    @Column(name = "condicion_venta", length = 2, nullable = false)
    private String condicionVenta;
    
    @Column(name = "plazo_credito")
    private Integer plazoCredito;
    
    // Moneda y tipo cambio
    @Column(name = "codigo_moneda", length = 3, nullable = false)
    private String codigoMoneda = "CRC";
    
    @Column(name = "tipo_cambio", precision = 18, scale = 5)
    private BigDecimal tipoCambio = BigDecimal.ONE;
    
    // Totales de servicios
    @Column(name = "total_serv_gravados", precision = 18, scale = 5)
    private BigDecimal totalServGravados = BigDecimal.ZERO;
    
    @Column(name = "total_serv_exentos", precision = 18, scale = 5)
    private BigDecimal totalServExentos = BigDecimal.ZERO;
    
    @Column(name = "total_serv_exonerado", precision = 18, scale = 5)
    private BigDecimal totalServExonerado = BigDecimal.ZERO;
    
    @Column(name = "total_serv_no_sujeto", precision = 18, scale = 5)
    private BigDecimal totalServNoSujeto = BigDecimal.ZERO;
    
    // Totales de mercancías
    @Column(name = "total_mercancias_gravadas", precision = 18, scale = 5)
    private BigDecimal totalMercanciasGravadas = BigDecimal.ZERO;
    
    @Column(name = "total_mercancias_exentas", precision = 18, scale = 5)
    private BigDecimal totalMercanciasExentas = BigDecimal.ZERO;
    
    @Column(name = "total_merc_exonerada", precision = 18, scale = 5)
    private BigDecimal totalMercExonerada = BigDecimal.ZERO;
    
    @Column(name = "total_merc_no_sujeta", precision = 18, scale = 5)
    private BigDecimal totalMercNoSujeta = BigDecimal.ZERO;
    
    // Totales generales
    @Column(name = "total_gravado", precision = 18, scale = 5)
    private BigDecimal totalGravado = BigDecimal.ZERO;
    
    @Column(name = "total_exento", precision = 18, scale = 5)
    private BigDecimal totalExento = BigDecimal.ZERO;
    
    @Column(name = "total_exonerado", precision = 18, scale = 5)
    private BigDecimal totalExonerado = BigDecimal.ZERO;
    
    @Column(name = "total_no_sujeto", precision = 18, scale = 5)
    private BigDecimal totalNoSujeto = BigDecimal.ZERO;
    
    @Column(name = "total_venta", precision = 18, scale = 5, nullable = false)
    private BigDecimal totalVenta;
    
    @Column(name = "total_descuentos", precision = 18, scale = 5)
    private BigDecimal totalDescuentos = BigDecimal.ZERO;
    
    @Column(name = "total_venta_neta", precision = 18, scale = 5, nullable = false)
    private BigDecimal totalVentaNeta;
    
    @Column(name = "total_impuesto", precision = 18, scale = 5)
    private BigDecimal totalImpuesto = BigDecimal.ZERO;
    
    @Column(name = "total_imp_asum_emisor_fabrica", precision = 18, scale = 5)
    private BigDecimal totalImpAsumEmisorFabrica = BigDecimal.ZERO;
    
    @Column(name = "total_iva_devuelto", precision = 18, scale = 5)
    private BigDecimal totalIVADevuelto = BigDecimal.ZERO;
    
    @Column(name = "total_otros_cargos", precision = 18, scale = 5)
    private BigDecimal totalOtrosCargos = BigDecimal.ZERO;
    
    @Column(name = "total_comprobante", precision = 18, scale = 5, nullable = false)
    private BigDecimal totalComprobante;
    
    // Información adicional
    @Column(name = "informacion_referencia", columnDefinition = "TEXT")
    private String informacionReferencia;
    
    @Column(name = "otros_texto", columnDefinition = "TEXT")
    private String otrosTexto;
    
    @Column(name = "otros_contenido", columnDefinition = "JSONB")
    private String otrosContenido;
    
    // Firma digital
    @Column(name = "firma_timestamp")
    private LocalDateTime firmaTimestamp;
    
    @Column(name = "firma_certificado_emisor", columnDefinition = "TEXT")
    private String firmaCertificadoEmisor;
    
    @Column(name = "firma_numero_serie", length = 50)
    private String firmaNumeroSerie;
    
    // Estado Hacienda
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_hacienda", length = 20)
    private EstadoBitacora estadoHacienda = EstadoBitacora.PENDIENTE;
    
    @Column(name = "mensaje_hacienda", columnDefinition = "TEXT")
    private String mensajeHacienda;
    
    @Column(name = "fecha_validacion")
    private LocalDateTime fechaValidacion;
    
    @Column(name = "detalle_mensaje_hacienda", columnDefinition = "JSONB")
    private String detalleMensajeHacienda;
    
    // Archivos S3
    @Column(name = "ruta_xml_s3", length = 500)
    private String rutaXmlS3;
    
    @Column(name = "ruta_pdf_s3", length = 500)
    private String rutaPdfS3;
    
    @Column(name = "ruta_respuesta_hacienda_s3", length = 500)
    private String rutaRespuestaHaciendaS3;
    
    // Control interno
    @Column(columnDefinition = "boolean default false")
    private Boolean procesado = false;
    
    @Column(name = "fecha_procesado")
    private LocalDateTime fechaProcesado;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compra_id")
    private Compra compra;
    
    @Column(name = "observaciones_internas", columnDefinition = "TEXT")
    private String observacionesInternas;
    
    // Auditoría
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Usuario createdBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private Usuario updatedBy;
    
    // Relaciones
    @OneToMany(mappedBy = "facturaRecepcion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FacturaRecepcionDetalle> detalles = new ArrayList<>();
    
    @OneToMany(mappedBy = "facturaRecepcion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FacturaRecepcionImpuestoTotal> impuestosTotales = new ArrayList<>();
    
    @OneToMany(mappedBy = "facturaRecepcion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FacturaRecepcionMedioPago> mediosPago = new ArrayList<>();
    
    @OneToMany(mappedBy = "facturaRecepcion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FacturaRecepcionReferencia> referencias = new ArrayList<>();
    
    @OneToMany(mappedBy = "facturaRecepcion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FacturaRecepcionOtrosCargos> otrosCargos = new ArrayList<>();
    
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