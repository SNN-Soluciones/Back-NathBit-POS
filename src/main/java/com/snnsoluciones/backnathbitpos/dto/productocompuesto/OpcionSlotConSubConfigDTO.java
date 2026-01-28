package com.snnsoluciones.backnathbitpos.dto.productocompuesto;

import com.snnsoluciones.backnathbitpos.dto.slots.OpcionSlotDTO;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class OpcionSlotConSubConfigDTO extends OpcionSlotDTO {
    
    private Boolean activaSubConfiguracion;
    private Long configuracionActivadaId;
    private String configuracionActivadaNombre;
    private Integer cantidadSlotsAdicionales;

    @Builder(builderMethodName = "extendedBuilder")
    public OpcionSlotConSubConfigDTO(
        Long opcionId,
        Long productoId,
        String nombre,
        String codigoInterno,
        String imagen,
        java.math.BigDecimal precioBase,
        java.math.BigDecimal precioAdicional,
        Boolean esGratuita,
        Boolean disponible,
        Boolean esDefault,
        Integer stockDisponible,
        Integer orden,
        String origen,
        Boolean activaSubConfiguracion,
        Long configuracionActivadaId,
        String configuracionActivadaNombre,
        Integer cantidadSlotsAdicionales) {
        
        super(opcionId, productoId, nombre, codigoInterno, imagen, 
              precioBase, precioAdicional, esGratuita, disponible, 
              esDefault, stockDisponible, orden, origen);
        
        this.activaSubConfiguracion = activaSubConfiguracion;
        this.configuracionActivadaId = configuracionActivadaId;
        this.configuracionActivadaNombre = configuracionActivadaNombre;
        this.cantidadSlotsAdicionales = cantidadSlotsAdicionales;
    }

    public boolean esOpcionTerminal() {
        return !Boolean.TRUE.equals(activaSubConfiguracion);
    }

    public boolean tieneSubConfiguracion() {
        return Boolean.TRUE.equals(activaSubConfiguracion) && 
               configuracionActivadaId != null;
    }
}