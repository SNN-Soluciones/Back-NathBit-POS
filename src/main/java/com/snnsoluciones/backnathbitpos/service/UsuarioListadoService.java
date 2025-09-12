package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.usuarios.UsuarioListadoResponse;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioEmpresaRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioSucursalRepository;
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

    private final UsuarioRepository usuarioRepository;
    private final UsuarioEmpresaRepository usuarioEmpresaRepository;
    private final UsuarioSucursalRepository usuarioSucursalRepository;
    private final UsuarioPermisosService permisosService;

    public Page<UsuarioListadoResponse> findAll(Pageable pageable) {
        // Para ROOT - mostrar todos menos otros ROOT
        return usuarioRepository.findAllExceptRoot(pageable)
            .map(this::convertirAResponse);
    }

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
            case ROOT -> listarParaRoot(empresaId, sucursalId, incluirSinAsignar, pageable);
            case SOPORTE -> listarParaSoporte(empresaId, sucursalId, incluirSinAsignar, pageable);
            case SUPER_ADMIN -> listarParaSuperAdmin(solicitante, empresaId, sucursalId, incluirSinAsignar, pageable);
            case ADMIN -> listarParaAdmin(solicitante, empresaId, sucursalId, pageable);
            default -> throw new RuntimeException("Rol sin permisos para listar usuarios");
        };

        return usuarios.map(this::convertirAResponse);
    }

    private Page<Usuario> listarParaRoot(Long empresaId, Long sucursalId,
        Boolean incluirSinAsignar, Pageable pageable) {
        // ROOT ve todos menos otros ROOT
        if (sucursalId != null && empresaId != null) {
            return usuarioRepository.findByEmpresaIdAndSucursalIdIncludeSuperiores(empresaId, sucursalId, pageable);
        } else if (sucursalId != null) {
            return usuarioRepository.findBySucursalId(sucursalId, pageable);
        } else if (empresaId != null) {
            return usuarioRepository.findByEmpresaId(empresaId, pageable);
        } else if (Boolean.TRUE.equals(incluirSinAsignar)) {
            return usuarioRepository.findAllExceptRoot(pageable);
        } else {
            return usuarioRepository.findConAsignacion(pageable);
        }
    }

    private Page<Usuario> listarParaSoporte(Long empresaId, Long sucursalId,
        Boolean incluirSinAsignar, Pageable pageable) {
        // SOPORTE es como ROOT pero no puede ver ROOT
        return listarParaRoot(empresaId, sucursalId, incluirSinAsignar, pageable);
    }

    private Page<Usuario> listarParaSuperAdmin(Usuario solicitante, Long empresaId,
        Long sucursalId, Boolean incluirSinAsignar,
        Pageable pageable) {
        // Obtener empresas del SUPER_ADMIN
        List<Long> empresasPermitidas = permisosService.obtenerEmpresasAsignables(solicitante)
            .stream()
            .map(Empresa::getId)
            .collect(Collectors.toList());

        if (sucursalId != null && empresaId != null) {
            // Validar que la empresa sea suya
            if (!empresasPermitidas.contains(empresaId)) {
                throw new RuntimeException("No tiene permisos para ver usuarios de esta empresa");
            }
            return usuarioRepository.findByEmpresaIdAndSucursalIdIncludeSuperiores(empresaId, sucursalId, pageable);
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
        // Obtener empresa del ADMIN
        List<UsuarioEmpresa> asignaciones = usuarioEmpresaRepository.findByUsuarioId(solicitante.getId());
        if (asignaciones.isEmpty()) {
            throw new RuntimeException("Admin sin empresa asignada");
        }

        Long empresaAdmin = asignaciones.get(0).getEmpresa().getId();

        // ADMIN solo ve usuarios de sus sucursales asignadas
        List<Long> sucursalesPermitidas = permisosService.obtenerSucursalesAsignables(solicitante, empresaAdmin)
            .stream()
            .map(Sucursal::getId)
            .collect(Collectors.toList());

        if (sucursalId != null) {
            // Validar que sea una de sus sucursales
            if (!sucursalesPermitidas.contains(sucursalId)) {
                throw new RuntimeException("No tiene permisos para ver usuarios de esta sucursal");
            }
            return usuarioRepository.findByEmpresaIdAndSucursalIdIncludeSuperiores(empresaAdmin, sucursalId, pageable);
        } else {
            // Todos los usuarios de sus sucursales + SUPER_ADMIN y ADMIN de la empresa
            return usuarioRepository.findBySucursalesIds(sucursalesPermitidas, empresaAdmin, pageable);
        }
    }

    private UsuarioListadoResponse convertirAResponse(Usuario usuario) {
        UsuarioListadoResponse response = new UsuarioListadoResponse();
        response.setId(usuario.getId());
        response.setEmail(usuario.getEmail());
        response.setNombre(usuario.getNombre());
        response.setApellidos(usuario.getApellidos());
        response.setTelefono(usuario.getTelefono());
        response.setRol(usuario.getRol());
        response.setActivo(usuario.getActivo());
        response.setCreatedAt(usuario.getCreatedAt());
        response.setUpdatedAt(usuario.getUpdatedAt());

        // Agregar empresas asignadas
        List<UsuarioEmpresa> asignacionesEmpresa = usuarioEmpresaRepository.findByUsuarioId(usuario.getId());
        response.setEmpresas(asignacionesEmpresa.stream()
            .map(ue -> ue.getEmpresa().getNombreComercial())
            .collect(Collectors.toList()));

        // Agregar sucursales asignadas
        List<UsuarioSucursal> asignacionesSucursal = usuarioSucursalRepository.findByUsuarioId(usuario.getId());
        response.setSucursales(asignacionesSucursal.stream()
            .map(us -> us.getSucursal().getNombre())
            .collect(Collectors.toList()));

        // Indicar si tiene asignaciones
        response.setTieneAsignacion(!asignacionesEmpresa.isEmpty() || !asignacionesSucursal.isEmpty());

        return response;
    }
}