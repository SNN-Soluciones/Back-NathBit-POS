package com.snnsoluciones.backnathbitpos.dto.compra;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class CompraDetalleDto {
    private Long id;
    private Integer numeroLinea;
    private Long productoId;
    private String productoNombre;
    private String codigo;
    private String codigoCabys;
    private String descripcion;
    private Boolean esServicio;
    private BigDecimal cantidad;
    private String unidadMedida;
    private BigDecimal precioUnitario;
    private BigDecimal montoTotal;
    private BigDecimal montoDescuento;
    private BigDecimal subTotal;
    private String codigoTarifaIVA;
    private BigDecimal tarifaIVA;
    private BigDecimal montoImpuesto;
    private BigDecimal montoTotalLinea;
}
