package com.snnsoluciones.backnathbitpos.dto.bitacora;

import com.snnsoluciones.backnathbitpos.enums.mh.EstadoBitacora;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Registro de cambios de estado en la bitácora
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BitacoraHistorialDto {
    private LocalDateTime fecha;
    private EstadoBitacora estadoAnterior;
    private EstadoBitacora estadoNuevo;
    private String mensaje;
    private String detalleError;
    private Integer intento;
}