package com.snnsoluciones.backnathbitpos.dto.sistema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoMantenimientoDTO {
    private String tarea;
    private Boolean exitoso;
    private LocalDateTime iniciadoEn;
    private LocalDateTime completadoEn;
    private Long duracionMs;
    private Map<String, Object> resultados;
    private String mensajeError;
}