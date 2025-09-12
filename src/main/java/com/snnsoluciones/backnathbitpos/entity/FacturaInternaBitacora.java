package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "factura_interna_bitacora")
@Data
public class FacturaInternaBitacora {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "factura_id", nullable = false)
    private FacturaInterna factura;
    
    @Column(name = "accion", nullable = false)
    private String accion; // CREADA, MODIFICADA, ANULADA, IMPRESA, ENVIADA_EMAIL
    
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    
    @Column(name = "fecha_accion", nullable = false)
    private LocalDateTime fechaAccion = LocalDateTime.now();
    
    @Column(name = "descripcion")
    private String descripcion;
    
    @Column(name = "datos_anteriores", columnDefinition = "TEXT")
    private String datosAnteriores; // JSON con datos antes del cambio
    
    @Column(name = "datos_nuevos", columnDefinition = "TEXT")
    private String datosNuevos; // JSON con datos después del cambio
}