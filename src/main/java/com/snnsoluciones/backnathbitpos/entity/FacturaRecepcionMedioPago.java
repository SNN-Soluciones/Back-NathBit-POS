package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.mh.MedioPago;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Medios de pago de una factura recibida
 * Estructura ESPEJO de FacturaMedioPago
 */
@Entity
@Table(name = "facturas_recepcion_medios_pago",
    indexes = {
        @Index(name = "idx_factura_recepcion_medio_pago_factura", columnList = "factura_recepcion_id")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "facturaRecepcion")
@ToString(exclude = "facturaRecepcion")
public class FacturaRecepcionMedioPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_recepcion_id", nullable = false)
    private FacturaRecepcion facturaRecepcion;

    @Column(name = "medio_pago_otro", length = 100)
    private String medioPagoOtro;

    /**
     * Medio de pago según nota 6 de Hacienda:
     * 01 - Efectivo
     * 02 - Tarjeta
     * 03 - Cheque
     * 04 - Transferencia - depósito bancario
     * 05 - Recaudado por terceros
     * 06 - SINPE Móvil
     * 07 - Plataforma Digital
     * 99 - Otros
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "medio_pago", nullable = false, length = 20)
    private MedioPago medioPago;

    /**
     * Monto pagado con este medio
     */
    @Column(name = "monto", nullable = false, precision = 18, scale = 5)
    private BigDecimal monto;

    /**
     * Referencia del pago (número de transferencia, últimos 4 dígitos tarjeta, etc.)
     */
    @Column(name = "referencia", length = 100)
    private String referencia;

    /**
     * Banco o emisor de tarjeta
     */
    @Column(name = "banco", length = 50)
    private String banco;
}