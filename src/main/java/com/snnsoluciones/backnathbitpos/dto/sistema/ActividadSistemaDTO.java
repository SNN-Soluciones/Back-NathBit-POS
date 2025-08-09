package com.snnsoluciones.backnathbitpos.dto.sistema;

import com.snnsoluciones.backnathbitpos.enums.TipoActividad;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActividadSistemaDTO {
    private Long id;
    private TipoActividad tipo;
    private String descripcion;
    private String empresa;
    private String usuario;
    private String ipAddress;
    private LocalDateTime fecha;
    private String detalles;
}