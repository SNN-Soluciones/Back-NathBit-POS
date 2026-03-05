package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.dispositivo.*;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.entity.global.Dispositivo;
import com.snnsoluciones.backnathbitpos.entity.global.Tenant;
import com.snnsoluciones.backnathbitpos.exception.BadRequestException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.exception.UnauthorizedException;
import com.snnsoluciones.backnathbitpos.repository.EmpresaRepository;
import com.snnsoluciones.backnathbitpos.repository.SucursalRepository;
import com.snnsoluciones.backnathbitpos.repository.TokenRegistroRepository;
import com.snnsoluciones.backnathbitpos.repository.global.DispositivoRepository;
import com.snnsoluciones.backnathbitpos.repository.global.TenantRepository;
import com.snnsoluciones.backnathbitpos.service.DispositivoService;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de gestión de dispositivos PDV
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DispositivoServiceImpl implements DispositivoService {

    private final TokenRegistroRepository tokenRegistroRepository;
    private final DispositivoRepository dispositivoRepository;   // ← el de global
    private final TenantRepository tenantRepository;             // ← NUEVO
    private final EmpresaRepository empresaRepository;
    private final SucursalRepository sucursalRepository;
    private final JdbcTemplate jdbcTemplate;                     // ← NUEVO (para query usuarios del tenant)

    @Value("${app.base-url:http://localhost:8081}")
    private String BASE_URL_API;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String BASE_URL_FRONTEND;

    @Override
    @Transactional
    public GenerarTokenResponse generarTokenRegistro(GenerarTokenRequest request) {
        log.info("Generando token de registro - Empresa: {}, Sucursal: {}, Dispositivo: {}",
            request.getEmpresaId(), request.getSucursalId(), request.getNombreDispositivo());

        // 1. Validar que empresa existe
        Empresa empresa = empresaRepository.findById(request.getEmpresaId())
            .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

        // 2. Validar que sucursal existe y pertenece a la empresa
        Sucursal sucursal = sucursalRepository.findById(request.getSucursalId())
            .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));

        if (!sucursal.getEmpresa().getId().equals(empresa.getId())) {
            throw new BadRequestException("La sucursal no pertenece a la empresa especificada");
        }

        // 3. Verificar si ya existe un token activo para esta sucursal y nombre
        boolean existeTokenActivo = tokenRegistroRepository.existsTokenActivoBySucursalAndNombre(
            sucursal.getId(),
            request.getNombreDispositivo(),
            LocalDateTime.now()
        );

        if (existeTokenActivo) {
            throw new BadRequestException(
                "Ya existe un token activo para un dispositivo con ese nombre en esta sucursal"
            );
        }

        // 4. Generar token único
        String token = generarTokenUnico();

        // 5. Crear registro de token (expira en 24 horas)
        TokenRegistro tokenRegistro = TokenRegistro.builder()
            .token(token)
            .empresa(empresa)
            .sucursal(sucursal)
            .nombreDispositivo(request.getNombreDispositivo())
            .usado(false)
            .expiraEn(LocalDateTime.now().plusHours(24))
            .build();

        tokenRegistroRepository.save(tokenRegistro);

        log.info("Token de registro generado exitosamente: {}", token);

        // 6. Construir response
        return GenerarTokenResponse.builder()
            .token(token)
            .qrCodeUrl(BASE_URL_API + "/qr/" + token)
            .registrationUrl(BASE_URL_FRONTEND + "/register?token=" + token)
            .expiraEn(tokenRegistro.getExpiraEn())
            .nombreDispositivo(request.getNombreDispositivo())
            .sucursalNombre(sucursal.getNombre())
            .build();
    }

    @Override
    @Transactional
    public RegistrarDispositivoResponse registrarDispositivo(RegistrarDispositivoRequest request, String ipCliente) {
        log.info("Registrando dispositivo con token: {}", request.getToken());

        // 1. Buscar y validar token
        TokenRegistro tokenRegistro = tokenRegistroRepository
            .findByTokenAndValidoTrue(request.getToken(), LocalDateTime.now())
            .orElseThrow(() -> new BadRequestException("Token inválido, expirado o ya utilizado"));

        // 2. Buscar tenant por empresa legacy
        Tenant tenant = tenantRepository.findByEmpresaLegacyId(tokenRegistro.getEmpresa().getId())
            .orElseThrow(() -> new BadRequestException(
                "La empresa aún no está migrada al sistema multi-tenant"));

        // 3. Crear dispositivo
        Dispositivo dispositivo = Dispositivo.builder()
            .tenant(tenant)
            .nombre(tokenRegistro.getNombreDispositivo())
            .token(Dispositivo.generarToken())
            .sucursalId(tokenRegistro.getSucursal().getId())
            .sucursalNombre(tokenRegistro.getSucursal().getNombre())
            .activo(true)
            .ipRegistro(ipCliente)
            .build();

        if (request.getDeviceInfo() != null) {
            dispositivo.setPlataforma(request.getDeviceInfo().getPlataforma() != null
                ? Dispositivo.Plataforma.valueOf(request.getDeviceInfo().getPlataforma().toUpperCase())
                : null);
            dispositivo.setUserAgent(request.getDeviceInfo().getUserAgent());
        }

        dispositivoRepository.save(dispositivo);

        // 4. Marcar token como usado
        tokenRegistro.marcarComoUsado();
        tokenRegistroRepository.save(tokenRegistro);

        log.info("Dispositivo registrado - ID: {}, Tenant: {}", dispositivo.getId(), tenant.getCodigo());

        return RegistrarDispositivoResponse.builder()
            .deviceToken(dispositivo.getToken())
            .empresa(RegistrarDispositivoResponse.EmpresaInfo.builder()
                .id(tokenRegistro.getEmpresa().getId())
                .nombre(tokenRegistro.getEmpresa().getNombreRazonSocial())
                .nombreComercial(tokenRegistro.getEmpresa().getNombreComercial())
                .build())
            .sucursal(RegistrarDispositivoResponse.SucursalInfo.builder()
                .id(tokenRegistro.getSucursal().getId())
                .nombre(tokenRegistro.getSucursal().getNombre())
                .build())
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DispositivoUsuariosResponse obtenerUsuariosDispositivo(String deviceToken, Boolean includeRoot) {
        log.info("Obteniendo usuarios para dispositivo - includeRoot: {}", includeRoot);

        // 1. Validar dispositivo
        Dispositivo dispositivo = dispositivoRepository.findByTokenAndActivoTrue(deviceToken)
            .orElseThrow(() -> new UnauthorizedException("Dispositivo no autorizado o inactivo"));

        // 2. Registrar uso
        registrarUso(dispositivo);

        // 3. Obtener usuarios del schema del tenant via JDBC
        String schemaName = dispositivo.getTenant().getSchemaName();
        String sql = String.format("""
        SELECT id, nombre, apellidos, rol, pin_longitud, requiere_cambio_pin
        FROM %s.usuarios
        WHERE activo = true AND pin IS NOT NULL
        """, schemaName);

        List<DispositivoUsuariosResponse.UsuarioInfo> usuariosInfo = jdbcTemplate.query(sql, (rs, rowNum) -> {
            String rol = rs.getString("rol");

            if (!Boolean.TRUE.equals(includeRoot) && (rol.equals("ROOT") || rol.equals("SOPORTE"))) {
                return null;
            }

            return DispositivoUsuariosResponse.UsuarioInfo.builder()
                .id(rs.getLong("id"))
                .nombre(rs.getString("nombre"))
                .apellidos(rs.getString("apellidos"))
                .nombreCompleto(rs.getString("nombre") + " " +
                    (rs.getString("apellidos") != null ? rs.getString("apellidos") : ""))
                .rol(rol)
                .longitudPin(rs.getInt("pin_longitud"))
                .requiereCambioPin(rs.getBoolean("requiere_cambio_pin"))
                .tienePin(true)
                .build();
        });

        List<DispositivoUsuariosResponse.UsuarioInfo> usuariosFiltrados = usuariosInfo.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        log.info("Usuarios encontrados: {}", usuariosFiltrados.size());

        return DispositivoUsuariosResponse.builder()
            .empresa(DispositivoUsuariosResponse.EmpresaInfo.builder()
                .id(dispositivo.getTenant().getId())
                .nombreComercial(dispositivo.getTenant().getNombre())
                .build())
            .sucursal(DispositivoUsuariosResponse.SucursalInfo.builder()
                .id(dispositivo.getSucursalId())
                .nombre(dispositivo.getSucursalNombre())
                .build())
            .usuarios(usuariosFiltrados)
            .build();
    }

    @Override
    public Optional<Dispositivo> buscarPorToken(String deviceToken) {
        return dispositivoRepository.findByToken(deviceToken);
    }

    @Override
    public Optional<Dispositivo> buscarActivoPorToken(String deviceToken) {
        return dispositivoRepository.findByTokenAndActivoTrue(deviceToken);
    }

    @Override
    @Transactional
    public void activarDispositivo(Long id) {
        Dispositivo dispositivo = dispositivoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Dispositivo no encontrado"));
        dispositivo.activar();
        dispositivoRepository.save(dispositivo);
        log.info("Dispositivo activado - ID: {}", id);
    }

    @Override
    @Transactional
    public void desactivarDispositivo(Long id) {
        Dispositivo dispositivo = dispositivoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Dispositivo no encontrado"));
        dispositivo.desactivar();
        dispositivoRepository.save(dispositivo);
        log.info("Dispositivo desactivado - ID: {}", id);
    }

    @Override
    @Transactional
    public void registrarUso(Dispositivo dispositivo) {
        dispositivo.registrarUso();
        dispositivoRepository.save(dispositivo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DispositivoDTO> listarDispositivosPorEmpresa(Long empresaId) {
        Tenant tenant = tenantRepository.findByEmpresaLegacyId(empresaId)
            .orElseThrow(() -> new ResourceNotFoundException("Empresa no migrada a tenant"));

        return dispositivoRepository.findByTenantId(tenant.getId()).stream()
            .map(d -> DispositivoDTO.builder()
                .id(d.getId())
                .nombre(d.getNombre())
                .deviceToken(d.getToken())
                .sucursalId(d.getSucursalId())
                .sucursalNombre(d.getSucursalNombre())
                .activo(d.getActivo())
                .ultimoUso(d.getUltimoUso())
                .plataforma(d.getPlataforma() != null ? d.getPlataforma().name() : null)
                .build())
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SucursalSimpleDTO> listarSucursalesPorEmpresa(Long empresaId) {
        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

        return sucursalRepository.findAllByEmpresaIdAndActivaTrue(empresaId).stream()
            .map(s -> SucursalSimpleDTO.builder()
                .id(s.getId())
                .nombre(s.getNombre())
                .build())
            .collect(Collectors.toList());
    }

    // ==================== MÉTODOS PRIVADOS ====================

    /**
     * Genera un token único para registro (formato: REG-uuid)
     */
    private String generarTokenUnico() {
        String uuid = UUID.randomUUID().toString().substring(0, 13);
        return "REG-" + uuid;
    }
}