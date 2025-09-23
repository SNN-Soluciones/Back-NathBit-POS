package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.Data;
import java.util.List;

@Data
public class ValidarSeleccionRequest {
    private List<Long> opcionesSeleccionadas;
}