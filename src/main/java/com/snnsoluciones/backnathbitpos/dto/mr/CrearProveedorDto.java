package com.snnsoluciones.backnathbitpos.dto.mr;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CrearProveedorDto {

    @NotBlank(message = "El tipo de identificación es requerido")
    private String tipoIdentificacion;

    @NotBlank(message = "El número de identificación es requerido")
    private String numeroIdentificacion;

    @NotBlank(message = "El nombre comercial es requerido")
    private String nombreComercial;

    private String razonSocial;

    @Email(message = "El email debe ser válido")
    private String email;

    private String telefono;

    private String direccion;

    private Integer diasCredito;

    private String contactoNombre;

    private String contactoTelefono;

    private String notas;
}