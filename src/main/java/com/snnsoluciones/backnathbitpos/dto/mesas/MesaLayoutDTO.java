// dto/mesas/MesaLayoutDTO.java
package com.snnsoluciones.backnathbitpos.dto.mesas;

import com.snnsoluciones.backnathbitpos.enums.TipoFormaBarra;
import com.snnsoluciones.backnathbitpos.enums.TipoFormaMesa;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesaLayoutDTO {
    // ========== IDENTIFICACIÓN ==========
    private Long mesaId;
    private Long barraId;
    private String tipoElemento; // "MESA" o "BARRA"

    // ========== 2D LAYOUT (EXISTENTE) ==========
    private Double x;
    private Double y;
    private Double width;
    private Double height;
    private Double rotation;

    // ========== FORMAS Y CONFIGURACIÓN ==========
    private TipoFormaMesa tipoFormaMesa;
    private TipoFormaBarra tipoFormaBarra;
    private Integer cantidadSillas;
    private Integer capacidad;
    private List<SillaLayoutDTO> sillas;

    // ========== 3D LAYOUT (NUEVO) ==========

    /**
     * Indica si este layout es 3D (true) o 2D (false/null)
     * null = 2D por defecto (backward compatible)
     */
    private Boolean modo3D;

    /**
     * Posición X en el espacio 3D
     */
    private Double positionX;

    /**
     * Posición Z en el espacio 3D
     */
    private Double positionZ;

    /**
     * Rotación en eje Y (radianes)
     */
    private Double rotationY;

    /**
     * Escala del modelo 3D (1.0 = 100%)
     */
    private Double scale;

    /**
     * Estado visual 3D: "libre" o "ocupada"
     */
    private String estado3D;
}