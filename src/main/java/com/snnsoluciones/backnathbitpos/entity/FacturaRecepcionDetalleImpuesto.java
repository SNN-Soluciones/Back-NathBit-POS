package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Impuestos aplicados a cada línea de detalle
 * Estructura ESPEJO de FacturaDetalleImpuesto
 */
@Entity
@Table(name = "facturas_recepcion_detalles_impuestos",
    indexes = {
        @Index(name = "idx_factura_recepcion_detalle_imp_detalle", columnList = "factura_recepcion_detalle_id")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "facturaRecepcionDetalle")
@ToString(exclude = "facturaRecepcionDetalle")
public class FacturaRecepcionDetalleImpuesto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_recepcion_detalle_id", nullable = false)
    private FacturaRecepcionDetalle facturaRecepcionDetalle;

    /**
     * Código del impuesto (01=IVA, 02=Selectivo de Consumo, etc.)
     * Ver nota 8 de Hacienda
     */
    @Column(name = "codigo_impuesto", length = 2, nullable = false)
    private String codigoImpuesto;

    /**
     * Código de tarifa del impuesto
     * Ver nota 8.1 de Hacienda
     */
    @Column(name = "codigo_tarifa", length = 2, nullable = false)
    private String codigoTarifa;

    /**
     * Tarifa (porcentaje) del impuesto
     */
    @Column(name = "tarifa", precision = 5, scale = 2, nullable = false)
    private BigDecimal tarifa;

    /**
     * Factor del impuesto (si aplica)
     */
    @Column(name = "factor_impuesto", precision = 18, scale = 5)
    private BigDecimal factorImpuesto;

    /**
     * Monto del impuesto
     */
    @Column(name = "monto", precision = 18, scale = 5, nullable = false)
    private BigDecimal monto;

    /**
     * Monto exonerado (si aplica)
     */
    @Column(name = "monto_exoneracion", precision = 18, scale = 5)
    @Builder.Default
    private BigDecimal montoExoneracion = BigDecimal.ZERO;

    /**
     * Impuesto neto (monto - exoneración)
     */
    @Column(name = "impuesto_neto", precision = 18, scale = 5)
    private BigDecimal impuestoNeto;

    /**
     * Información de exoneración (JSON o text)
     * Contiene: numeroDocumento, nombreInstitucion, fechaEmision, porcentajeExoneracion, montoExoneracion
     */
    @Column(name = "exoneracion", columnDefinition = "TEXT")
    private String exoneracion;
}