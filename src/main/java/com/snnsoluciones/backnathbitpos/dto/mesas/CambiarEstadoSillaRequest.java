// dto/mesas/CambiarEstadoSillaRequest.java
package com.snnsoluciones.backnathbitpos.dto.mesas;

import com.snnsoluciones.backnathbitpos.enums.EstadoSillaBarra;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoSillaRequest(
    @NotNull EstadoSillaBarra nuevoEstado,
    Long ordenId,
    Long ordenPersonaId
) {}