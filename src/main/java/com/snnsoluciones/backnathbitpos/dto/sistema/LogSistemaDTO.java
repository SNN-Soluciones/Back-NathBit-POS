package com.snnsoluciones.backnathbitpos.dto.sistema;

import com.snnsoluciones.backnathbitpos.enums.TipoLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogSistemaDTO {
    private Long id;
    private TipoLog tipo;
    private String modulo;
    private String mensaje;
    private String usuario;
    private String detalles;
    private LocalDateTime fecha;
    private String stackTrace;
}