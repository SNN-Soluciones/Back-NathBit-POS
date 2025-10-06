package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento;
import jakarta.persistence.*;
import lombok.*;

/**
 * Información de referencia para NC/ND/FEC
 * Permite hasta 10 referencias según Hacienda v4.4
 */
@Entity
@Table(name = "facturas_recepcion_referencias",
    indexes = {
        @Index(name = "idx_factura_recepcion_referencia_factura", columnList = "factura_recepcion_id"),
        @Index(name = "idx_factura_recepcion_referencia_doc", columnList = "numero_documento_referencia")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "facturaRecepcion")
@ToString(exclude = "facturaRecepcion")
public class FacturaRecepcionReferencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_recepcion_id", nullable = false)
    private FacturaRecepcion facturaRecepcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "factura_recepcion_automatica_id", nullable = false)
    private FacturaRecepcionAutomatica facturaRecepcionAutomatica;

    /**
     * Número de línea (1-10)
     */
    @Column(name = "numero_linea", nullable = false)
    private Integer numeroLinea;

    /**
     * Tipo de documento referenciado
     * Ver nota 10 de Hacienda
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_doc_referencia", length = 20, nullable = false)
    private TipoDocumento tipoDocReferencia;

    /**
     * Descripción cuando se usa código 99
     */
    @Column(name = "tipo_doc_referencia_otro", length = 100)
    private String tipoDocReferenciaOTRO;

    /**
     * Número del documento referenciado (clave o consecutivo)
     */
    @Column(name = "numero_documento_referencia", length = 50, nullable = false)
    private String numeroDocumentoReferencia;

    /**
     * Fecha de emisión del documento referenciado
     */
    @Column(name = "fecha_emision_referencia", length = 25)
    private String fechaEmisionReferencia;

    /**
     * Código de referencia según nota 9 de Hacienda:
     * 01 - Anula Documento de Referencia
     * 02 - Corrige texto de documento de referencia
     * 03 - Corrige monto
     * 04 - Referencia a otro documento
     * 05 - Sustituye comprobante provisional
     * 06 - Devolución de mercancía
     * 07 - Sustituye Comprobante electrónico
     * 08 - Factura Endosada
     * 09 - Nota de crédito financiera
     * 10 - Nota de débito financiera
     * 11 - Proveedor No Domiciliado
     * 12 - Crédito por exoneración posterior a la facturación
     * 99 - Otros
     */
    @Column(name = "codigo_referencia", length = 2, nullable = false)
    private String codigoReferencia;

    /**
     * Descripción cuando se usa código 99
     */
    @Column(name = "codigo_referencia_otro", length = 100)
    private String codigoReferenciaOTRO;

    /**
     * Razón de la referencia
     */
    @Column(name = "razon_referencia", length = 180)
    private String razonReferencia;
}