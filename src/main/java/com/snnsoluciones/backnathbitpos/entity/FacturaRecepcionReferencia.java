package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.time.LocalDateTime;

@Entity
@Table(name = "factura_recepcion_referencia")
@Data
@EqualsAndHashCode(exclude = {"facturaRecepcion"})
@ToString(exclude = {"facturaRecepcion"})
public class FacturaRecepcionReferencia {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_recepcion_id", nullable = false)
    private FacturaRecepcionAutomatica facturaRecepcion;
    
    @Column(name = "tipo_doc", length = 2, nullable = false)
    private String tipoDoc;
    
    @Column(length = 50)
    private String numero;
    
    @Column(name = "fecha_emision")
    private LocalDateTime fechaEmision;
    
    @Column(length = 2)
    private String codigo;
    
    @Column(columnDefinition = "TEXT")
    private String razon;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}