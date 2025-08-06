package com.snnsoluciones.backnathbitpos.mapper;

import com.snnsoluciones.backnathbitpos.dto.usuario.UsuarioDTO;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Mapper para convertir entre Usuario entity y UsuarioDTO.
 * Usa MapStruct para generar las implementaciones automáticamente.
 */
@Mapper(componentModel = "spring")
public interface UsuarioMapper {
    
    /**
     * Convierte de entidad Usuario a UsuarioDTO.
     * 
     * @param usuario entidad a convertir
     * @return DTO con la información del usuario
     */
    @Mapping(target = "password", ignore = true) // Nunca incluir password en el DTO
    UsuarioDTO toDTO(Usuario usuario);
    
    /**
     * Convierte de UsuarioDTO a entidad Usuario.
     * Ignora campos que no deben ser actualizados directamente.
     * 
     * @param usuarioDTO DTO con los datos
     * @return entidad Usuario
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "usuarioEmpresaRoles", ignore = true)
    @Mapping(target = "ultimoAcceso", ignore = true)
    @Mapping(target = "intentosFallidos", ignore = true)
    @Mapping(target = "bloqueadoHasta", ignore = true)
    Usuario toEntity(UsuarioDTO usuarioDTO);
    
    /**
     * Actualiza una entidad Usuario existente con datos del DTO.
     * Solo actualiza campos permitidos.
     * 
     * @param usuarioDTO DTO con los nuevos datos
     * @param usuario entidad a actualizar
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "usuarioEmpresaRoles", ignore = true)
    @Mapping(target = "ultimoAcceso", ignore = true)
    @Mapping(target = "intentosFallidos", ignore = true)
    @Mapping(target = "bloqueadoHasta", ignore = true)
    @Mapping(target = "activo", ignore = true)
    void updateEntityFromDTO(UsuarioDTO usuarioDTO, @MappingTarget Usuario usuario);
}