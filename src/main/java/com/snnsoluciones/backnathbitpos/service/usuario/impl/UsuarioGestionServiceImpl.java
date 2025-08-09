package com.snnsoluciones.backnathbitpos.service.usuario.impl;

import com.snnsoluciones.backnathbitpos.dto.usuario.*;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import com.snnsoluciones.backnathbitpos.enums.TipoUsuario;
import com.snnsoluciones.backnathbitpos.exception.*;
import com.snnsoluciones.backnathbitpos.mapper.UsuarioEmpresaRolMapper;
import com.snnsoluciones.backnathbitpos.mapper.UsuarioMapper;
import com.snnsoluciones.backnathbitpos.repository.*;
import com.snnsoluciones.backnathbitpos.service.usuario.PermisoService;
import com.snnsoluciones.backnathbitpos.service.usuario.UsuarioGestionService;
import com.snnsoluciones.backnathbitpos.service.usuario.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

        // Crear usuario con rol único global
        Usuario usuario = Usuario.builder()
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .nombre(request.getNombre())
            .apellidos(request.getApellidos())
            .telefono(request.getTelefono())
            .identificacion(request.getIdentificacion())
            .tipoIdentificacion(request.getTipoIdentificacion())
            .rol(request.getRol()) // ROL ÚNICO GLOBAL
            .tipoUsuario(determinarTipoUsuario(request.getRol()))
            .activo(true)
            .build();

        // Establecer username si no se proporciona
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            usuario.setUsername(generarUsername(request.getNombre(), request.getApellidos()));
        } else {
            usuario.setUsername(request.getEmail());
        }

        usuario = usuarioRepository.save(usuario);

        log.info("Usuario creado exitosamente: {} con rol {}", usuario.getEmail(), usuario.getRol());

        return usuarioMapper.toDto(usuario);
    }

    @Override
    public UsuarioDTO actualizarUsuario(Long id, ActualizarUsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Validar email único si cambió
        if (request.getEmail() != null && !request.getEmail().equals(usuario.getEmail())) {
            if (usuarioService.existePorEmail(request.getEmail())) {
                throw new ConflictException("El email ya está en uso");
            }
            usuario.setEmail(request.getEmail());
        }

        // Actualizar campos básicos
        if (request.getNombre() != null) usuario.setNombre(request.getNombre());
        if (request.getApellidos() != null) usuario.setApellidos(request.getApellidos());
        if (request.getTelefono() != null) usuario.setTelefono(request.getTelefono());
        if (request.getDireccion() != null) usuario.setDireccion(request.getDireccion());

        // Actualizar rol global si se proporciona
        if (request.getRol() != null) {
            usuario.setRol(request.getRol());
            usuario.setTipoUsuario(determinarTipoUsuario(request.getRol()));
        }

        usuario = usuarioRepository.save(usuario);

        log.info("Usuario actualizado: {}", usuario.getEmail());

        return usuarioMapper.toDto(usuario);
    }

    @Override
    public UsuarioEmpresaRolDTO asignarRol(Long usuarioId, AsignarRolRequest request)
        throws BadRequestException {
        // Validar usuario
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Validar empresa
        Empresa empresa = empresaRepository.findById(request.getEmpresaId())
            .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

        // Validar sucursal si se proporciona
        Sucursal sucursal = null;
        if (request.getSucursalId() != null) {
            sucursal = sucursalRepository.findById(request.getSucursalId())
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));

            // Verificar que la sucursal pertenece a la empresa
            if (!sucursal.getEmpresa().getId().equals(empresa.getId())) {
                throw new BadRequestException("La sucursal no pertenece a la empresa indicada");
            }
        }

        // Verificar si ya existe la asignación
        Optional<UsuarioEmpresaRol> existente;
        if (sucursal != null) {
            existente = usuarioEmpresaRolRepository.findByUsuarioIdAndEmpresaIdAndSucursalId(
                usuario.getId(), empresa.getId(), sucursal.getId());
        } else {
            existente = usuarioEmpresaRolRepository.findByUsuarioIdAndEmpresaIdAndSucursalIsNull(
                usuario.getId(), empresa.getId());
        }

        if (existente.isPresent() && existente.get().getActivo()) {
            throw new ConflictException("El usuario ya tiene un rol asignado en este contexto");
        }

        // Crear nueva asignación
        UsuarioEmpresaRol nuevoRol = UsuarioEmpresaRol.builder()
            .usuario(usuario)
            .empresa(empresa)
            .sucursal(sucursal)
            .rol(usuario.getRol()) // Usar el rol global del usuario
            .esPrincipal(request.getEsPrincipal() != null ? request.getEsPrincipal() : false)
            .activo(true)
            .asignadoPor(obtenerUsuarioActualId())
            .notas(request.getNotas())
            .build();

        // Si es principal, desmarcar otros
        if (Boolean.TRUE.equals(nuevoRol.getEsPrincipal())) {
            usuarioEmpresaRolRepository.desmarcarOtrosComoPrincipal(
                usuario.getId(), 0L); // 0L porque aún no tiene ID
        }

        nuevoRol = usuarioEmpresaRolRepository.save(nuevoRol);

        log.info("Rol asignado: {} a {} en {}",
            usuario.getEmail(), empresa.getNombre(),
            sucursal != null ? sucursal.getNombre() : "todas las sucursales");

        return usuarioEmpresaRolMapper.toDTO(nuevoRol);
    }

    @Override
    public void removerRol(Long usuarioId, Long usuarioEmpresaRolId) throws BadRequestException {
        UsuarioEmpresaRol uer = usuarioEmpresaRolRepository.findById(usuarioEmpresaRolId)
            .orElseThrow(() -> new ResourceNotFoundException("Asignación no encontrada"));

        // Verificar que el rol pertenece al usuario
        if (!uer.getUsuario().getId().equals(usuarioId)) {
            throw new BadRequestException("El rol no pertenece al usuario especificado");
        }

        // Verificar que no sea el último rol activo del usuario
        long rolesActivos = usuarioEmpresaRolRepository
            .findByUsuarioIdAndActivoTrue(uer.getUsuario().getId())
            .size();

        if (rolesActivos <= 1 && !uer.getUsuario().esRolSistema()) {
            throw new BadRequestException("No se puede eliminar el último rol del usuario");
        }

        // Desactivar rol
        uer.setActivo(false);
        usuarioEmpresaRolRepository.save(uer);

        // Si era el principal, asignar otro
        if (uer.getEsPrincipal()) {
            List<UsuarioEmpresaRol> otrosRoles = usuarioEmpresaRolRepository
                .findByUsuarioIdAndActivoTrue(uer.getUsuario().getId()).stream()
                .filter(r -> !r.getId().equals(usuarioEmpresaRolId))
                .collect(Collectors.toList());

            if (!otrosRoles.isEmpty()) {
                UsuarioEmpresaRol nuevoPrincipal = otrosRoles.get(0);
                nuevoPrincipal.setEsPrincipal(true);
                usuarioEmpresaRolRepository.save(nuevoPrincipal);
            }
        }

        log.info("Rol removido del usuario {}", uer.getUsuario().getEmail());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UsuarioDTO> listarUsuarios(Long empresaId, Long sucursalId,
        String rol, Boolean activo,
        String search, Pageable pageable) {

        RolNombre rolEnum = null;
        if (rol != null && !rol.isBlank()) {
            try {
                rolEnum = RolNombre.valueOf(rol.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Rol inválido: {}", rol);
            }
        }

        Page<UsuarioEmpresaRol> rolesPage = usuarioEmpresaRolRepository.buscarConFiltros(
            empresaId, sucursalId, rolEnum, activo, search, pageable);

        return rolesPage.map(uer -> {
            UsuarioDTO dto = usuarioMapper.toDto(uer.getUsuario());
            // Agregar información del contexto
            dto.setEmpresaActual(uer.getEmpresa().getNombre());
            if (uer.getSucursal() != null) {
                dto.setSucursalActual(uer.getSucursal().getNombre());
            }
            return dto;
        });
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
        throws BadRequestException, org.apache.coyote.BadRequestException {

        PermisoDTO permisoActualizado = permisoService.actualizarPermisos(
            usuarioEmpresaRolId, permisos);

        UsuarioEmpresaRol uer = usuarioEmpresaRolRepository.findById(usuarioEmpresaRolId)
            .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));

        return usuarioEmpresaRolMapper.toDTO(uer);
    }

    @Override
    public void establecerRolPrincipal(Long usuarioId, Long usuarioEmpresaRolId)
        throws BadRequestException {

        UsuarioEmpresaRol uer = usuarioEmpresaRolRepository.findById(usuarioEmpresaRolId)
            .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));

        // Verificar que el rol pertenece al usuario
        if (!uer.getUsuario().getId().equals(usuarioId)) {
            throw new BadRequestException("El rol no pertenece al usuario especificado");
        }

        // Verificar que esté activo
        if (!uer.getActivo()) {
            throw new BadRequestException("No se puede establecer como principal un rol inactivo");
        }

        // Desmarcar otros como principal
        usuarioEmpresaRolRepository.desmarcarOtrosComoPrincipal(usuarioId, usuarioEmpresaRolId);

        // Marcar este como principal
        uer.setEsPrincipal(true);
        usuarioEmpresaRolRepository.save(uer);

        log.info("Rol principal establecido para usuario {}: {}",
            uer.getUsuario().getEmail(), uer.getDescripcionCompleta());
    }

    @Override
    @Transactional
    public int transferirUsuariosSucursal(Long sucursalOrigenId, Long sucursalDestinoId)
        throws BadRequestException {

        // Validar sucursales
        Sucursal sucursalOrigen = sucursalRepository.findById(sucursalOrigenId)
            .orElseThrow(() -> new ResourceNotFoundException("Sucursal origen no encontrada"));

        Sucursal sucursalDestino = sucursalRepository.findById(sucursalDestinoId)
            .orElseThrow(() -> new ResourceNotFoundException("Sucursal destino no encontrada"));

        // Verificar que pertenecen a la misma empresa
        if (!sucursalOrigen.getEmpresa().getId().equals(sucursalDestino.getEmpresa().getId())) {
            throw new BadRequestException("Las sucursales deben pertenecer a la misma empresa");
        }

        // Obtener usuarios de la sucursal origen
        List<UsuarioEmpresaRol> usuariosTransferir = usuarioEmpresaRolRepository
            .findBySucursalId(sucursalOrigenId).stream()
            .filter(UsuarioEmpresaRol::getActivo)
            .collect(Collectors.toList());

        // Transferir cada usuario
        for (UsuarioEmpresaRol uer : usuariosTransferir) {
            uer.setSucursal(sucursalDestino);
            usuarioEmpresaRolRepository.save(uer);
        }

        int cantidad = usuariosTransferir.size();
        log.info("Transferidos {} usuarios de {} a {}",
            cantidad, sucursalOrigen.getNombre(), sucursalDestino.getNombre());

        return cantidad;
    }

    @Override
    @Transactional
    public int desactivarUsuariosMasivo(Long empresaId, Long sucursalId) {
        List<UsuarioEmpresaRol> rolesDesactivar;

        if (sucursalId != null) {
            rolesDesactivar = usuarioEmpresaRolRepository.findBySucursalId(sucursalId);
        } else {
            rolesDesactivar = usuarioEmpresaRolRepository.findByEmpresaId(empresaId);
        }

        // Filtrar solo los activos
        rolesDesactivar = rolesDesactivar.stream()
            .filter(UsuarioEmpresaRol::getActivo)
            .collect(Collectors.toList());

        // Desactivar cada rol
        for (UsuarioEmpresaRol uer : rolesDesactivar) {
            uer.setActivo(false);
            usuarioEmpresaRolRepository.save(uer);
        }

        int cantidad = rolesDesactivar.size();
        log.info("Desactivados {} roles en empresa {} sucursal {}",
            cantidad, empresaId, sucursalId);

        return cantidad;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean puedeGestionarUsuario(Long gestorId, Long usuarioId, Long empresaId) {
        // Si es el mismo usuario, no puede gestionarse
        if (gestorId.equals(usuarioId)) {
            return false;
        }

        // Obtener el usuario gestor
        Usuario gestor = usuarioRepository.findById(gestorId)
            .orElseThrow(() -> new ResourceNotFoundException("Gestor no encontrado"));

        // Si es ROOT o SOPORTE, puede gestionar a cualquiera
        if (gestor.esRolSistema()) {
            return true;
        }

        // Verificar que el gestor tiene acceso a la empresa
        boolean tieneAcceso = usuarioEmpresaRolRepository
            .existsByUsuarioIdAndEmpresaIdAndActivoTrue(gestorId, empresaId);

        if (!tieneAcceso) {
            return false;
        }

        // Obtener el usuario a gestionar
        Usuario usuarioGestionar = usuarioRepository.findById(usuarioId)
            .orElse(null);

        if (usuarioGestionar == null) {
            return true; // Puede crear nuevo usuario
        }

        // No puede gestionar usuarios con rol superior o igual
        return esRolSuperior(gestor.getRol(), usuarioGestionar.getRol());
    }

    // Métodos auxiliares
    private TipoUsuario determinarTipoUsuario(RolNombre rol) {
        switch (rol) {
            case ROOT:
            case SOPORTE:
                return TipoUsuario.SISTEMA;
            case SUPER_ADMIN:
                return TipoUsuario.EMPRESARIAL;
            case ADMIN:
                return TipoUsuario.GERENCIAL;
            default:
                return TipoUsuario.OPERATIVO;
        }
    }

    private String generarUsername(String nombre, String apellidos) {
        String base = nombre.toLowerCase().replaceAll("[^a-z0-9]", "");
        if (apellidos != null && !apellidos.isBlank()) {
            base += apellidos.substring(0, 1).toLowerCase();
        }

        // Agregar número si ya existe
        String username = base;
        int contador = 1;
        while (usuarioRepository.existsByUsername(username)) {
            username = base + contador;
            contador++;
        }

        return username;
    }

    private Long obtenerUsuarioActualId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof String) {
            try {
                return Long.parseLong((String) auth.getPrincipal());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private boolean esRolSuperior(RolNombre rolGestor, RolNombre rolUsuario) {
        // Jerarquía: ROOT > SOPORTE > SUPER_ADMIN > ADMIN > JEFE_CAJAS > CAJERO/MESERO/COCINA
        int nivelGestor = obtenerNivelRol(rolGestor);
        int nivelUsuario = obtenerNivelRol(rolUsuario);

        return nivelGestor > nivelUsuario;
    }

    private int obtenerNivelRol(RolNombre rol) {
        switch (rol) {
            case ROOT: return 6;
            case SOPORTE: return 5;
            case SUPER_ADMIN: return 4;
            case ADMIN: return 3;
            case JEFE_CAJAS: return 2;
            case CAJERO:
            case MESERO:
            case COCINA: return 1;
            default: return 0;
        }
    }
}