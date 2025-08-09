package com.snnsoluciones.backnathbitpos.mapper;

import com.snnsoluciones.backnathbitpos.dto.usuario.UsuarioEmpresaRolDTO;
import com.snnsoluciones.backnathbitpos.entity.UsuarioEmpresaRol;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper para convertir entre UsuarioEmpresaRol entity y DTO.
 * Usa MapStruct para generar la implementación automáticamente.
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UsuarioEmpresaRolMapper {

    /**
     * Convierte entity a DTO con mapeos personalizados
     */
    @Mappings({
        @Mapping(source = "usuario.id", target = "usuarioId"),
        @Mapping(source = "empresa.id", target = "empresaId"),
        @Mapping(source = "empresa.nombre", target = "empresaNombre"),
        @Mapping(source = "sucursal.id", target = "sucursalId"),
        @Mapping(source = "sucursal.nombre", target = "sucursalNombre"),
        @Mapping(target = "descripcionCompleta", expression = "java(entity.getDescripcionCompleta())")
    })
    UsuarioEmpresaRolDTO toDTO(UsuarioEmpresaRol entity);

    /**
     * Convierte lista de entities a DTOs
     */
    List<UsuarioEmpresaRolDTO> toDTOList(List<UsuarioEmpresaRol> entities);

    /**
     * Actualiza entity existente desde DTO (para updates parciales)
     */
    @Mappings({
        @Mapping(target = "id", ignore = true),
        @Mapping(target = "usuario", ignore = true),
        @Mapping(target = "empresa", ignore = true),
        @Mapping(target = "sucursal", ignore = true),
        @Mapping(target = "createdAt", ignore = true),
        @Mapping(target = "updatedAt", ignore = true),
        @Mapping(target = "fechaAsignacion", ignore = true)
    })
    void updateEntityFromDTO(UsuarioEmpresaRolDTO dto, @MappingTarget UsuarioEmpresaRol entity);

    /**
     * Método personalizado para mapear con contexto adicional
     */
    @AfterMapping
    default void enriquecerDTO(@MappingTarget UsuarioEmpresaRolDTO dto, UsuarioEmpresaRol entity) {
        // Verificar si tiene permisos personalizados
        if (entity.getPermisos() != null && !entity.getPermisos().isEmpty()) {
            dto.setPermisos(entity.getPermisos());
        } else {
            // Si no tiene permisos personalizados, usar los default del rol
            dto.setPermisos(UsuarioEmpresaRol.getPermisosDefault(entity.getRol()));
        }
        
        // Agregar información adicional si es necesario
        if (entity.getFechaVencimiento() != null && 
            entity.getFechaVencimiento().isBefore(java.time.LocalDateTime.now())) {
            // Marcar como vencido en la descripción
            dto.setDescripcionCompleta(dto.getDescripcionCompleta() + " (VENCIDO)");
        }
    }
}