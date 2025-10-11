package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Medio de pago para factura interna
 * Permite múltiples formas de pago en una misma factura
 */
@Entity
@Table(name = "factura_interna_medios_pago")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class FacturaInternaMedioPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_interna_id", nullable = false)
    private FacturaInterna facturaInterna;

    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo; // EFECTIVO, TARJETA, SINPE, TRANSFERENCIA, CHEQUE

    @Column(name = "monto", nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(name = "referencia", length = 100)
    private String referencia; // Número de voucher, SINPE, transferencia, etc.

    @Column(name = "banco", length = 50)
    private String banco; // Para tarjetas o transferencias

    @Column(name = "notas", length = 200)
    private String notas;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plataforma_digital_id")
    private PlataformaDigitalConfig plataformaDigital;
}