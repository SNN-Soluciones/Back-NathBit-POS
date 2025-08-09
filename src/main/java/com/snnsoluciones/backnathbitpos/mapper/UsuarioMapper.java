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
    @Mapping(target = "requiereSeleccionContexto", expression = "java(usuario.requiereSeleccionContexto())")
    @Mapping(target = "esRolSistema", expression = "java(usuario.esRolSistema())")
    @Mapping(target = "esRolAdministrativo", expression = "java(usuario.esRolAdministrativo())")
    @Mapping(target = "esRolOperativo", expression = "java(usuario.esRolOperativo())")
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
    @Mapping(target = "usuarioEmpresas", ignore = true) // Cambio: usuarioEmpresaRoles → usuarioEmpresas
    @Mapping(target = "ultimoAcceso", ignore = true)
    @Mapping(target = "intentosFallidos", ignore = true)
    @Mapping(target = "fechaUltimoIntento", ignore = true)
    @Mapping(target = "fechaDesbloqueo", ignore = true)
    @Mapping(target = "ultimoCambioPassword", ignore = true)
    @Mapping(target = "tokenRecuperacion", ignore = true)
    @Mapping(target = "fechaTokenRecuperacion", ignore = true)
    @Mapping(target = "bloqueado", ignore = true)
    Usuario toEntity(UsuarioDTO usuarioDTO);

    /**
     * Actualiza una entidad Usuario existente con datos del DTO.
     * Solo actualiza campos permitidos para edición.
     *
     * @param usuarioDTO DTO con los nuevos datos
     * @param usuario entidad a actualizar
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true) // Email no debe cambiar
    @Mapping(target = "username", ignore = true) // Username no debe cambiar
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "rol", ignore = true) // Rol se cambia con endpoint específico
    @Mapping(target = "tipoUsuario", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "usuarioEmpresas", ignore = true)
    @Mapping(target = "ultimoAcceso", ignore = true)
    @Mapping(target = "intentosFallidos", ignore = true)
    @Mapping(target = "fechaUltimoIntento", ignore = true)
    @Mapping(target = "fechaDesbloqueo", ignore = true)
    @Mapping(target = "bloqueado", ignore = true)
    @Mapping(target = "activo", ignore = true) // Se cambia con endpoint específico
    @Mapping(target = "ultimoCambioPassword", ignore = true)
    @Mapping(target = "passwordTemporal", ignore = true)
    @Mapping(target = "tokenRecuperacion", ignore = true)
    @Mapping(target = "fechaTokenRecuperacion", ignore = true)
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
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "usuarioEmpresas", ignore = true)
    @Mapping(target = "ultimoAcceso", ignore = true)
    @Mapping(target = "intentosFallidos", ignore = true)
    @Mapping(target = "fechaUltimoIntento", ignore = true)
    @Mapping(target = "fechaDesbloqueo", ignore = true)
    @Mapping(target = "bloqueado", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "ultimoCambioPassword", ignore = true)
    @Mapping(target = "passwordTemporal", ignore = true)
    @Mapping(target = "tokenRecuperacion", ignore = true)
    @Mapping(target = "fechaTokenRecuperacion", ignore = true)
    void updatePerfilFromDto(UsuarioDTO usuarioDTO, @MappingTarget Usuario usuario);
}