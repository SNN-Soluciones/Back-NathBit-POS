package com.snnsoluciones.backnathbitpos.dto.empresa;

import com.snnsoluciones.backnathbitpos.dto.confighacienda.ConfigHaciendaResponse;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CrearEmpresaCompletaResponse {
    private Long empresaId;
    private String nombre;
    private String nombreComercial;
    private String identificacion;
    private String logoUrl;
    private Boolean requiereHacienda;
    private ConfigHaciendaResponse configHacienda;
    private String mensaje;
}