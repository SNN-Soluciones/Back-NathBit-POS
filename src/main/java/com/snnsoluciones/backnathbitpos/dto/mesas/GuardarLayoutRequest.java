package com.snnsoluciones.backnathbitpos.dto.mesas;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GuardarLayoutRequest {
    private List<MesaLayoutDTO> mesas;
}
