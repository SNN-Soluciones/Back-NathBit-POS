package com.snnsoluciones.backnathbitpos.mapper;

import com.snnsoluciones.backnathbitpos.dto.empresa.SucursalDTO;
import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

/**
 * Mapper para convertir entre Sucursal entity y SucursalDTO.
 * Usa MapStruct para generar las implementaciones automáticamente.
 */
@Mapper(componentModel = "spring")
public interface SucursalMapper {
    
    /**
     * Convierte de entidad Sucursal a SucursalDTO básico.
     * No incluye información de la empresa para evitar referencias circulares.
     * 
     * @param sucursal entidad a convertir
     * @return DTO con la información de la sucursal
     */
    @Mapping(source = "empresa.id", target = "empresaId")
    @Mapping(target = "empresaNombre", ignore = true)
    @Mapping(target = "cantidadUsuarios", ignore = true)
    SucursalDTO toDTO(Sucursal sucursal);
    
    /**
     * Convierte de entidad Sucursal a SucursalDTO incluyendo información de empresa.
     * Usar cuando se necesite mostrar el nombre de la empresa.
     * 
     * @param sucursal entidad con empresa cargada
     * @return DTO con información completa
     */
    @Mapping(source = "empresa.id", target = "empresaId")
    @Mapping(source = "empresa.nombre", target = "empresaNombre")
    @Mapping(target = "cantidadUsuarios", expression = "java(contarUsuariosActivos(sucursal))")
    @Named("toDTOWithEmpresa")
    SucursalDTO toDTOWithEmpresa(Sucursal sucursal);
    
    /**
     * Convierte de SucursalDTO a entidad Sucursal.
     * Ignora campos que no deben ser actualizados directamente.
     * 
     * @param sucursalDTO DTO con los datos
     * @return entidad Sucursal
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "empresa", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "configuracion", ignore = true)
    @Mapping(target = "provincia", ignore = true)
    @Mapping(target = "canton", ignore = true)
    @Mapping(target = "distrito", ignore = true)
    @Mapping(target = "barrio", ignore = true)
    @Mapping(target = "otrasSenas", ignore = true)
    @Mapping(target = "horaApertura", ignore = true)
    @Mapping(target = "horaCierre", ignore = true)
    Sucursal toEntity(SucursalDTO sucursalDTO);
    
    /**
     * Actualiza una entidad Sucursal existente con datos del DTO.
     * Solo actualiza campos permitidos.
     * 
     * @param sucursalDTO DTO con los nuevos datos
     * @param sucursal entidad a actualizar
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "empresa", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "configuracion", ignore = true)
    @Mapping(target = "activa", ignore = true)
    @Mapping(target = "esPrincipal", ignore = true)
    void updateEntityFromDTO(SucursalDTO sucursalDTO, @MappingTarget Sucursal sucursal);
}