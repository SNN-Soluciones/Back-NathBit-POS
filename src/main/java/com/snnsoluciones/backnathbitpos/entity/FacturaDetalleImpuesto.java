package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Entidad para manejar impuestos por línea de detalle
 * Permite múltiples impuestos y exoneraciones según Hacienda 4.4
 * Parte de la Arquitectura La Jachuda 🚀
 */
@Data
@Entity
@Table(name = "factura_detalle_impuesto")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"facturaDetalle", "productoImpuesto"})
public class FacturaDetalleImpuesto {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_impuesto_id")
    private ProductoImpuesto productoImpuesto;
    
    /**
     * Código del impuesto según nota 8:
     * 01 - IVA
     * 02 - Impuesto selectivo consumo
     * 07 - IVA cálculo especial
     * 08 - IVA Régimen bienes usados
     * 12 - Impuesto específico al cemento
     * 99 - Otros
     */
    @Column(name = "codigo_impuesto", length = 50, nullable = false)
    private String codigoImpuesto;
    
    @Column(name = "codigo_impuesto_otro", length = 100)
    private String codigoImpuestoOTRO;
    
    /**
     * Código tarifa IVA según nota 8.1
     * Solo aplica para códigos 01 y 07
     */
    @Column(name = "codigo_tarifa_iva", length = 50)
    private String codigoTarifaIVA;
    
    @Column(name = "tarifa", nullable = false, precision = 5, scale = 10)
    private BigDecimal tarifa;
    
    /**
     * Factor para cálculo especial (bienes usados)
     */
    @Column(name = "factor_calculo", precision = 5, scale = 4)
    private BigDecimal factorCalculo;
    
    /**
     * Monto del impuesto calculado
     */
    @Column(name = "monto_impuesto", nullable = false, precision = 18, scale = 5)
    private BigDecimal montoImpuesto;
    
    /**
     * Base imponible si es diferente al subtotal
     */
    @Column(name = "base_imponible", precision = 18, scale = 5)
    private BigDecimal baseImponible;
    
    // ========== CAMPOS DE EXONERACIÓN ==========
    
    /**
     * Indica si esta línea tiene exoneración
     */
    @Column(name = "tiene_exoneracion", nullable = false)
    private Boolean tieneExoneracion = false;
    
    /**
     * Tipo documento exoneración según nota 10.1
     */
    @Column(name = "tipo_documento_exoneracion", length = 50)
    private String tipoDocumentoExoneracion;
    
    @Column(name = "tipo_documento_exoneracion_otro", length = 100)
    private String tipoDocumentoExoneracionOTRO;
    
    @Column(name = "numero_documento_exoneracion", length = 40)
    private String numeroDocumentoExoneracion;
    
    @Column(name = "articulo_exoneracion", length = 6)
    private String articuloExoneracion;
    
    @Column(name = "inciso_exoneracion", length = 6)
    private String incisoExoneracion;
    
    /**
     * Institución que emite según nota 23
     */
    @Column(name = "nombre_institucion", length = 100)
    private String nombreInstitucion;
    
    @Column(name = "nombre_institucion_otros", length = 160)
    private String nombreInstitucionOtros;
    
    @Column(name = "fecha_emision_exoneracion")
    private String fechaEmisionExoneracion;
    
    @Column(name = "tarifa_exonerada", precision = 5, scale = 2)
    private BigDecimal tarifaExonerada;
    
    @Column(name = "monto_exoneracion", precision = 18, scale = 5)
    private BigDecimal montoExoneracion;
    
