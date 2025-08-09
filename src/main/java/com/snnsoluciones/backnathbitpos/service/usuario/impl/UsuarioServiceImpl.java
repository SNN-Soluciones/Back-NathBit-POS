package com.snnsoluciones.backnathbitpos.service.usuario.impl;

import com.snnsoluciones.backnathbitpos.dto.usuario.AccesoDTO;
import com.snnsoluciones.backnathbitpos.dto.usuario.CrearUsuarioRequest;
import com.snnsoluciones.backnathbitpos.dto.usuario.UsuarioDTO;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.entity.UsuarioEmpresa;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import com.snnsoluciones.backnathbitpos.enums.TipoUsuario;
import com.snnsoluciones.backnathbitpos.exception.BadRequestException;
import com.snnsoluciones.backnathbitpos.exception.ConflictException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.mapper.UsuarioMapper;
import com.snnsoluciones.backnathbitpos.repository.UsuarioEmpresaRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import com.snnsoluciones.backnathbitpos.service.usuario.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioEmpresaRepository usuarioEmpresaRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Usuario findByEmail(String email) {
        return usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con email: " + email));
    }

    @Override
    public UsuarioDTO findById(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
        return usuarioMapper.toDto(usuario);
    }

    @Override
    public List<AccesoDTO> obtenerAccesos(Long usuarioId) {
        // Obtener usuario con sus relaciones
        Usuario usuario = usuarioRepository.findByIdWithEmpresas(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Si es usuario del sistema, no tiene accesos empresariales
        if (usuario.esRolSistema()) {
            return List.of();
        }

        // Mapear accesos desde UsuarioEmpresa
        return usuario.getUsuarioEmpresas().stream()
            .filter(UsuarioEmpresa::esAsignacionVigente)
            .map(this::mapearAcceso)
            .collect(Collectors.toList());
    }

    @Override
    public boolean validarAcceso(Long usuarioId, Long empresaId, Long sucursalId) {
        // Obtener usuario
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Usuarios del sistema tienen acceso total
        if (usuario.esRolSistema()) {
            return true;
        }

        // Validar acceso específico
        if (sucursalId != null) {
            // Verificar acceso a sucursal específica
            return usuarioEmpresaRepository
                .findByUsuarioIdAndEmpresaIdAndSucursalId(usuarioId, empresaId, sucursalId)
                .map(UsuarioEmpresa::esAsignacionVigente)
                .orElse(false);
        } else {
            // Verificar acceso a nivel empresa
            return usuarioEmpresaRepository
                .findByUsuarioIdAndEmpresaIdAndSucursalIdIsNull(usuarioId, empresaId)
                .map(UsuarioEmpresa::esAsignacionVigente)
                .orElse(false);
        }
    }

    @Override
    @Transactional
    public UsuarioDTO crearUsuario(CrearUsuarioRequest request) {
        // Validar que no exista email
        if (existePorEmail(request.getEmail())) {
            throw new ConflictException("Ya existe un usuario con el email: " + request.getEmail());
        }

        // Validar username si se proporciona
        if (request.getEmail() != null && existePorUsername(request.getEmail())) {
            throw new ConflictException("Ya existe un usuario con el username: " + request.getEmail());
        }

        // Validar identificación si se proporciona
        if (request.getIdentificacion() != null &&
            existePorIdentificacion(request.getIdentificacion())) {
            throw new ConflictException("Ya existe un usuario con la identificación: " +
                request.getIdentificacion());
        }

        // Crear usuario
        Usuario usuario = Usuario.builder()
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
            .build();

        usuario = usuarioRepository.save(usuario);

        log.info("Usuario creado: {} con rol: {}", usuario.getEmail(), usuario.getRol());
        return usuarioMapper.toDto(usuario);
    }

    @Override
    @Transactional
    public UsuarioDTO actualizarUsuario(Long id, UsuarioDTO usuarioDTO) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));

        // El email y username no se pueden cambiar
        if (!usuario.getEmail().equals(usuarioDTO.getEmail())) {
            throw new BadRequestException("El email no se puede cambiar");
        }

        // Validar identificación si cambió
        if (usuarioDTO.getIdentificacion() != null &&
            !usuarioDTO.getIdentificacion().equals(usuario.getIdentificacion()) &&
            existePorIdentificacion(usuarioDTO.getIdentificacion())) {
            throw new ConflictException("Ya existe un usuario con la identificación: " +
                usuarioDTO.getIdentificacion());
        }

        // Actualizar campos permitidos
        usuarioMapper.updateEntityFromDto(usuarioDTO, usuario);

        usuario = usuarioRepository.save(usuario);

        log.info("Usuario actualizado: {}", usuario.getEmail());
        return usuarioMapper.toDto(usuario);
    }

    @Override
    public Page<UsuarioDTO> listarUsuarios(Long empresaId, Long sucursalId,
        String search, RolNombre rol, Pageable pageable) {
        Page<Usuario> usuarios;

        // Aplicar filtros según los parámetros
        usuarios = usuarioRepository.buscarUsuarios(empresaId, sucursalId, search, rol, pageable);

        // Mapear a DTOs
        return usuarios.map(usuario -> {
            UsuarioDTO dto = usuarioMapper.toDto(usuario);

            // Agregar contadores de asignaciones si es necesario
            if (!usuario.esRolSistema()) {
                dto.setEmpresasAsignadas(usuario.getUsuarioEmpresas().size());
                dto.setSucursalesAsignadas(
                    (int) usuario.getUsuarioEmpresas().stream()
                        .filter(ue -> ue.getSucursal() != null)
                        .count()
                );
            }

            return dto;
        });
    }

    @Override
    @Transactional
    public UsuarioDTO cambiarEstadoUsuario(Long id, boolean activo) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));

        // No se puede desactivar usuarios del sistema
        if (!activo && usuario.esRolSistema()) {
            throw new BadRequestException("No se puede desactivar usuarios del sistema");
        }

        usuario.setActivo(activo);

        // Si se desactiva, también desactivar sus asignaciones
        if (!activo) {
            usuarioEmpresaRepository.revocarTodasLasAsignaciones(id, obtenerUsuarioActualId());
        }

        usuario = usuarioRepository.save(usuario);

        log.info("Estado de usuario {} cambiado a: {}", usuario.getEmail(), activo);
        return usuarioMapper.toDto(usuario);
    }

    @Override
    @Transactional
    public void actualizarUltimoAcceso(Long usuarioId) {
        usuarioRepository.actualizarUltimoAcceso(usuarioId, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void cambiarPassword(Long usuarioId, String passwordActual, String passwordNueva) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + usuarioId));

        // Verificar password actual
        if (!passwordEncoder.matches(passwordActual, usuario.getPassword())) {
            throw new BadRequestException("La contraseña actual es incorrecta");
        }

        // Actualizar password
        usuario.setPassword(passwordEncoder.encode(passwordNueva));
        usuario.setUltimoCambioPassword(LocalDateTime.now());
        usuario.setPasswordTemporal(false);

        usuarioRepository.save(usuario);

        log.info("Contraseña cambiada para usuario: {}", usuario.getEmail());
    }

    @Transactional
    public void cambiarRol(Long usuarioId, RolNombre nuevoRol) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        RolNombre rolAnterior = usuario.getRol();

        // Validar cambio de rol
        if (usuario.esRolSistema()) {
            throw new BadRequestException("No se puede cambiar el rol de usuarios del sistema");
        }

        // Actualizar rol
        usuario.setRol(nuevoRol);
        usuario.setTipoUsuario(determinarTipoUsuario(nuevoRol));

        usuarioRepository.save(usuario);

        log.info("Rol de usuario {} cambiado de {} a {}",
            usuario.getEmail(), rolAnterior, nuevoRol);
    }

    @Override
    public boolean existePorEmail(String email) {
        return usuarioRepository.existsByEmail(email);
    }

    public boolean existePorUsername(String username) {
        return username != null && usuarioRepository.existsByUsername(username);
    }

    @Override
    public boolean existePorIdentificacion(String identificacion) {
        return identificacion != null && usuarioRepository.existsByIdentificacion(identificacion);
    }

    // Métodos auxiliares privados

    private AccesoDTO mapearAcceso(UsuarioEmpresa ue) {
        AccesoDTO acceso = new AccesoDTO();

        // Mapear empresa
        AccesoDTO.EmpresaInfo empresaInfo = new AccesoDTO.EmpresaInfo();
        empresaInfo.setId(ue.getEmpresa().getId());
        empresaInfo.setNombre(ue.getEmpresa().getNombre());
        empresaInfo.setCodigo(ue.getEmpresa().getCodigo());
        empresaInfo.setLogoUrl(ue.getEmpresa().getLogoUrl());
        acceso.setEmpresa(empresaInfo);

        // Mapear sucursal si existe
        if (ue.getSucursal() != null) {
            AccesoDTO.SucursalInfo sucursalInfo = new AccesoDTO.SucursalInfo();
            sucursalInfo.setId(ue.getSucursal().getId());
            sucursalInfo.setNombre(ue.getSucursal().getNombre());
            sucursalInfo.setCodigo(ue.getSucursal().getCodigo());
            acceso.setSucursal(sucursalInfo);
        }

        // El rol ahora está en el usuario, no en la relación
        acceso.setRol(ue.getUsuario().getRol());
        acceso.setPermisos(ue.getPermisos());
        acceso.setAccesoTodasSucursales(ue.tieneAccesoTodasSucursales());

        return acceso;
    }

    private TipoUsuario determinarTipoUsuario(RolNombre rol) {
        if (rol == RolNombre.ROOT || rol == RolNombre.SOPORTE) {
            return TipoUsuario.SISTEMA;
        }
        return TipoUsuario.EMPRESARIAL;
    }

    private Long obtenerUsuarioActualId() {
        // TODO: Obtener del contexto de seguridad
        return 1L;
    }
}