package com.snnsoluciones.backnathbitpos.dto.orden;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request para procesar pago parcial de una orden
 * Permite pagar items específicos y generar factura (electrónica o interna)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoParcialRequest {

    /**
     * IDs de los items a pagar
     */
    @NotEmpty(message = "Debe seleccionar al menos un item para pagar")
    private List<Long> itemIds;

    /**
     * Tipo de documento a generar:
     * - TI: Tiquete Interno
     * - FI: Factura Interna  
     * - TE: Tiquete Electrónico
     * - FE: Factura Electrónica
     */
    @NotNull(message = "Debe especificar el tipo de documento")
    private String tipoDocumento;

    /**
     * ID de la sucursal (requerido)
     */
    @NotNull(message = "Debe especificar la sucursal")
    private Long sucursalId;

    /**
     * ID del terminal (requerido para documentos electrónicos)
     */
    private Long terminalId;

    /**
     * ID del cliente (opcional, requerido para FE)
     */
    private Long clienteId;

    /**
     * Nombre del cliente (para documentos internos sin cliente registrado)
     */
    private String nombreCliente;

    /**
     * Medios de pago utilizados
     */
    @NotEmpty(message = "Debe especificar al menos un medio de pago")
    private List<MedioPagoItemRequest> mediosPago;

    /**
     * Monto recibido (para calcular vuelto en efectivo)
     */
    private BigDecimal montoRecibido;

    /**
     * Notas u observaciones para la factura
     */
    private String notas;

    /**
     * Número viper (opcional)
     */
    private String numeroViper;

    // ===== INNER CLASS: Medio de Pago =====
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedioPagoItemRequest {
        
        /**
         * Tipo: EFECTIVO, TARJETA, SINPE, TRANSFERENCIA
         */
        @NotNull(message = "Debe especificar el tipo de medio de pago")
        private String tipo;

        /**
         * Monto pagado con este medio
         */
        @NotNull(message = "Debe especificar el monto")
        private BigDecimal monto;

        /**
         * Referencia (voucher, número SINPE, etc.)
         */
        private String referencia;

        /**
         * Banco (para tarjetas o transferencias)
         */
        private String banco;
    }
}