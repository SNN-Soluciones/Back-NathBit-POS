package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Descuentos por línea de detalle de factura recibida
 * Estructura ESPEJO de FacturaDescuento
 * Permite hasta 5 descuentos por línea según Hacienda v4.4
 */
@Entity
@Table(name = "facturas_recepcion_descuentos",
    indexes = {
        @Index(name = "idx_factura_recepcion_descuento_detalle", columnList = "factura_recepcion_detalle_id")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "facturaRecepcionDetalle")
@ToString(exclude = "facturaRecepcionDetalle")
public class FacturaRecepcionDescuento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_recepcion_detalle_id", nullable = false)
    private FacturaRecepcionDetalle facturaRecepcionDetalle;

    /**
     * Código según nota 20 de Hacienda:
     * 01 - Descuento por Regalía
     * 02 - Descuento por Regalía o Bonificaciones IVA Cobrado al Cliente
     * 03 - Descuento por Bonificación
     * 04 - Descuento por volumen
     * 05 - Descuento por Temporada (estacional)
     * 06 - Descuento promocional
     * 07 - Descuento Comercial
     * 08 - Descuento por frecuencia
     * 09 - Descuento sostenido
     * 99 - Otros descuentos
     */
    @Column(name = "codigo_descuento", length = 2, nullable = false)
    private String codigoDescuento;

    /**
     * Descripción cuando se usa código 99
     * Mínimo 5 caracteres, máximo 100
     */
    @Column(name = "codigo_descuento_otro", length = 100)
    private String codigoDescuentoOTRO;

    /**
     * Naturaleza del descuento (obligatorio para código 99)
     * Mínimo 3 caracteres, máximo 80
     */
    @Column(name = "naturaleza_descuento", length = 80)
    private String naturalezaDescuento;

    /**
     * Porcentaje de descuento
     * Ej: 10.00 para 10%
     */
    @Column(name = "porcentaje", precision = 5, scale = 2)
    private BigDecimal porcentaje;

    /**
     * Monto del descuento
     * Debe ser menor o igual al monto total de la línea
     */
    @Column(name = "monto_descuento", nullable = false, precision = 18, scale = 5)
    private BigDecimal montoDescuento;

    /**
     * Orden del descuento (1-5)
     * Los descuentos se aplican en cascada según este orden
     */
    @Column(name = "orden", nullable = false)
    private Integer orden;
}