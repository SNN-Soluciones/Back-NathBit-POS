package com.snnsoluciones.backnathbitpos.mapper;

import com.snnsoluciones.backnathbitpos.dto.empresa.EmpresaDTO;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

/**
 * Mapper para convertir entre Empresa entity y EmpresaDTO.
 * Usa MapStruct para generar las implementaciones automáticamente.
 */
@Mapper(componentModel = "spring", uses = {SucursalMapper.class})
public interface EmpresaMapper {
    
    /**
     * Convierte de entidad Empresa a EmpresaDTO básico.
     * No incluye las sucursales para evitar cargas innecesarias.
     * 
     * @param empresa entidad a convertir
     * @return DTO con la información de la empresa
     */
    @Mapping(target = "cantidadSucursales", expression = "java(empresa.getCantidadSucursalesActivas())")
    @Mapping(target = "cantidadUsuarios", expression = "java(empresa.getCantidadUsuariosActivos())")
    @Mapping(source = "planSuscripcion", target = "plan")
    EmpresaDTO toDTO(Empresa empresa);
    
    /**
     * Convierte de entidad Empresa a EmpresaDTO incluyendo sucursales.
     * Usar con cuidado para evitar N+1 queries.
     * 
     * @param empresa entidad con sucursales cargadas
     * @return DTO con información completa incluyendo sucursales
     */
    @Mapping(target = "cantidadSucursales", expression = "java(empresa.getCantidadSucursalesActivas())")
    @Mapping(target = "cantidadUsuarios", expression = "java(empresa.getCantidadUsuariosActivos())")
    @Mapping(source = "planSuscripcion", target = "plan")
    @Named("toDTOWithSucursales")
    EmpresaDTO toDTOWithSucursales(Empresa empresa);
    
    /**
     * Convierte de EmpresaDTO a entidad Empresa.
     * Ignora campos que no deben ser actualizados directamente.
     * 
     * @param empresaDTO DTO con los datos
     * @return entidad Empresa
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "sucursales", ignore = true)
    @Mapping(target = "configuracion", ignore = true)
    @Mapping(target = "limiteUsuarios", ignore = true)
    @Mapping(target = "limiteSucursales", ignore = true)
    @Mapping(source = "plan", target = "planSuscripcion")
    Empresa toEntity(EmpresaDTO empresaDTO);
    
    /**
     * Actualiza una entidad Empresa existente con datos del DTO.
     * Solo actualiza campos permitidos.
     * 
     * @param empresaDTO DTO con los nuevos datos
     * @param empresa entidad a actualizar
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "sucursales", ignore = true)
    @Mapping(target = "configuracion", ignore = true)
    @Mapping(target = "activa", ignore = true)
    @Mapping(target = "planSuscripcion", ignore = true)
    @Mapping(target = "limiteUsuarios", ignore = true)
    @Mapping(target = "limiteSucursales", ignore = true)
    void updateEntityFromDTO(EmpresaDTO empresaDTO, @MappingTarget Empresa empresa);
}