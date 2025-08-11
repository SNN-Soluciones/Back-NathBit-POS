package com.snnsoluciones.backnathbitpos.dto.cliente;

import com.snnsoluciones.backnathbitpos.enums.TipoDocumentoExoneracion;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteExoneracionCreateDTO {
    
    @NotNull(message = "El tipo de documento es obligatorio")
    private TipoDocumentoExoneracion tipoDocumento;
    
    @NotBlank(message = "El número de documento es obligatorio")
    @Size(max = 50, message = "El número de documento no puede exceder 50 caracteres")
    private String numeroDocumento;
    
    @NotBlank(message = "El nombre de la institución es obligatorio")
    @Size(max = 100, message = "El nombre de la institución no puede exceder 100 caracteres")
    private String nombreInstitucion;
    
    @NotNull(message = "La fecha de emisión es obligatoria")
    @PastOrPresent(message = "La fecha de emisión no puede ser futura")
    private LocalDate fechaEmision;
    
    @Future(message = "La fecha de vencimiento debe ser futura")
    private LocalDate fechaVencimiento;
    
    @NotNull(message = "El porcentaje de exoneración es obligatorio")
    @DecimalMin(value = "0.00", message = "El porcentaje debe ser mayor o igual a 0")
    @DecimalMax(value = "100.00", message = "El porcentaje no puede exceder 100")
    private BigDecimal porcentajeExoneracion;
    
    @Size(max = 100, message = "La categoría de compra no puede exceder 100 caracteres")
    private String categoriaCompra;
    
    @DecimalMin(value = "0.00", message = "El monto máximo debe ser mayor o igual a 0")
    private BigDecimal montoMaximo;
    
    @Size(max = 500, message = "Las observaciones no pueden exceder 500 caracteres")
    private String observaciones;
}