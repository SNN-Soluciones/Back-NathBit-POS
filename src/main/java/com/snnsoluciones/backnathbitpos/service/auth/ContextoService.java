package com.snnsoluciones.backnathbitpos.service.auth;

import com.snnsoluciones.backnathbitpos.dto.auth.ContextoDTO;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.entity.UsuarioEmpresa;
import com.snnsoluciones.backnathbitpos.exception.BadRequestException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.exception.UnauthorizedException;
import com.snnsoluciones.backnathbitpos.repository.UsuarioEmpresaRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio para manejar el contexto de trabajo (empresa/sucursal) de los usuarios
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContextoService {
    
    private final UsuarioRepository usuarioRepository;
    private final UsuarioEmpresaRepository usuarioEmpresaRepository;
    
    // Cache en memoria para contextos activos (podría ser Redis en producción)
    private final ConcurrentHashMap<Long, ContextoDTO> contextosActivos = new ConcurrentHashMap<>();
    
    /**
     * Establece el contexto de trabajo para un usuario
     */
    @Transactional
    public ContextoDTO establecerContexto(Long usuarioId, Long empresaId, Long sucursalId) {
        log.debug("Estableciendo contexto para usuario: {}, empresa: {}, sucursal: {}", 
                  usuarioId, empresaId, sucursalId);
        
        // Validar usuario
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        
        // Usuarios de sistema no necesitan contexto
        if (usuario.esRolSistema()) {
            throw new BadRequestException("Usuarios de sistema no requieren contexto");
        }
        
        // Validar acceso a empresa/sucursal
        UsuarioEmpresa acceso = validarAcceso(usuarioId, empresaId, sucursalId);
        
        // Crear contexto
        ContextoDTO contexto = ContextoDTO.builder()
            .usuarioId(usuarioId)
            .empresaId(empresaId)
            .empresaNombre(acceso.getEmpresa().getNombre())
            .empresaCodigo(acceso.getEmpresa().getCodigo())
            .sucursalId(sucursalId)
            .sucursalNombre(sucursalId != null ? acceso.getSucursal().getNombre() : null)
            .sucursalCodigo(sucursalId != null ? acceso.getSucursal().getCodigo() : null)
            .permisos(acceso.getPermisos())
            .establecidoEn(System.currentTimeMillis())
            .build();
        
        // Guardar en cache
        contextosActivos.put(usuarioId, contexto);
        
        log.info("Contexto establecido exitosamente para usuario: {}", usuarioId);
        return contexto;
    }
    
    /**
     * Obtiene el contexto actual de un usuario
     */
    @Cacheable(value = "contextos", key = "#usuarioId")
    public ContextoDTO obtenerContextoActual(Long usuarioId) {
        ContextoDTO contexto = contextosActivos.get(usuarioId);
        
        if (contexto == null) {
            // Intentar obtener contexto por defecto para usuarios operativos
            contexto = obtenerContextoPorDefecto(usuarioId);
        }
        
        return contexto;
    }
    
    /**
     * Limpia el contexto de un usuario (al cerrar sesión)
     */
    @CacheEvict(value = "contextos", key = "#usuarioId")
    public void limpiarContexto(Long usuarioId) {
        contextosActivos.remove(usuarioId);
        log.debug("Contexto limpiado para usuario: {}", usuarioId);
    }
    
    /**
     * Valida si un usuario puede cambiar de contexto
     */
    public boolean puedeCambiarContexto(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        
        return usuario.puedeCambiarContexto();
    }
    
    /**
     * Obtiene el contexto por defecto para usuarios operativos
     */
    private ContextoDTO obtenerContextoPorDefecto(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        
        // Solo usuarios operativos con una sola asignación tienen contexto por defecto
        if (!usuario.esRolOperativo() || usuario.getUsuarioEmpresas().size() != 1) {
            return null;
        }
        
        UsuarioEmpresa unicaAsignacion = usuario.getUsuarioEmpresas().iterator().next();
        
        if (!unicaAsignacion.esAsignacionVigente()) {
            throw new UnauthorizedException("La asignación no está activa");
        }
        
        return ContextoDTO.builder()
            .usuarioId(usuarioId)
            .empresaId(unicaAsignacion.getEmpresa().getId())
            .empresaNombre(unicaAsignacion.getEmpresa().getNombre())
            .empresaCodigo(unicaAsignacion.getEmpresa().getCodigo())
            .sucursalId(unicaAsignacion.getSucursal() != null ? 
                       unicaAsignacion.getSucursal().getId() : null)
            .sucursalNombre(unicaAsignacion.getSucursal() != null ? 
                           unicaAsignacion.getSucursal().getNombre() : null)
            .sucursalCodigo(unicaAsignacion.getSucursal() != null ? 
                           unicaAsignacion.getSucursal().getCodigo() : null)
            .permisos(unicaAsignacion.getPermisos())
            .establecidoEn(System.currentTimeMillis())
            .esPorDefecto(true)
            .build();
    }
    
    /**
     * Valida el acceso del usuario a la empresa/sucursal
     */
    private UsuarioEmpresa validarAcceso(Long usuarioId, Long empresaId, Long sucursalId) {
        // Buscar la asignación
        UsuarioEmpresa acceso;
        
        if (sucursalId != null) {
            // Acceso específico a sucursal
            acceso = usuarioEmpresaRepository
                .findByUsuarioIdAndEmpresaIdAndSucursalId(usuarioId, empresaId, sucursalId)
                .orElseThrow(() -> new UnauthorizedException(
                    "No tienes acceso a esta sucursal"));
        } else {
            // Acceso a nivel empresa (todas las sucursales)
            acceso = usuarioEmpresaRepository
                .findByUsuarioIdAndEmpresaIdAndSucursalIdIsNull(usuarioId, empresaId)
                .orElseThrow(() -> new UnauthorizedException(
                    "No tienes acceso a esta empresa"));
        }
        
        // Validar que esté activo
        if (!acceso.esAsignacionVigente()) {
            throw new UnauthorizedException("Tu acceso a esta empresa/sucursal no está activo");
        }
        
        return acceso;
    }
    
    /**
     * Valida si el contexto actual permite acceso a una sucursal específica
     */
    public boolean contextoPermiteAccesoSucursal(ContextoDTO contexto, Long sucursalId) {
        if (contexto == null) return false;
        
        // Si tiene acceso a todas las sucursales (sucursalId null en contexto)
        if (contexto.getSucursalId() == null) {
            return true;
        }
        
        // Si no, solo puede acceder a su sucursal específica
        return contexto.getSucursalId().equals(sucursalId);
    }
    
    /**
     * Actualiza el tiempo de última actividad del contexto
     */
    public void actualizarActividad(Long usuarioId) {
        ContextoDTO contexto = contextosActivos.get(usuarioId);
        if (contexto != null) {
            contexto.setUltimaActividad(System.currentTimeMillis());
        }
    }
}