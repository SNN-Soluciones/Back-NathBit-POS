package com.snnsoluciones.backnathbitpos.dto.cliente;

import com.snnsoluciones.backnathbitpos.enums.TipoIdentificacion;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteUpdateDTO {
    
    @NotNull(message = "El tipo de identificación es obligatorio")
    private TipoIdentificacion tipoIdentificacion;
    
    @NotBlank(message = "El número de identificación es obligatorio")
    @Size(max = 20, message = "El número de identificación no puede exceder 20 caracteres")
    private String numeroIdentificacion;
    
    @NotBlank(message = "La razón social es obligatoria")
    @Size(min = 3, max = 100, message = "La razón social debe tener entre 3 y 100 caracteres")
    private String razonSocial;
    
    @NotBlank(message = "Debe proporcionar al menos un email")
    private String emails;
    
    @Size(min = 1, max = 3, message = "El código de país debe tener entre 1 y 3 dígitos")
    @Pattern(regexp = "^\\d*$", message = "El código de país debe contener solo números")
    private String telefonoCodigoPais;
    
    @Size(min = 8, max = 20, message = "El número de teléfono debe tener entre 8 y 20 dígitos")
    @Pattern(regexp = "^\\d*$", message = "El número de teléfono debe contener solo números")
    private String telefonoNumero;
    
    private Boolean permiteCredito;
    
    @Size(max = 500, message = "Las observaciones no pueden exceder 500 caracteres")
    private String observaciones;
}