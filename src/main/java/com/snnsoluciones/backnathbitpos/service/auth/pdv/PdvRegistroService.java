// Path: src/main/java/com/snnsoluciones/backnathbitpos/service/auth/pdv/PdvRegistroService.java

package com.snnsoluciones.backnathbitpos.service.auth.pdv;

import com.snnsoluciones.backnathbitpos.dto.auth.pdv.PdvRegistroDTOs.*;
import com.snnsoluciones.backnathbitpos.entity.Terminal;
import com.snnsoluciones.backnathbitpos.entity.global.*;
import com.snnsoluciones.backnathbitpos.exception.BadRequestException;
import com.snnsoluciones.backnathbitpos.exception.NotFoundException;
import com.snnsoluciones.backnathbitpos.exception.UnauthorizedException;
import com.snnsoluciones.backnathbitpos.repository.SucursalRepository;
import com.snnsoluciones.backnathbitpos.repository.TerminalRepository;
import com.snnsoluciones.backnathbitpos.repository.global.*;
import com.snnsoluciones.backnathbitpos.security.jwt.JwtTokenProvider;
import com.snnsoluciones.backnathbitpos.service.EmailService;
import com.snnsoluciones.backnathbitpos.service.auth.multitenant.AuthDispositivoService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PdvRegistroService {

    private final TenantRepository tenantRepository;
    private final CodigoRegistroRepository codigoRegistroRepository;
    private final DispositivoRepository dispositivoRepository;
    private final UsuarioGlobalRepository usuarioGlobalRepository;
    private final SuperAdminTenantRepository superAdminTenantRepository;
    private final EmailService emailService;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TerminalRepository terminalRepository;
    private final SucursalRepository sucursalRepository;
    private final AuthDispositivoService authDispositivoService;

    // Email fijo de SNN que siempre recibe el código
    private static final String EMAIL_SNN = "info@snnsoluciones.com";
    // Duración del registration token en minutos
    private static final int MINUTOS_REGISTRATION_TOKEN = 5;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    // ══════════════════════════════════════════════════════════════════════
    // 1. SOLICITAR CÓDIGO OTP
    // ══════════════════════════════════════════════════════════════════════
    public SolicitarCodigoResponse solicitarCodigo(SolicitarCodigoRequest request, String ip, String userAgent) {
        log.info("Solicitar código OTP para tenant: {}", request.getTenantCodigo());

        // Buscar tenant
        Tenant tenant = tenantRepository.findByCodigoIgnoreCase(request.getTenantCodigo())
            .orElseThrow(() -> new NotFoundException("Empresa no encontrada"));

        if (!tenant.estaActivo()) {
            throw new BadRequestException("La empresa no está activa");
        }

        // Invalidar códigos anteriores del mismo tenant sin nombre de dispositivo
        LocalDateTime ahora = LocalDateTime.now();
        codigoRegistroRepository.invalidarCodigosAnteriores(tenant.getId(), "PDV_REGISTRO", ahora);

        // Crear nuevo código — sin nombre de dispositivo (se pide en el registro)
        CodigoRegistro codigo = CodigoRegistro.crear(tenant, "PDV_REGISTRO", ip, userAgent);
        codigo = codigoRegistroRepository.save(codigo);

        // Recolectar destinatarios: SNN + todos los SUPER_ADMIN del tenant
        Set<String> destinatarios = new LinkedHashSet<>();
        destinatarios.add(EMAIL_SNN);

        List<UsuarioGlobal> admins = usuarioGlobalRepository.findPropietariosByTenantId(tenant.getId());
        admins.forEach(a -> destinatarios.add(a.getEmail()));

        // Enviar a todos
        final String codigoStr = codigo.getCodigo();
        final String tenantNombre = tenant.getNombre();
        for (String dest : destinatarios) {
            try {
                emailService.enviarCodigoRegistroDispositivo(
                    dest,
                    dest.equals(EMAIL_SNN) ? "Equipo NathBit" : admins.stream()
                        .filter(a -> a.getEmail().equals(dest)).findFirst()
                        .map(UsuarioGlobal::getNombre).orElse("Administrador"),
                    tenantNombre,
                    "Nuevo dispositivo PDV",
                    codigoStr,
                    ip,
                    "WEB",
                    CodigoRegistro.MINUTOS_EXPIRACION
                );
                log.info("OTP enviado a: {}", dest);
            } catch (Exception e) {
                log.warn("Error enviando OTP a {}: {}", dest, e.getMessage());
            }
        }

        return SolicitarCodigoResponse.builder()
            .tenantNombre(tenantNombre)
            .mensaje("Código enviado al correo de administradores")
            .expiraEnSegundos(CodigoRegistro.MINUTOS_EXPIRACION * 60)
            .build();
    }

    // ══════════════════════════════════════════════════════════════════════
    // 2. VALIDAR CÓDIGO → devuelve registrationToken + sucursales
    // ══════════════════════════════════════════════════════════════════════
    @Transactional
    public ValidarCodigoResponse validarCodigo(ValidarCodigoRequest request) {
        log.info("Validar OTP para tenant: {}", request.getTenantCodigo());

        Tenant tenant = tenantRepository.findByCodigoIgnoreCase(request.getTenantCodigo())
            .orElseThrow(() -> new NotFoundException("Empresa no encontrada"));

        // Buscar código válido — sin nombre de dispositivo específico
        CodigoRegistro codigo = codigoRegistroRepository.findCodigoValido(
            tenant.getId(), request.getCodigo(), LocalDateTime.now()
        ).orElseThrow(() -> new BadRequestException("Código inválido o expirado"));

        // Marcar como usado
        codigo.marcarComoUsado();
        codigoRegistroRepository.save(codigo);

        // Generar registrationToken: JWT corto (5 min) con tenantId
        String registrationToken = generarRegistrationToken(tenant.getId());

        // Obtener sucursales del schema
        List<SucursalSimple> sucursales = obtenerSucursales(tenant.getSchemaName());

        log.info("OTP validado para tenant {}. Sucursales: {}", tenant.getCodigo(), sucursales.size());

        return ValidarCodigoResponse.builder()
            .registrationToken(registrationToken)
            .tenantId(tenant.getId())
            .tenantNombre(tenant.getNombre())
            .sucursales(sucursales)
            .build();
    }

    // ══════════════════════════════════════════════════════════════════════
    // 3. REGISTRAR DISPOSITIVO (Flujo A con registrationToken O Flujo B con JWT)
    // ══════════════════════════════════════════════════════════════════════
    @Transactional
    public RegistrarDispositivoResponse registrarDispositivo(
            RegistrarDispositivoRequest request,
            Long usuarioGlobalId, // null si es Flujo A
            String ip,
            String userAgent) {

        Long tenantId;

        if (request.getRegistrationToken() != null && !request.getRegistrationToken().isBlank()) {
            // ── Flujo A: validar registrationToken ────────────────────────
            tenantId = extraerTenantIdDeRegistrationToken(request.getRegistrationToken());
            log.info("Flujo A — registrationToken válido. tenantId={}", tenantId);

        } else if (usuarioGlobalId != null) {
            // ── Flujo B: validar que el usuario tiene acceso al tenant ────
            tenantId = request.getTenantId();
            if (tenantId == null) throw new BadRequestException("tenantId requerido en Flujo B");

            UsuarioGlobal usuario = usuarioGlobalRepository.findById(usuarioGlobalId)
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));

            boolean tieneAcceso = usuario.esRolSistema() ||
                superAdminTenantRepository.existsActiveByUsuarioIdAndTenantId(usuarioGlobalId, tenantId);

            if (!tieneAcceso) throw new UnauthorizedException("No tiene acceso a esta empresa");
            log.info("Flujo B — usuario {} autorizado para tenantId={}", usuarioGlobalId, tenantId);

        } else {
            throw new BadRequestException("Se requiere registrationToken o autenticación con credenciales");
        }

        // Buscar tenant
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new NotFoundException("Empresa no encontrada"));

        // Validar que la sucursal existe en el schema
        String sucursalNombre = obtenerNombreSucursal(tenant.getSchemaName(), request.getSucursalId());
        if (sucursalNombre == null) throw new BadRequestException("Sucursal no encontrada");

        // Crear dispositivo
        Dispositivo.Plataforma plataforma = parsePlataforma(request.getPlataforma());
