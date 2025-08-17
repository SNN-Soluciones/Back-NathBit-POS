package com.snnsoluciones.backnathbitpos.dto.cliente;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientePOSDto {
    private Long id;
    private String tipoIdentificacion;
    private String numeroIdentificacion;
    private String razonSocial;
    private String primerEmail;
    private String telefonoNumero;
    private boolean activo;
}