package com.snnsoluciones.backnathbitpos.service.usuario.impl;

import com.snnsoluciones.backnathbitpos.dto.usuario.PermisoDTO;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.service.usuario.PermisoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermisoServiceImpl implements PermisoService {
    
    private final UsuarioEmpresaRolRepository usuarioEmpresaRolRepository;
    
    // Módulos válidos del sistema
    private static final Set<String> MODULOS_VALIDOS = Set.of(
        "productos", "ordenes", "caja", "reportes", "clientes",
        "usuarios", "inventario", "proveedores", "mesas", "descuentos"
    );
    
    // Acciones válidas
    private static final Set<String> ACCIONES_VALIDAS = Set.of(
        "ver", "crear", "editar", "eliminar", "autorizar"
    );
    
    @Override
    public boolean tienePermiso(Long usuarioId, Long empresaId, Long sucursalId,
                               String modulo, String accion) {
        // Validar entrada
        if (!MODULOS_VALIDOS.contains(modulo) || !ACCIONES_VALIDAS.contains(accion)) {
            log.warn("Módulo o acción inválida: {}.{}", modulo, accion);
            return false;
        }
        
        // Buscar rol del usuario en el contexto
        Optional<UsuarioEmpresaRol> rolOpt;
        if (sucursalId != null) {
            rolOpt = usuarioEmpresaRolRepository
                .findByUsuarioIdAndEmpresaIdAndSucursalId(usuarioId, empresaId, sucursalId);
        } else {
            rolOpt = usuarioEmpresaRolRepository
                .findByUsuarioIdAndEmpresaIdAndSucursalIsNull(usuarioId, empresaId);
        }
        
        if (rolOpt.isEmpty()) {
            return false;
        }
        
        UsuarioEmpresaRol uer = rolOpt.get();
        
        // Verificar que esté activo y válido
        if (!uer.getActivo() || !uer.esValido()) {
            return false;
        }
        
        // Super admin y root tienen todos los permisos
        if (uer.getRol() == RolNombre.ROOT || uer.getRol() == RolNombre.SUPER_ADMIN) {
            return true;
        }
        
        // Verificar permiso específico
        return uer.tienePermiso(modulo, accion);
    }
    
    @Override
    public PermisoDTO obtenerPermisos(UsuarioEmpresaRol usuarioEmpresaRol) {
        PermisoDTO permisoDTO = new PermisoDTO();
        permisoDTO.setRol(usuarioEmpresaRol.getRol());
        permisoDTO.setEmpresaId(usuarioEmpresaRol.getEmpresa().getId());
        
        if (usuarioEmpresaRol.getSucursal() != null) {
            permisoDTO.setSucursalId(usuarioEmpresaRol.getSucursal().getId());
        }
        
        return permisoDTO;
    }
    
    @Override
    public Map<String, Map<String, Boolean>> obtenerPermisosDefaultPorRol(RolNombre rol) {
        return UsuarioEmpresaRol.getPermisosDefault(rol);
    }
    
    @Override
    @Transactional
    public PermisoDTO actualizarPermisos(Long usuarioEmpresaRolId,
                                        Map<String, Map<String, Boolean>> permisos)
        throws BadRequestException {
        // Validar estructura de permisos
        if (!validarEstructuraPermisos(permisos)) {
            throw new BadRequestException("Estructura de permisos inválida");
        }
        
        UsuarioEmpresaRol uer = usuarioEmpresaRolRepository.findById(usuarioEmpresaRolId)
            .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
        
        // No permitir cambiar permisos de ROOT o SUPER_ADMIN
        if (uer.getRol() == RolNombre.ROOT || uer.getRol() == RolNombre.SUPER_ADMIN) {
            throw new BadRequestException("No se pueden modificar permisos de roles administrativos");
        }
        
        // Actualizar permisos
        uer = usuarioEmpresaRolRepository.save(uer);
        
        log.info("Permisos actualizados para usuario {} en empresa {}", 
                uer.getUsuario().getEmail(), uer.getEmpresa().getNombre());
        
        return obtenerPermisos(uer);
    }
    
    @Override
    public boolean tieneRol(Long usuarioId, Long empresaId, Long sucursalId, RolNombre... roles) {
        Optional<UsuarioEmpresaRol> rolOpt;
        if (sucursalId != null) {
            rolOpt = usuarioEmpresaRolRepository
                .findByUsuarioIdAndEmpresaIdAndSucursalId(usuarioId, empresaId, sucursalId);
        } else {
            rolOpt = usuarioEmpresaRolRepository
                .findByUsuarioIdAndEmpresaIdAndSucursalIsNull(usuarioId, empresaId);
        }
        
        if (rolOpt.isEmpty()) {
            return false;
        }
        
        UsuarioEmpresaRol uer = rolOpt.get();
        
        // Verificar que esté activo
        if (!uer.getActivo() || !uer.esValido()) {
            return false;
        }
        
        // Verificar si tiene alguno de los roles solicitados
        return Stream.of(roles).anyMatch(rol -> uer.getRol() == rol);
    }
    
    @Override
    public boolean puedeRealizarAccion(Long usuarioId, Long empresaId, Long sucursalId, String accion) {
        // Parsear acción (formato: "modulo.accion")
        String[] partes = accion.split("\\.");
        if (partes.length != 2) {
            log.warn("Formato de acción inválido: {}", accion);
            return false;
        }
        
        String modulo = partes[0];
        String operacion = partes[1];
        
        return tienePermiso(usuarioId, empresaId, sucursalId, modulo, operacion);
    }
    
    @Override
    public UsuarioEmpresaRol obtenerRolPrincipal(Long usuarioId) {
        // Primero buscar el marcado como principal
        Optional<UsuarioEmpresaRol> principal = usuarioEmpresaRolRepository
            .findRolPrincipalByUsuarioId(usuarioId);
        
        if (principal.isPresent()) {
            return principal.get();
        }
        
        // Si no hay principal, devolver el primero activo
        List<UsuarioEmpresaRol> roles = usuarioEmpresaRolRepository
            .findByUsuarioIdAndActivoTrue(usuarioId);
        
        if (roles.isEmpty()) {
            throw new ResourceNotFoundException("Usuario sin roles activos");
        }
        
        return roles.get(0);
    }
    
    @Override
    public boolean validarEstructuraPermisos(Map<String, Map<String, Boolean>> permisos) {
        if (permisos == null || permisos.isEmpty()) {
            return false;
        }
        
        for (Map.Entry<String, Map<String, Boolean>> entry : permisos.entrySet()) {
            String modulo = entry.getKey();
            Map<String, Boolean> acciones = entry.getValue();
            
            // Validar módulo
            if (!MODULOS_VALIDOS.contains(modulo)) {
                log.warn("Módulo inválido en permisos: {}", modulo);
                return false;
            }
            
            // Validar acciones
            if (acciones == null || acciones.isEmpty()) {
                return false;
            }
            
            for (String accion : acciones.keySet()) {
                if (!ACCIONES_VALIDAS.contains(accion)) {
                    log.warn("Acción inválida en permisos: {}.{}", modulo, accion);
                    return false;
                }
            }
        }
        
        return true;
    }
}