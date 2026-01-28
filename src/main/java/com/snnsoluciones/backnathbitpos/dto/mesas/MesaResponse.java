// dto/mesas/MesaResponse.java
package com.snnsoluciones.backnathbitpos.dto.mesas;

import com.snnsoluciones.backnathbitpos.enums.EstadoMesa;
import com.snnsoluciones.backnathbitpos.enums.TipoFormaMesa;

public record MesaResponse(
    Long id,
    String codigo,
    String nombre,
    Integer capacidad,
    Integer orden,
    EstadoMesa estado,
    Boolean activa,
    Long zonaId,
    Long unionGroupId,
    TipoFormaMesa tipoForma, // ✅ NUEVO
    Long sucursalId // ✅ NUEVO
) {}