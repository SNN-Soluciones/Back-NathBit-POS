package com.snnsoluciones.backnathbitpos.dto.factura;

import com.snnsoluciones.backnathbitpos.enums.mh.Moneda;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO completo para crear factura con todos los elementos
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearFacturaRequest {

    /**
     * Cliente - Opcional para Tiquete Electrónico
     */
    private Long clienteId;

    @NotNull(message = "El tipo de documento es requerido")
    private TipoDocumento tipoDocumento;

    @NotBlank(message = "La condición de venta es requerida")
    @Pattern(regexp = "^(01|02|03|04|05|06|07|08|09|99)$",
        message = "Condición de venta no válida")
    private String condicionVenta;

    /**
     * Plazo en días para condición crédito
     */
    @Min(value = 0, message = "El plazo no puede ser negativo")
    private Integer plazoCredito;

    @NotNull(message = "La terminal es requerida")
    private Long terminalId;

    @NotNull(message = "La sesión de caja es requerida")
    private Long sesionCajaId;

    @NotNull(message = "El usuario de caja es requerida")
    private Long usuarioId;

    // ========== MONEDA ==========
    @NotNull(message = "La moneda es requerida")
    @Builder.Default
    private Moneda moneda = Moneda.CRC;

    @NotNull(message = "El tipo de cambio es requerido")
    @DecimalMin(value = "0.0001", message = "El tipo de cambio debe ser mayor a 0")
    @Digits(integer = 10, fraction = 4, message = "Formato de tipo cambio inválido")
    @Builder.Default
    private BigDecimal tipoCambio = BigDecimal.ONE;

    // ========== DETALLES ==========
    @NotEmpty(message = "Debe incluir al menos un detalle")
    @Valid
    @Builder.Default
    private List<DetalleFacturaRequest> detalles = new ArrayList<>();

    // ========== OTROS CARGOS ==========
    @Valid
    @Size(max = 15, message = "Máximo 15 otros cargos permitidos")
    @Builder.Default
    private List<OtroCargoRequest> otrosCargos = new ArrayList<>();

    // ========== DESCUENTO GLOBAL ==========
    @DecimalMin(value = "0.00", message = "El porcentaje no puede ser negativo")
    @DecimalMax(value = "100.00", message = "El porcentaje no puede exceder 100%")
    @Digits(integer = 3, fraction = 2, message = "Formato de porcentaje inválido")
    private BigDecimal descuentoGlobalPorcentaje;

    @DecimalMin(value = "0.00", message = "El monto no puede ser negativo")
    @Digits(integer = 13, fraction = 5, message = "Formato de monto inválido")
    private BigDecimal montoDescuentoGlobal;

    @Size(max = 200, message = "El motivo no puede exceder 200 caracteres")
    private String motivoDescuentoGlobal;

    // ========== MEDIOS DE PAGO ==========
    @NotEmpty(message = "Debe incluir al menos un medio de pago")
    @Valid
    @Builder.Default
    private List<MedioPagoRequest> mediosPago = new ArrayList<>();

    /**
     * Observaciones generales
     */
    @Size(max = 500, message = "Las observaciones no pueden exceder 500 caracteres")
    private String observaciones;

    /**
     * Situación del comprobante
     * 1 = Normal, 2 = Contingencia, 3 = Sin Internet
     */
    @Pattern(regexp = "^[123]$", message = "Situación inválida")
    @Builder.Default
    private String situacionComprobante = "1";

    /**
     * Validaciones adicionales
     */
    public boolean isValid() {
        // Si es Factura Electrónica, cliente es obligatorio
        if (tipoDocumento == TipoDocumento.FACTURA_ELECTRONICA && clienteId == null) {
            return false;
        }

        // Si condición es crédito, debe tener plazo
        if ("02".equals(condicionVenta) && (plazoCredito == null || plazoCredito <= 0)) {
            return false;
        }

        // Validar que si hay descuento global, tenga porcentaje O monto
        if (descuentoGlobalPorcentaje == null && montoDescuentoGlobal == null) {
            // Está bien, no hay descuento global
        } else if (descuentoGlobalPorcentaje != null && montoDescuentoGlobal != null) {
            // No puede tener ambos
            return false;
        }

        // Validar otros cargos
        for (OtroCargoRequest oc : otrosCargos) {
            if (!oc.isValid()) {
                return false;
            }
        }

        // Validar descuentos en detalles
        for (DetalleFacturaRequest det : detalles) {
            for (DescuentoRequest desc : det.getDescuentos()) {
                if (!desc.isValid()) {
                    return false;
                }
            }
        }

        return true;
    }
}