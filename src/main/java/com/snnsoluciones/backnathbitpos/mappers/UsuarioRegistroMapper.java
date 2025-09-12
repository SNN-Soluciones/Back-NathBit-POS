package com.snnsoluciones.backnathbitpos.mappers;

import com.snnsoluciones.backnathbitpos.dto.usuarios.UsuarioListadoResponse;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.entity.UsuarioEmpresa;
import com.snnsoluciones.backnathbitpos.entity.UsuarioRegistro;
import com.snnsoluciones.backnathbitpos.entity.UsuarioSucursal;
import com.snnsoluciones.backnathbitpos.repository.UsuarioEmpresaRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioSucursalRepository;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public abstract class UsuarioRegistroMapper {

    @Autowired
    protected UsuarioEmpresaRepository usuarioEmpresaRepository;

    @Autowired
    protected UsuarioSucursalRepository usuarioSucursalRepository;

    @Mapping(target = "id",            source = "id")
    @Mapping(target = "email",         source = "usuario.email")
    @Mapping(target = "nombre",        source = "usuario.nombre")
    @Mapping(target = "apellidos",     source = "usuario.apellidos")
    @Mapping(target = "telefono",      source = "usuario.telefono")
    @Mapping(target = "rol",           source = "usuario.rol")
    @Mapping(target = "activo",        source = "usuario.activo")
    @Mapping(target = "createdAt",     source = "fechaCreacion")
    @Mapping(target = "updatedAt",     source = "usuario.updatedAt")

    // Estos campos se cargan en @AfterMapping
    @Mapping(target = "empresas",          ignore = true)
    @Mapping(target = "sucursales",        ignore = true)
    @Mapping(target = "tieneAsignacion",   ignore = true)
    public abstract UsuarioListadoResponse toUsuarioListadoResponse(UsuarioRegistro entity);

    // Alternativa: mapear directamente desde Usuario
    @Mapping(target = "id",            source = "id")
    @Mapping(target = "email",         source = "email")
    @Mapping(target = "nombre",        source = "nombre")
    @Mapping(target = "apellidos",     source = "apellidos")
    @Mapping(target = "telefono",      source = "telefono")
    @Mapping(target = "rol",           source = "rol")
    @Mapping(target = "activo",        source = "activo")
    @Mapping(target = "createdAt",     source = "createdAt")
    @Mapping(target = "updatedAt",     source = "updatedAt")

    // Estos campos se cargan en @AfterMapping
    @Mapping(target = "empresas",          ignore = true)
    @Mapping(target = "sucursales",        ignore = true)
    @Mapping(target = "tieneAsignacion",   ignore = true)
    public abstract UsuarioListadoResponse toUsuarioListadoResponse(Usuario usuario);

    @AfterMapping
    protected void loadRelaciones(@MappingTarget UsuarioListadoResponse response, UsuarioRegistro entity) {
        if (entity != null && entity.getUsuario() != null) {
            loadRelacionesFromUsuario(response, entity.getUsuario());
        }
    }

    @AfterMapping
    protected void loadRelaciones(@MappingTarget UsuarioListadoResponse response, Usuario usuario) {
        loadRelacionesFromUsuario(response, usuario);
    }

    private void loadRelacionesFromUsuario(UsuarioListadoResponse response, Usuario usuario) {
        // Cargar empresas asignadas
        List<UsuarioEmpresa> asignacionesEmpresa = usuarioEmpresaRepository.findByUsuarioId(usuario.getId());
        List<String> empresas = asignacionesEmpresa.stream()
            .map(ue -> ue.getEmpresa().getNombreComercial())
            .collect(Collectors.toList());
        response.setEmpresas(empresas);

        // Cargar sucursales asignadas
        List<UsuarioSucursal> asignacionesSucursal = usuarioSucursalRepository.findByUsuarioId(usuario.getId());
        List<String> sucursales = asignacionesSucursal.stream()
            .map(us -> us.getSucursal().getNombre())
            .collect(Collectors.toList());
        response.setSucursales(sucursales);

        // Determinar si tiene asignación
        response.setTieneAsignacion(!empresas.isEmpty() || !sucursales.isEmpty());
    }

    // Helper method para formatear usuario creador
    protected String formatUsuario(Usuario usuario) {
        if (usuario == null) return null;
        return usuario.getNombre() + " " + usuario.getApellidos();
    }
}