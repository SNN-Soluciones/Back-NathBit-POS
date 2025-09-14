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
    
    @Data
    public static class EmisorInfo {
        private String identificacion;
        private String nombre;
        private boolean existeEnSistema;
        private Long proveedorId;
    }
    
    @Data
    public static class ProductoNoEncontrado {
        private String codigo;
        private String descripcion;
        private String codigoCabys;
    }
}