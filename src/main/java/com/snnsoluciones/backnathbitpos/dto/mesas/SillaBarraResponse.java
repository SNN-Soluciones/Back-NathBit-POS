// dto/mesas/SillaBarraResponse.java
package com.snnsoluciones.backnathbitpos.dto.mesas;

import com.snnsoluciones.backnathbitpos.enums.EstadoSillaBarra;

public record SillaBarraResponse(
    Long id,
    Integer numero,
    EstadoSillaBarra estado,
    Long ordenPersonaId,
    Long ordenId,
    Long barraId
) {}