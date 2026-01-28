// dto/mesas/CrearMesaRequest.java
package com.snnsoluciones.backnathbitpos.dto.mesas;

import com.snnsoluciones.backnathbitpos.enums.TipoFormaMesa;
import jakarta.validation.constraints.*;

public record CrearMesaRequest(
    @NotBlank @Size(max = 20) String codigo,
    @Size(max = 60) String nombre,
    @Min(1) Integer capacidad,
    @PositiveOrZero Integer orden,
    TipoFormaMesa tipoForma // ✅ NUEVO - Opcional, default CUADRADA
) {}