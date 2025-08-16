package com.snnsoluciones.backnathbitpos.dto.cliente;

import com.snnsoluciones.backnathbitpos.enums.mh.TipoIdentificacion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteDTO {
    private Long id;
    private Long empresaId;
    private String empresaNombre;
    private TipoIdentificacion tipoIdentificacion;
    private String numeroIdentificacion;
    private String razonSocial;
    private List<String> emails; // Lista parseada
    private String telefonoCompleto; // Formato: +506 88888888
    private Boolean permiteCredito;
    private Boolean tieneExoneracion;
    private String observaciones;
    private Boolean activo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Datos de ubicación si existe
    private ClienteUbicacionDTO ubicacion;
    
    // Resumen de exoneraciones
    private Integer exoneracionesActivas;
    private Boolean tieneExoneracionVigente;
}