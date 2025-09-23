package com.snnsoluciones.backnathbitpos.mappers;

import com.snnsoluciones.backnathbitpos.dto.producto.ProductoProveedorDto;
import com.snnsoluciones.backnathbitpos.entity.ProductoCodigoProveedor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductoProveedorMapper {
    
    @Mapping(target = "productoId", source = "producto.id")
    @Mapping(target = "productoNombre", source = "producto.nombre")
    @Mapping(target = "productoCodigoInterno", source = "producto.codigoInterno")
    @Mapping(target = "proveedorId", source = "proveedor.id")
    @Mapping(target = "proveedorNombre", source = "proveedor.nombreComercial")
    @Mapping(target = "proveedorIdentificacion", source = "proveedor.numeroIdentificacion")
    @Mapping(target = "codigoProveedor", source = "codigo")
    ProductoProveedorDto toDto(ProductoCodigoProveedor entity);
}