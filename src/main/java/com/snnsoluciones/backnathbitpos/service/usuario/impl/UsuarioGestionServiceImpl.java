package com.snnsoluciones.backnathbitpos.service.usuario.impl;

import com.snnsoluciones.backnathbitpos.dto.usuario.*;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import com.snnsoluciones.backnathbitpos.exception.*;
import com.snnsoluciones.backnathbitpos.mapper.UsuarioEmpresaRolMapper;
import com.snnsoluciones.backnathbitpos.mapper.UsuarioMapper;
import com.snnsoluciones.backnathbitpos.repository.*;
import com.snnsoluciones.backnathbitpos.service.usuario.PermisoService;
import com.snnsoluciones.backnathbitpos.service.usuario.UsuarioGestionService;
import com.snnsoluciones.backnathbitpos.service.usuario.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioGestionServiceImpl implements UsuarioGestionService {
    
    private final UsuarioRepository usuarioRepository;
    private final UsuarioEmpresaRolRepository usuarioEmpresaRolRepository;
    private final EmpresaRepository empresaRepository;
    private final SucursalRepository sucursalRepository;
    private final UsuarioService usuarioService;
    private final PermisoService permisoService;
    private final UsuarioMapper usuarioMapper;
    private final UsuarioEmpresaRolMapper usuarioEmpresaRolMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UsuarioDTO crearUsuario(CrearUsuarioRequest request) throws BadRequestException {
        // Validar que no exista el usuario
        if (usuarioService.existePorEmail(request.getEmail())) {
            throw new ConflictException("Ya existe un usuario con el email: " + request.getEmail());
        }

        if (request.getIdentificacion() != null &&
            usuarioService.existePorIdentificacion(request.getIdentificacion())) {
            throw new ConflictException("Ya existe un usuario con la identificación: " +
                request.getIdentificacion());
        }

        // Obtener usuario actual (si existe)
        Long usuarioActualId = null;
        RolNombre rolCreador = null;

        try {
            usuarioActualId = obtenerUsuarioActualId();
            rolCreador = obtenerRolPrincipalUsuario(usuarioActualId);

            // Validar que puede crear este tipo de usuario
            validarPermisoCreacionUsuario(rolCreador, request.getRol());

        } catch (Exception e) {
            // Si no hay usuario autenticado, verificar si es el primer usuario ROOT
            long totalUsuarios = usuarioRepository.count();

            if (totalUsuarios == 0 && request.getRol() == RolNombre.ROOT) {
                log.warn("Creando primer usuario ROOT del sistema sin autenticación");
                // Permitir crear el primer ROOT sin autenticación
            } else {
                throw new UnauthorizedException("Debe estar autenticado para crear usuarios");
            }
        }

        // Validar empresa y sucursal
        Empresa empresa = empresaRepository.findById(request.getEmpresaId())
            .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

        Sucursal sucursal = null;
        if (request.getSucursalId() != null) {
            sucursal = sucursalRepository.findById(request.getSucursalId())
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));

            // Validar que la sucursal pertenezca a la empresa
            if (!sucursal.getEmpresa().getId().equals(empresa.getId())) {
                throw new BadRequestException("La sucursal no pertenece a la empresa indicada");
            }
        }

        // Validaciones adicionales según el rol que se está creando
        validarContextoParaRol(request.getRol(), rolCreador, empresa, sucursal);

        // Crear usuario
        Usuario usuario = new Usuario();
        usuario.setEmail(request.getEmail());
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setNombre(request.getNombre());
        usuario.setApellidos(request.getApellidos());
        usuario.setTelefono(request.getTelefono());
        usuario.setIdentificacion(request.getIdentificacion());
        usuario.setActivo(true);

        usuario = usuarioRepository.save(usuario);

        // Asignar rol inicial
        UsuarioEmpresaRol uer = new UsuarioEmpresaRol();
        uer.setUsuario(usuario);
        uer.setEmpresa(empresa);
        uer.setSucursal(sucursal);
        uer.setRol(request.getRol());
        uer.setEsPrincipal(true); // Primer rol es principal
        uer.setActivo(true);
        uer.setAsignadoPor(usuarioActualId); // Puede ser null para el primer ROOT

        // Asignar permisos por defecto del rol
        uer.setPermisos(permisoService.obtenerPermisosDefaultPorRol(request.getRol()));

        usuarioEmpresaRolRepository.save(uer);

        log.info("Usuario creado: {} con rol {} en empresa {} por usuario {}",
            usuario.getEmail(), request.getRol(), empresa.getNombre(),
            usuarioActualId != null ? usuarioActualId : "SISTEMA");

        return usuarioMapper.toDTO(usuario);
    }


    // Validaciones adicionales según el contexto
    private void validarContextoParaRol(RolNombre rolACrear, RolNombre rolCreador,
        Empresa empresa, Sucursal sucursal) throws BadRequestException {

        // Si no hay rol creador (primer usuario), solo validar que sea ROOT
        if (rolCreador == null) {
            if (rolACrear != RolNombre.ROOT) {
                throw new BadRequestException("El primer usuario debe ser ROOT");
            }
            return;
        }

        // Validar que usuarios de sistema (ROOT/SOPORTE) no se asignen a sucursales específicas
        if (rolACrear.esDeSistema() && sucursal != null) {
            throw new BadRequestException("Usuarios de sistema no deben asignarse a sucursales específicas");
        }

        // Validar que SUPER_ADMIN tenga acceso a nivel empresa (sin sucursal específica)
        if (rolACrear == RolNombre.SUPER_ADMIN && sucursal != null) {
            throw new BadRequestException("SUPER_ADMIN debe tener acceso a nivel empresa, no sucursal");
        }

        // Validar que roles operativos tengan sucursal asignada
        if (rolACrear.esOperativo() && sucursal == null) {
            throw new BadRequestException("Roles operativos deben estar asignados a una sucursal específica");
        }

        // Si el creador no es de sistema, validar que solo cree en su propia empresa
        if (!rolCreador.esDeSistema()) {
            Long empresaCreadorId = obtenerEmpresaPrincipalUsuario(obtenerUsuarioActualId());
            if (!empresaCreadorId.equals(empresa.getId())) {
                throw new UnauthorizedException("Solo puede crear usuarios en su propia empresa");
            }
        }
    }
    
    @Override
    public UsuarioDTO actualizarUsuario(Long id, ActualizarUsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        
        // Validar email si cambió
        if (!usuario.getEmail().equals(request.getEmail()) && 
            usuarioService.existePorEmail(request.getEmail())) {
            throw new ConflictException("Ya existe un usuario con el email: " + request.getEmail());
        }
        
        // Validar identificación si cambió
        if (request.getIdentificacion() != null &&
            !request.getIdentificacion().equals(usuario.getIdentificacion()) &&
            usuarioService.existePorIdentificacion(request.getIdentificacion())) {
            throw new ConflictException("Ya existe un usuario con la identificación: " + 
                request.getIdentificacion());
        }
        
        // Actualizar datos básicos
        usuario.setEmail(request.getEmail());
        usuario.setNombre(request.getNombre());
        usuario.setApellidos(request.getApellidos());
        usuario.setTelefono(request.getTelefono());
        usuario.setIdentificacion(request.getIdentificacion());
        
        usuario = usuarioRepository.save(usuario);
        
        log.info("Usuario actualizado: {}", usuario.getEmail());
        return usuarioMapper.toDTO(usuario);
    }
    
    @Override
    public UsuarioEmpresaRolDTO asignarRol(Long usuarioId, AsignarRolRequest request)
        throws BadRequestException {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        
        Empresa empresa = empresaRepository.findById(request.getEmpresaId())
            .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));
        
        Sucursal sucursal = null;
        if (request.getSucursalId() != null) {
            sucursal = sucursalRepository.findById(request.getSucursalId())
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));
            
            if (!sucursal.getEmpresa().getId().equals(empresa.getId())) {
                throw new BadRequestException("La sucursal no pertenece a la empresa indicada");
            }
        }
        
        // Verificar si ya tiene ese rol
        Optional<UsuarioEmpresaRol> existente;
        if (sucursal != null) {
            existente = usuarioEmpresaRolRepository
                .findByUsuarioIdAndEmpresaIdAndSucursalId(usuarioId, empresa.getId(), sucursal.getId());
        } else {
            existente = usuarioEmpresaRolRepository
                .findByUsuarioIdAndEmpresaIdAndSucursalIsNull(usuarioId, empresa.getId());
        }
        
        if (existente.isPresent()) {
            throw new ConflictException("El usuario ya tiene un rol asignado en ese contexto");
        }
        
        // Crear nueva asignación
        UsuarioEmpresaRol uer = new UsuarioEmpresaRol();
        uer.setUsuario(usuario);
        uer.setEmpresa(empresa);
        uer.setSucursal(sucursal);
        uer.setRol(request.getRol());
        uer.setActivo(true);
        uer.setAsignadoPor(obtenerUsuarioActualId());
        
        // Establecer permisos
        if (request.getPermisos() != null && !request.getPermisos().isEmpty()) {
            if (!permisoService.validarEstructuraPermisos(request.getPermisos())) {
                throw new BadRequestException("Estructura de permisos inválida");
            }
            uer.setPermisos(request.getPermisos());
        } else {
            uer.setPermisos(permisoService.obtenerPermisosDefaultPorRol(request.getRol()));
        }
        
        // Si es el primer rol, marcarlo como principal
        List<UsuarioEmpresaRol> rolesExistentes = usuarioEmpresaRolRepository
            .findByUsuarioIdAndActivoTrue(usuarioId);
        if (rolesExistentes.isEmpty()) {
            uer.setEsPrincipal(true);
        }
        
        uer = usuarioEmpresaRolRepository.save(uer);
        
        log.info("Rol {} asignado a usuario {} en empresa {}", 
                request.getRol(), usuario.getEmail(), empresa.getNombre());
        
        return usuarioEmpresaRolMapper.toDTO(uer);
    }
    
    @Override
    public void removerRol(Long usuarioId, Long usuarioEmpresaRolId) throws BadRequestException {
        UsuarioEmpresaRol uer = usuarioEmpresaRolRepository.findById(usuarioEmpresaRolId)
            .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
        
        // Verificar que el rol pertenece al usuario
        if (!uer.getUsuario().getId().equals(usuarioId)) {
            throw new BadRequestException("El rol no pertenece al usuario indicado");
        }
        
        // No permitir eliminar el último rol activo
        long rolesActivos = usuarioEmpresaRolRepository
            .findByUsuarioIdAndActivoTrue(usuarioId).size();
        
        if (rolesActivos <= 1) {
            throw new BadRequestException("No se puede eliminar el último rol del usuario");
        }
        
        // Desactivar rol
        uer.setActivo(false);
        usuarioEmpresaRolRepository.save(uer);
        
        // Si era el principal, asignar otro
        if (uer.getEsPrincipal()) {
            List<UsuarioEmpresaRol> otrosRoles = usuarioEmpresaRolRepository
                .findByUsuarioIdAndActivoTrue(usuarioId).stream()
                .filter(r -> !r.getId().equals(usuarioEmpresaRolId))
                .collect(Collectors.toList());
            
            if (!otrosRoles.isEmpty()) {
                otrosRoles.get(0).setEsPrincipal(true);
                usuarioEmpresaRolRepository.save(otrosRoles.get(0));
            }
        }
        
        log.info("Rol removido del usuario {}", uer.getUsuario().getEmail());
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<UsuarioDTO> listarUsuarios(Long empresaId, Long sucursalId,
                                          String rol, Boolean activo,
                                          String search, Pageable pageable) {
        // Aquí implementarías una consulta más compleja con Specification o @Query
        // Por simplicidad, uso el método existente
        return usuarioService.listarUsuarios(empresaId, sucursalId, search, pageable);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<UsuarioEmpresaRolDTO> obtenerRolesUsuario(Long usuarioId) {
        List<UsuarioEmpresaRol> roles = usuarioEmpresaRolRepository
            .findByUsuarioIdWithRelaciones(usuarioId);
        
        return roles.stream()
            .filter(UsuarioEmpresaRol::getActivo)
            .map(usuarioEmpresaRolMapper::toDTO)
            .collect(Collectors.toList());
    }
    
    @Override
    public UsuarioEmpresaRolDTO actualizarPermisosRol(Long usuarioEmpresaRolId,
                                                      Map<String, Map<String, Boolean>> permisos)
        throws BadRequestException {
        permisoService.actualizarPermisos(usuarioEmpresaRolId, permisos);
        
        UsuarioEmpresaRol uer = usuarioEmpresaRolRepository.findById(usuarioEmpresaRolId)
            .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
        
        return usuarioEmpresaRolMapper.toDTO(uer);
    }
    
    @Override
    public void establecerRolPrincipal(Long usuarioId, Long usuarioEmpresaRolId)
        throws BadRequestException {
        UsuarioEmpresaRol uer = usuarioEmpresaRolRepository.findById(usuarioEmpresaRolId)
            .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
        
        if (!uer.getUsuario().getId().equals(usuarioId)) {
            throw new BadRequestException("El rol no pertenece al usuario indicado");
        }
        
        // Desmarcar otros roles como principales
        usuarioEmpresaRolRepository.desmarcarRolesPrincipales(usuarioId, usuarioEmpresaRolId);
        
        // Marcar este como principal
        uer.setEsPrincipal(true);
        usuarioEmpresaRolRepository.save(uer);
        
        log.info("Rol principal establecido para usuario {}", uer.getUsuario().getEmail());
    }
    
    @Override
    public int transferirUsuariosSucursal(Long sucursalOrigenId, Long sucursalDestinoId)
        throws BadRequestException {
        // Validar sucursales
        Sucursal origen = sucursalRepository.findById(sucursalOrigenId)
            .orElseThrow(() -> new ResourceNotFoundException("Sucursal origen no encontrada"));
        
        Sucursal destino = sucursalRepository.findById(sucursalDestinoId)
            .orElseThrow(() -> new ResourceNotFoundException("Sucursal destino no encontrada"));
        
        // Deben ser de la misma empresa
        if (!origen.getEmpresa().getId().equals(destino.getEmpresa().getId())) {
            throw new BadRequestException("Las sucursales deben pertenecer a la misma empresa");
        }
        
        // Transferir usuarios
        List<UsuarioEmpresaRol> roles = usuarioEmpresaRolRepository.findBySucursalId(sucursalOrigenId);
        
        for (UsuarioEmpresaRol rol : roles) {
            rol.setSucursal(destino);
        }
        
        usuarioEmpresaRolRepository.saveAll(roles);
        
        log.info("Transferidos {} usuarios de sucursal {} a {}", 
                roles.size(), origen.getNombre(), destino.getNombre());
        
        return roles.size();
    }
    
    @Override
    public int desactivarUsuariosMasivo(Long empresaId, Long sucursalId) {
        List<UsuarioEmpresaRol> roles;
        
        if (sucursalId != null) {
            roles = usuarioEmpresaRolRepository.findBySucursalId(sucursalId);
        } else {
            roles = usuarioEmpresaRolRepository.findByEmpresaId(empresaId);
        }
        
        // Filtrar solo los activos
        roles = roles.stream()
            .filter(UsuarioEmpresaRol::getActivo)
            .collect(Collectors.toList());
        
        // Desactivar
        for (UsuarioEmpresaRol rol : roles) {
            rol.setActivo(false);
        }
        
        usuarioEmpresaRolRepository.saveAll(roles);
        
        log.info("Desactivados {} usuarios", roles.size());
        
        return roles.size();
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean puedeGestionarUsuario(Long gestorId, Long usuarioId, Long empresaId) {
        // Obtener rol del gestor
        Optional<UsuarioEmpresaRol> rolGestor = usuarioEmpresaRolRepository
            .findByUsuarioIdAndEmpresaIdAndSucursalIsNull(gestorId, empresaId);
        
        if (rolGestor.isEmpty()) {
            return false;
        }
        
        RolNombre rol = rolGestor.get().getRol();
        
        // ROOT y SUPER_ADMIN pueden gestionar a cualquiera
        if (rol == RolNombre.ROOT || rol == RolNombre.SUPER_ADMIN) {
            return true;
        }
        
        // ADMIN puede gestionar usuarios de su empresa
        if (rol == RolNombre.ADMIN) {
            return usuarioEmpresaRolRepository.existsByUsuarioIdAndEmpresaIdAndActivoTrue(usuarioId, empresaId);
        }
        
        // JEFE_CAJAS puede gestionar usuarios de su sucursal con roles operativos
        if (rol == RolNombre.JEFE_CAJAS) {
            Long sucursalId = rolGestor.get().getSucursal() != null ? 
                rolGestor.get().getSucursal().getId() : null;
                
            if (sucursalId == null) {
                return false;
            }
            
            Optional<UsuarioEmpresaRol> rolUsuario = usuarioEmpresaRolRepository
                .findByUsuarioIdAndEmpresaIdAndSucursalId(usuarioId, empresaId, sucursalId);
            
            if (rolUsuario.isEmpty()) {
                return false;
            }
            
            // Solo puede gestionar roles operativos
            RolNombre rolDelUsuario = rolUsuario.get().getRol();
            return rolDelUsuario == RolNombre.CAJERO || 
                   rolDelUsuario == RolNombre.MESERO || 
                   rolDelUsuario == RolNombre.COCINA;
        }
        
        // Otros roles no pueden gestionar usuarios
        return false;
    }
    
    // Métodos auxiliares privados

    private Long obtenerUsuarioActualId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() ||
            auth.getPrincipal().equals("anonymousUser")) {
            throw new UnauthorizedException("No hay usuario autenticado");
        }

        // Dependiendo de cómo configures Spring Security, puede ser:
        // Opción 1: Si usas el ID como principal
        if (auth.getPrincipal() instanceof Long) {
            return (Long) auth.getPrincipal();
        }

        // Opción 2: Si usas UserDetails
        if (auth.getPrincipal() instanceof UserDetails) {
            String username = ((UserDetails) auth.getPrincipal()).getUsername();
            // Si el username es el ID
            try {
                return Long.parseLong(username);
            } catch (NumberFormatException e) {
                // Si el username es el email
                Usuario usuario = usuarioRepository.findByEmail(username)
                    .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));
                return usuario.getId();
            }
        }

        throw new UnauthorizedException("No se pudo obtener el ID del usuario actual");
    }

    // Agregar este método en UsuarioGestionServiceImpl

    private void validarPermisoCreacionUsuario(RolNombre rolCreador, RolNombre rolACrear) {
        // Usar la validación del enum
        if (!rolCreador.puedeCrear(rolACrear)) {
            String mensaje = String.format(
                "Un usuario con rol %s no puede crear usuarios con rol %s",
                rolCreador.getDisplayName(),
                rolACrear.getDisplayName()
            );
            throw new UnauthorizedException(mensaje);
        }

        // Log para auditoría
        log.info("Usuario con rol {} creando usuario con rol {}", rolCreador, rolACrear);

        // Advertencias especiales
        if (rolCreador == RolNombre.ROOT && rolACrear != RolNombre.SOPORTE &&
            rolACrear != RolNombre.SUPER_ADMIN) {
            log.warn("ROOT creando rol {} directamente. Considerar si es necesario.", rolACrear);
        }
    }

    // Método helper para obtener el rol principal del usuario actual
    private RolNombre obtenerRolPrincipalUsuario(Long usuarioId) {
        Optional<UsuarioEmpresaRol> rolPrincipal = usuarioEmpresaRolRepository
            .findRolPrincipalByUsuarioId(usuarioId);

        if (rolPrincipal.isEmpty()) {
            // Si no tiene rol principal, buscar el de mayor jerarquía
            List<UsuarioEmpresaRol> roles = usuarioEmpresaRolRepository
                .findByUsuarioIdAndActivoTrue(usuarioId);

            if (roles.isEmpty()) {
                throw new UnauthorizedException("Usuario sin roles asignados");
            }

            // Ordenar por jerarquía y tomar el más alto
            return roles.stream()
                .map(UsuarioEmpresaRol::getRol)
                .min((r1, r2) -> Integer.compare(
                    r1.getNivelJerarquia(),
                    r2.getNivelJerarquia()
                ))
                .orElseThrow(() -> new UnauthorizedException("No se pudo determinar el rol"));
        }

        return rolPrincipal.get().getRol();
    }

    private Long obtenerEmpresaPrincipalUsuario(Long usuarioId) {
        // Buscar el rol principal del usuario
        Optional<UsuarioEmpresaRol> rolPrincipal = usuarioEmpresaRolRepository
            .findRolPrincipalByUsuarioId(usuarioId);

        if (rolPrincipal.isPresent()) {
            return rolPrincipal.get().getEmpresa().getId();
        }

        // Si no tiene rol principal, buscar cualquier rol activo
        List<UsuarioEmpresaRol> roles = usuarioEmpresaRolRepository
            .findByUsuarioIdAndActivoTrue(usuarioId);

        if (roles.isEmpty()) {
            throw new UnauthorizedException("Usuario sin roles asignados");
        }

        // Si el usuario tiene roles en múltiples empresas, esto podría ser un problema
        // Por ahora, tomamos la primera empresa
        Set<Long> empresasIds = roles.stream()
            .map(r -> r.getEmpresa().getId())
            .collect(Collectors.toSet());

        if (empresasIds.size() > 1) {
            log.warn("Usuario {} tiene roles en múltiples empresas, usando la primera", usuarioId);
        }

        return roles.get(0).getEmpresa().getId();
    }
}