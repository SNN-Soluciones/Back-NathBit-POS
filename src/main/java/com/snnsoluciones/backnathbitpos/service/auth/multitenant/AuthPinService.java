package com.snnsoluciones.backnathbitpos.service.auth.multitenant;

import com.snnsoluciones.backnathbitpos.config.tenant.TenantContext;
import com.snnsoluciones.backnathbitpos.dto.auth.multitenant.AuthMultitenantDTOs.*;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.entity.UsuarioSucursal;
import com.snnsoluciones.backnathbitpos.entity.global.Dispositivo;
import com.snnsoluciones.backnathbitpos.entity.global.Tenant;
import com.snnsoluciones.backnathbitpos.exception.BadRequestException;
import com.snnsoluciones.backnathbitpos.exception.NotFoundException;
import com.snnsoluciones.backnathbitpos.exception.UnauthorizedException;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioSucursalRepository;
import com.snnsoluciones.backnathbitpos.repository.global.DispositivoRepository;
import com.snnsoluciones.backnathbitpos.security.jwt.JwtTokenProvider;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de autenticación con PIN para usuarios locales.
 * Estos usuarios pertenecen a un tenant específico y se autentican
 * solo desde dispositivos registrados.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthPinService {

    private final DispositivoRepository dispositivoRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioSucursalRepository usuarioSucursalRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    /**
     * PIN por defecto para usuarios migrados o nuevos
     */
    public static final String PIN_DEFAULT = "123456";

    /**
     * Duración del token de sesión en milisegundos (8 horas)
     */
    private static final long SESSION_TOKEN_DURATION = 8 * 60 * 60 * 1000;

    /**
     * Obtiene la lista de usuarios disponibles para un dispositivo.
     * El dispositivo ya debe estar validado (token válido).
     */
    @Transactional(readOnly = true)
    public ObtenerUsuariosResponse obtenerUsuarios(String deviceToken) {
        log.info("Obteniendo usuarios para dispositivo");

        // Validar dispositivo
        Dispositivo dispositivo = dispositivoRepository.findByTokenAndActivoTrueWithTenant(deviceToken)
            .orElseThrow(() -> new UnauthorizedException("Dispositivo no autorizado"));

        Tenant tenant = dispositivo.getTenant();
        if (!tenant.estaActivo()) {
            throw new BadRequestException("La empresa no está activa");
        }

        // Actualizar último uso
        dispositivo.registrarUso();
        dispositivoRepository.save(dispositivo);

        // Establecer contexto del tenant para la consulta
        TenantContext.setTenant(tenant.getId(), tenant.getSchemaName());

        try {
            // Obtener usuarios activos del tenant
            // NOTA: Esto asume que el schema ya está configurado por el TenantConnectionProvider
            List<Usuario> usuarios = obtenerUsuariosDelTenant(tenant.getEmpresaLegacyId());

            List<UsuarioLocalInfo> usuariosInfo = usuarios.stream()
                .map(this::mapToUsuarioLocalInfo)
                .collect(Collectors.toList());

            return ObtenerUsuariosResponse.builder()
                .tenant(TenantResumen.builder()
                    .id(tenant.getId())
                    .codigo(tenant.getCodigo())
                    .nombre(tenant.getNombre())
                    .build())
                .dispositivo(DispositivoInfo.builder()
                    .id(dispositivo.getId())
                    .nombre(dispositivo.getNombre())
                    .plataforma(dispositivo.getPlataforma() != null ? dispositivo.getPlataforma().name() : null)
                    .ultimoUso(dispositivo.getUltimoUso())
                    .build())
                .usuarios(usuariosInfo)
                .build();

        } finally {
            TenantContext.clear();
        }
    }

    private Map<String, Object> buscarUsuarioEnTenant(String schemaName, Long usuarioId) {
        String sql = String.format(
            "SELECT id, nombre, apellidos, email, pin, pin_longitud, requiere_cambio_pin, rol, activo " +
                "FROM %s.usuarios WHERE id = ?",
            schemaName
        );

        try {
            return jdbcTemplate.queryForMap(sql, usuarioId);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Realiza el login con PIN.
     */
    public LoginPinResponse loginConPin(String deviceToken, LoginPinRequest request) {
        log.info("Intento de login con PIN para usuario ID: {}", request.getUsuarioId());

        // Validar dispositivo
        Dispositivo dispositivo = dispositivoRepository.findByTokenWithTenant(deviceToken)
            .orElseThrow(() -> new UnauthorizedException("Dispositivo no autorizado"));

        Tenant tenant = dispositivo.getTenant();
        if (!tenant.estaActivo()) {
            throw new BadRequestException("La empresa no está activa");
        }

        // Buscar usuario directamente en el schema del tenant
        String schemaName = tenant.getSchemaName();
        Map<String, Object> usuarioData = buscarUsuarioEnTenant(schemaName, request.getUsuarioId());

        if (usuarioData == null) {
            throw new NotFoundException("Usuario no encontrado");
        }

        // Verificar que esté activo
        Boolean activo = (Boolean) usuarioData.get("activo");
        if (!Boolean.TRUE.equals(activo)) {
            throw new UnauthorizedException("Usuario desactivado");
        }

        // Verificar PIN
        String pinAlmacenado = (String) usuarioData.get("pin");
        if (pinAlmacenado == null || pinAlmacenado.isBlank()) {
            log.warn("Usuario {} sin PIN configurado", request.getUsuarioId());
            throw new BadRequestException("Usuario requiere configuración de PIN");
        }

        if (!passwordEncoder.matches(request.getPin(), pinAlmacenado)) {
            log.warn("PIN incorrecto para usuario {}", request.getUsuarioId());
            throw new UnauthorizedException("PIN incorrecto");
        }

        // Extraer datos del usuario
        Long usuarioId = ((Number) usuarioData.get("id")).longValue();
        String nombre = (String) usuarioData.get("nombre");
        String apellidos = (String) usuarioData.get("apellidos");
        String email = (String) usuarioData.get("email");
        String rol = (String) usuarioData.get("rol");
        Boolean requiereCambioPin = (Boolean) usuarioData.get("requiere_cambio_pin");

        // Obtener sucursales del usuario
        List<SucursalResumen> sucursales = obtenerSucursalesDelTenant(schemaName, usuarioId);

        // Generar token de sesión
        Long empresaId = tenant.getEmpresaLegacyId() != null
            ? tenant.getEmpresaLegacyId()
            : -tenant.getId();

        String sessionToken = jwtTokenProvider.generateTokenWithContext(
            usuarioId,
            email != null ? email : "user_" + usuarioId,
            rol,
            empresaId,
            sucursales.isEmpty() ? null : sucursales.get(0).getId()
        );

        log.info("Login exitoso con PIN para usuario {} en tenant {}", usuarioId, tenant.getCodigo());

        // Construir respuesta
        String nombreCompleto = nombre;
        if (apellidos != null && !apellidos.isBlank()) {
            nombreCompleto += " " + apellidos;
        }

        String avatar = "";
        if (nombre != null && !nombre.isBlank()) {
            avatar = nombre.substring(0, 1).toUpperCase();
            if (apellidos != null && !apellidos.isBlank()) {
                avatar += apellidos.substring(0, 1).toUpperCase();
            }
        }

        return LoginPinResponse.builder()
            .sessionToken(sessionToken)
            .usuario(UsuarioLocalInfo.builder()
                .id(usuarioId)
                .nombre(nombre)
                .apellidos(apellidos)
                .nombreCompleto(nombreCompleto)
                .rol(rol)
                .avatar(avatar)
                .build())
            .tenant(TenantResumen.builder()
                .id(tenant.getId())
                .codigo(tenant.getCodigo())
                .nombre(tenant.getNombre())
                .build())
            .sucursales(sucursales)
            .requiereCambioPin(Boolean.TRUE.equals(requiereCambioPin))
            .build();
    }

    /**
     * Obtiene sucursales del usuario desde el schema del tenant
     */
    private List<SucursalResumen> obtenerSucursalesDelTenant(String schemaName, Long usuarioId) {
        String sql = String.format(
            "SELECT DISTINCT s.id, s.nombre, s.numero_sucursal " +
                "FROM %s.sucursales s " +
                "INNER JOIN %s.usuarios_empresas ue ON s.id = ue.sucursal_id " +
                "WHERE ue.usuario_id = ? AND ue.activo = true AND ue.sucursal_id IS NOT NULL",
            schemaName, schemaName
        );

        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, usuarioId);
            return rows.stream()
                .map(row -> SucursalResumen.builder()
                    .id(((Number) row.get("id")).longValue())
                    .nombre((String) row.get("nombre"))
                    .numeroSucursal(row.get("numero_sucursal") != null ? row.get("numero_sucursal").toString() : "001")
                    .build())
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Error obteniendo sucursales: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Cambia el PIN del usuario.
     */
    public void cambiarPin(Long usuarioId, CambiarPinRequest request) {
        log.info("Cambiando PIN para usuario {}", usuarioId);

        // Validar longitud
        if (request.getLongitud() != 4 && request.getLongitud() != 6) {
            throw new BadRequestException("La longitud del PIN debe ser 4 o 6 dígitos");
        }

        // Validar que el PIN tenga la longitud correcta
        if (request.getNuevoPin().length() != request.getLongitud()) {
            throw new BadRequestException("El PIN debe tener exactamente " + request.getLongitud() + " dígitos");
        }

        // Validar que sean solo dígitos
        if (!request.getNuevoPin().matches("\\d+")) {
            throw new BadRequestException("El PIN solo debe contener dígitos");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        // Encriptar y guardar
        usuario.setPin(passwordEncoder.encode(request.getNuevoPin()));
        usuario.setPinLongitud(request.getLongitud());
        usuario.setRequiereCambioPin(false);
        usuario.setUpdatedAt(LocalDateTime.now());
        usuarioRepository.save(usuario);

        log.info("PIN cambiado exitosamente para usuario {}", usuarioId);
    }

    /**
     * Asigna el PIN por defecto a un usuario (para migración).
     */
    public void asignarPinPorDefecto(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        usuario.setPin(passwordEncoder.encode(PIN_DEFAULT));
        usuario.setPinLongitud(6);
        usuario.setRequiereCambioPin(true);
        usuarioRepository.save(usuario);

        log.info("PIN por defecto asignado a usuario {}", usuarioId);
    }

    // ==================== Métodos privados ====================

    /**
     * Obtiene usuarios del tenant.
     * Usa la empresa legacy si existe, sino consulta directamente en el schema.
     */
    private List<Usuario> obtenerUsuariosDelTenant(Long empresaLegacyId) {
        if (empresaLegacyId != null) {
            // Sistema legacy: usuarios por empresa_id
            return usuarioRepository.findByEmpresaId(empresaLegacyId);
        }
        // TODO: Sistema nuevo: usuarios directamente del schema del tenant
        // Por ahora retornar lista vacía
        return List.of();
    }

    /**
     * Obtiene las sucursales asignadas a un usuario.
     */
    private List<SucursalResumen> obtenerSucursalesDeUsuario(Long usuarioId) {
        try {
            List<UsuarioSucursal> asignaciones = usuarioSucursalRepository
                .findByUsuarioIdAndActivoTrue(usuarioId);

            return asignaciones.stream()
                .map(us -> SucursalResumen.builder()
                    .id(us.getSucursal().getId())
                    .nombre(us.getSucursal().getNombre())
                    .numeroSucursal(us.getSucursal().getNumeroSucursal())
                    .build())
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Error obteniendo sucursales de usuario {}: {}", usuarioId, e.getMessage());
            return List.of();
        }
    }

    /**
     * Mapea un Usuario a UsuarioLocalInfo.
     */
    private UsuarioLocalInfo mapToUsuarioLocalInfo(Usuario usuario) {
        String nombreCompleto = usuario.getNombre();
        if (usuario.getApellidos() != null && !usuario.getApellidos().isBlank()) {
            nombreCompleto += " " + usuario.getApellidos();
        }

        // Generar avatar (iniciales)
        String avatar = "";
        if (usuario.getNombre() != null && !usuario.getNombre().isBlank()) {
            avatar = usuario.getNombre().substring(0, 1).toUpperCase();
            if (usuario.getApellidos() != null && !usuario.getApellidos().isBlank()) {
                avatar += usuario.getApellidos().substring(0, 1).toUpperCase();
            }
        }

        return UsuarioLocalInfo.builder()
            .id(usuario.getId())
            .nombre(usuario.getNombre())
            .apellidos(usuario.getApellidos())
            .nombreCompleto(nombreCompleto)
            .rol(usuario.getRol().name())
            .avatar(avatar)
            .build();
    }
}
