// dto/mesas/BarraResponse.java
package com.snnsoluciones.backnathbitpos.dto.mesas;

import com.snnsoluciones.backnathbitpos.enums.TipoFormaBarra;

import java.time.OffsetDateTime;
import java.util.List;

public record BarraResponse(
    Long id,
    String codigo,
    String nombre,
    TipoFormaBarra tipoForma,
    Integer cantidadSillas,
    Integer orden,
    Boolean activa,
    Long zonaId,
    Long sucursalId,
    Long sillasDisponibles,
    Long sillasOcupadas,
    List<SillaBarraResponse> sillas,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}