    /**
     * Impuesto neto = montoImpuesto - montoExoneracion
     */
    @Column(name = "impuesto_neto", nullable = false, precision = 18, scale = 5)
    private BigDecimal impuestoNeto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "detalle_id", nullable = false)
    private FacturaDetalle detalle;

    // ---- IMPUESTO ASUMIDO (v4.4) ----
    @Column(name = "impuesto_asumido_por_emisor")
    private Boolean impuestoAsumidoPorEmisor = Boolean.FALSE;

    @Column(name = "monto_impuesto_asumido", precision = 19, scale = 5)
    private BigDecimal montoImpuestoAsumido;
    
    // ========== MÉTODOS DE CÁLCULO ==========
    
    @PrePersist
    @PreUpdate
    public void calcular() {
        // Calcular impuesto neto
        if (tieneExoneracion && montoExoneracion != null) {
            impuestoNeto = montoImpuesto.subtract(montoExoneracion);
        } else {
            impuestoNeto = montoImpuesto;
        }
        
        // Asegurar que no sea negativo
        if (impuestoNeto.compareTo(BigDecimal.ZERO) < 0) {
            impuestoNeto = BigDecimal.ZERO;
        }
        
        // Validaciones según código
        validarSegunCodigo();
    }
    
    private void validarSegunCodigo() {
        // Si es IVA, debe tener código de tarifa
        if ("01".equals(codigoImpuesto) || "07".equals(codigoImpuesto)) {
            if (codigoTarifaIVA == null || codigoTarifaIVA.trim().isEmpty()) {
                throw new IllegalStateException(
                    "Código de tarifa IVA es obligatorio para impuestos 01 y 07"
                );
            }
        }
        
        // Si es código 99, validar descripción
        if ("99".equals(codigoImpuesto)) {
            if (codigoImpuestoOTRO == null || codigoImpuestoOTRO.trim().length() < 5) {
                throw new IllegalStateException(
                    "Descripción de otro impuesto debe tener mínimo 5 caracteres"
                );
            }
        }
        
        // Validar exoneración
        if (tieneExoneracion) {
            validarExoneracion();
        }
    }
    
    private void validarExoneracion() {
        if (tipoDocumentoExoneracion == null) {
            throw new IllegalStateException("Tipo de documento de exoneración es obligatorio");
        }
        
        if ("99".equals(tipoDocumentoExoneracion) && 
            (tipoDocumentoExoneracionOTRO == null || tipoDocumentoExoneracionOTRO.trim().length() < 5)) {
            throw new IllegalStateException(
                "Descripción de otro tipo de exoneración debe tener mínimo 5 caracteres"
            );
        }
        
        if (numeroDocumentoExoneracion == null || numeroDocumentoExoneracion.trim().length() < 3) {
            throw new IllegalStateException(
                "Número de documento de exoneración debe tener mínimo 3 caracteres"
            );
        }
        
        // Si es compra autorizada o ley, requiere artículo
        if ("02".equals(tipoDocumentoExoneracion) || "03".equals(tipoDocumentoExoneracion)) {
            if (articuloExoneracion == null || articuloExoneracion.trim().isEmpty()) {
                throw new IllegalStateException(
                    "Artículo es obligatorio para compras autorizadas y leyes"
                );
            }
        }
    }
    
    /**
     * Calcula el monto del impuesto basado en la base y tarifa
     */
    public void calcularMontoImpuesto(BigDecimal base) {
        BigDecimal baseCalculo = baseImponible != null ? baseImponible : base;
        
        if (factorCalculo != null) {
            // Para bienes usados u otros con factor
            montoImpuesto = baseCalculo.multiply(factorCalculo)
                .setScale(5, RoundingMode.HALF_UP);
        } else {
            // Cálculo normal con tarifa
            montoImpuesto = baseCalculo.multiply(tarifa)
                .divide(new BigDecimal("100"), 5, RoundingMode.HALF_UP);
        }
    }
    
    /**
     * Calcula el monto de exoneración
     */
    public void calcularMontoExoneracion(BigDecimal base) {
        if (tieneExoneracion && tarifaExonerada != null) {
            BigDecimal baseCalculo = baseImponible != null ? baseImponible : base;
            montoExoneracion = baseCalculo.multiply(tarifaExonerada)
                .divide(new BigDecimal("100"), 5, RoundingMode.HALF_UP);
                
            // No puede ser mayor al impuesto
            if (montoExoneracion.compareTo(montoImpuesto) > 0) {
                montoExoneracion = montoImpuesto;
            }
        }
    }
}