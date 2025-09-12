package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.mh.MedioPago;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "factura_interna_medios_pago")
@Data
public class FacturaInternaMediosPago {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "factura_id", nullable = false)
    private FacturaInterna factura;
    
    // Usando el enum MedioPago existente
    @Column(name = "tipo_pago", nullable = false)
    @Enumerated(EnumType.STRING)
    private MedioPago tipoPago;
    
    @Column(name = "monto", nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;
    
    @Column(name = "referencia")
    private String referencia; // Número de voucher, transferencia, etc
    
    @Column(name = "cambio", precision = 15, scale = 2)
    private BigDecimal cambio = BigDecimal.ZERO; // Solo para efectivo
    
    @Column(name = "banco")
    private String banco; // Si aplica
    
    @Column(name = "numero_autorizacion")
    private String numeroAutorizacion; // Para tarjetas
}