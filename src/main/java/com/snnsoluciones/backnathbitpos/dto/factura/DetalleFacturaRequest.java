package com.snnsoluciones.backnathbitpos.dto.factura;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleFacturaRequest {
    
    @NotNull
    @Min(value = 1)
    private Integer numeroLinea;
    
    @NotNull(message = "Producto es requerido")
    private Long productoId;
    
    @NotNull
    @DecimalMin(value = "0.001", message = "Cantidad debe ser mayor a 0")
    private BigDecimal cantidad;
    
    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal precioUnitario;
    
    private String descripcionPersonalizada;
    
    @NotNull
    private Boolean esServicio;
    
    @NotNull
    private Boolean aplicaImpuestoServicio;
    
    // Montos calculados por el frontend
    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal montoTotal;
    
    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal montoDescuento;
    
    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal subtotal;
    
    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal montoImpuesto;
    
    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal montoTotalLinea;

    @NotBlank(message = "Unidad de medida es requerida")
    @Pattern(regexp = "^[0-9A-Z._-]{1,8}$", message = "Unidad de medida inválida")
    private String unidadMedida;

    @NotBlank(message = "Código CAByS es requerido")
    @Pattern(regexp = "^[0-9]{13}$", message = "CAByS debe tener 13 dígitos")
    private String codigoCabys;
    
    // Listas de componentes
    @Valid
    private List<DescuentoRequest> descuentos;
    
    @Valid
    private List<ImpuestoLineaRequest> impuestos;
}