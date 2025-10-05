package com.snnsoluciones.backnathbitpos.dto.mr;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MensajeReceptorEstadoDto {
    private Long id;
    private String claveHacienda;
    private String consecutivoMr;
    private String tipoMensaje; // "05", "06", "07"
    private String tipoMensajeDescripcion; // "Aceptación", "Aceptación Parcial", "Rechazo"
    private String estado; // PENDIENTE, ENVIADO, PROCESADO, ERROR
    private String estadoHacienda;
    private String mensajeHacienda;
    private Integer intentos;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaEnvio;
    private LocalDateTime proximoIntento;
    private String ultimoError;
    private Long compraId;
    private boolean tieneMensajeGenerado;
}