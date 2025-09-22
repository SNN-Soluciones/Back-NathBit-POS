package com.snnsoluciones.backnathbitpos.mappers;

import com.snnsoluciones.backnathbitpos.dto.auth.EmpresaResumen;
import com.snnsoluciones.backnathbitpos.dto.auth.SucursalResumen;
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
        // Cargar empresas asignadas usando EmpresaResumen
        List<UsuarioEmpresa> asignacionesEmpresa = usuarioEmpresaRepository.findByUsuarioId(usuario.getId());
        List<EmpresaResumen> empresas = asignacionesEmpresa.stream()
            .map(ue -> EmpresaResumen.builder()
                .id(ue.getEmpresa().getId())
                .nombre(ue.getEmpresa().getNombreRazonSocial())
                .nombreComercial(ue.getEmpresa().getNombreComercial())
                .email(ue.getEmpresa().getEmail())
                .identificacion(ue.getEmpresa().getIdentificacion())
                .logo(ue.getEmpresa().getLogoUrl())
                .requiereHacienda(ue.getEmpresa().getRequiereHacienda())
                .activa(ue.getEmpresa().getActiva())
                .build())
            .collect(Collectors.toList());
        response.setEmpresas(empresas);

        // Cargar sucursales asignadas usando SucursalResumen
        List<UsuarioSucursal> asignacionesSucursal = usuarioSucursalRepository.findByUsuarioId(usuario.getId());
        List<SucursalResumen> sucursales = asignacionesSucursal.stream()
            .map(us -> new SucursalResumen(
                us.getSucursal().getId(),
                us.getSucursal().getNombre(),
                us.getSucursal().getNumeroSucursal(),
                us.getSucursal().getModoFacturacion(),
                us.getSucursal().getActiva()
            ))
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