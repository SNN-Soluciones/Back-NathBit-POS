package com.snnsoluciones.backnathbitpos.enums;

import jakarta.validation.constraints.NotNull;

public record TrasladarOrdenRequest(

    @NotNull(message = "La mesa destino es obligatoria")
    Long mesaDestinoId
) {}