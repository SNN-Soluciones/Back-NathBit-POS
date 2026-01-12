package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.dispositivo.*;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.exception.BadRequestException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.exception.UnauthorizedException;
import com.snnsoluciones.backnathbitpos.repository.*;
import com.snnsoluciones.backnathbitpos.service.AsistenciaService;
import com.snnsoluciones.backnathbitpos.service.DispositivoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final DispositivoPdvRepository dispositivoRepository;
    private final TokenRegistroRepository tokenRegistroRepository;
    private final EmpresaRepository empresaRepository;
    private final SucursalRepository sucursalRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioEmpresaRepository usuarioEmpresaRepository;
    private final AsistenciaService asistenciaService;

    private static final String BASE_URL_FRONTEND = "https://pos.nathbit.com"; // TODO: Mover a properties
    private static final String BASE_URL_API = "https://api.nathbit.com"; // TODO: Mover a properties

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

        // 2. Verificar si ya existe un dispositivo con ese UUID
        if (request.getDeviceInfo() != null && request.getDeviceInfo().getUuid() != null) {
            Optional<DispositivoPdv> dispositivoExistente = dispositivoRepository
                .findByUuidHardware(request.getDeviceInfo().getUuid());

            if (dispositivoExistente.isPresent()) {
                throw new BadRequestException("Este dispositivo ya está registrado");
            }
        }

        // 3. Generar device token permanente
        String deviceToken = generarDeviceToken();

        // 4. Crear dispositivo
        DispositivoPdv dispositivo = DispositivoPdv.builder()
            .deviceToken(deviceToken)
            .nombre(tokenRegistro.getNombreDispositivo())
            .empresa(tokenRegistro.getEmpresa())
            .sucursal(tokenRegistro.getSucursal())
            .sucursalNombre(tokenRegistro.getSucursal().getNombre())
            .activo(true)
            .ultimoUso(LocalDateTime.now())
            .ipRegistro(ipCliente)
            .build();

        // 5. Agregar info técnica del dispositivo si está disponible
        if (request.getDeviceInfo() != null) {
            dispositivo.setUuidHardware(request.getDeviceInfo().getUuid());
            dispositivo.setModelo(request.getDeviceInfo().getModelo());
            dispositivo.setPlataforma(request.getDeviceInfo().getPlataforma());
            dispositivo.setUserAgent(request.getDeviceInfo().getUserAgent());
        }

        dispositivoRepository.save(dispositivo);

        // 6. Marcar token como usado
        tokenRegistro.marcarComoUsado();
        tokenRegistroRepository.save(tokenRegistro);

        log.info("Dispositivo registrado exitosamente - ID: {}, Token: {}",
            dispositivo.getId(), deviceToken);

        // 7. Construir response
        Empresa empresa = tokenRegistro.getEmpresa();
        Sucursal sucursal = tokenRegistro.getSucursal();

        return RegistrarDispositivoResponse.builder()
            .deviceToken(deviceToken)
            .empresa(RegistrarDispositivoResponse.EmpresaInfo.builder()
                .id(empresa.getId())
                .nombre(empresa.getNombreRazonSocial())
                .nombreComercial(empresa.getNombreComercial())
                .build())
            .sucursal(RegistrarDispositivoResponse.SucursalInfo.builder()
                .id(sucursal.getId())
                .nombre(sucursal.getNombre())
                .build())
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DispositivoUsuariosResponse obtenerUsuariosDispositivo(String deviceToken) {
        log.info("Obteniendo usuarios para dispositivo");

        // 1. Validar dispositivo
        DispositivoPdv dispositivo = dispositivoRepository.findByDeviceTokenAndActivoTrue(deviceToken)
            .orElseThrow(() -> new UnauthorizedException("Dispositivo no autorizado o inactivo"));

        // 2. Actualizar último uso (en transacción aparte para no afectar la consulta)
        registrarUso(dispositivo);

        // 3. Obtener usuarios de la empresa (excluyendo ROOT y SOPORTE)
        List<Usuario> usuarios = usuarioRepository.findByEmpresaId(dispositivo.getEmpresa().getId())
            .stream()
            .filter(u -> u.getActivo() && !u.esRolSistema() && u.getPin() != null)
            .collect(Collectors.toList());

        // 4. Mapear a DTOs con campos adicionales
        List<DispositivoUsuariosResponse.UsuarioInfo> usuariosInfo = usuarios.stream()
            .map(u -> {
                // Calcular longitud del PIN
                Integer longitudPin = null;
                if (u.getPin() != null) {
                    longitudPin = u.getPinLongitud() != null
                        ? u.getPinLongitud()
                        : u.getPin().length(); // Fallback si no está en pinLongitud
                }

                // Verificar si tiene entrada activa hoy
                boolean tieneEntradaActiva = asistenciaService.tieneEntradaActiva(u.getId());

                // Generar color de avatar
                String avatarColor = generarAvatarColor(u.getId());

                return DispositivoUsuariosResponse.UsuarioInfo.builder()
                    .id(u.getId())
                    .nombre(u.getNombre())
                    .apellidos(u.getApellidos())
                    .nombreCompleto(u.getNombre() + " " + (u.getApellidos() != null ? u.getApellidos() : ""))
                    .rol(u.getRol().name())
                    .tienePin(u.getPin() != null)
                    .longitudPin(longitudPin)
                    .tieneEntradaActiva(tieneEntradaActiva)
                    .avatarColor(avatarColor)
                    .build();
            })
            .collect(Collectors.toList());

        // 5. Construir response
        return DispositivoUsuariosResponse.builder()
            .empresa(DispositivoUsuariosResponse.EmpresaInfo.builder()
                .id(dispositivo.getEmpresa().getId())
                .nombreComercial(dispositivo.getEmpresa().getNombreComercial())
                .build())
            .sucursal(DispositivoUsuariosResponse.SucursalInfo.builder()
                .id(dispositivo.getSucursal().getId())
                .nombre(dispositivo.getSucursal().getNombre())
                .build())
            .usuarios(usuariosInfo)
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DispositivoPdv> buscarPorToken(String deviceToken) {
        return dispositivoRepository.findByDeviceToken(deviceToken);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DispositivoPdv> buscarActivoPorToken(String deviceToken) {
        return dispositivoRepository.findByDeviceTokenAndActivoTrue(deviceToken);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DispositivoDTO> listarDispositivosPorEmpresa(Long empresaId) {
        List<DispositivoPdv> dispositivos = dispositivoRepository.findByEmpresaIdWithRelations(empresaId);

        return dispositivos.stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SucursalSimpleDTO> listarSucursalesPorEmpresa(Long empresaId) {
        // Validar que empresa existe
        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

        // Obtener sucursales activas
        List<Sucursal> sucursales = sucursalRepository.findAllByEmpresaIdAndActivaTrue(empresaId);

        // Mapear a DTO simple
        return sucursales.stream()
            .map(s -> SucursalSimpleDTO.builder()
                .id(s.getId())
                .nombre(s.getNombre())
                .build())
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void activarDispositivo(Long id) {
        DispositivoPdv dispositivo = dispositivoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Dispositivo no encontrado"));

        dispositivo.activar();
        dispositivoRepository.save(dispositivo);

        log.info("Dispositivo activado - ID: {}", id);
    }

    @Override
    @Transactional
    public void desactivarDispositivo(Long id) {
        DispositivoPdv dispositivo = dispositivoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Dispositivo no encontrado"));

        dispositivo.desactivar();
        dispositivoRepository.save(dispositivo);

        log.info("Dispositivo desactivado - ID: {}", id);
    }

    @Override
    @Transactional
    public void registrarUso(DispositivoPdv dispositivo) {
        dispositivo.registrarUso();
        dispositivoRepository.save(dispositivo);
    }

    // ==================== MÉTODOS PRIVADOS ====================

    /**
     * Genera un token único para registro (formato: REG-uuid)
     */
    private String generarTokenUnico() {
        String uuid = UUID.randomUUID().toString().substring(0, 13);
        return "REG-" + uuid;
    }

    /**
     * Genera un device token permanente (formato: DEV-uuid)
     */
    private String generarDeviceToken() {
        String uuid = UUID.randomUUID().toString();
        return "DEV-" + uuid;
    }

    /**
     * Mapea Dispositivo a DTO
     */
    private DispositivoDTO mapToDTO(DispositivoPdv dispositivo) {
        return DispositivoDTO.builder()
            .id(dispositivo.getId())
            .deviceToken(dispositivo.getDeviceToken())
            .nombre(dispositivo.getNombre())
            .empresaId(dispositivo.getEmpresa().getId())
            .empresaNombre(dispositivo.getEmpresa().getNombreComercial())
            .sucursalId(dispositivo.getSucursal().getId())
            .sucursalNombre(dispositivo.getSucursal().getNombre())
            .modelo(dispositivo.getModelo())
            .plataforma(dispositivo.getPlataforma())
            .activo(dispositivo.getActivo())
            .ultimoUso(dispositivo.getUltimoUso())
            .createdAt(dispositivo.getCreatedAt())
            .build();
    }

    /**
     * Genera un color único para el avatar del usuario basado en su ID
     */
    private String generarAvatarColor(Long usuarioId) {
        String[] colores = {
            "#9333ea", // purple
            "#3b82f6", // blue
            "#10b981", // green
            "#f59e0b", // amber
            "#ef4444", // red
            "#8b5cf6", // violet
            "#06b6d4", // cyan
            "#f97316"  // orange
        };

        int index = (int) (usuarioId % colores.length);
        return colores[index];
    }
}