// ✅ NUEVO
        String tipo = request.getTipo() != null ? request.getTipo() : "PDV";

        Dispositivo dispositivo = Dispositivo.crear(tenant, request.getNombreDispositivo(), plataforma, userAgent, ip);
        dispositivo.setSucursalId(request.getSucursalId());
        dispositivo.setSucursalNombre(sucursalNombre);
        dispositivo.setTipo(tipo);

        if ("KIOSKO".equals(tipo)) {
            Terminal terminal = authDispositivoService.asignarOCrearTerminalKiosko(tenant.getSchemaName(), request.getSucursalId());
            dispositivo.setTerminalId(terminal.getId());
        } else {
            dispositivo.setTerminalId(request.getTerminalId());
        }

        dispositivo = dispositivoRepository.save(dispositivo);

        if ("KIOSKO".equals(tipo)) {
            final Long terminalId = dispositivo.getTerminalId();
            final Long dispositivoId = dispositivo.getId();
            jdbcTemplate.update(
                String.format("UPDATE %s.terminales SET dispositivo_id = ? WHERE id = ?", tenant.getSchemaName()),
                dispositivoId, terminalId
            );
        }
        log.info("Dispositivo '{}' registrado para tenant '{}' sucursal '{}'",
            dispositivo.getNombre(), tenant.getCodigo(), sucursalNombre);

        return RegistrarDispositivoResponse.builder()
            .deviceToken(dispositivo.getToken())
            .tenantId(tenant.getId())
            .tenantNombre(tenant.getNombre())
            .sucursalId(request.getSucursalId())
            .sucursalNombre(sucursalNombre)
            .dispositivoId(dispositivo.getId())
            .dispositivoNombre(dispositivo.getNombre())
            .plataforma(plataforma != null ? plataforma.name() : null)
            .build();
    }

    // ══════════════════════════════════════════════════════════════════════
    // 4. LOGIN CON CREDENCIALES → retorna tenants + sucursales
    // ══════════════════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public LoginCredencialesResponse loginCredenciales(LoginCredencialesRequest request) {
        log.info("Login credenciales PDV: {}", request.getEmail());

        UsuarioGlobal usuario = usuarioGlobalRepository.findByEmailIgnoreCase(request.getEmail())
            .orElseThrow(() -> new UnauthorizedException("Credenciales inválidas"));

        if (!usuario.estaActivo()) throw new UnauthorizedException("Usuario desactivado");

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new UnauthorizedException("Credenciales inválidas");
        }

        // Solo ROOT, SOPORTE y SUPER_ADMIN pueden usar este endpoint
        String rol = usuario.getRol().name();
        if (!List.of("ROOT", "SOPORTE", "SUPER_ADMIN").contains(rol)) {
            throw new UnauthorizedException("No tiene permisos para registrar dispositivos");
        }

        // Generar JWT normal
        String token = jwtTokenProvider.generateToken(usuario.getId(), usuario.getEmail(), rol);

        // Obtener tenants con sus sucursales
        List<TenantConSucursales> tenants = obtenerTenantsConSucursales(usuario);

        return LoginCredencialesResponse.builder()
            .token(token)
            .usuario(UsuarioInfo.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .apellidos(usuario.getApellidos())
                .email(usuario.getEmail())
                .rol(rol)
                .build())
            .tenants(tenants)
            .build();
    }

    // ══════════════════════════════════════════════════════════════════════
    // HELPERS PRIVADOS
    // ══════════════════════════════════════════════════════════════════════

    private String generarRegistrationToken(Long tenantId) {
        return Jwts.builder()
            .setSubject("pdv-registration")
            .claim("tenantId", tenantId)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + MINUTOS_REGISTRATION_TOKEN * 60 * 1000L))
            .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
            .compact();
    }

    private Long extraerTenantIdDeRegistrationToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();

            if (!"pdv-registration".equals(claims.getSubject())) {
                throw new BadRequestException("Token de registro inválido");
            }

            return claims.get("tenantId", Long.class);
        } catch (Exception e) {
            log.warn("registrationToken inválido o expirado: {}", e.getMessage());
            throw new BadRequestException("Token de registro inválido o expirado. Solicite un nuevo código.");
        }
    }

    private List<SucursalSimple> obtenerSucursales(String schemaName) {
        String sql = String.format("""
            SELECT id, nombre, numero_sucursal
            FROM %s.sucursales
            WHERE activa = true
            ORDER BY nombre
            """, schemaName);
        try {
            return jdbcTemplate.query(sql, (rs, rn) -> SucursalSimple.builder()
                .id(rs.getLong("id"))
                .nombre(rs.getString("nombre"))
                .numeroSucursal(rs.getString("numero_sucursal"))
                .build());
        } catch (Exception e) {
            log.warn("Error obteniendo sucursales de {}: {}", schemaName, e.getMessage());
            return List.of();
        }
    }

    private String obtenerNombreSucursal(String schemaName, Long sucursalId) {
        try {
            return jdbcTemplate.queryForObject(
                String.format("SELECT nombre FROM %s.sucursales WHERE id = ? AND activa = true", schemaName),
                String.class, sucursalId);
        } catch (Exception e) {
            return null;
        }
    }

    private List<TenantConSucursales> obtenerTenantsConSucursales(UsuarioGlobal usuario) {
        List<Tenant> tenants;

        if (usuario.esRolSistema()) {
            tenants = tenantRepository.findByActivoTrueOrderByNombreAsc();
        } else {
            tenants = superAdminTenantRepository.findByUsuarioIdAndActivoTrue(usuario.getId())
                .stream()
                .map(SuperAdminTenant::getTenant)
                .filter(Tenant::estaActivo)
                .toList();
        }

        return tenants.stream().map(t -> TenantConSucursales.builder()
            .id(t.getId())
            .codigo(t.getCodigo())
            .nombre(t.getNombre())
            .sucursales(obtenerSucursales(t.getSchemaName()))
            .build()
        ).toList();
    }

    private Dispositivo.Plataforma parsePlataforma(String p) {
        if (p == null || p.isBlank()) return Dispositivo.Plataforma.WEB;
        try { return Dispositivo.Plataforma.valueOf(p.toUpperCase()); }
        catch (Exception e) { return Dispositivo.Plataforma.WEB; }
    }
}