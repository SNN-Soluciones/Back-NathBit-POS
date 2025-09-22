// ========== ProveedorRequest.java ==========
package com.snnsoluciones.backnathbitpos.dto.proveedor;

import com.snnsoluciones.backnathbitpos.enums.mh.TipoIdentificacion;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProveedorRequest {
    
    @NotNull(message = "La empresa es requerida")
    private Long empresaId;

    private Long sucursalId;
    
    @NotNull(message = "El tipo de identificación es requerido")
    private TipoIdentificacion tipoIdentificacion;
    
    @NotBlank(message = "El número de identificación es requerido")
    @Size(max = 20, message = "El número de identificación no puede exceder 20 caracteres")
    private String numeroIdentificacion;
    
    @NotBlank(message = "El nombre comercial es requerido")
    @Size(max = 200, message = "El nombre comercial no puede exceder 200 caracteres")
    private String nombreComercial;
    
    @Size(max = 200, message = "La razón social no puede exceder 200 caracteres")
    private String razonSocial;
    
    @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    private String telefono;
    
    @Email(message = "El email debe ser válido")
    @Size(max = 100, message = "El email no puede exceder 100 caracteres")
    private String email;
    
    private String direccion;
    
    @Min(value = 0, message = "Los días de crédito no pueden ser negativos")
    @Max(value = 365, message = "Los días de crédito no pueden exceder 365")
    private Integer diasCredito;
    
    @Size(max = 100, message = "El nombre del contacto no puede exceder 100 caracteres")
    private String contactoNombre;
    
    @Size(max = 20, message = "El teléfono del contacto no puede exceder 20 caracteres")
    private String contactoTelefono;
    
    private String notas;
}