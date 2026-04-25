package com.snnsoluciones.backnathbitpos.service.auth.multitenant;

import com.snnsoluciones.backnathbitpos.config.tenant.TenantContext;
import com.snnsoluciones.backnathbitpos.dto.auth.CambiarPinRequest;
import com.snnsoluciones.backnathbitpos.dto.auth.multitenant.AuthMultitenantDTOs.*;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.entity.UsuarioSucursal;
import com.snnsoluciones.backnathbitpos.entity.global.Dispositivo;
import com.snnsoluciones.backnathbitpos.entity.global.Tenant;
import com.snnsoluciones.backnathbitpos.entity.global.UsuarioGlobal;
import com.snnsoluciones.backnathbitpos.exception.BadRequestException;
import com.snnsoluciones.backnathbitpos.exception.NotFoundException;
import com.snnsoluciones.backnathbitpos.exception.UnauthorizedException;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioSucursalRepository;
import com.snnsoluciones.backnathbitpos.repository.global.DispositivoRepository;
import com.snnsoluciones.backnathbitpos.repository.global.UsuarioGlobalRepository;
import com.snnsoluciones.backnathbitpos.security.jwt.JwtTokenProvider;
import java.util.ArrayList;
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
    private final UsuarioGlobalRepository usuarioGlobalRepository;

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
            // Usuarios del schema (CAJERO, MESERO, ADMIN, etc.) → fuente SCHEMA
            List<UsuarioLocalInfo> usuariosInfo = new ArrayList<>(
                obtenerUsuariosDelTenantViaJdbc(tenant.getSchemaName())
            );

            // Usuarios globales del tenant (SUPER_ADMIN) → fuente GLOBAL
            List<UsuarioGlobal> globales = new ArrayList<>(
                usuarioGlobalRepository.findByTenantId(tenant.getId())
            );
            // ROOT y SOPORTE también aparecen en todos los dispositivos
            globales.addAll(usuarioGlobalRepository.findUsuariosSistemaActivos());

            for (UsuarioGlobal ug : globales) {
                if (ug.getPin() == null || ug.getPin().isBlank()) continue;

                String nombreCompleto = ug.getNombre() +
                    (ug.getApellidos() != null ? " " + ug.getApellidos() : "");
                String avatar = ug.getNombre().substring(0, 1).toUpperCase() +
                    (ug.getApellidos() != null ? ug.getApellidos().substring(0, 1).toUpperCase() : "");

                usuariosInfo.add(UsuarioLocalInfo.builder()
                    .id(ug.getId())
                    .nombre(ug.getNombre())
                    .apellidos(ug.getApellidos())
                    .nombreCompleto(nombreCompleto)
                    .rol(ug.getRol().name())
                    .avatar(avatar)
                    .fuente("GLOBAL")
                    .build());
            }

            SucursalResumen sucursalResumen = null;
            if (dispositivo.getSucursalId() != null) {
                String sql = String.format(
                    "SELECT id, nombre, numero_sucursal FROM %s.sucursales WHERE id = ?",
                    tenant.getSchemaName()
                );
                try {
                    Map<String, Object> row = jdbcTemplate.queryForMap(sql, dispositivo.getSucursalId());
                    sucursalResumen = SucursalResumen.builder()
                        .id(((Number) row.get("id")).longValue())
                        .nombre((String) row.get("nombre"))
                        .numeroSucursal(row.get("numero_sucursal") != null ? row.get("numero_sucursal").toString() : "001")
                        .build();
                } catch (Exception e) {
                    log.warn("No se pudo obtener sucursal del dispositivo: {}", e.getMessage());
                }
            }

            return ObtenerUsuariosResponse.builder()
                .tenant(TenantResumen.builder()
                    .id(tenant.getId())
                    .codigo(tenant.getCodigo())
                    .nombre(tenant.getNombre())
                    .build())
                .sucursal(sucursalResumen)
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
        log.info("Login PIN usuario={} fuente={}", request.getUsuarioId(), request.getFuente());

        Dispositivo dispositivo = dispositivoRepository.findByTokenWithTenant(deviceToken)
            .orElseThrow(() -> new UnauthorizedException("Dispositivo no autorizado"));

        Tenant tenant = dispositivo.getTenant();
        if (!tenant.estaActivo()) throw new BadRequestException("La empresa no está activa");

        String schemaName = tenant.getSchemaName();

        // ── Flujo GLOBAL (ROOT, SOPORTE, SUPER_ADMIN) ──────────────────────
        if ("GLOBAL".equals(request.getFuente())) {
            // ← Reemplazar usuarioGlobalRepository.findById() con JDBC directo
            Map<String, Object> ugData;
            try {
                ugData = jdbcTemplate.queryForMap(
                    "SELECT id, nombre, apellidos, email, pin, pin_longitud, requiere_cambio_pin, rol, activo " +
                        "FROM public.usuarios_globales WHERE id = ?",
                    request.getUsuarioId()
                );
            } catch (Exception e) {
                throw new NotFoundException("Usuario no encontrado");
            }

            log.info("DEBUG ugData id={} pin={}", ugData.get("id"), ugData.get("pin"));
            log.info("DEBUG pin ingresado={}", request.getPin());
            log.info("DEBUG matches={}", passwordEncoder.matches(request.getPin(), (String) ugData.get("pin")));

            if (!Boolean.TRUE.equals(ugData.get("activo")))
                throw new UnauthorizedException("Usuario desactivado");

            String pin = (String) ugData.get("pin");
            if (pin == null || pin.isBlank())
                throw new BadRequestException("Usuario requiere configuración de PIN");

            if (!passwordEncoder.matches(request.getPin(), pin))
                throw new UnauthorizedException("PIN incorrecto");

            Long ugId = ((Number) ugData.get("id")).longValue();
            String nombre = (String) ugData.get("nombre");
            String apellidos = (String) ugData.get("apellidos");
            String email = (String) ugData.get("email");
            String rol = (String) ugData.get("rol");
            Boolean requiereCambioPin = (Boolean) ugData.get("requiere_cambio_pin");

            Long empresaId = tenant.getEmpresaLegacyId() != null
                ? tenant.getEmpresaLegacyId() : -tenant.getId();

            String sessionToken = jwtTokenProvider.generateTokenWithContext(
                ugId, email, rol, empresaId, dispositivo.getSucursalId()
            );

            String nombreCompleto = nombre + (apellidos != null ? " " + apellidos : "");
            String avatar = nombre.substring(0, 1).toUpperCase() +
                (apellidos != null ? apellidos.substring(0, 1).toUpperCase() : "");

            log.info("Login GLOBAL exitoso: {} en tenant {}", email, tenant.getCodigo());

            return LoginPinResponse.builder()
                .sessionToken(sessionToken)
                .usuario(UsuarioLocalInfo.builder()
                    .id(ugId).nombre(nombre).apellidos(apellidos)
                    .nombreCompleto(nombreCompleto).rol(rol).avatar(avatar)
                    .build())
                .tenant(TenantResumen.builder()
                    .id(tenant.getId()).codigo(tenant.getCodigo()).nombre(tenant.getNombre()).build())
                .sucursales(List.of())
                .requiereCambioPin(Boolean.TRUE.equals(requiereCambioPin))
                .build();
        }

        // ── Flujo SCHEMA (ADMIN, CAJERO, MESERO, etc.) ──────────────────────
        Map<String, Object> usuarioData = buscarUsuarioEnTenant(schemaName, request.getUsuarioId());
        if (usuarioData == null) throw new NotFoundException("Usuario no encontrado");

        Boolean activo = (Boolean) usuarioData.get("activo");
        if (!Boolean.TRUE.equals(activo)) throw new UnauthorizedException("Usuario desactivado");

        String pinAlmacenado = (String) usuarioData.get("pin");
        if (pinAlmacenado == null || pinAlmacenado.isBlank())
            throw new BadRequestException("Usuario requiere configuración de PIN");
        if (!passwordEncoder.matches(request.getPin(), pinAlmacenado))
            throw new UnauthorizedException("PIN incorrecto");

        Long usuarioId = ((Number) usuarioData.get("id")).longValue();
        String nombre = (String) usuarioData.get("nombre");
        String apellidos = (String) usuarioData.get("apellidos");
        String email = (String) usuarioData.get("email");
        String rol = (String) usuarioData.get("rol");
        Boolean requiereCambioPin = (Boolean) usuarioData.get("requiere_cambio_pin");

        List<SucursalResumen> sucursales = obtenerSucursalesDelTenant(schemaName, usuarioId);

        Long empresaId = tenant.getEmpresaLegacyId() != null
            ? tenant.getEmpresaLegacyId() : -tenant.getId();

        String sessionToken = jwtTokenProvider.generateTokenWithContext(
            usuarioId,
            email != null ? email : "user_" + usuarioId,
            rol, empresaId,
            sucursales.isEmpty() ? null : sucursales.get(0).getId()
        );

        log.info("Login SCHEMA exitoso: usuario={} en tenant {}", usuarioId, tenant.getCodigo());

        String nombreCompleto = nombre + (apellidos != null && !apellidos.isBlank() ? " " + apellidos : "");
        String avatar = nombre != null && !nombre.isBlank() ? nombre.substring(0, 1).toUpperCase() +
                                                              (apellidos != null && !apellidos.isBlank() ? apellidos.substring(0, 1).toUpperCase() : "") : "";

        return LoginPinResponse.builder()
            .sessionToken(sessionToken)
            .usuario(UsuarioLocalInfo.builder()
                .id(usuarioId).nombre(nombre).apellidos(apellidos)
                .nombreCompleto(nombreCompleto).rol(rol).avatar(avatar)
                .build())
            .tenant(TenantResumen.builder()
                .id(tenant.getId()).codigo(tenant.getCodigo()).nombre(tenant.getNombre()).build())
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
        log.info("Cambiando PIN para usuario {} fuente={}", usuarioId, request.getFuente());

        if (request.getLongitud() != 4 && request.getLongitud() != 6)
            throw new BadRequestException("La longitud del PIN debe ser 4 o 6 dígitos");
        if (request.getNuevoPin().length() != request.getLongitud())
            throw new BadRequestException("El PIN debe tener exactamente " + request.getLongitud() + " dígitos");
        if (!request.getNuevoPin().matches("\\d+"))
            throw new BadRequestException("El PIN solo debe contener dígitos");

        String pinEncriptado = passwordEncoder.encode(request.getNuevoPin());

        if ("GLOBAL".equals(request.getFuente())) {
            // Actualizar en public.usuarios_globales
            int updated = jdbcTemplate.update(
                "UPDATE public.usuarios_globales SET pin = ?, pin_longitud = ?, requiere_cambio_pin = false WHERE id = ?",
                pinEncriptado, request.getLongitud(), usuarioId
            );
            if (updated == 0) throw new NotFoundException("Usuario global no encontrado");
        } else {
            // Actualizar en schema del tenant via JPA
            Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
            usuario.setPin(pinEncriptado);
            usuario.setPinLongitud(request.getLongitud());
            usuario.setRequiereCambioPin(false);
            usuario.setUpdatedAt(LocalDateTime.now());
            usuarioRepository.save(usuario);
        }

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
            return usuarioRepository.findByEmpresaId(empresaLegacyId); // ← lee public.usuarios
        }
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
        if (usuario.getApellidos() != null && !usuario.getApellidos().isBlank())
            nombreCompleto += " " + usuario.getApellidos();

        String avatar = "";
        if (usuario.getNombre() != null && !usuario.getNombre().isBlank()) {
            avatar = usuario.getNombre().substring(0, 1).toUpperCase();
            if (usuario.getApellidos() != null && !usuario.getApellidos().isBlank())
                avatar += usuario.getApellidos().substring(0, 1).toUpperCase();
        }

        return UsuarioLocalInfo.builder()
            .id(usuario.getId())
            .nombre(usuario.getNombre())
            .apellidos(usuario.getApellidos())
            .nombreCompleto(nombreCompleto)
            .rol(usuario.getRol().name())
            .avatar(avatar)
            .fuente("SCHEMA")  // ← siempre SCHEMA para usuarios del tenant
            .build();
    }

    private List<UsuarioLocalInfo> obtenerUsuariosDelTenantViaJdbc(String schemaName) {
        String sql = String.format("""
        SELECT id, nombre, apellidos, rol, pin_longitud, requiere_cambio_pin
        FROM %s.usuarios
        WHERE activo = true
          AND pin IS NOT NULL
          AND rol NOT IN ('ROOT', 'SOPORTE')
        ORDER BY nombre
        """, schemaName);
        try {
            return jdbcTemplate.query(sql, (rs, rn) -> {
                String nombre = rs.getString("nombre");
                String apellidos = rs.getString("apellidos");
                String nombreCompleto = nombre + (apellidos != null ? " " + apellidos : "");
                String avatar = nombre.substring(0, 1).toUpperCase() +
                    (apellidos != null && !apellidos.isBlank()
                        ? apellidos.substring(0, 1).toUpperCase() : "");
                return UsuarioLocalInfo.builder()
                    .id(rs.getLong("id"))
                    .nombre(nombre)
                    .apellidos(apellidos)
                    .nombreCompleto(nombreCompleto)
                    .rol(rs.getString("rol"))
                    .avatar(avatar)
                    .fuente("SCHEMA")
                    .build();
            });
        } catch (Exception e) {
            log.warn("Error obteniendo usuarios del schema {}: {}", schemaName, e.getMessage());
            return List.of();
        }
    }
}
