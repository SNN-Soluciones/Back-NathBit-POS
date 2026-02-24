package com.snnsoluciones.backnathbitpos.dto.compuestoV2;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotV2Dto {
    private Long id;
    private String nombre;
    private String descripcion;
    private Boolean esRequerido;
    private Integer cantidadMinima;
    private Integer cantidadMaxima;
    private Boolean permiteCantidadPorOpcion;
    private Integer maxTiposDiferentes;
    private Integer orden;
    private Boolean usaFamilia;
    private Long familiaId;
    private String familiaNombre;
    private BigDecimal precioAdicionalPorOpcion;
    private List<OpcionV2Dto> opciones;
    private Map<Long, BigDecimal> preciosOverride;
}