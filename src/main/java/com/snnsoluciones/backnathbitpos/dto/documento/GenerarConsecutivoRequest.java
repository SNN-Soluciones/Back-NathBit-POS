package com.snnsoluciones.backnathbitpos.dto.documento;

import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerarConsecutivoRequest {
    
    @NotNull(message = "El tipo de documento es requerido")
    private TipoDocumento tipoDocumento;
    
    @NotNull(message = "La terminal es requerida")
    private Long terminalId;
    
    // Para casos de contingencia o sin internet
    private Integer situacion = 1; // 1=Normal, 2=Contingencia, 3=Sin Internet
}