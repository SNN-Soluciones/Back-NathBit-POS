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
    @Mapping(target = "nombreCompleto", expression = "java(usuario.getNombreCompleto())")
    @Mapping(target = "empresaActual", ignore = true) // Se setea manualmente cuando se necesita
    @Mapping(target = "sucursalActual", ignore = true) // Se setea manualmente cuando se necesita
    UsuarioDTO toDto(Usuario usuario);

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
    @Mapping(target = "fechaUltimoAcceso", ignore = true)
    @Mapping(target = "intentosFallidos", ignore = true)
    @Mapping(target = "fechaBloqueo", ignore = true)
    @Mapping(target = "bloqueado", ignore = true)
    @Mapping(target = "imagenUrl", ignore = true)
    Usuario toEntity(UsuarioDTO usuarioDTO);

    /**
     * Actualiza una entidad Usuario existente con datos del DTO.
     * Solo actualiza campos permitidos para edición.
     *
     * @param usuarioDTO DTO con los nuevos datos
     * @param usuario entidad a actualizar
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true) // Email no debe cambiar en updates normales
    @Mapping(target = "username", ignore = true) // Username no debe cambiar
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "rol", ignore = true) // Rol se cambia con endpoint específico
    @Mapping(target = "tipoUsuario", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "usuarioEmpresaRoles", ignore = true)
    @Mapping(target = "fechaUltimoAcceso", ignore = true)
    @Mapping(target = "intentosFallidos", ignore = true)
    @Mapping(target = "fechaBloqueo", ignore = true)
    @Mapping(target = "bloqueado", ignore = true)
    @Mapping(target = "activo", ignore = true) // Se cambia con endpoint específico
    void updateEntityFromDto(UsuarioDTO usuarioDTO, @MappingTarget Usuario usuario);

    /**
     * Actualiza solo datos del perfil personal.
     * Permite al usuario actualizar su información básica.
     *
     * @param usuarioDTO DTO con los nuevos datos
     * @param usuario entidad a actualizar
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "rol", ignore = true)
    @Mapping(target = "tipoUsuario", ignore = true)
    @Mapping(target = "identificacion", ignore = true) // Identificación no debe cambiar
    @Mapping(target = "tipoIdentificacion", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "usuarioEmpresaRoles", ignore = true)
    @Mapping(target = "fechaUltimoAcceso", ignore = true)
    @Mapping(target = "intentosFallidos", ignore = true)
    @Mapping(target = "fechaBloqueo", ignore = true)
    @Mapping(target = "bloqueado", ignore = true)
    @Mapping(target = "activo", ignore = true)
    void updatePerfilFromDto(UsuarioDTO usuarioDTO, @MappingTarget Usuario usuario);
}