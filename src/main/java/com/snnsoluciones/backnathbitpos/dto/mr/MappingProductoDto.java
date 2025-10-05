package com.snnsoluciones.backnathbitpos.dto.mr;

import lombok.Data;

@Data
public class MappingProductoDto {
    private String codigoProveedor;
    private Long productoId;
    private CrearProductoDto crearProducto;
}