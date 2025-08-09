package com.snnsoluciones.backnathbitpos.service.usuario.impl;

import com.snnsoluciones.backnathbitpos.dto.usuario.*;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import com.snnsoluciones.backnathbitpos.enums.TipoUsuario;
import com.snnsoluciones.backnathbitpos.exception.*;
import com.snnsoluciones.backnathbitpos.mapper.UsuarioMapper;
import com.snnsoluciones.backnathbitpos.repository.*;
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
    private final UsuarioEmpresaRepository usuarioEmpresaRepository;
    private final EmpresaRepository empresaRepository;
    private final SucursalRepository sucursalRepository;
    private final UsuarioService usuarioService;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UsuarioDTO crearUsuario(CrearUsuarioRequest request) {
        // Validaciones de unicidad
        if (usuarioService.existePorEmail(request.getEmail())) {
            throw new ConflictException("Ya existe un usuario con el email: " + request.getEmail());
        }

        if (request.getEmail() != null && usuarioService.existePorEmail(request.getEmail())) {
            throw new ConflictException("Ya existe un usuario con el username: " + request.getEmail());
        }

        if (request.getIdentificacion() != null &&
            usuarioService.existePorIdentificacion(request.getIdentificacion())) {
            throw new ConflictException("Ya existe un usuario con la identificación: " +
                request.getIdentificacion());
        }

        // Verificar si hay usuario autenticado
        Usuario usuarioCreador = null;
        try {
            usuarioCreador = obtenerUsuarioActual();
        } catch (Exception e) {
            // Permitir crear el primer ROOT sin autenticación
            long totalUsuarios = usuarioRepository.count();
            if (totalUsuarios == 0 && request.getRol() == RolNombre.ROOT) {
                log.warn("Creando primer usuario ROOT del sistema sin autenticación");
            } else {
                throw new UnauthorizedException("Debe estar autenticado para crear usuarios");
            }
        }

        // Validar permisos de creación
        if (usuarioCreador != null) {
            validarPermisoCreacionUsuario(usuarioCreador.getRol(), request.getRol());
        }

        // Crear usuario con rol global
        Usuario nuevoUsuario = Usuario.builder()
            .email(request.getEmail())
            .username(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .nombre(request.getNombre())
            .apellidos(request.getApellidos())
            .telefono(request.getTelefono())
            .identificacion(request.getIdentificacion())
            .rol(request.getRol()) // ROL GLOBAL
            .tipoUsuario(determinarTipoUsuario(request.getRol()))
            .activo(true)
            .bloqueado(false)
            .intentosFallidos(0)
            .passwordTemporal(request.getPasswordTemporal() != null ? request.getPasswordTemporal() : false)
            .build();

        nuevoUsuario = usuarioRepository.save(nuevoUsuario);

        log.info("Usuario creado: {} con rol {} por {}",
            nuevoUsuario.getEmail(), nuevoUsuario.getRol(),
            usuarioCreador != null ? usuarioCreador.getEmail() : "SISTEMA");

        return usuarioMapper.toDto(nuevoUsuario);
    }

    @Override
    public UsuarioDTO actualizarUsuario(Long id, ActualizarUsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Email y username no se pueden cambiar
        if (!usuario.getEmail().equals(request.getEmail())) {
            throw new BadRequestException("El email no se puede cambiar");
        }

        // Validar identificación si cambió
        if (request.getIdentificacion() != null &&
            !request.getIdentificacion().equals(usuario.getIdentificacion()) &&
            usuarioService.existePorIdentificacion(request.getIdentificacion())) {
            throw new ConflictException("Ya existe un usuario con la identificación: " +
                request.getIdentificacion());
        }

        // Actualizar datos básicos
        usuario.setNombre(request.getNombre());
        usuario.setApellidos(request.getApellidos());
        usuario.setTelefono(request.getTelefono());
        usuario.setIdentificacion(request.getIdentificacion());

        usuario = usuarioRepository.save(usuario);

        log.info("Usuario actualizado: {}", usuario.getEmail());
        return usuarioMapper.toDto(usuario);
    }

    public UsuarioEmpresaDTO asignarEmpresaSucursal(Long usuarioId, Long empresaId,
        Long sucursalId, Map<String, Object> permisos) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Usuarios del sistema no necesitan asignaciones
        if (usuario.esRolSistema()) {
            throw new BadRequestException("Usuarios del sistema no requieren asignación a empresas");
        }

        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

        Sucursal sucursal = null;
        if (sucursalId != null) {
            sucursal = sucursalRepository.findById(sucursalId)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));

            if (!sucursal.getEmpresa().getId().equals(empresaId)) {
                throw new BadRequestException("La sucursal no pertenece a la empresa indicada");
            }
        }

        // Validar contexto según rol
        validarContextoParaRol(usuario.getRol(), empresa, sucursal);

        // Verificar si ya existe asignación
        Optional<UsuarioEmpresa> existente = sucursalId != null ?
            usuarioEmpresaRepository.findByUsuarioIdAndEmpresaIdAndSucursalId(usuarioId, empresaId, sucursalId) :
            usuarioEmpresaRepository.findByUsuarioIdAndEmpresaIdAndSucursalIdIsNull(usuarioId, empresaId);

        if (existente.isPresent() && existente.get().esAsignacionVigente()) {
            throw new ConflictException("El usuario ya tiene una asignación activa en ese contexto");
        }

        // Crear o reactivar asignación
        UsuarioEmpresa usuarioEmpresa = existente.orElse(new UsuarioEmpresa());
        usuarioEmpresa.setUsuario(usuario);
        usuarioEmpresa.setEmpresa(empresa);
        usuarioEmpresa.setSucursal(sucursal);
        usuarioEmpresa.setActivo(true);
        usuarioEmpresa.setFechaAsignacion(LocalDateTime.now());
        usuarioEmpresa.setAsignadoPor(obtenerUsuarioActualId());
        usuarioEmpresa.setFechaRevocacion(null);
        usuarioEmpresa.setRevocadoPor(null);

        usuarioEmpresa = usuarioEmpresaRepository.save(usuarioEmpresa);

        log.info("Usuario {} asignado a empresa {} - sucursal {}",
            usuario.getEmail(), empresa.getNombre(),
            sucursal != null ? sucursal.getNombre() : "TODAS");

        return mapearUsuarioEmpresaDTO(usuarioEmpresa);
    }

    public void removerAsignacion(Long usuarioId, Long usuarioEmpresaId) {
        UsuarioEmpresa asignacion = usuarioEmpresaRepository.findById(usuarioEmpresaId)
            .orElseThrow(() -> new ResourceNotFoundException("Asignación no encontrada"));

        // Verificar que la asignación pertenece al usuario
        if (!asignacion.getUsuario().getId().equals(usuarioId)) {
            throw new BadRequestException("La asignación no pertenece al usuario indicado");
        }

        // Revocar asignación
        asignacion.revocarAcceso(obtenerUsuarioActualId());
        usuarioEmpresaRepository.save(asignacion);

        log.info("Asignación removida para usuario {} en empresa {}",
            asignacion.getUsuario().getEmail(),
            asignacion.getEmpresa().getNombre());
    }

    @Override
    public UsuarioDTO asignarRol(Long usuarioId, RolNombre nuevoRol) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        RolNombre rolAnterior = usuario.getRol();

        // Validaciones
        if (usuario.esRolSistema()) {
            throw new BadRequestException("No se puede cambiar el rol de usuarios del sistema");
        }

        Usuario usuarioActual = obtenerUsuarioActual();
        if (!usuarioActual.getRol().puedeCrear(nuevoRol)) {
            throw new UnauthorizedException("No tiene permisos para asignar el rol " + nuevoRol);
        }

        // Cambiar rol
        usuario.setRol(nuevoRol);
        usuario.setTipoUsuario(determinarTipoUsuario(nuevoRol));

        // Si cambia a rol operativo, verificar que tenga sucursal asignada
        if (nuevoRol.esOperativo()) {
            boolean tieneSucursal = usuario.getUsuarioEmpresas().stream()
                .anyMatch(ue -> ue.esAsignacionVigente() && ue.getSucursal() != null);

            if (!tieneSucursal) {
                log.warn("Usuario {} cambiado a rol operativo {} sin sucursal asignada",
                    usuario.getEmail(), nuevoRol);
            }
        }

        usuario = usuarioRepository.save(usuario);

        log.info("Rol de usuario {} cambiado de {} a {}",
            usuario.getEmail(), rolAnterior, nuevoRol);

        return usuarioMapper.toDto(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UsuarioDTO> listarUsuarios(Long empresaId, Long sucursalId,
        String rol, Boolean activo,
        String search, Pageable pageable) {
        RolNombre rolNombre = rol != null ? RolNombre.valueOf(rol) : null;
        return usuarioService.listarUsuarios(empresaId, sucursalId, search, rolNombre, pageable);
    }

    @Transactional(readOnly = true)
    public List<UsuarioEmpresaDTO> obtenerAsignacionesUsuario(Long usuarioId) {
        Usuario usuario = usuarioRepository.findByIdWithEmpresas(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        return usuario.getUsuarioEmpresas().stream()
            .filter(UsuarioEmpresa::esAsignacionVigente)
            .map(this::mapearUsuarioEmpresaDTO)
            .collect(Collectors.toList());
    }

    public UsuarioEmpresaDTO actualizarPermisos(Long usuarioEmpresaId, Map<String, Object> permisos) {
        UsuarioEmpresa asignacion = usuarioEmpresaRepository.findById(usuarioEmpresaId)
            .orElseThrow(() -> new ResourceNotFoundException("Asignación no encontrada"));

        asignacion = usuarioEmpresaRepository.save(asignacion);

        log.info("Permisos actualizados para usuario {} en empresa {}",
            asignacion.getUsuario().getEmail(),
            asignacion.getEmpresa().getNombre());

        return mapearUsuarioEmpresaDTO(asignacion);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean puedeGestionarUsuario(Long gestorId, Long usuarioId, Long empresaId) {
        Usuario gestor = usuarioRepository.findById(gestorId)
            .orElseThrow(() -> new ResourceNotFoundException("Gestor no encontrado"));

        // ROOT y SOPORTE pueden gestionar a cualquiera
        if (gestor.esRolSistema()) {
            return true;
        }

        // SUPER_ADMIN puede gestionar usuarios de sus empresas
        if (gestor.getRol() == RolNombre.SUPER_ADMIN) {
            return usuarioEmpresaRepository.existsByUsuarioIdAndEmpresaIdAndActivoTrue(gestorId, empresaId) &&
                usuarioEmpresaRepository.existsByUsuarioIdAndEmpresaIdAndActivoTrue(usuarioId, empresaId);
        }

        // ADMIN puede gestionar usuarios de su empresa
        if (gestor.getRol() == RolNombre.ADMIN) {
            return usuarioEmpresaRepository.existsByUsuarioIdAndEmpresaIdAndActivoTrue(usuarioId, empresaId);
        }

        // JEFE_CAJAS puede ver usuarios de su sucursal (pero no gestionar)
        return false;
    }

    // Métodos auxiliares privados

    private void validarPermisoCreacionUsuario(RolNombre rolCreador, RolNombre rolACrear) {
        if (!rolCreador.puedeCrear(rolACrear)) {
            String mensaje = String.format(
                "Un usuario con rol %s no puede crear usuarios con rol %s",
                rolCreador, rolACrear
            );
            throw new UnauthorizedException(mensaje);
        }
    }

    private void validarContextoParaRol(RolNombre rol, Empresa empresa, Sucursal sucursal) {
        // SUPER_ADMIN y ADMIN deben tener acceso a nivel empresa
        if ((rol == RolNombre.SUPER_ADMIN || rol == RolNombre.ADMIN) && sucursal != null) {
            throw new BadRequestException(rol + " debe tener acceso a nivel empresa, no sucursal específica");
        }

        // Roles operativos deben tener sucursal específica
        if (rol.esOperativo() && sucursal == null) {
            throw new BadRequestException("Roles operativos deben estar asignados a una sucursal específica");
        }
    }

    private TipoUsuario determinarTipoUsuario(RolNombre rol) {
        return (rol == RolNombre.ROOT || rol == RolNombre.SOPORTE) ?
            TipoUsuario.SISTEMA : TipoUsuario.EMPRESARIAL;
    }

    private Usuario obtenerUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("No hay usuario autenticado");
        }

        String username = auth.getName();
        return usuarioRepository.findByEmailOrUsername(username, username)
            .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));
    }

    private Long obtenerUsuarioActualId() {
        return obtenerUsuarioActual().getId();
    }

    private UsuarioEmpresaDTO mapearUsuarioEmpresaDTO(UsuarioEmpresa ue) {
        return UsuarioEmpresaDTO.builder()
            .id(ue.getId())
            .usuarioId(ue.getUsuario().getId())
            .usuarioNombre(ue.getUsuario().getNombre())
            .usuarioEmail(ue.getUsuario().getEmail())
            .empresaId(ue.getEmpresa().getId())
            .empresaNombre(ue.getEmpresa().getNombre())
            .empresaCodigo(ue.getEmpresa().getCodigo())
            .sucursalId(ue.getSucursal() != null ? ue.getSucursal().getId() : null)
            .sucursalNombre(ue.getSucursal() != null ? ue.getSucursal().getNombre() : null)
            .sucursalCodigo(ue.getSucursal() != null ? ue.getSucursal().getCodigo() : null)
            .accesoTodasSucursales(ue.tieneAccesoTodasSucursales())
            .activo(ue.getActivo())
            .fechaAsignacion(ue.getFechaAsignacion())
            .fechaRevocacion(ue.getFechaRevocacion())
            .notas(ue.getNotas())
            .build();
    }
}