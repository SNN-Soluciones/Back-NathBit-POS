package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "factura_interna_descuentos")
@Data
public class FacturaInternaDescuentos {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "factura_id", nullable = false)
    private FacturaInterna factura;
    
    @Column(name = "tipo_descuento", nullable = false)
    private String tipoDescuento; // PORCENTAJE, MONTO_FIJO, PROMOCION
    
    @Column(name = "descripcion", nullable = false)
    private String descripcion;
    
    @Column(name = "porcentaje", precision = 5, scale = 2)
    private BigDecimal porcentaje; // Si es por porcentaje
    
    @Column(name = "monto", nullable = false, precision = 15, scale = 2)
    private BigDecimal monto; // Monto final del descuento
    
    @Column(name = "codigo_promocion")
    private String codigoPromocion; // Si aplica
    
    @Column(name = "autorizado_por")
    private Long autorizadoPor; // ID usuario que autorizó (si requiere)
}