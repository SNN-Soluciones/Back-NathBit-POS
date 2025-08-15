package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.usuarios.UsuarioListadoResponse;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.entity.UsuarioEmpresa;
import com.snnsoluciones.backnathbitpos.entity.UsuarioSucursal;
import com.snnsoluciones.backnathbitpos.mapper.UsuarioRegistroMapper;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRegistroRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioListadoService {

    private final UsuarioRegistroMapper usuarioRegistroMapper;

    private final UsuarioRepository usuarioRepository;
    private final UsuarioRegistroRepository usuarioRegistroRepository;
    private final UsuarioPermisosService permisosService;

    public Page<UsuarioListadoResponse> listarUsuariosFiltrados(
            Usuario solicitante,
            Long empresaId,
            Long sucursalId,
            Boolean incluirSinAsignar,
            Pageable pageable) {

        log.info("Listando usuarios - Solicitante: {} ({})", solicitante.getEmail(), solicitante.getRol());
        log.info("Filtros - Empresa: {}, Sucursal: {}, Sin asignar: {}",
            empresaId, sucursalId, incluirSinAsignar);

        Page<Usuario> usuarios = switch (solicitante.getRol()) {
          case ROOT, SOPORTE ->
            // ROOT y SOPORTE ven todos
              listarParaRootSoporte(empresaId, sucursalId, incluirSinAsignar, pageable);
          case SUPER_ADMIN ->
            // SUPER_ADMIN ve solo usuarios de sus empresas
              listarParaSuperAdmin(solicitante, empresaId, sucursalId, incluirSinAsignar, pageable);
          case ADMIN ->
            // ADMIN ve solo usuarios de sus sucursales
              listarParaAdmin(solicitante, empresaId, sucursalId, pageable);
          default ->
            // Otros roles no deberían llegar aquí
              throw new RuntimeException("Rol sin permisos para listar usuarios");
        };

        // Lógica según el rol del solicitante

      // Convertir a DTO de respuesta
        return usuarios.map(this::convertirAResponse);
    }

    private Page<Usuario> listarParaRootSoporte(Long empresaId, Long sucursalId,
                                               Boolean incluirSinAsignar, Pageable pageable) {
        if (sucursalId != null) {
            // Filtrar por sucursal específica
            return usuarioRepository.findBySucursalId(sucursalId, pageable);
        } else if (empresaId != null) {
            // Filtrar por empresa específica
            return usuarioRepository.findByEmpresaId(empresaId, pageable);
        } else if (Boolean.TRUE.equals(incluirSinAsignar)) {
            // Mostrar todos incluyendo sin asignar
            return usuarioRepository.findAll(pageable);
        } else {
            // Por defecto, solo con asignación
            return usuarioRepository.findConAsignacion(pageable);
        }
    }

    private Page<Usuario> listarParaSuperAdmin(Usuario solicitante, Long empresaId,
                                              Long sucursalId, Boolean incluirSinAsignar, Pageable pageable) {
        // Obtener empresas del SUPER_ADMIN
        List<Long> empresasPermitidas = permisosService.obtenerEmpresasAsignables(solicitante)
            .stream()
            .map(Empresa::getId)
            .collect(Collectors.toList());

        if (sucursalId != null) {
            // Validar que la sucursal pertenezca a una de sus empresas
            return usuarioRepository.findBySucursalIdYEmpresasPermitidas(sucursalId, empresasPermitidas, pageable);
        } else if (empresaId != null) {
            // Validar que sea una de sus empresas
            if (!empresasPermitidas.contains(empresaId)) {
                throw new RuntimeException("No tiene permisos para ver usuarios de esta empresa");
            }
            return usuarioRepository.findByEmpresaId(empresaId, pageable);
        } else {
            // Todos los usuarios de sus empresas
            return usuarioRepository.findByEmpresasIds(empresasPermitidas, pageable);
        }
    }

    private Page<Usuario> listarParaAdmin(Usuario solicitante, Long empresaId,
                                         Long sucursalId, Pageable pageable) {
        // ADMIN solo ve usuarios de sus sucursales asignadas
        List<Long> sucursalesPermitidas = permisosService.obtenerSucursalesAsignables(solicitante, null)
            .stream()
            .map(Sucursal::getId)
            .collect(Collectors.toList());

        if (sucursalId != null) {
            // Validar que sea una de sus sucursales
            if (!sucursalesPermitidas.contains(sucursalId)) {
                throw new RuntimeException("No tiene permisos para ver usuarios de esta sucursal");
            }
            return usuarioRepository.findBySucursalId(sucursalId, pageable);
        } else {
            // Todos los usuarios de sus sucursales
            return usuarioRepository.findBySucursalesIds(sucursalesPermitidas, pageable);
        }
    }

    private UsuarioListadoResponse convertirAResponse(Usuario usuario) {
        UsuarioListadoResponse response = new UsuarioListadoResponse();
        response.setId(usuario.getId());
        response.setEmail(usuario.getEmail());
        response.setNombre(usuario.getNombre());
        response.setApellidos(usuario.getApellidos());
        response.setRol(usuario.getRol());
        response.setActivo(usuario.getActivo());

        // Obtener empresas asignadas
        response.setEmpresas(
            usuario.getUsuarioEmpresas().stream()
                .filter(UsuarioEmpresa::getActivo)
                .map(ue -> ue.getEmpresa().getNombreComercial())
                .collect(Collectors.toList())
        );

        // Obtener sucursales asignadas
        response.setSucursales(
            usuario.getUsuarioSucursales().stream()
                .filter(UsuarioSucursal::getActivo)
                .map(us -> us.getSucursal().getNombre())
                .collect(Collectors.toList())
        );

        // Obtener info de auditoría
        usuarioRegistroRepository.findByUsuarioId(usuario.getId())
            .ifPresent(registro -> {
                if (registro.getCreadoPor() != null) {
                    response.setCreadoPor(
                        registro.getCreadoPor().getNombre() + " " +
                        registro.getCreadoPor().getApellidos()
                    );
                }
                response.setFechaCreacion(registro.getFechaCreacion());
            });

        return response;
    }

    public Page<UsuarioListadoResponse> findAll(Pageable pageable) {
        return usuarioRegistroRepository.findAll(pageable)
            .map(usuarioRegistroMapper::toUsuarioListadoResponse);
    }
}