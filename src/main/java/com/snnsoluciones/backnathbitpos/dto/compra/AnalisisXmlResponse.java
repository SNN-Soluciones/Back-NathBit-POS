package com.snnsoluciones.backnathbitpos.dto.compra;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

// DTO para análisis de XML antes de procesar
@Data
public class AnalisisXmlResponse {
    private boolean esValido;
    private String tipoDocumento;
    private String numeroDocumento;
    private String clave;
    private LocalDateTime fechaEmision;
    private EmisorInfo emisor;
    private BigDecimal totalComprobante;
    private String moneda;
    private Integer cantidadLineas;
    private List<String> erroresValidacion;
    private List<ProductoNoEncontrado> productosNoEncontrados;
    private List<LineaAnalisis> lineas; // AGREGAR ESTA LÍNEA
    private Integer plazoCredito; // AGREGAR PLAZO CRÉDITO
    private String condicionVenta; // AGREGAR CONDICIÓN DE VENTA

    @Data
    public static class EmisorInfo {
        private String identificacion;
        private String tipoIdentificacion; // Agregar
        private String nombre;
        private String razonSocial; // Agregar
        private boolean existeEnSistema;
        private Long proveedorId;
        private String telefono; // Agregar
        private String email; // Agregar
        private String provincia; // Agregar
        private String canton; // Agregar
        private String distrito; // Agregar
        private String otrasSenas; // Agregar
    }

    @Data
    public static class ProductoNoEncontrado {
        private String codigo;
        private String descripcion;
        private String codigoCabys;
    }

    // AGREGAR ESTA CLASE INTERNA
    @Data
    public static class LineaAnalisis {
        private Integer numeroLinea;
        private String codigo;
        private String codigoCabys;
        private BigDecimal cantidad;
        private String unidadMedida;
        private String descripcion;
        private BigDecimal precioUnitario;
        private BigDecimal montoDescuento;
        private BigDecimal montoTotal;
        private BigDecimal montoImpuesto;
        private boolean existeEnSistema;
        private Long productoId;
    }
}
