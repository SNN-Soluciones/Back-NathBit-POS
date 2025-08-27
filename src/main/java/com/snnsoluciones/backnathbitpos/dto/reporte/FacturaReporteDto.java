package com.snnsoluciones.backnathbitpos.dto.reporte;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * DTO principal para generar el reporte de factura 80mm
 */
@Data
@Builder
public class FacturaReporteDto {
    // Identificación
    private String clave;
    private String consecutivo;
    private String tipoDocumento;
    private String fechaEmision;
    private String numeroInterno;
    
    // Datos del emisor
    private String emisorNombre;
    private String emisorNombreComercial;
    private String emisorIdentificacion;
    private String emisorTelefono;
    private String emisorCorreo;
    private String emisorDireccion;
    private byte[] logoEmpresa;
    private boolean tieneLogo;
    
    // Datos del receptor
    private String receptorNombre;
    private String receptorIdentificacion;
    private String receptorTelefono;
    private String receptorCorreo;
    private String receptorDireccion;
    private String clienteContado; // Para clientes de contado
    
    // Condiciones de venta
    private String condicionVenta;
    private String medioPago;
    private String plazoCredito;
    private String tipoCambio;
    private String moneda;
    private String simboloMoneda;
    
    // Totales (como String para evitar problemas de formato)
    private String totalVenta;
    private String totalDescuentos;
    private String totalVentaNeta;
    private String totalImpuesto;
    private String totalOtrosCargos;
    private String totalExonerado;
    private String totalComprobante;
    
    // Pagos (para facturas de contado)
    private String efectivo;
    private String tarjeta;
    private String cheque;
    private String transferencia;
    private String suVuelto;
    
    // Otros
    private byte[] qrImage;
    private String observaciones;
    private String mensajeHacienda;
    private String vendedor;
    private String resolucion;
    
    // Listas para subreportes
    private List<DetalleFacturaReporteDto> detalles;
    private List<OtroCargoReporteDto> otrosCargos;
    private List<ExoneracionReporteDto> exoneraciones;
}

/**
 * DTO para las líneas de detalle
 */
@Data
@Builder
class DetalleFacturaReporteDto {
    private Integer numeroLinea;
    private String codigo;
    private String descripcion;
    private String unidadMedida;
    private String cantidad;
    private String precioUnitario;
    private String montoTotal;
    private String montoDescuento;
    private String subtotal;
    private String montoImpuesto;
    private String montoTotalLinea;
}

/**
 * DTO para otros cargos
 */
@Data
@Builder
class OtroCargoReporteDto {
    private String tipoDocumento;
    private String numeroIdentidadTercero;
    private String nombreTercero;
    private String detalle;
    private String porcentaje;
    private String montoCargo;
}

/**
 * DTO para exoneraciones
 */
@Data
@Builder
class ExoneracionReporteDto {
    private String tipoExoneracion;
    private String numeroDocumento;
    private String nombreInstitucion;
    private String fechaEmision;
    private String porcentajeExoneracion;
    private String montoExonerado;
}