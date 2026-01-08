package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * DTO para representar un impuesto asociado a un producto.
 * Usado tanto para crear/actualizar impuestos como para retornar información.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImpuestoDto {

    /**
     * Tipo de impuesto según Hacienda.
     * Valores posibles: IVA, EXENTO, etc.
     * Ver enum: TipoImpuesto
     */
    @NotBlank(message = "El tipo de impuesto es requerido")
    private String tipoImpuesto;

    /**
     * Código de tarifa según catálogo de Hacienda.
     * Valores posibles: TARIFA_0, TARIFA_REDUCIDA_1, TARIFA_REDUCIDA_2, TARIFA_REDUCIDA_4, 
     *                   TARIFA_GENERAL_13, TARIFA_GENERAL_TRANSITORIA_13
     * Ver enum: CodigoTarifaIVA
     */
    @NotBlank(message = "El código de tarifa es requerido")
    private String codigoTarifa;

    /**
     * Porcentaje de la tarifa.
     * Ejemplo: 13.00 para IVA al 13%
     */
    @NotNull(message = "La tarifa es requerida")
    @DecimalMin(value = "0.00", message = "La tarifa no puede ser negativa")
    @DecimalMax(value = "100.00", message = "La tarifa no puede exceder 100%")
    private BigDecimal tarifa;

    /**
     * Monto del impuesto calculado.
     * Este campo se calcula automáticamente en el backend.
     */
    private BigDecimal monto;

    /**
     * Factor de IVA para cálculos.
     * Ejemplo: 1.13 para IVA al 13%
     */
    private BigDecimal factorIVA;

    /**
     * Indica si el impuesto tiene exoneración.
     * Default: false
     */
    @Builder.Default
    private Boolean exoneracion = false;

    /**
     * Información de la exoneración (si aplica).
     * Incluye: número de documento, nombre de institución, fecha, porcentaje, monto.
     */
    private ExoneracionDto exoneracionInfo;

    /**
     * DTO anidado para información de exoneración
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExoneracionDto {
        
        /**
         * Tipo de documento de exoneración.
         * Ejemplo: "01" = Compras autorizadas
         */
        private String tipoDocumento;

        /**
         * Número del documento de exoneración.
         */
        private String numeroDocumento;

        /**
         * Nombre de la institución que emitió la exoneración.
         */
        private String nombreInstitucion;

        /**
         * Fecha de emisión del documento de exoneración.
         * Formato: yyyy-MM-dd
         */
        private String fechaEmision;

        /**
         * Porcentaje de exoneración aplicado.
         * Ejemplo: 100.00 para exoneración total
         */
        @DecimalMin(value = "0.00")
        @DecimalMax(value = "100.00")
        private BigDecimal porcentajeExoneracion;

        /**
         * Monto exonerado.
         */
        @DecimalMin(value = "0.00")
        private BigDecimal montoExoneracion;
    }
}