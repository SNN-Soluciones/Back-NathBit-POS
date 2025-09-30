package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "factura_recepcion_otros_cargos")
@Data
@EqualsAndHashCode(exclude = {"facturaRecepcion"})
@ToString(exclude = {"facturaRecepcion"})
public class FacturaRecepcionOtrosCargos {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_recepcion_id", nullable = false)
    private FacturaRecepcionAutomatica facturaRecepcion;
    
    @Column(name = "tipo_documento", length = 2)
    private String tipoDocumento;
    
    @Column(name = "numero_identidad_tercero", length = 20)
    private String numeroIdentidadTercero;
    
    @Column(name = "nombre_tercero", length = 160)
    private String nombreTercero;
    
    @Column(columnDefinition = "TEXT")
    private String detalle;
    
    @Column(precision = 5, scale = 2)
    private BigDecimal porcentaje;
    
    @Column(name = "monto_cargo", precision = 18, scale = 5, nullable = false)
    private BigDecimal montoCargo;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}