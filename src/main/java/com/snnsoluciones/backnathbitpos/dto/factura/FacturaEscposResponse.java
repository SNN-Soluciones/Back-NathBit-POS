package com.snnsoluciones.backnathbitpos.dto.factura;// package com.snnsoluciones.backnathbitpos.dto.factura;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class FacturaEscposResponse {

    private EmisorTicketDto emisor;
    private DocumentoTicketDto documento;
    private ReceptorTicketDto receptor;

    private List<DetalleTicketDto> detalles;

    private TotalesTicketDto totales;

    private PagosTicketDto pagos;

    private List<ExoneracionTicketDto> exoneraciones; // opcional
    private List<OtroCargoTicketDto> otrosCargos;     // opcional

    private String qrData; // normalmente la clave o payload QR

    // 👉 campos “de cortesía” para el ticket
    private String mesa;
    private String cajero;
    private String terminal;

    // --- inner DTOs (pueden ser clases separadas si querés) ---

    @Data
    @Builder
    public static class EmisorTicketDto {
        private String nombreComercial;
        private String identificacionFormateada;
        private String telefono;
        private String email;
        private String direccionCorta;
    }

    @Data
    @Builder
    public static class DocumentoTicketDto {
        private String tipoDocumentoNombre;
        private String consecutivo;
        private String clave;
        private String fechaEmisionFormateada;
    }

    @Data
    @Builder
    public static class ReceptorTicketDto {
        private String nombre;
        private String identificacion;
        private String telefono;
        private String email;
    }

    @Data
    @Builder
    public static class DetalleTicketDto {
        private Integer numeroLinea;
        private BigDecimal cantidad;
        private String descripcion;
        private String descripcionPersonalizada;
        private BigDecimal precioUnitario;
        private BigDecimal subtotal;
        private BigDecimal montoImpuesto;
        private BigDecimal montoTotalLinea;
    }

    @Data
    @Builder
    public static class TotalesTicketDto {
        private BigDecimal totalVentaNeta;
        private BigDecimal totalImpuesto;
        private BigDecimal totalComprobante;
        private BigDecimal totalDescuentos;
        private BigDecimal totalOtrosCargos;
    }

    @Data
    @Builder
    public static class MedioPagoTicketDto {
        private String medioPagoCodigo;
        private String medioPagoNombre;
        private BigDecimal monto;
        private String referencia;
        private String banco;
    }

    @Data
    @Builder
    public static class PagosTicketDto {
        private String mediosPagoConcatenados;
        private List<MedioPagoTicketDto> mediosPago;
        private BigDecimal pagoCon; // si lo manejás
        private BigDecimal vuelto;
    }

    // Opcionales (pueden ser súper simples)
    @Data
    @Builder
    public static class ExoneracionTicketDto {
        private String tipoDocumento;
        private String numeroDocumento;
        private String institucion;
        private BigDecimal porcentajeExonerado;
        private BigDecimal montoExonerado;
    }

    @Data
    @Builder
    public static class OtroCargoTicketDto {
        private String nombreCargo;
        private BigDecimal montoCargo;
        private BigDecimal porcentaje;
    }
}