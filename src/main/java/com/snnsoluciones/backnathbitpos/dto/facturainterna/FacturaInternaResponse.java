package com.snnsoluciones.backnathbitpos.dto.facturainterna;

import com.snnsoluciones.backnathbitpos.dto.factura.FacturaResponse.MedioPagoDto;
import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class FacturaInternaResponse {
    private Long id;
    private String numeroFactura;
    private String estado;
    private LocalDateTime fechaEmision;
    
    // Cliente
    private Long clienteId;
    private String nombreCliente;
    
    // Totales
    private BigDecimal subtotal;
    private BigDecimal totalDescuentos;
    private BigDecimal totalOtrosCargos;
    private BigDecimal totalVenta;
    
    // Detalles
    private List<DetalleResponse> detalles;
    private List<MedioPagoResponse> mediosPago;
    private List<OtroCargoResponse> otrosCargos;
    private List<DescuentoResponse> descuentos;
    
    // Info adicional
    private String empresaNombre;
    private String sucursalNombre;
    private String cajeroNombre;
    private String notas;
    
    @Data
    @Builder
    public static class DetalleResponse {
        private Long id;
        private Integer numeroLinea;
        private String codigoProducto;
        private String descripcion;
        private BigDecimal cantidad;
        private String unidadMedida;
        private BigDecimal precioUnitario;
        private BigDecimal montoDescuento;
        private BigDecimal subtotal;
        private BigDecimal montoImpuestoServicio;
        private BigDecimal montoTotalLinea;
    }
    
    // Similar para otros responses...
}