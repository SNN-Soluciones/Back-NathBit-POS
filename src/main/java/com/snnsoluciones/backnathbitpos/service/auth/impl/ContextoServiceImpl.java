package com.snnsoluciones.backnathbitpos.service.auth.impl;

import com.snnsoluciones.backnathbitpos.dto.auth.ContextoDTO;
import com.snnsoluciones.backnathbitpos.entity.UsuarioEmpresaRol;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.exception.UnauthorizedException;
import com.snnsoluciones.backnathbitpos.repository.UsuarioEmpresaRolRepository;
import com.snnsoluciones.backnathbitpos.service.auth.ContextoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContextoServiceImpl implements ContextoService {

    private final UsuarioEmpresaRolRepository usuarioEmpresaRolRepository;

    // Cache simple en memoria para contextos activos
    // En producción, usar Redis o similar
    private final Map<Long, ContextoDTO> contextosActivos = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public ContextoDTO establecerContexto(Long usuarioId, Long empresaId, Long sucursalId) {
        log.debug("Estableciendo contexto para usuario {} en empresa {} sucursal {}",
            usuarioId, empresaId, sucursalId);

        // Validar acceso
        if (!validarAccesoContexto(usuarioId, empresaId, sucursalId)) {
            throw new UnauthorizedException("No tiene acceso a la empresa/sucursal seleccionada");
        }

        // Buscar la relación usuario-empresa-rol
        Optional<UsuarioEmpresaRol> usuarioEmpresaRol;
        if (sucursalId != null) {
            usuarioEmpresaRol = usuarioEmpresaRolRepository
                .findByUsuarioIdAndEmpresaIdAndSucursalId(usuarioId, empresaId, sucursalId);
        } else {
            usuarioEmpresaRol = usuarioEmpresaRolRepository
                .findByUsuarioIdAndEmpresaIdAndSucursalIsNull(usuarioId, empresaId);
        }

        if (usuarioEmpresaRol.isEmpty()) {
            throw new ResourceNotFoundException("No se encontró asignación para el contexto especificado");
        }

        // Construir y guardar el contexto
        ContextoDTO contexto = construirContexto(usuarioEmpresaRol.get());
        contextosActivos.put(usuarioId, contexto);

        log.info("Contexto establecido para usuario {}: {}", usuarioId, contexto.getContextoCompleto());

        return contexto;
    }

    @Override
    @Cacheable(value = "contextos", key = "#usuarioId")
    public ContextoDTO obtenerContextoActual(Long usuarioId) {
        return contextosActivos.get(usuarioId);
    }

    @Override
    @CacheEvict(value = "contextos", key = "#usuarioId")
    public void limpiarContexto(Long usuarioId) {
        contextosActivos.remove(usuarioId);
        log.debug("Contexto limpiado para usuario {}", usuarioId);
    }

    @Override
    public boolean validarAccesoContexto(Long usuarioId, Long empresaId, Long sucursalId) {
        if (sucursalId != null) {
            return usuarioEmpresaRolRepository
                .existsByUsuarioIdAndEmpresaIdAndSucursalIdAndActivoTrue(usuarioId, empresaId, sucursalId);
        } else {
            return usuarioEmpresaRolRepository
                .existsByUsuarioIdAndEmpresaIdAndActivoTrue(usuarioId, empresaId);
        }
    }

    @Override
    public ContextoDTO construirContexto(UsuarioEmpresaRol usuarioEmpresaRol) {
        ContextoDTO contexto = ContextoDTO.builder()
            .usuarioId(usuarioEmpresaRol.getUsuario().getId())
            .empresaId(usuarioEmpresaRol.getEmpresa().getId())
            .empresaNombre(usuarioEmpresaRol.getEmpresa().getNombre())
            .empresaCodigo(usuarioEmpresaRol.getEmpresa().getCodigo())
            .rol(usuarioEmpresaRol.getRol())
            .establecidoEn(System.currentTimeMillis())
            .build();

        // Agregar información de sucursal si existe
        if (usuarioEmpresaRol.getSucursal() != null) {
            contexto.setSucursalId(usuarioEmpresaRol.getSucursal().getId());
            contexto.setSucursalNombre(usuarioEmpresaRol.getSucursal().getNombre());
            contexto.setSucursalCodigo(usuarioEmpresaRol.getSucursal().getCodigo());
        }

        // Obtener permisos
        Map<String, Map<String, Boolean>> permisos = usuarioEmpresaRol.getPermisos();
        if (permisos == null || permisos.isEmpty()) {
            // Si no hay permisos personalizados, usar los default del rol
            permisos = UsuarioEmpresaRol.getPermisosDefault(usuarioEmpresaRol.getRol());
        }
        contexto.setPermisos(permisos);

        return contexto;
    }

    @Override
    public void actualizarActividad(Long usuarioId) {
        // Actualizar timestamp de última actividad
        ContextoDTO contexto = contextosActivos.get(usuarioId);
        if (contexto != null) {
            contexto.setEstablecidoEn(System.currentTimeMillis());
        }
    }
}