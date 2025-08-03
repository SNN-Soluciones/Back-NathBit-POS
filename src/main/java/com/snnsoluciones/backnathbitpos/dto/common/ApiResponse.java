package com.snnsoluciones.backnathbitpos.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Clase genérica para respuestas de la API
 * @param <T> Tipo de datos a devolver
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta estándar de la API")
public class ApiResponse<T> {

  @Schema(description = "Indica si la operación fue exitosa", example = "true")
  private boolean success;

  @Schema(description = "Mensaje descriptivo del resultado", example = "Operación exitosa")
  private String message;

  @Schema(description = "Datos de la respuesta")
  private T data;

  @Schema(description = "Código de error (solo en caso de fallo)", example = "USER_NOT_FOUND")
  private String errorCode;

  @Schema(description = "Detalles del error (solo en caso de fallo)")
  private Object errorDetails;

  @Schema(description = "Timestamp de la respuesta")
  @Builder.Default
  private LocalDateTime timestamp = LocalDateTime.now();

  /**
   * Método factory para crear una respuesta exitosa
   */
  public static <T> ApiResponse<T> success(String message, T data) {
    return ApiResponse.<T>builder()
        .success(true)
        .message(message)
        .data(data)
        .build();
  }

  /**
   * Método factory para crear una respuesta exitosa sin datos
   */
  public static <T> ApiResponse<T> success(String message) {
    return ApiResponse.<T>builder()
        .success(true)
        .message(message)
        .build();
  }

  /**
   * Método factory para crear una respuesta de error
   */
  public static <T> ApiResponse<T> error(String message, String errorCode) {
    return ApiResponse.<T>builder()
        .success(false)
        .message(message)
        .errorCode(errorCode)
        .build();
  }

  /**
   * Método factory para crear una respuesta de error con detalles
   */
  public static <T> ApiResponse<T> error(String message, String errorCode, Object errorDetails) {
    return ApiResponse.<T>builder()
        .success(false)
        .message(message)
        .errorCode(errorCode)
        .errorDetails(errorDetails)
        .build();
  }
}