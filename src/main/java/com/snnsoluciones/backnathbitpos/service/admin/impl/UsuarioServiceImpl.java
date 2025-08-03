package com.snnsoluciones.backnathbitpos.service.admin.impl;

import com.snnsoluciones.backnathbitpos.config.tenant.TenantContext;
import com.snnsoluciones.backnathbitpos.dto.request.CambioPasswordRequest;
import com.snnsoluciones.backnathbitpos.dto.request.UsuarioCreateRequest;
import com.snnsoluciones.backnathbitpos.dto.request.UsuarioUpdateRequest;
import com.snnsoluciones.backnathbitpos.dto.response.AuditEventResponse;
import com.snnsoluciones.backnathbitpos.dto.response.UsuarioResponse;
import com.snnsoluciones.backnathbitpos.entity.base.BaseEntity;
import com.snnsoluciones.backnathbitpos.entity.operacion.Caja;
import com.snnsoluciones.backnathbitpos.entity.security.AuditEvent;
import com.snnsoluciones.backnathbitpos.entity.security.Rol;
import com.snnsoluciones.backnathbitpos.entity.security.Usuario;
import com.snnsoluciones.backnathbitpos.entity.tenant.Sucursal;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.mapper.UsuarioMapper;
import com.snnsoluciones.backnathbitpos.repository.AuditEventRepository;
import com.snnsoluciones.backnathbitpos.repository.CajaRepository;
import com.snnsoluciones.backnathbitpos.repository.RolRepository;
import com.snnsoluciones.backnathbitpos.repository.SucursalRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import com.snnsoluciones.backnathbitpos.service.admin.UsuarioService;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final SucursalRepository sucursalRepository;
    private final CajaRepository cajaRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;
    private final CacheManager cacheManager;
    private final AuditEventRepository auditEventRepository;

    @Value("${app.security.max-intentos-login:3}")
    private int maxIntentosLogin;

    @Override
    public UsuarioResponse crear(UsuarioCreateRequest request) {
        log.info("Creando nuevo usuario con email: {}", request.getEmail());

        // 1. Verificar que el email no exista para el tenant actual
        String tenantId = TenantContext.getCurrentTenant();
        if (usuarioRepository.existsByEmailAndTenantId(request.getEmail(), tenantId)) {
            throw new BusinessException("Ya existe un usuario con el email: " + request.getEmail());
        }

        // 2. Validar y encriptar contraseña
        validarPasswordSegura(request.getPassword());
        String passwordEncriptado = passwordEncoder.encode(request.getPassword());

        // 3. Crear entidad usuario
        Usuario usuario = usuarioMapper.toEntity(request);
        usuario.setPassword(passwordEncriptado);
        usuario.setTenantId(tenantId);

        // 4. Asignar rol
        if (request.getRolId() != null) {
            Rol rol = rolRepository.findById(request.getRolId())
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
            usuario.setRol(rol);
        }

        // 5. Asignar sucursal predeterminada
        if (request.getSucursalPredeterminadaId() != null) {
            Sucursal sucursal = sucursalRepository.findById(request.getSucursalPredeterminadaId())
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));
            usuario.setSucursalPredeterminada(sucursal);
        }

        // 6. Guardar usuario
        usuario = usuarioRepository.save(usuario);

        // 7. Asignar sucursales y cajas
        asignarSucursalesYCajas(usuario, request.getSucursalesIds(), request.getCajasIds());

        log.info("Usuario creado exitosamente con ID: {}", usuario.getId());

        return usuarioMapper.toResponse(usuario);
    }

    @Override
    public UsuarioResponse actualizar(UUID id, UsuarioUpdateRequest request) {
        log.info("Actualizando usuario con ID: {}", id);

        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Verificar si el email cambió y no está duplicado
        if (!usuario.getEmail().equals(request.getEmail())) {
            String tenantId = TenantContext.getCurrentTenant();
            if (usuarioRepository.existsByEmailAndTenantId(request.getEmail(), tenantId)) {
                throw new BusinessException("Ya existe un usuario con el email: " + request.getEmail());
            }
        }

        // Actualizar datos básicos
        usuarioMapper.updateEntity(usuario, request);

        // Actualizar rol si cambió
        if (request.getRolId() != null) {
            // Verificar si el rol cambió
            if (usuario.getRol() == null || !request.getRolId().equals(usuario.getRol().getId())) {
                Rol rol = rolRepository.findById(request.getRolId())
                    .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
                usuario.setRol(rol);
            }
        }

        // Actualizar sucursal predeterminada si cambió
        if (request.getSucursalPredeterminadaId() != null) {
            Sucursal sucursal = sucursalRepository.findById(request.getSucursalPredeterminadaId())
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));
            usuario.setSucursalPredeterminada(sucursal);
        }

        // Actualizar sucursales y cajas
        asignarSucursalesYCajas(usuario, request.getSucursalesIds(), request.getCajasIds());

        usuario = usuarioRepository.save(usuario);

        log.info("Usuario actualizado exitosamente");

        return usuarioMapper.toResponse(usuario);
    }

    @Override
    public void cambiarPassword(UUID id, CambioPasswordRequest request) {
        log.info("Cambiando contraseña para usuario ID: {}", id);

        Usuario usuario = usuarioRepository.findById(id)
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

        usuarioRepository.save(usuario);

        log.info("Contraseña cambiada exitosamente");
    }

    @Override
    public void bloquearUsuario(UUID id) {
        log.info("Bloqueando usuario ID: {}", id);

        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        usuario.setBloqueado(true);
        usuarioRepository.save(usuario);

        log.info("Usuario bloqueado exitosamente");
    }

    @Override
    public void desbloquearUsuario(UUID id) {
        log.info("Desbloqueando usuario ID: {}", id);

        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        usuario.setBloqueado(false);
        usuario.setIntentosFallidos(0);
        usuarioRepository.save(usuario);

        log.info("Usuario desbloqueado exitosamente");
    }

    @Override
    public void resetearIntentosFallidos(String email) {
        log.debug("Reseteando intentos fallidos para: {}", email);

        String tenantId = TenantContext.getCurrentTenant();
        usuarioRepository.findByEmailAndTenantId(email, tenantId).ifPresent(usuario -> {
            usuario.setIntentosFallidos(0);
            usuarioRepository.save(usuario);
        });
    }

    @Override
    public void manejarLoginExitoso(String email) {
        log.debug("Manejando login exitoso para: {}", email);

        String tenantId = TenantContext.getCurrentTenant();
        usuarioRepository.findByEmailAndTenantId(email, tenantId).ifPresent(usuario -> {
            usuario.setIntentosFallidos(0);
            usuario.setUltimoAcceso(LocalDateTime.now());
            usuarioRepository.save(usuario);
        });
    }

    @Override
    public void manejarLoginFallido(String email) {
        log.debug("Manejando login fallido para: {}", email);

        String tenantId = TenantContext.getCurrentTenant();
        usuarioRepository.findByEmailAndTenantId(email, tenantId).ifPresent(usuario -> {
            int intentos = usuario.getIntentosFallidos() + 1;
            usuario.setIntentosFallidos(intentos);

            if (intentos >= maxIntentosLogin) {
                usuario.setBloqueado(true);
                log.warn("Usuario {} bloqueado por exceder intentos de login", email);
            }

            usuarioRepository.save(usuario);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorId(UUID id) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        return usuarioMapper.toResponse(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listar(Pageable pageable) {
        String tenantId = TenantContext.getCurrentTenant();
        Page<Usuario> usuarios = usuarioRepository.findByTenantId(tenantId, pageable);

        return usuarios.map(usuarioMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeEmail(String email) {
        String tenantId = TenantContext.getCurrentTenant();
        return usuarioRepository.existsByEmailAndTenantId(email, tenantId);
    }

    @Override
    public void eliminar(UUID id) {
        log.info("Eliminando usuario ID: {}", id);

        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Soft delete
        usuario.setActivo(false);
        usuarioRepository.save(usuario);

        log.info("Usuario eliminado (soft delete) exitosamente");
    }

    /**
     * Método auxiliar para validar la seguridad de la contraseña
     */
    private void validarPasswordSegura(String password) {
        if (password == null || password.length() < 8) {
            throw new BusinessException("La contraseña debe tener al menos 8 caracteres");
        }

        // Verificar que contenga al menos una mayúscula, una minúscula, un número y un carácter especial
        String pattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";
        if (!password.matches(pattern)) {
            throw new BusinessException(
                "La contraseña debe contener al menos una mayúscula, una minúscula, " +
                    "un número y un carácter especial (@#$%^&+=!)"
            );
        }
    }

    /**
     * Método auxiliar para asignar sucursales y cajas al usuario
     */
    private void asignarSucursalesYCajas(Usuario usuario, Set<UUID> sucursalesIds, Set<UUID> cajasIds) {
        // Limpiar y asignar sucursales
        if (sucursalesIds != null && !sucursalesIds.isEmpty()) {
            usuario.getSucursales().clear();
            Set<Sucursal> sucursales = new HashSet<>(sucursalRepository.findAllById(sucursalesIds));

            if (sucursales.size() != sucursalesIds.size()) {
                throw new BusinessException("Una o más sucursales no fueron encontradas");
            }

            usuario.setSucursales(sucursales);
        }

        // Limpiar y asignar cajas
        if (cajasIds != null && !cajasIds.isEmpty()) {
            usuario.getCajas().clear();
            Set<Caja> cajas = new HashSet<>(cajaRepository.findAllById(cajasIds));

            if (cajas.size() != cajasIds.size()) {
                throw new BusinessException("Una o más cajas no fueron encontradas");
            }

            usuario.setCajas(cajas);
        }

        // Guardar cambios
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public UsuarioResponse cambiarRol(UUID userId, String nuevoRol) {
        log.info("Cambiando rol de usuario {} a {}", userId, nuevoRol);

        Usuario usuario = usuarioRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Buscar el nuevo rol
        Rol rol = rolRepository.findByNombre(RolNombre.valueOf(nuevoRol))
            .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado: " + nuevoRol));

        String rolAnterior = usuario.getRol() != null ? usuario.getRol().getNombre().name() : "SIN_ROL";
        usuario.setRol(rol);
        usuario = usuarioRepository.save(usuario);

        log.info("Rol cambiado exitosamente de {} a {}", rolAnterior, nuevoRol);

        return usuarioMapper.toResponse(usuario);
    }

    @Override
    @Transactional
    public UsuarioResponse asignarSucursales(UUID userId, List<UUID> sucursalIds) {
        log.info("Asignando {} sucursales a usuario {}", sucursalIds.size(), userId);

        Usuario usuario = usuarioRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Obtener las sucursales
        List<Sucursal> sucursales = sucursalRepository.findAllById(sucursalIds);

        if (sucursales.size() != sucursalIds.size()) {
            throw new BusinessException("Una o más sucursales no fueron encontradas");
        }

        // Limpiar sucursales actuales y asignar las nuevas
        usuario.getSucursales().clear();
        usuario.getSucursales().addAll(sucursales);

        // Si no tiene sucursal predeterminada, asignar la primera
        if (usuario.getSucursalPredeterminada() == null && !sucursales.isEmpty()) {
            usuario.setSucursalPredeterminada(sucursales.get(0));
        }

        usuario = usuarioRepository.save(usuario);

        log.info("Sucursales asignadas exitosamente");

        return usuarioMapper.toResponse(usuario);
    }

    @Override
    @Transactional
    public UsuarioResponse asignarCajas(UUID userId, List<UUID> cajaIds) {
        log.info("Asignando {} cajas a usuario {}", cajaIds.size(), userId);

        Usuario usuario = usuarioRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Obtener las cajas
        List<Caja> cajas = cajaRepository.findAllById(cajaIds);

        if (cajas.size() != cajaIds.size()) {
            throw new BusinessException("Una o más cajas no fueron encontradas");
        }

        // Verificar que todas las cajas pertenezcan a sucursales asignadas al usuario
        Set<UUID> sucursalesUsuario = usuario.getSucursales().stream()
            .map(BaseEntity::getId)
            .collect(Collectors.toSet());

        boolean cajasValidas = cajas.stream()
            .allMatch(caja -> sucursalesUsuario.contains(caja.getSucursal().getId()));

        if (!cajasValidas) {
            throw new BusinessException("Solo se pueden asignar cajas de las sucursales del usuario");
        }

        // Limpiar cajas actuales y asignar las nuevas
        usuario.getCajas().clear();
        usuario.getCajas().addAll(cajas);

        usuario = usuarioRepository.save(usuario);

        log.info("Cajas asignadas exitosamente");

        return usuarioMapper.toResponse(usuario);
    }

    @Override
    @Transactional
    public void resetearIntentos(UUID userId) {
        log.info("Reseteando intentos fallidos para usuario {}", userId);

        Usuario usuario = usuarioRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        usuario.setIntentosFallidos(0);
        usuario.setBloqueado(false);
        usuarioRepository.save(usuario);

        log.info("Intentos reseteados exitosamente");
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEventResponse> obtenerHistorialLogin(UUID userId, int page, int size) {
        log.info("Obteniendo historial de login para usuario {}", userId);

        Usuario usuario = usuarioRepository.findById(userId)
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
    @Transactional
    public void cerrarTodasLasSesiones(UUID userId) {
        log.info("Cerrando todas las sesiones del usuario {}", userId);

        Usuario usuario = usuarioRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Agregar todos los tokens del usuario a la blacklist
        // Esto requeriría tener un registro de tokens activos por usuario
        // Por ahora, simplemente marcamos en el usuario

        usuario.setForzarRelogin(true); // Este campo habría que agregarlo a la entidad
        usuarioRepository.save(usuario);

        // Limpiar cache si existe
        if (cacheManager != null) {
            Cache userCache = cacheManager.getCache("users");
            if (userCache != null) {
                userCache.evict(usuario.getEmail());
            }
        }

        log.info("Todas las sesiones cerradas para usuario {}", usuario.getEmail());
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
}