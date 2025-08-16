package com.snnsoluciones.backnathbitpos.dto.cliente;

import com.snnsoluciones.backnathbitpos.enums.mh.TipoIdentificacion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteListDTO {
    private Long id;
    private TipoIdentificacion tipoIdentificacion;
    private String numeroIdentificacion;
    private String razonSocial;
    private String primerEmail; // Solo el primer email para el listado
    private String telefonoCompleto;
    private Boolean tieneExoneracion;
    private Boolean activo;
    private Boolean inscritoHacienda;
    private Boolean permiteCredito;
}