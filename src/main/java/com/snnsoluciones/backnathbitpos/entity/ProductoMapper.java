package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.dto.producto.ProductoCreateDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants.ComponentModel;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = ComponentModel.SPRING)
public interface ProductoMapper {

  ProductoCreateDto toDto(Producto producto);
}