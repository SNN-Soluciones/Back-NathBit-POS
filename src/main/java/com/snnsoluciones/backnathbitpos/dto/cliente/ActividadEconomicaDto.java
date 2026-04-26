package com.snnsoluciones.backnathbitpos.dto.cliente;

import lombok.*;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActividadEconomicaDto implements Serializable {
    private String codigo;
    private String descripcion;
    private String tipo;   // "P" = Principal, "S" = Secundaria
    private String estado; // "A" = Activa, "I" = Inactiva
}