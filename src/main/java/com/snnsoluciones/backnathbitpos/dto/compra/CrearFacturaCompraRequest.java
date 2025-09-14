package com.snnsoluciones.backnathbitpos.dto.compra;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

// DTO para crear FEC manual
@Data
public class CrearFacturaCompraRequest {
    private Long proveedorId;
    private LocalDateTime fechaEmision;
    private String numeroFacturaProveedor; // Número de factura física
    private String condicionVenta; // 01=Contado, 02=Crédito
    private Integer plazoCredito; // En días si es crédito
    private String medioPago; // 01=Efectivo, 02=Tarjeta, etc.
    private String moneda;
    private BigDecimal tipoCambio;
    private String observaciones;
    private List<DetalleCompraRequest> detalles;
    
    @Data
    public static class DetalleCompraRequest {
        private Long productoId; // Opcional, puede ser null
        private String codigo;
        private String codigoCabys;
        private String descripcion;
        private Boolean esServicio;
        private BigDecimal cantidad;
        private String unidadMedida;
        private BigDecimal precioUnitario;
        private BigDecimal montoDescuento;
        private String naturalezaDescuento;
        private String codigoTarifaIVA; // 01=0%, 02=1%, 03=2%, 04=4%, 08=13%
        private Boolean aplicaInventario = true;
    }
}
