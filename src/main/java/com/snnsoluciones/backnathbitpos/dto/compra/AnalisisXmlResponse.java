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
    private List<LineaAnalisis> lineas;
    private Integer plazoCredito;
    private String condicionVenta;

    private ResumenTotales resumenTotales;

    @Data
    public static class EmisorInfo {
        private String identificacion;
        private String tipoIdentificacion;
        private String nombre;
        private String razonSocial;
        private boolean existeEnSistema;
        private Long proveedorId;
        private String telefono;
        private String email;
        private String provincia;
        private String canton;
        private String distrito;
        private String otrasSenas;
    }

    @Data
    public static class ProductoNoEncontrado {
        private String codigo;
        private String descripcion;
        private String codigoCabys;
    }

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
        private String naturalezaDescuento; // AGREGAR
        private BigDecimal montoTotal;
        private BigDecimal montoImpuesto;
        private String codigoTarifaIVA; // AGREGAR
        private BigDecimal tarifaIVA; // AGREGAR
        private BigDecimal montoTotalLinea; // AGREGAR (montoTotal + montoImpuesto)
        private boolean existeEnSistema;
        private Long productoId;

        // AGREGAR: Información de exoneraciones si aplica
        private ExoneracionInfo exoneracion;
    }

    // NUEVA CLASE: Resumen de totales según estructura de Hacienda
    @Data
    public static class ResumenTotales {
        private BigDecimal totalGravado;
        private BigDecimal totalExento;
        private BigDecimal totalExonerado;
        private BigDecimal totalVenta;
        private BigDecimal totalDescuentos;
        private BigDecimal totalVentaNeta;
        private BigDecimal totalImpuesto;
        private BigDecimal totalIVADevuelto; // Para casos especiales
        private BigDecimal totalOtrosCargos;
        private BigDecimal totalComprobante;

        // Desglose de impuestos si necesario
        private List<ImpuestoDetalle> detalleImpuestos;
    }

    @Data
    public static class ImpuestoDetalle {
        private String codigo; // 01=IVA, 02=Selectivo consumo, etc
        private String codigoTarifa; // 01=0%, 02=1%, 04=2%, 08=4%, etc
        private BigDecimal tarifa;
        private BigDecimal monto;
        private BigDecimal montoExportacion;
    }

    @Data
    public static class ExoneracionInfo {
        private String tipoDocumento;
        private String numeroDocumento;
        private String nombreInstitucion;
        private LocalDateTime fechaEmision;
        private BigDecimal porcentajeExoneracion;
        private BigDecimal montoExoneracion;
    }
}