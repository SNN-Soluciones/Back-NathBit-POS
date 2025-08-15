package com.snnsoluciones.backnathbitpos.mapper;

import com.snnsoluciones.backnathbitpos.config.MapStructConfig;
import com.snnsoluciones.backnathbitpos.dto.usuarios.UsuarioListadoResponse;
import com.snnsoluciones.backnathbitpos.entity.UsuarioRegistro;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.Collections;
import java.util.List;

@Mapper(config = MapStructConfig.class)
public interface UsuarioRegistroMapper {

    @Mapping(target = "id",            source = "id")
    @Mapping(target = "email",         source = "usuario.email")
    @Mapping(target = "nombre",        source = "usuario.nombre")
    @Mapping(target = "apellidos",     source = "usuario.apellidos")
    @Mapping(target = "rol",           source = "usuario.rol")
    @Mapping(target = "activo",        source = "usuario.activo")
    @Mapping(target = "creadoPor",     expression = "java(formatUsuario(entity.getCreadoPor()))")
    @Mapping(target = "fechaCreacion", source = "fechaCreacion")

    // empresas / sucursales vienen de relaciones del usuario; las cargamos en @AfterMapping
    @Mapping(target = "empresas",   ignore = true)
    @Mapping(target = "sucursales", ignore = true)
    UsuarioListadoResponse toUsuarioListadoResponse(UsuarioRegistro entity);

    // ----- Helpers -----

    /** Devuelve "Nombre Apellidos" o null si el usuario es null. */
    default String formatUsuario(Usuario u) {
        if (u == null) return null;
        String n = u.getNombre();
        String a = u.getApellidos();
        if ((n == null || n.isBlank()) && (a == null || a.isBlank())) return null;
        return (n == null ? "" : n.trim()) + (a == null ? "" : " " + a.trim());
    }

    /** Obtiene nombres de empresas asignadas al usuario (ajusta rutas según tu modelo real). */
    default List<String> mapEmpresas(UsuarioRegistro entity) {
        if (entity == null || entity.getUsuario() == null) return Collections.emptyList();
        // Ejemplos de rutas comunes (descomenta la que aplique y elimina el resto):
        // return entity.getUsuario().getUsuarioEmpresas().stream()
        //         .map(ue -> ue.getEmpresa().getNombre())
        //         .filter(Objects::nonNull)
        //         .collect(Collectors.toList());

        // Si aún no tienes esa relación, deja lista vacía:
        return Collections.emptyList();
    }

    /** Obtiene nombres de sucursales asignadas al usuario (ajusta rutas según tu modelo real). */
    default List<String> mapSucursales(UsuarioRegistro entity) {
        if (entity == null || entity.getUsuario() == null) return Collections.emptyList();
        // return entity.getUsuario().getUsuarioSucursales().stream()
        //         .map(us -> us.getSucursal().getNombre())
        //         .filter(Objects::nonNull)
        //         .collect(Collectors.toList());

        return Collections.emptyList();
    }

    /** Cargamos empresas/sucursales después del mapeo principal. */
    @AfterMapping
    default void fillAsignaciones(UsuarioRegistro entity, @MappingTarget UsuarioListadoResponse dto) {
        dto.setEmpresas(mapEmpresas(entity));
        dto.setSucursales(mapSucursales(entity));
    }
}