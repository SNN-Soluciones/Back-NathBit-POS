package com.snnsoluciones.backnathbitpos.dto.facturarecepcion;

import com.snnsoluciones.backnathbitpos.enums.factura.EstadoFacturaRecepcion;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class FacturaRecepcionResponse {
    
    private Long id;
    private String clave;
    private TipoDocumento tipoDocumento;
    private String numeroConsecutivo;
    private LocalDateTime fechaEmision;
    private EstadoFacturaRecepcion estadoInterno;
    
    // Proveedor
    private Long proveedorId;
    private String proveedorNombre;
    private String proveedorIdentificacion;
    private String proveedorCorreo;
    
    // Receptor (nosotros)
    private String receptorNombre;
    private String receptorIdentificacion;
    private boolean duplicada;

    // Condiciones
    private String condicionVenta;
    private Integer plazoCredito;
    
    // Totales
    private BigDecimal totalGravado;
    private BigDecimal totalExento;
    private BigDecimal totalExonerado;
    private BigDecimal totalVenta;
    private BigDecimal totalDescuentos;
    private BigDecimal totalVentaNeta;
    private BigDecimal totalImpuesto;
    private BigDecimal totalOtrosCargos;
    private BigDecimal totalComprobante;

    private Boolean convertidaCompra;
    
    // Estado procesamiento
    private Boolean mensajeReceptorEnviado;
    private LocalDateTime fechaMensajeReceptor;
    private String tipoMensajeReceptor;
    
    private Boolean convertidaACompra;
    private Long compraId;
    private LocalDateTime fechaConversion;
    
    // Archivos
    private String rutaXmlS3;
    private String rutaPdfS3;
    
    // Detalles
    private List<FacturaRecepcionDetalleResponse> detalles;

    /**
     * Verifica si la factura fue convertida a compra
     */
    public boolean isConvertidaACompra() {
        return convertidaCompra != null && convertidaCompra;
    }
}