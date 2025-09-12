package com.snnsoluciones.backnathbitpos.dto.facturainterna;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.util.List;

@Data
public class FacturaInternaRequest {
    
    @NotNull(message = "La empresa es requerida")
    private Long empresaId;
    
    @NotNull(message = "La sucursal es requerida")
    private Long sucursalId;
    
    private Long clienteId; // Opcional
    private String nombreCliente; // Si no hay cliente registrado
    
    @NotEmpty(message = "Debe incluir al menos un detalle")
    private List<DetalleRequest> detalles;
    
    private List<MedioPagoRequest> mediosPago;
    private List<OtroCargoRequest> otrosCargos;
    private List<DescuentoRequest> descuentos;
    
    private String notas;
    
    @Data
    public static class DetalleRequest {
        @NotNull private Long productoId;
        @NotNull private BigDecimal cantidad;
        @NotNull private BigDecimal precioUnitario;
        private BigDecimal porcentajeDescuento = BigDecimal.ZERO;
        private BigDecimal montoImpuestoServicio = BigDecimal.ZERO;
        private String notas;
    }
    
    @Data
    public static class MedioPagoRequest {
        @NotNull private String tipoPago; // MedioPago enum
        @NotNull private BigDecimal monto;
        private String referencia;
        private String banco;
        private String numeroAutorizacion;
    }
    
    @Data
    public static class OtroCargoRequest {
        @NotNull private String tipoCargo;
        @NotNull private String descripcion;
        private BigDecimal porcentaje;
        private BigDecimal monto;
        private Boolean aplicadoAutomaticamente = false;
    }
    
    @Data
    public static class DescuentoRequest {
        @NotNull private String tipoDescuento;
        @NotNull private String descripcion;
        private BigDecimal porcentaje;
        private BigDecimal monto;
        private String codigoPromocion;
    }
}