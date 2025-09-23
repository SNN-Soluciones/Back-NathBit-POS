package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.Data;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class CalculoPrecioRequest {
    @NotEmpty(message = "Debe seleccionar al menos una opción")
    private List<Long> opcionesSeleccionadas; // IDs de ProductoCompuestoOpcion
}