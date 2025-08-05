package com.snnsoluciones.backnathbitpos.service.admin.impl;

import com.snnsoluciones.backnathbitpos.config.tenant.TenantContext;
import com.snnsoluciones.backnathbitpos.dto.request.AsignarSucursalesRequest;
import com.snnsoluciones.backnathbitpos.dto.request.CambioPasswordRequest;
import com.snnsoluciones.backnathbitpos.dto.request.UsuarioCreateRequest;
import com.snnsoluciones.backnathbitpos.dto.request.UsuarioUpdateRequest;
import com.snnsoluciones.backnathbitpos.dto.response.AuditEventResponse;
import com.snnsoluciones.backnathbitpos.dto.response.UsuarioResponse;
import com.snnsoluciones.backnathbitpos.entity.global.*;
import com.snnsoluciones.backnathbitpos.entity.operacion.AsignacionCajas;
import com.snnsoluciones.backnathbitpos.entity.security.AuditEvent;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.exception.UnauthorizedException;
import com.snnsoluciones.backnathbitpos.mapper.UsuarioMapper;
import com.snnsoluciones.backnathbitpos.repository.AsignacionCajasRepository;
import com.snnsoluciones.backnathbitpos.repository.AuditEventRepository;
import com.snnsoluciones.backnathbitpos.repository.global.*;
import com.snnsoluciones.backnathbitpos.service.admin.UsuarioService;
import com.snnsoluciones.backnathbitpos.util.ContextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioGlobalRepository usuarioGlobalRepository;
    private final UsuarioEmpresaRepository usuarioEmpresaRepository;
    private final UsuarioSucursalRepository usuarioSucursalRepository;
    private final EmpresaRepository empresaRepository;
    private final EmpresaSucursalRepository empresaSucursalRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;
    private final CacheManager cacheManager;
    private final AuditEventRepository auditEventRepository;
    private final AsignacionCajasRepository asignacionCajasRepository;

    @Value("${app.security.max-intentos-login:3}")
    private int maxIntentosLogin;

    @Override
    public UsuarioResponse crear(UsuarioCreateRequest request) {
        log.info("Creando nuevo usuario con email: {} para empresa: {}",
            request.getEmail(), request.getEmpresaId());

        // 1. Verificar que el email no exista globalmente
        if (usuarioGlobalRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Ya existe un usuario con el email: " + request.getEmail());
        }

        // 2. Validar y encriptar contraseña
        validarPasswordSegura(request.getPassword());
        String passwordEncriptado = passwordEncoder.encode(request.getPassword());

        // 3. Crear usuario global
        UsuarioGlobal usuario = usuarioMapper.toEntity(request);
        usuario.setPassword(passwordEncriptado);
        usuario.setDebeCambiarPassword(request.getDebeCambiarPassword());

        usuario = usuarioGlobalRepository.save(usuario);

        // 4. Asignar a empresa con rol
        Empresa empresa = empresaRepository.findById(request.getEmpresaId())
            .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

        UsuarioEmpresa usuarioEmpresa = UsuarioEmpresa.builder()
            .usuario(usuario)
            .empresa(empresa)
            .rol(request.getRol())
            .esPropietario(request.getEsPropietario())
            .activo(true)
            .asignadoPor(getCurrentUser())
            .build();

        usuarioEmpresaRepository.save(usuarioEmpresa);

        // 5. Asignar a sucursales si se especificaron
        if (request.getSucursalesIds() != null && !request.getSucursalesIds().isEmpty()) {
            asignarSucursalesInicial(usuario, empresa, request);
        }

        log.info("Usuario creado exitosamente con ID: {}", usuario.getId());

        // 6. Retornar con contexto de empresa
        return usuarioMapper.toResponseWithContext(usuario, request.getEmpresaId());
    }

    @Override
    public UsuarioResponse actualizar(UUID id, UsuarioUpdateRequest request) {
        log.info("Actualizando usuario con ID: {}", id);

        UsuarioGlobal usuario = usuarioGlobalRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Verificar acceso a este usuario
        if (!canManageUser(id)) {
            throw new UnauthorizedException("No tiene permisos para actualizar este usuario");
        }

        // Verificar si el email cambió y no está duplicado
        if (request.getEmail() != null && !usuario.getEmail().equals(request.getEmail())) {
            if (usuarioGlobalRepository.existsByEmail(request.getEmail())) {
                throw new BusinessException("Ya existe un usuario con el email: " + request.getEmail());
            }
        }

        // Actualizar datos básicos
        usuarioMapper.updateEntity(usuario, request);
        usuario = usuarioGlobalRepository.save(usuario);

        log.info("Usuario actualizado exitosamente");

        // Retornar con contexto actual
        UUID empresaId = ContextUtils.getCurrentEmpresaId();
        return usuarioMapper.toResponseWithContext(usuario, empresaId);
    }

    @Override
    public void cambiarPassword(UUID id, CambioPasswordRequest request) {
        log.info("Cambiando contraseña para usuario ID: {}", id);

        UsuarioGlobal usuario = usuarioGlobalRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Verificar contraseña actual
        if (!passwordEncoder.matches(request.getPasswordActual(), usuario.getPassword())) {
            throw new BusinessException("La contraseña actual es incorrecta");
        }

        // Verificar que la nueva contraseña sea diferente
        if (passwordEncoder.matches(request.getPasswordNueva(), usuario.getPassword())) {
            throw new BusinessException("La nueva contraseña debe ser diferente a la actual");
        }

        // Verificar que las contraseñas coincidan
        if (!request.getPasswordNueva().equals(request.getPasswordConfirmacion())) {
            throw new BusinessException("Las contraseñas no coinciden");
        }

        // Validar y actualizar contraseña
        validarPasswordSegura(request.getPasswordNueva());
        usuario.setPassword(passwordEncoder.encode(request.getPasswordNueva()));
        usuario.setDebeCambiarPassword(false);
        usuario.setFechaPasswordExpira(LocalDateTime.now().plusDays(90)); // 90 días

        usuarioGlobalRepository.save(usuario);

        log.info("Contraseña cambiada exitosamente");
    }

    @Override
    public void bloquearUsuario(UUID id) {
        log.info("Bloqueando usuario ID: {}", id);

        UsuarioGlobal usuario = usuarioGlobalRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (!canManageUser(id)) {
            throw new UnauthorizedException("No tiene permisos para bloquear este usuario");
        }

        usuario.setBloqueado(true);
        usuarioGlobalRepository.save(usuario);

        // Invalidar cache
        invalidateUserCache(usuario.getEmail());

        log.info("Usuario bloqueado exitosamente");
    }

    @Override
    public void desbloquearUsuario(UUID id) {
        log.info("Desbloqueando usuario ID: {}", id);

        UsuarioGlobal usuario = usuarioGlobalRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (!canManageUser(id)) {
            throw new UnauthorizedException("No tiene permisos para desbloquear este usuario");
        }

        usuario.setBloqueado(false);
        usuario.setIntentosFallidos(0);
        usuarioGlobalRepository.save(usuario);

        log.info("Usuario desbloqueado exitosamente");
    }

    @Override
    public void resetearIntentosFallidos(String email) {
        log.debug("Reseteando intentos fallidos para: {}", email);

        usuarioGlobalRepository.findByEmail(email).ifPresent(usuario -> {
            usuario.setIntentosFallidos(0);
            usuarioGlobalRepository.save(usuario);
        });
    }

    @Override
    public void manejarLoginExitoso(String email) {
        log.debug("Manejando login exitoso para: {}", email);

        usuarioGlobalRepository.findByEmail(email).ifPresent(usuario -> {
            usuario.setIntentosFallidos(0);
            usuario.setUltimoAcceso(LocalDateTime.now());
            usuarioGlobalRepository.save(usuario);
        });
    }

    @Override
    public void manejarLoginFallido(String email) {
        log.debug("Manejando login fallido para: {}", email);

        usuarioGlobalRepository.findByEmail(email).ifPresent(usuario -> {
            int intentos = usuario.getIntentosFallidos() + 1;
            usuario.setIntentosFallidos(intentos);

            if (intentos >= maxIntentosLogin) {
                usuario.setBloqueado(true);
                log.warn("Usuario {} bloqueado por exceder intentos de login", email);
            }

            usuarioGlobalRepository.save(usuario);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorId(UUID id) {
        UsuarioGlobal usuario = usuarioGlobalRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        UUID empresaId = ContextUtils.getCurrentEmpresaId();
        return usuarioMapper.toResponseWithContext(usuario, empresaId);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorIdConContexto(UUID id, UUID empresaId) {
        UsuarioGlobal usuario = usuarioGlobalRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Verificar que el usuario tiene acceso a la empresa
        if (!tieneAccesoEmpresa(id, empresaId)) {
            throw new UnauthorizedException("Usuario no tiene acceso a esta empresa");
        }

        return usuarioMapper.toResponseWithContext(usuario, empresaId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listar(String rol, UUID sucursalId, String search,
        boolean incluirInactivos, Pageable pageable) {
        UUID empresaId = ContextUtils.getCurrentEmpresaId();
        if (empresaId == null) {
            throw new BusinessException("No se ha establecido el contexto de empresa");
        }

        Specification<UsuarioGlobal> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filtrar por empresa actual
            Join<UsuarioGlobal, UsuarioEmpresa> joinEmpresa = root.join("usuarioEmpresas", JoinType.INNER);
            predicates.add(cb.equal(joinEmpresa.get("empresa").get("id"), empresaId));
            if (!incluirInactivos) {
                predicates.add(cb.isTrue(joinEmpresa.get("activo")));
            }

            // Filtrar por rol si se especifica
            if (rol != null && !rol.isEmpty()) {
                predicates.add(cb.equal(joinEmpresa.get("rol"), RolNombre.valueOf(rol)));
            }

            // Filtrar por sucursal si se especifica
            if (sucursalId != null) {
                Join<UsuarioEmpresa, UsuarioSucursal> joinSucursal = joinEmpresa.join("usuarioSucursales", JoinType.INNER);
                predicates.add(cb.equal(joinSucursal.get("sucursal").get("id"), sucursalId));
                predicates.add(cb.isTrue(joinSucursal.get("activo")));
            }

            // Buscar por nombre o email
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("email")), searchPattern),
                    cb.like(cb.lower(root.get("nombre")), searchPattern),
                    cb.like(cb.lower(root.get("apellidos")), searchPattern)
                ));
            }

            // Solo usuarios activos globalmente (a menos que se incluyan inactivos)
            if (!incluirInactivos) {
                predicates.add(cb.isTrue(root.get("activo")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<UsuarioGlobal> usuarios = usuarioGlobalRepository.findAll(spec, pageable);

        return usuarios.map(u -> usuarioMapper.toResponseWithContext(u, empresaId));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeEmail(String email) {
        return usuarioGlobalRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeEmailEnEmpresa(String email, UUID empresaId) {
        Optional<UsuarioGlobal> usuario = usuarioGlobalRepository.findByEmail(email);
        if (usuario.isEmpty()) {
            return false;
        }

        return usuario.get().getUsuarioEmpresas().stream()
            .anyMatch(ue -> ue.getEmpresa().getId().equals(empresaId) && ue.getActivo());
    }

    @Override
    public void desactivar(UUID id) {
        log.info("Desactivando usuario ID: {}", id);

        UsuarioGlobal usuario = usuarioGlobalRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (!canManageUser(id)) {
            throw new UnauthorizedException("No tiene permisos para desactivar este usuario");
        }

        // Desactivar en la empresa actual
        UUID empresaId = ContextUtils.getCurrentEmpresaId();
        UsuarioEmpresa usuarioEmpresa = usuario.getUsuarioEmpresas().stream()
            .filter(ue -> ue.getEmpresa().getId().equals(empresaId))
            .findFirst()
            .orElseThrow(() -> new BusinessException("Usuario no tiene acceso a esta empresa"));

        usuarioEmpresa.setActivo(false);
        usuarioEmpresaRepository.save(usuarioEmpresa);

        log.info("Usuario desactivado en la empresa actual");
    }

    @Override
    public void reactivar(UUID id) {
        log.info("Reactivando usuario ID: {}", id);

        UsuarioGlobal usuario = usuarioGlobalRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (!canManageUser(id)) {
            throw new UnauthorizedException("No tiene permisos para reactivar este usuario");
        }

        // Reactivar en la empresa actual
        UUID empresaId = ContextUtils.getCurrentEmpresaId();
        UsuarioEmpresa usuarioEmpresa = usuario.getUsuarioEmpresas().stream()
            .filter(ue -> ue.getEmpresa().getId().equals(empresaId))
            .findFirst()
            .orElseThrow(() -> new BusinessException("Usuario no tiene acceso a esta empresa"));

        usuarioEmpresa.setActivo(true);
        usuarioEmpresaRepository.save(usuarioEmpresa);

        log.info("Usuario reactivado en la empresa actual");
    }

    @Override
    @Transactional
    public UsuarioResponse cambiarRol(UUID userId, String nuevoRol) {
        log.info("Cambiando rol de usuario {} a {}", userId, nuevoRol);

        UsuarioGlobal usuario = usuarioGlobalRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        UUID empresaId = ContextUtils.getCurrentEmpresaId();
        UsuarioEmpresa usuarioEmpresa = usuario.getUsuarioEmpresas().stream()
            .filter(ue -> ue.getEmpresa().getId().equals(empresaId))
            .findFirst()
            .orElseThrow(() -> new BusinessException("Usuario no tiene acceso a esta empresa"));

        RolNombre rolAnterior = usuarioEmpresa.getRol();
        usuarioEmpresa.setRol(RolNombre.valueOf(nuevoRol));
        usuarioEmpresaRepository.save(usuarioEmpresa);

        log.info("Rol cambiado exitosamente de {} a {}", rolAnterior, nuevoRol);
        return null;
    }

    @Override
    @Transactional
    public void asignarSucursales(UUID userId, AsignarSucursalesRequest request) {
        log.info("Asignando {} sucursales a usuario {}", request.getSucursalesIds().size(), userId);

        UsuarioGlobal usuario = usuarioGlobalRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        UUID empresaId = ContextUtils.getCurrentEmpresaId();
        UsuarioEmpresa usuarioEmpresa = usuario.getUsuarioEmpresas().stream()
            .filter(ue -> ue.getEmpresa().getId().equals(empresaId))
            .findFirst()
            .orElseThrow(() -> new BusinessException("Usuario no tiene acceso a esta empresa"));

        // Limpiar sucursales actuales
        usuarioEmpresa.getUsuarioSucursales().clear();

        // Asignar nuevas sucursales
        for (UUID sucursalId : request.getSucursalesIds()) {
            EmpresaSucursal sucursal = empresaSucursalRepository.findById(sucursalId)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada: " + sucursalId));

            // Verificar que la sucursal pertenece a la empresa
            if (!sucursal.getEmpresa().getId().equals(empresaId)) {
                throw new BusinessException("La sucursal no pertenece a esta empresa: " + sucursalId);
            }

            AsignarSucursalesRequest.PermisosSucursal permisos = request.getPermisosParaSucursal(sucursalId);

            UsuarioSucursal usuarioSucursal = UsuarioSucursal.builder()
                .usuarioId(usuario.getId())
                .sucursal(sucursal)
                .empresaId(empresaId)
                .puedeLeer(permisos.getPuedeLeer())
                .puedeEscribir(permisos.getPuedeEscribir())
                .puedeEliminar(permisos.getPuedeEliminar())
                .puedeAprobar(permisos.getPuedeAprobar())
                .esPrincipal(sucursalId.equals(request.getSucursalPrincipalId()))
                .activo(true)
                .asignadoPor(getCurrentUser())
                .build();

            usuarioSucursalRepository.save(usuarioSucursal);
        }

        log.info("Sucursales asignadas exitosamente");
    }

    @Override
    @Transactional
    public void asignarEmpresas(UUID userId, Map<UUID, RolNombre> empresasRoles) {
        log.info("Asignando usuario {} a {} empresas", userId, empresasRoles.size());

        UsuarioGlobal usuario = usuarioGlobalRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        for (Map.Entry<UUID, RolNombre> entry : empresasRoles.entrySet()) {
            UUID empresaId = entry.getKey();
            RolNombre rol = entry.getValue();

            // Verificar si ya existe la relación
            boolean existeRelacion = usuario.getUsuarioEmpresas().stream()
                .anyMatch(ue -> ue.getEmpresa().getId().equals(empresaId));

            if (!existeRelacion) {
                Empresa empresa = empresaRepository.findById(empresaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada: " + empresaId));

                UsuarioEmpresa usuarioEmpresa = UsuarioEmpresa.builder()
                    .usuario(usuario)
                    .empresa(empresa)
                    .rol(rol)
                    .activo(true)
                    .asignadoPor(getCurrentUser())
                    .build();

                usuarioEmpresaRepository.save(usuarioEmpresa);
            }
        }

        log.info("Empresas asignadas exitosamente");
    }

    @Override
    @Transactional
    public void removerAccesoEmpresa(UUID userId, UUID empresaId) {
        log.info("Removiendo acceso de usuario {} a empresa {}", userId, empresaId);

        UsuarioGlobal usuario = usuarioGlobalRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        UsuarioEmpresa usuarioEmpresa = usuario.getUsuarioEmpresas().stream()
            .filter(ue -> ue.getEmpresa().getId().equals(empresaId))
            .findFirst()
            .orElseThrow(() -> new BusinessException("Usuario no tiene acceso a esta empresa"));

        // Desactivar en lugar de eliminar (para mantener historial)
        usuarioEmpresa.setActivo(false);
        usuarioEmpresaRepository.save(usuarioEmpresa);

        log.info("Acceso removido exitosamente");
    }

    @Override
    @Transactional
    public void removerAccesoSucursal(UUID userId, UUID sucursalId) {
        log.info("Removiendo acceso de usuario {} a sucursal {}", userId, sucursalId);

        EmpresaSucursal sucursal = empresaSucursalRepository.findById(sucursalId)
            .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));

        UsuarioSucursal usuarioSucursal = usuarioSucursalRepository
            .findByUsuarioIdAndSucursalId(userId, sucursalId)
            .orElseThrow(() -> new BusinessException("Usuario no tiene acceso a esta sucursal"));

        usuarioSucursal.setActivo(false);
        usuarioSucursalRepository.save(usuarioSucursal);

        log.info("Acceso a sucursal removido exitosamente");
    }

    @Override
    public void resetearIntentos(UUID userId) {
        log.info("Reseteando intentos fallidos para usuario {}", userId);

        UsuarioGlobal usuario = usuarioGlobalRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        usuario.setIntentosFallidos(0);
        usuario.setBloqueado(false);
        usuarioGlobalRepository.save(usuario);

        log.info("Intentos reseteados exitosamente");
    }

    @Override
    public void forzarCambioPassword(UUID userId) {
        log.info("Forzando cambio de contraseña para usuario {}", userId);

        UsuarioGlobal usuario = usuarioGlobalRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        usuario.setDebeCambiarPassword(true);
        usuarioGlobalRepository.save(usuario);

        log.info("Cambio de contraseña marcado como obligatorio");
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEventResponse> obtenerHistorialLogin(UUID userId, int page, int size) {
        log.info("Obteniendo historial de login para usuario {}", userId);

        UsuarioGlobal usuario = usuarioGlobalRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Pageable pageable = PageRequest.of(page, size, Sort.by("eventDate").descending());

        Page<AuditEvent> eventos = auditEventRepository.findByUsernameAndEventTypeIn(
            usuario.getEmail(),
            Arrays.asList("LOGIN_SUCCESS", "LOGIN_FAILED", "LOGOUT", "LOGIN_BLOCKED"),
            pageable
        );

        return eventos.getContent().stream()
            .map(this::mapToAuditEventResponse)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEventResponse> obtenerHistorialCambios(UUID userId, int page, int size) {
        log.info("Obteniendo historial de cambios para usuario {}", userId);

        UsuarioGlobal usuario = usuarioGlobalRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Pageable pageable = PageRequest.of(page, size, Sort.by("eventDate").descending());

        Page<AuditEvent> eventos = auditEventRepository.findByUsernameAndEventTypeIn(
            usuario.getEmail(),
            Arrays.asList("USER_UPDATED", "PASSWORD_CHANGED", "ROLE_CHANGED", "PERMISSIONS_CHANGED"),
            pageable
        );

        return eventos.getContent().stream()
            .map(this::mapToAuditEventResponse)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void cerrarTodasLasSesiones(UUID userId) {
        log.info("Cerrando todas las sesiones del usuario {}", userId);

        UsuarioGlobal usuario = usuarioGlobalRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Invalidar tokens JWT (requiere implementación de blacklist de tokens)
        // Por ahora, solo limpiar cache
        invalidateUserCache(usuario.getEmail());

        // Registrar evento
        AuditEvent event = AuditEvent.builder()
            .username(usuario.getEmail())
            .eventType("FORCE_LOGOUT")
            .eventDate(LocalDateTime.now())
            .success(true)
            .build();
        auditEventRepository.save(event);

        log.info("Todas las sesiones cerradas para usuario {}", usuario.getEmail());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canManageUser(UUID targetUserId) {
        UsuarioGlobal currentUser = getCurrentUser();
        if (currentUser == null) {
            return false;
        }

        UUID empresaId = ContextUtils.getCurrentEmpresaId();
        if (empresaId == null) {
            return false;
        }

        // Obtener rol del usuario actual en la empresa
        RolNombre rolActual = currentUser.getUsuarioEmpresas().stream()
            .filter(ue -> ue.getEmpresa().getId().equals(empresaId) && ue.getActivo())
            .map(UsuarioEmpresa::getRol)
            .findFirst()
            .orElse(null);

        if (rolActual == null) {
            return false;
        }

        // SUPER_ADMIN puede gestionar a cualquiera
        if (rolActual == RolNombre.SUPER_ADMIN) {
            return true;
        }

        // ADMIN puede gestionar a usuarios de su empresa (excepto otros SUPER_ADMIN)
        if (rolActual == RolNombre.ADMIN) {
            UsuarioGlobal targetUser = usuarioGlobalRepository.findById(targetUserId)
                .orElse(null);

            if (targetUser == null) {
                return false;
            }

            RolNombre targetRol = targetUser.getUsuarioEmpresas().stream()
                .filter(ue -> ue.getEmpresa().getId().equals(empresaId))
                .map(UsuarioEmpresa::getRol)
                .findFirst()
                .orElse(null);

            return targetRol != RolNombre.SUPER_ADMIN;
        }

        // JEFE_CAJAS puede gestionar CAJEROS y MESEROS
        if (rolActual == RolNombre.JEFE_CAJAS) {
            UsuarioGlobal targetUser = usuarioGlobalRepository.findById(targetUserId)
                .orElse(null);

            if (targetUser == null) {
                return false;
            }

            RolNombre targetRol = targetUser.getUsuarioEmpresas().stream()
                .filter(ue -> ue.getEmpresa().getId().equals(empresaId))
                .map(UsuarioEmpresa::getRol)
                .findFirst()
                .orElse(null);

            return targetRol == RolNombre.CAJERO || targetRol == RolNombre.MESERO;
        }

        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean tieneAccesoEmpresa(UUID userId, UUID empresaId) {
        UsuarioGlobal usuario = usuarioGlobalRepository.findById(userId)
            .orElse(null);

        if (usuario == null) {
            return false;
        }

        return usuario.getUsuarioEmpresas().stream()
            .anyMatch(ue -> ue.getEmpresa().getId().equals(empresaId)
                && ue.getActivo()
                && ue.estaVigente());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean tieneAccesoSucursal(UUID userId, UUID sucursalId) {
        return usuarioSucursalRepository
            .findByUsuarioIdAndSucursalId(userId, sucursalId)
            .map(UsuarioSucursal::getActivo)
            .orElse(false);
    }

    @Override
    @Transactional
    public List<UsuarioResponse> crearEnLote(List<UsuarioCreateRequest> requests) {
        log.info("Creando {} usuarios en lote", requests.size());

        List<UsuarioResponse> usuariosCreados = new ArrayList<>();

        for (UsuarioCreateRequest request : requests) {
            try {
                UsuarioResponse usuario = crear(request);
                usuariosCreados.add(usuario);
            } catch (Exception e) {
                log.error("Error creando usuario {}: {}", request.getEmail(), e.getMessage());
                // Continuar con los demás usuarios
            }
        }

        log.info("Creados {} de {} usuarios", usuariosCreados.size(), requests.size());
        return usuariosCreados;
    }

    @Override
    @Transactional
    public void desactivarEnLote(Set<UUID> userIds) {
        log.info("Desactivando {} usuarios en lote", userIds.size());

        for (UUID userId : userIds) {
            try {
                desactivar(userId);
            } catch (Exception e) {
                log.error("Error desactivando usuario {}: {}", userId, e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public void asignarUsuariosASucursal(UUID sucursalId, Set<UUID> userIds,
        Map<String, Boolean> permisos) {
        log.info("Asignando {} usuarios a sucursal {}", userIds.size(), sucursalId);

        EmpresaSucursal sucursal = empresaSucursalRepository.findById(sucursalId)
            .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));

        for (UUID userId : userIds) {
            try {
                AsignarSucursalesRequest request = AsignarSucursalesRequest.builder()
                    .sucursalesIds(Set.of(sucursalId))
                    .aplicarPermisosPorDefecto(true)
                    .permisosPorDefecto(AsignarSucursalesRequest.PermisosSucursal.builder()
                        .puedeLeer(permisos.getOrDefault("puedeLeer", true))
                        .puedeEscribir(permisos.getOrDefault("puedeEscribir", true))
                        .puedeEliminar(permisos.getOrDefault("puedeEliminar", false))
                        .puedeAprobar(permisos.getOrDefault("puedeAprobar", false))
                        .build())
                    .build();

                asignarSucursales(userId, request);
            } catch (Exception e) {
                log.error("Error asignando usuario {} a sucursal: {}", userId, e.getMessage());
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadisticasEmpresa(UUID empresaId) {
        Map<String, Object> stats = new HashMap<>();

        // Total usuarios
        long totalUsuarios = usuarioEmpresaRepository.countUsuariosActivosByEmpresa(empresaId);
        stats.put("totalUsuarios", totalUsuarios);

        // Usuarios por rol
        Map<String, Long> usuariosPorRol = new HashMap<>();
        for (RolNombre rol : RolNombre.values()) {
            long count = usuarioEmpresaRepository.countByEmpresaIdAndRol(empresaId, rol);
            usuariosPorRol.put(rol.name(), count);
        }
        stats.put("usuariosPorRol", usuariosPorRol);

        // Usuarios activos vs inactivos
        long usuariosActivos = usuarioEmpresaRepository.countByEmpresaIdAndActivoTrue(empresaId);
        stats.put("usuariosActivos", usuariosActivos);
        stats.put("usuariosInactivos", totalUsuarios - usuariosActivos);

        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponse> obtenerUsuariosPorRol(RolNombre rol) {
        UUID empresaId = ContextUtils.getCurrentEmpresaId();
        if (empresaId == null) {
            throw new BusinessException("No se ha establecido el contexto de empresa");
        }

        List<UsuarioEmpresa> usuariosEmpresa = usuarioEmpresaRepository
            .findByEmpresaIdAndRolAndActivoTrue(empresaId, rol);

        return usuariosEmpresa.stream()
            .map(ue -> usuarioMapper.toResponseWithContext(ue.getUsuario(), empresaId))
            .collect(Collectors.toList());
    }

    @Override
    public byte[] exportarUsuarios(Map<String, Object> filtros) {
        // TODO: Implementar exportación a Excel
        throw new UnsupportedOperationException("Exportación a Excel no implementada aún");
    }

    // Métodos auxiliares privados

    private void validarPasswordSegura(String password) {
        if (password == null || password.length() < 8) {
            throw new BusinessException("La contraseña debe tener al menos 8 caracteres");
        }

        String pattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";
        if (!password.matches(pattern)) {
            throw new BusinessException(
                "La contraseña debe contener al menos una mayúscula, una minúscula, " +
                    "un número y un carácter especial (@#$%^&+=!)"
            );
        }
    }

    private void asignarSucursalesInicial(UsuarioGlobal usuario, Empresa empresa,
        UsuarioCreateRequest request) {
        UsuarioEmpresa usuarioEmpresa = usuario.getUsuarioEmpresas().stream()
            .filter(ue -> ue.getEmpresa().getId().equals(empresa.getId()))
            .findFirst()
            .orElseThrow(() -> new BusinessException("Error en asignación de empresa"));

        for (UUID sucursalId : request.getSucursalesIds()) {
            EmpresaSucursal sucursal = empresaSucursalRepository.findById(sucursalId)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada: " + sucursalId));

            if (!sucursal.getEmpresa().getId().equals(empresa.getId())) {
                throw new BusinessException("La sucursal no pertenece a la empresa especificada");
            }

            UsuarioSucursal usuarioSucursal = UsuarioSucursal.builder()
                .usuarioId(usuario.getId())
                .sucursal(sucursal)
                .empresaId(empresa.getId())
                .usuarioEmpresa(usuarioEmpresa)
                .puedeLeer(request.getPuedeLeer())
                .puedeEscribir(request.getPuedeEscribir())
                .puedeEliminar(request.getPuedeEliminar())
                .puedeAprobar(request.getPuedeAprobar())
                .esPrincipal(sucursalId.equals(request.getSucursalPrincipalId()))
                .activo(true)
                .asignadoPor(getCurrentUser())
                .build();

            usuarioSucursalRepository.save(usuarioSucursal);
        }
    }

    private UsuarioGlobal getCurrentUser() {
        return ContextUtils.getCurrentUser();
    }

    private void invalidateUserCache(String email) {
        if (cacheManager != null) {
            Cache userCache = cacheManager.getCache("users");
            if (userCache != null) {
                userCache.evict(email);
            }
        }
    }

    private AuditEventResponse mapToAuditEventResponse(AuditEvent event) {
        return AuditEventResponse.builder()
            .eventType(event.getEventType())
            .eventDate(event.getEventDate())
            .ipAddress(event.getIpAddress())
            .userAgent(event.getUserAgent())
            .details(event.getDetails())
            .success(event.getSuccess())
            .errorMessage(event.getErrorMessage())
            .build();
    }

    @Override
    @Transactional
    public UsuarioResponse asignarCajas(UUID userId, List<UUID> cajaIds) {
        log.info("Asignando {} cajas a usuario {}", cajaIds.size(), userId);

        UsuarioGlobal usuario = usuarioGlobalRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        UUID empresaId = ContextUtils.getCurrentEmpresaId();
        UUID sucursalId = ContextUtils.getCurrentSucursalId();
        String tenantId = TenantContext.getCurrentTenant();

        if (sucursalId == null || tenantId == null) {
            throw new BusinessException("Debe establecer el contexto completo (sucursal y tenant)");
        }

        // Verificar acceso y rol
        UsuarioSucursal usuarioSucursal = usuarioSucursalRepository
            .findByUsuarioIdAndSucursalId(userId, sucursalId)
            .orElseThrow(() -> new BusinessException("Usuario no tiene acceso a esta sucursal"));

        if (!usuarioSucursal.getActivo()) {
            throw new BusinessException("Usuario no está activo en esta sucursal");
        }

        RolNombre rol = usuarioSucursal.getRol();
        if (rol != RolNombre.CAJERO && rol != RolNombre.JEFE_CAJAS &&
            rol != RolNombre.ADMIN && rol != RolNombre.SUPER_ADMIN) {
            throw new BusinessException("Rol no autorizado para operar cajas");
        }

        // Crear registro de asignación de cajas
        AsignacionCajas asignacion = AsignacionCajas.builder()
            .usuarioId(userId)
            .usuarioNombre(usuario.getNombreCompleto())
            .empresaId(empresaId)
            .sucursalId(sucursalId)
            .tenantId(tenantId)
            .cajasIds(cajaIds)
            .rol(rol)
            .fechaAsignacion(LocalDateTime.now())
            .asignadoPor(ContextUtils.getCurrentUser().getId())
            .activo(true)
            .build();

        // Guardar asignación (esto sería en una tabla en el schema público)
        asignacionCajasRepository.save(asignacion);

        // Invalidar asignaciones anteriores
        asignacionCajasRepository.invalidarAsignacionesAnteriores(userId, sucursalId, asignacion.getId());

        log.info("Cajas asignadas exitosamente: Usuario={}, Sucursal={}, Cajas={}",
            usuario.getNombreCompleto(), sucursalId, cajaIds);

        // Construir respuesta
        UsuarioResponse response = usuarioMapper.toResponseWithContext(usuario, empresaId);
        response.setCajasIds(new HashSet<>(cajaIds));

        return response;
    }
}