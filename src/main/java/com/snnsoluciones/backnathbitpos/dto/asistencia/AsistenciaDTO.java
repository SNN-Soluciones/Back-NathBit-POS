package com.snnsoluciones.backnathbitpos.dto.asistencia;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO para mostrar información de asistencia en listados
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsistenciaDTO {
    
    /**
     * ID del registro
     */
    private Long id;
    
    /**
     * Usuario
     */
    private Long usuarioId;
    private String usuarioNombre;
    private String usuarioNombreCompleto;
    
    /**
     * Fecha del registro
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha;
    
    /**
     * Hora de entrada
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime horaEntrada;
    
    /**
     * Hora de salida (null si aún no sale)
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime horaSalida;
    
    /**
     * Indica si tiene entrada activa
     */
    private Boolean tieneEntradaActiva;
    
    /**
     * Observaciones
     */
    private String observaciones;
    
    /**
     * Sucursal
     */
    private Long sucursalId;
    private String sucursalNombre;
}