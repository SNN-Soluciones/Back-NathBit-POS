// ClientePOSDto.java  (extiende el tuyo)
package com.snnsoluciones.backnathbitpos.dto.cliente;

import com.snnsoluciones.backnathbitpos.enums.mh.TipoIdentificacion;
import lombok.*;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientePOSDto implements Serializable {
    private Long id;
    private TipoIdentificacion tipoIdentificacion;
    private String numeroIdentificacion;
    private String razonSocial;
    private String emails; // Solo el primer email para el listado
    private String telefonoNumero;
    private Boolean tieneExoneracion;
    private Boolean activo;
    private Boolean inscritoHacienda;
    private Boolean permiteCredito;

    // NUEVO
    private boolean exonerado;
    private ExoneracionClienteDto exoneracion; // null si no aplica
}