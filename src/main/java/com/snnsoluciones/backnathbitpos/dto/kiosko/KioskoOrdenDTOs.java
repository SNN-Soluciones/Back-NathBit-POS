package com.snnsoluciones.backnathbitpos.dto.kiosko;
 
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
 
public class KioskoOrdenDTOs {
 
    // ── Request ────────────────────────────────────────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CrearOrdenKioskoRequest {
 
        // Datos del cliente (opcional)
        private String nombreCliente;
 
        @NotBlank(message = "El tipo de consumo es requerido")
        private String tipoConsumo; // AQUI | LLEVAR
 
        @NotEmpty(message = "La orden debe tener al menos un item")
        private List<ItemKioskoRequest> items;
 
        @NotBlank(message = "El método de pago es requerido")
        private String metodoPago; // CAJA | TARJETA | SINPE | EFECTIVO
 
        // Cupón o código de promoción (opcional)
        private String cupon;
    }
 
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ItemKioskoRequest {
 
        @NotNull(message = "El productoId es requerido")
        private Long productoId;
 
        @NotNull @Min(1)
        private Integer cantidad;
 
        private String notas;
 
        // Opciones para productos compuestos
        private List<OpcionKioskoRequest> opciones;
    }
 
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OpcionKioskoRequest {
        private Long slotId;
        private Long productoOpcionId;
    }
 
    // ── Response ───────────────────────────────────────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OrdenKioskoResponse {
        private Long ordenId;
        private String numeroOrden;    // ej: ORD-210426-042
        private int numeroDisplay;     // ej: 42 — el número que ve el cliente en pantalla
        private BigDecimal subtotal;
        private BigDecimal totalImpuesto;
        private BigDecimal total;
        private BigDecimal descuentoAplicado;
        private String estado;         // PENDIENTE_PAGO | EN_PREPARACION
        private String metodoPago;
        private String tipoConsumo;
        private String nombreCliente;
        private String tipoDocumento;  // ELECTRONICA | INTERNA | PENDIENTE (si paga en caja)
        private Integer tiempoEstimadoMinutos;
        private LocalDateTime creadaEn;
        private List<ItemOrdenKioskoResponse> items;
    }
 
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ItemOrdenKioskoResponse {
        private Long id;
        private String nombreProducto;
        private Integer cantidad;
        private BigDecimal precioUnitario;
        private BigDecimal total;
        private String notas;
    }
 
    // ── Response para cajero ───────────────────────────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OrdenPendientePagoResponse {
        private Long ordenId;
        private String numeroOrden;
        private int numeroDisplay;
        private String nombreCliente;
        private String tipoConsumo;     // AQUI | LLEVAR
        private BigDecimal total;
        private LocalDateTime creadaEn;
        private long minutosEspera;
        private String sucursalNombre;
        private List<ItemOrdenKioskoResponse> items;
    }
}