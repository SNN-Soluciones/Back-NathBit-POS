package com.snnsoluciones.backnathbitpos.dto.mr;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class MensajeReceptorBitacoraDto {
    private Long id;
    private String claveHacienda;
    private String consecutivoMr;
    private String tipoMensaje;
    private String tipoMensajeDescripcion;
    private String estado;
    private String estadoHacienda;
    private String mensajeHacienda;
    private Integer intentos;
    private LocalDateTime createdAt;
    private LocalDateTime fechaEnvio;
    private LocalDateTime proximoIntento;
    private String ultimoError;
    private Long compraId;
    private boolean tieneMensajeGenerado;
}