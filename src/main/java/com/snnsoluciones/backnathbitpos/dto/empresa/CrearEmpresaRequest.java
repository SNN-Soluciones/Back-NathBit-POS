package com.snnsoluciones.backnathbitpos.dto.empresa;

import com.snnsoluciones.backnathbitpos.enums.TipoEmpresa;
import com.snnsoluciones.backnathbitpos.enums.PlanSuscripcion;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CrearEmpresaRequest {
    @NotBlank(message = "El código es requerido")
    @Size(min = 3, max = 10, message = "El código debe tener entre 3 y 10 caracteres")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "El código solo puede contener letras mayúsculas y números")
    private String codigo;
    
    @NotBlank(message = "El nombre es requerido")
    private String nombre;
    
    @NotBlank(message = "El nombre comercial es requerido")
    private String nombreComercial;
    
    @NotBlank(message = "La cédula jurídica es requerida")
    private String cedulaJuridica;
    
    @NotBlank(message = "El teléfono es requerido")
    private String telefono;
    
    @NotBlank(message = "El email es requerido")
    @Email(message = "El email debe ser válido")
    private String email;
    
    @NotBlank(message = "La dirección es requerida")
    private String direccion;
    
    private TipoEmpresa tipo = TipoEmpresa.RESTAURANTE;
    private PlanSuscripcion plan = PlanSuscripcion.BASICO;
}