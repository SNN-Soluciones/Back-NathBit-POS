package com.snnsoluciones.backnathbitpos.dto.documento;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerarClaveRequest {
    
    @NotBlank(message = "El consecutivo es requerido")
    @Pattern(regexp = "^[0-9]{20}$", message = "El consecutivo debe tener 20 dígitos")
    private String consecutivo;
    
    @NotNull(message = "La fecha de emisión es requerida")
    private LocalDateTime fechaEmision;
    
    @NotBlank(message = "La identificación del emisor es requerida")
    @Pattern(regexp = "^[0-9]{9,12}$", message = "Identificación inválida")
    private String identificacionEmisor;
    
    @NotNull(message = "El tipo de identificación es requerido")
    @Min(1) @Max(4)
    private Integer tipoIdentificacion; // 1=Física, 2=Jurídica, 3=DIMEX, 4=NITE
    
    @NotNull(message = "La situación es requerida")
    @Min(1) @Max(3)
    private Integer situacion; // 1=Normal, 2=Contingencia, 3=Sin Internet
    
    // Para generar el código de seguridad
    @NotNull(message = "El ID del documento es requerido")
    private Long documentoId;
}