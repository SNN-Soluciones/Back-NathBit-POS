package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "factura_recepcion_detalle_impuesto")
@Data
@EqualsAndHashCode(exclude = {"facturaDetalle"})
@ToString(exclude = {"facturaDetalle"})
public class FacturaRecepcionDetalleImpuesto {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_detalle_id", nullable = false)
    private FacturaRecepcionDetalle facturaDetalle;
    
    // Datos del impuesto
    @Column(length = 2, nullable = false)
    private String codigo;
    
    @Column(name = "codigo_tarifa_iva", length = 2)
    private String codigoTarifaIVA;
    
    @Column(precision = 5, scale = 2, nullable = false)
    private BigDecimal tarifa;
    
    @Column(name = "factor_iva", precision = 5, scale = 2)
    private BigDecimal factorIVA;
    
    @Column(precision = 18, scale = 5, nullable = false)
    private BigDecimal monto;
    
    @Column(name = "monto_exportacion", precision = 18, scale = 5)
    private BigDecimal montoExportacion;
    
    // Exoneraciones
    @Column(name = "tipo_documento_exoneracion", length = 2)
    private String tipoDocumentoExoneracion;
    
    @Column(name = "numero_documento_exoneracion", length = 40)
    private String numeroDocumentoExoneracion;
    
    @Column(name = "nombre_institucion_exoneracion", length = 160)
    private String nombreInstitucionExoneracion;
    
    @Column(name = "fecha_emision_exoneracion")
    private LocalDate fechaEmisionExoneracion;
    
    @Column(name = "porcentaje_exoneracion", precision = 5, scale = 2)
    private BigDecimal porcentajeExoneracion;
    
    @Column(name = "monto_exoneracion", precision = 18, scale = 5)
    private BigDecimal montoExoneracion;
    
    // Impuesto asumido
    @Column(name = "impuesto_asumido_emisor_fabrica", precision = 18, scale = 5)
    private BigDecimal impuestoAsumidoEmisorFabrica = BigDecimal.ZERO;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}