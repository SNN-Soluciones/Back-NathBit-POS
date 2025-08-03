package com.snnsoluciones.backnathbitpos.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

/**
 * Respuesta estándar para errores en la API.
 * Proporciona información consistente sobre los errores.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta estándar de error")
public class ErrorResponse {

    @Schema(description = "Timestamp del error", example = "2024-08-02T10:15:30")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    @Schema(description = "Código de estado HTTP", example = "404")
    private int status;

    @Schema(description = "Descripción del error", example = "Not Found")
    private String error;

    @Schema(description = "Mensaje detallado del error", example = "Usuario no encontrado con ID: 123")
    private String message;

    @Schema(description = "Código interno del error", example = "RESOURCE_NOT_FOUND")
    private String code;

    @Schema(description = "Path donde ocurrió el error", example = "/api/usuarios/123")
    private String path;

    @Schema(description = "Detalles adicionales del error")
    private Object details;

    /**
     * Constructor simplificado para errores básicos
     */
    public ErrorResponse(HttpStatus status, String message) {
        this.timestamp = LocalDateTime.now();
        this.status = status.value();
        this.error = status.getReasonPhrase();
        this.message = message;
    }

    /**
     * Constructor con código de error
     */
    public ErrorResponse(HttpStatus status, String message, String code) {
        this.timestamp = LocalDateTime.now();
        this.status = status.value();
        this.error = status.getReasonPhrase();
        this.message = message;
        this.code = code;
    }
}