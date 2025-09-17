package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.EstadoPago;
import com.snnsoluciones.backnathbitpos.enums.mh.MedioPago;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagos")
@Data
@ToString(exclude = {"cuentaPorCobrar", "cliente", "cajero", "sesionCaja"})
public class Pago {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_por_cobrar_id", nullable = false)
    private CuentaPorCobrar cuentaPorCobrar;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cajero_id", nullable = false)
    private Usuario cajero;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sesion_caja_id")
    private SesionCaja sesionCaja;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "medio_pago", nullable = false, length = 20)
    private MedioPago medioPago = MedioPago.EFECTIVO;
    
    @Column(nullable = false, precision = 18, scale = 5)
    private BigDecimal monto;
    
    @Column(name = "numero_recibo", length = 50)
    private String numeroRecibo;
    
    @Column(length = 100)
    private String referencia;
    
    @Column(length = 500)
    private String observaciones;
    
    @Column(name = "fecha_pago", nullable = false)
    private LocalDateTime fechaPago;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPago estado = EstadoPago.APLICADO;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}