package com.snnsoluciones.backnathbitpos.dto.empresa;

import lombok.Data;

@Data
public class EstadisticasSucursalDTO {
    private Long sucursalId;
    private String nombreSucursal;
    private Long empresaId;
    private String nombreEmpresa;
    private Long usuariosActivos;
    private Double ventasDelDia;
    private Long ordenesActivas;
    private Long mesasOcupadas;
    private Long mesasTotales;
}