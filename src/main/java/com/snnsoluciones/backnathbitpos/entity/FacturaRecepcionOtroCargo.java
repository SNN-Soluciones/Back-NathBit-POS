package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.mh.TipoIdentificacion;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Otros cargos de una factura recibida
 * Estructura ESPEJO de OtroCargo
 * Permite hasta 15 cargos según Hacienda v4.4
 */
@Entity
@Table(name = "facturas_recepcion_otros_cargos",
    indexes = {
        @Index(name = "idx_factura_recepcion_otro_cargo_factura", columnList = "factura_recepcion_id")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "facturaRecepcion")
@ToString(exclude = "facturaRecepcion")
public class FacturaRecepcionOtroCargo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_recepcion_id", nullable = false)
    private FacturaRecepcion facturaRecepcion;

    /**
     * Código según nota 16 de Hacienda:
     * 01 - Contribución parafiscal
     * 02 - Timbre Cruz Roja
     * 03 - Timbre Bomberos
     * 04 - Cobro de tercero
     * 05 - Costos exportación
     * 06 - Impuesto servicio 10%
     * 07 - Timbre colegios profesionales
     * 08 - Depósitos garantía
     * 09 - Multas o penalizaciones
     * 10 - Intereses moratorios
     * 99 - Otros cargos
     */
    @Column(name = "tipo_documento_oc", length = 2, nullable = false)
    private String tipoDocumentoOC;

    /**
     * Descripción cuando se usa código 99
     * Mínimo 5 caracteres, máximo 100
     */
    @Column(name = "tipo_documento_otros", length = 100)
    private String tipoDocumentoOTROS;

    /**
     * Nombre descriptivo del cargo
     * Ej: "Impuesto de servicio 10%"
     */
    @Column(name = "nombre_cargo", length = 100, nullable = false)
    private String nombreCargo;

    /**
     * Porcentaje del cargo si aplica
     * Ej: 10.00 para servicio 10%
     */
    @Column(name = "porcentaje", precision = 9, scale = 5)
    private BigDecimal porcentaje;

    /**
     * Monto total del cargo
     */
    @Column(name = "monto_cargo", nullable = false, precision = 18, scale = 5)
    private BigDecimal montoCargo;

    /**
     * Número de línea para orden en el XML
     * 1-15 según Hacienda
     */
    @Column(name = "numero_linea", nullable = false)
    private Integer numeroLinea;

    // Campos para terceros (cuando tipoDocumentoOC = '04')
    
    @Enumerated(EnumType.STRING)
    @Column(name = "tercero_tipo_identificacion", length = 20)
    private TipoIdentificacion terceroTipoIdentificacion;

    @Column(name = "tercero_numero_identificacion", length = 20)
    private String terceroNumeroIdentificacion;

    @Column(name = "tercero_nombre", length = 100)
    private String terceroNombre;
}