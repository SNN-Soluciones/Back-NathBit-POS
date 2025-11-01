package com.snnsoluciones.backnathbitpos.dto.impresion;

import com.snnsoluciones.backnathbitpos.entity.ImpresoraAndroid;
import com.snnsoluciones.backnathbitpos.entity.ImpresoraAndroid.TipoImpresoraAndroid;
import com.snnsoluciones.backnathbitpos.entity.ImpresoraAndroid.TipoUsoImpresora;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para crear/actualizar impresora Android
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImpresoraAndroidDTO {

    private Long id;

    @NotNull(message = "La sucursal es obligatoria")
    private Long sucursalId;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String nombre;

    @NotNull(message = "El tipo es obligatorio")
    private ImpresoraAndroid.TipoImpresoraAndroid tipo;

    @NotBlank(message = "La IP es obligatoria")
    @Pattern(regexp = "^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$|^[a-zA-Z0-9.-]+$", 
             message = "IP inválida. Debe ser una dirección IP o hostname válido")
    private String ip;

    @NotNull(message = "El puerto es obligatorio")
    @Min(value = 1, message = "El puerto debe ser mayor a 0")
    @Max(value = 65535, message = "El puerto debe ser menor a 65536")
    private Integer puerto;

    @NotNull(message = "El ancho de papel es obligatorio")
    private Integer anchoPapel;

    private TipoUsoImpresora tipoUso;

    private Boolean predeterminada;

    private Boolean activa;

    // Campos de auditoría (solo lectura)
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private String usuarioCreacion;
    private String usuarioActualizacion;
}

/**
 * DTO de respuesta con información completa
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
class ImpresoraAndroidResponseDTO {

    private Long id;
    private Long sucursalId;
    private String sucursalNombre;
    private String nombre;
    private TipoImpresoraAndroid tipo;
    private String ip;
    private Integer puerto;
    private Integer anchoPapel;
    private TipoUsoImpresora tipoUso;
    private Boolean predeterminada;
    private Boolean activa;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private String usuarioCreacion;
    private String usuarioActualizacion;
}

/**
 * DTO simplificado para el frontend Angular (compatible con ImpresoraConfig)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
class ImpresoraAndroidSimpleDTO {

    private Long id;
    private String nombre;
    private String tipo; // "RED" o "WIFI"
    private String ip;
    private Integer puerto;
    private Integer ancho; // Para compatibilidad con Angular (58 o 80)
    private Boolean activa;
    private Boolean predeterminada;
    private String tipoUso;
}