package com.snnsoluciones.backnathbitpos.dto.asistencia;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response al marcar entrada o salida exitosamente
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarcarAsistenciaResponse {
    
    /**
     * ID del registro de asistencia
     */
    private Long id;
    
    /**
     * Tipo de marcación realizada: "ENTRADA" o "SALIDA"
     */
    private String tipo;
    
    /**
     * Hora en que se marcó (entrada o salida según tipo)
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime hora;
    
    /**
     * Fecha del registro
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha;
    
    /**
     * Hora de entrada (para referencia)
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime horaEntrada;
    
    /**
     * Hora de salida (solo si se marcó salida)
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime horaSalida;
}