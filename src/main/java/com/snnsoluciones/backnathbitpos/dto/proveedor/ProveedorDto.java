// ========== ProveedorDto.java ==========
package com.snnsoluciones.backnathbitpos.dto.proveedor;

import com.snnsoluciones.backnathbitpos.enums.mh.TipoIdentificacion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProveedorDto {
    private Long id;
    private Long empresaId;
    private String empresaNombre;
    private TipoIdentificacion tipoIdentificacion;
    private String numeroIdentificacion;
    private String nombreComercial;
    private String razonSocial;
    private String telefono;
    private String email;
    private String direccion;
    private Integer diasCredito;
    private String contactoNombre;
    private String contactoTelefono;
    private String notas;
    private Boolean activo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}