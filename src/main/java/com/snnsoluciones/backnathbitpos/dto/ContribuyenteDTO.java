package com.snnsoluciones.backnathbitpos.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContribuyenteDTO {

    private String fuente; // HACIENDA | GOMETA
    private String identificacion;
    private String tipoIdentificacion;
    private String nombre;

    private RegimenDTO regimen;
    private SituacionDTO situacion;
    private List<ActividadDTO> actividades;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegimenDTO {
        private Integer codigo;
        private String descripcion;
    }

    // SituacionDTO.java
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SituacionDTO {
        private String estado;
        private String moroso;
        private String omiso;
        private String administracionTributaria;
        private String mensaje;
    }

    // ActividadDTO.java
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActividadDTO {
        private String codigo;
        private String descripcion;
        private String estado;
        private String tipo;
    }
}
