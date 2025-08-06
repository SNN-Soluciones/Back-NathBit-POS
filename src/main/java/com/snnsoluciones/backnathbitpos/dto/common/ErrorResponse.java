package com.snnsoluciones.backnathbitpos.dto.common;

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
public class ErrorResponse {
    private String mensaje;
    private String codigo;
    private Integer status;
    private LocalDateTime timestamp;
    private String path;
    private Map<String, String> errores;
}
