package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "factura_interna_otros_cargos")
@Data
public class FacturaInternaOtrosCargos {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "factura_id", nullable = false)
    private FacturaInterna factura;
    
    @Column(name = "tipo_cargo", nullable = false)
    private String tipoCargo; // SERVICIO, PROPINA, DELIVERY, OTRO
    
    @Column(name = "descripcion", nullable = false)
    private String descripcion;
    
    @Column(name = "porcentaje", precision = 5, scale = 2)
    private BigDecimal porcentaje; // Si es por porcentaje (ej: 10% servicio)
    
    @Column(name = "monto", nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;
    
    @Column(name = "aplicado_automaticamente")
    private Boolean aplicadoAutomaticamente = false; // Para servicio automático
}