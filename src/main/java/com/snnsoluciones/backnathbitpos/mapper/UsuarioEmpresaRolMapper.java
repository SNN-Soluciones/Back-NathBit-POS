package com.snnsoluciones.backnathbitpos.mapper;

import com.snnsoluciones.backnathbitpos.dto.usuario.UsuarioEmpresaRolDTO;
import com.snnsoluciones.backnathbitpos.entity.UsuarioEmpresaRol;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.AfterMapping;
import org.mapstruct.MappingTarget;

/**
 * Mapper para convertir entre UsuarioEmpresaRol entity y UsuarioEmpresaRolDTO.
 * Usa MapStruct para generar las implementaciones automáticamente.
 */
@Mapper(componentModel = "spring")
public interface UsuarioEmpresaRolMapper {
    
    /**
     * Convierte de entidad UsuarioEmpresaRol a UsuarioEmpresaRolDTO.
     * 
     * @param usuarioEmpresaRol entidad a convertir
     * @return DTO con la información del rol asignado
     */
    @Mapping(source = "usuario.id", target = "usuarioId")
    @Mapping(source = "empresa.id", target = "empresaId")
    @Mapping(source = "empresa.nombre", target = "empresaNombre")
    @Mapping(source = "sucursal.id", target = "sucursalId")
    @Mapping(source = "sucursal.nombre", target = "sucursalNombre")
    @Mapping(target = "descripcionCompleta", ignore = true)
    UsuarioEmpresaRolDTO toDTO(UsuarioEmpresaRol usuarioEmpresaRol);
    
    /**
     * Convierte de UsuarioEmpresaRolDTO a entidad UsuarioEmpresaRol.
     * Ignora las relaciones que deben ser establecidas manualmente.
     * 
     * @param dto DTO con los datos
     * @return entidad UsuarioEmpresaRol
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "empresa", ignore = true)
    @Mapping(target = "sucursal", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "asignadoPor", ignore = true)
    @Mapping(target = "notas", ignore = true)
    UsuarioEmpresaRol toEntity(UsuarioEmpresaRolDTO dto);
    
    /**
     * Actualiza una entidad UsuarioEmpresaRol existente con datos del DTO.
     * Solo actualiza campos permitidos.
     * 
     * @param dto DTO con los nuevos datos
     * @param usuarioEmpresaRol entidad a actualizar
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "empresa", ignore = true)
    @Mapping(target = "sucursal", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "fechaAsignacion", ignore = true)
    @Mapping(target = "asignadoPor", ignore = true)
    void updateEntityFromDTO(UsuarioEmpresaRolDTO dto, @MappingTarget UsuarioEmpresaRol usuarioEmpresaRol);
    
    /**
     * Completa la descripción después del mapeo.
     * 
     * @param usuarioEmpresaRol entidad origen
     * @param dto DTO destino
     */
    @AfterMapping
    default void completarDescripcion(UsuarioEmpresaRol usuarioEmpresaRol, @MappingTarget UsuarioEmpresaRolDTO dto) {
        if (dto != null) {
            StringBuilder descripcion = new StringBuilder();
            descripcion.append(dto.getRol().name());
            descripcion.append(" en ");
            descripcion.append(dto.getEmpresaNombre());
            
            if (dto.getSucursalNombre() != null) {
                descripcion.append(" - ");
                descripcion.append(dto.getSucursalNombre());
            } else {
                descripcion.append(" (Todas las sucursales)");
            }
            
            dto.setDescripcionCompleta(descripcion.toString());
        }
    }
}