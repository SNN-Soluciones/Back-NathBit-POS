package com.snnsoluciones.backnathbitpos.service.auth.multitenant;

import com.snnsoluciones.backnathbitpos.dto.auth.multitenant.AuthMultitenantDTOs.*;
import com.snnsoluciones.backnathbitpos.entity.global.*;
import com.snnsoluciones.backnathbitpos.entity.global.Dispositivo.Plataforma;
import com.snnsoluciones.backnathbitpos.exception.BadRequestException;
import com.snnsoluciones.backnathbitpos.exception.NotFoundException;
import com.snnsoluciones.backnathbitpos.repository.global.*;
import com.snnsoluciones.backnathbitpos.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio de autenticación para dispositivos.
 * Maneja el flujo de registro de nuevos dispositivos con código OTP.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthDispositivoService {

    private final TenantRepository tenantRepository;
    private final DispositivoRepository dispositivoRepository;
    private final CodigoRegistroRepository codigoRegistroRepository;
    private final UsuarioGlobalRepository usuarioGlobalRepository;
    private final EmailService emailService;

    /**
     * Máximo de códigos activos por tenant
     */
    private static final int MAX_CODIGOS_ACTIVOS_POR_TENANT = 10;

    /**
     * Valida el código de empresa y verifica el dispositivo.
     * Si el dispositivo ya está registrado, retorna la lista de usuarios.
     * Si no, indica que requiere registro.
     */
    public LoginEmpresaResponse validarEmpresa(String codigoEmpresa, String deviceToken, String ipCliente) {
        log.info("Validando empresa: código={}", codigoEmpresa);

        // Buscar tenant
        Tenant tenant = tenantRepository.findByCodigoIgnoreCase(codigoEmpresa)
            .orElseThrow(() -> new NotFoundException("Empresa no encontrada"));

        if (!tenant.estaActivo()) {
            throw new BadRequestException("La empresa no está activa");
        }

        TenantResumen tenantResumen = TenantResumen.builder()
            .id(tenant.getId())
            .codigo(tenant.getCodigo())
            .nombre(tenant.getNombre())
            .build();

        // Si no hay token, requiere registro
        if (deviceToken == null || deviceToken.isBlank()) {
            log.info("Dispositivo sin token, requiere registro");
            return LoginEmpresaResponse.builder()
                .tenant(tenantResumen)
                .requiereRegistro(true)
                .build();
        }

        // Verificar dispositivo
        Optional<Dispositivo> dispositivoOpt = dispositivoRepository.findByTokenWithTenant(deviceToken);

        if (dispositivoOpt.isEmpty()) {
            log.info("Token de dispositivo inválido o inactivo");
            return LoginEmpresaResponse.builder()
                .tenant(tenantResumen)
                .requiereRegistro(true)
                .build();
        }

        Dispositivo dispositivo = dispositivoOpt.get();

        // Verificar que el dispositivo pertenece al tenant correcto
        if (!dispositivo.getTenant().getId().equals(tenant.getId())) {
            log.warn("Dispositivo {} no pertenece al tenant {}", dispositivo.getId(), tenant.getCodigo());
            return LoginEmpresaResponse.builder()
                .tenant(tenantResumen)
                .requiereRegistro(true)
                .build();
        }

        // Actualizar último uso
        dispositivo.registrarUso();
        dispositivoRepository.save(dispositivo);

        // TODO: Obtener usuarios del tenant (esto requiere consultar el schema del tenant)
        // Por ahora retornamos lista vacía, se implementará en AuthPinService
        List<UsuarioLocalInfo> usuarios = List.of();

        log.info("Dispositivo {} validado para tenant {}", dispositivo.getNombre(), tenant.getCodigo());

        return LoginEmpresaResponse.builder()
            .tenant(tenantResumen)
            .dispositivo(DispositivoInfo.builder()
                .id(dispositivo.getId())
                .nombre(dispositivo.getNombre())
                .plataforma(dispositivo.getPlataforma() != null ? dispositivo.getPlataforma().name() : null)
                .ultimoUso(dispositivo.getUltimoUso())
                .build())
            .usuarios(usuarios)
            .requiereRegistro(false)
            .build();
    }

    /**
     * Solicita un código OTP para registrar un nuevo dispositivo.
     * Envía el código por email a los SUPER_ADMIN del tenant.
     */
    public SolicitarCodigoResponse solicitarCodigo(SolicitarCodigoRequest request, 
                                                    String ipCliente, 
                                                    String userAgent) {
        log.info("Solicitando código OTP para tenant={}, dispositivo={}", 
                 request.getTenantCodigo(), request.getNombreDispositivo());

        // Buscar tenant
        Tenant tenant = tenantRepository.findByCodigoIgnoreCase(request.getTenantCodigo())
            .orElseThrow(() -> new NotFoundException("Empresa no encontrada"));

        if (!tenant.estaActivo()) {
            throw new BadRequestException("La empresa no está activa");
        }

        // Verificar límite de códigos activos
        LocalDateTime ahora = LocalDateTime.now();
        long codigosActivos = codigoRegistroRepository.countCodigosActivosPorTenant(tenant.getId(), ahora);
        
        if (codigosActivos >= MAX_CODIGOS_ACTIVOS_POR_TENANT) {
            throw new BadRequestException("Demasiadas solicitudes pendientes. Intente más tarde.");
        }

        // Invalidar códigos anteriores del mismo dispositivo
        codigoRegistroRepository.invalidarCodigosAnteriores(
            tenant.getId(), 
            request.getNombreDispositivo(), 
            ahora
        );

        // Crear nuevo código
        CodigoRegistro codigo = CodigoRegistro.crear(
            tenant,
            request.getNombreDispositivo(),
            ipCliente,
            userAgent
        );
        codigo = codigoRegistroRepository.save(codigo);

        // Obtener emails de SUPER_ADMINs del tenant
        List<UsuarioGlobal> admins = usuarioGlobalRepository.findByTenantId(tenant.getId());
        
        if (admins.isEmpty()) {
            log.warn("No hay SUPER_ADMINs para el tenant {}", tenant.getCodigo());
            // Buscar propietarios específicamente
            admins = usuarioGlobalRepository.findPropietariosByTenantId(tenant.getId());
        }

        if (admins.isEmpty()) {
            throw new BadRequestException("No hay administradores configurados para esta empresa");
        }

        // Enviar email a cada admin
        for (UsuarioGlobal admin : admins) {
            try {
                emailService.enviarCodigoRegistroDispositivo(
                    admin.getEmail(),
                    admin.getNombre(),
                    tenant.getNombre(),
                    request.getNombreDispositivo(),
                    codigo.getCodigo(),
                    ipCliente,
                    request.getPlataforma(),
                    CodigoRegistro.MINUTOS_EXPIRACION
                );
                log.info("Email de código OTP enviado a: {}", admin.getEmail());
            } catch (Exception e) {
                log.error("Error enviando email a {}: {}", admin.getEmail(), e.getMessage());
            }
        }

        return SolicitarCodigoResponse.builder()
            .mensaje("Código enviado a los administradores de la empresa")
            .expiraEnSegundos(CodigoRegistro.MINUTOS_EXPIRACION * 60)
            .intentosRestantes(3) // TODO: Implementar límite de intentos
            .build();
    }

    /**
     * Verifica el código OTP y registra el dispositivo.
     */
    public VerificarCodigoResponse verificarCodigo(VerificarCodigoRequest request,
                                                    String ipCliente,
                                                    String userAgent) {
        log.info("Verificando código OTP para tenant={}, dispositivo={}", 
                 request.getTenantCodigo(), request.getNombreDispositivo());

        // Buscar tenant
        Tenant tenant = tenantRepository.findByCodigoIgnoreCase(request.getTenantCodigo())
            .orElseThrow(() -> new NotFoundException("Empresa no encontrada"));

        // Buscar código válido
        LocalDateTime ahora = LocalDateTime.now();
        CodigoRegistro codigo = codigoRegistroRepository.findCodigoValido(
            tenant.getId(),
            request.getCodigo(),
            ahora
        ).orElseThrow(() -> new BadRequestException("Código inválido o expirado"));

        // Verificar que coincide el nombre del dispositivo
        if (!codigo.getDispositivoNombre().equals(request.getNombreDispositivo())) {
            throw new BadRequestException("El código no corresponde a este dispositivo");
        }

        // Marcar código como usado
        codigo.marcarComoUsado();
        codigoRegistroRepository.save(codigo);

        // Determinar plataforma
        Plataforma plataforma = parsePlataforma(request.getPlataforma());

        // Crear dispositivo
        Dispositivo dispositivo = Dispositivo.crear(
            tenant,
            request.getNombreDispositivo(),
            plataforma,
            userAgent,
            ipCliente
        );
        dispositivo = dispositivoRepository.save(dispositivo);

        log.info("Dispositivo {} registrado exitosamente para tenant {}", 
                 dispositivo.getNombre(), tenant.getCodigo());

        // TODO: Obtener usuarios del tenant
        List<UsuarioLocalInfo> usuarios = List.of();

        return VerificarCodigoResponse.builder()
            .deviceToken(dispositivo.getToken())
            .tenant(TenantResumen.builder()
                .id(tenant.getId())
                .codigo(tenant.getCodigo())
                .nombre(tenant.getNombre())
                .build())
            .dispositivo(DispositivoInfo.builder()
                .id(dispositivo.getId())
                .nombre(dispositivo.getNombre())
                .plataforma(plataforma != null ? plataforma.name() : null)
                .build())
            .usuarios(usuarios)
            .build();
    }

    /**
     * Reenvía el código OTP.
     */
    public SolicitarCodigoResponse reenviarCodigo(String tenantCodigo, 
                                                   String nombreDispositivo,
                                                   String ipCliente,
                                                   String userAgent) {
        SolicitarCodigoRequest request = SolicitarCodigoRequest.builder()
            .tenantCodigo(tenantCodigo)
            .nombreDispositivo(nombreDispositivo)
            .build();
        
        return solicitarCodigo(request, ipCliente, userAgent);
    }

    /**
     * Lista los dispositivos de un tenant.
     */
    @Transactional(readOnly = true)
    public List<DispositivoInfo> listarDispositivos(Long tenantId) {
        return dispositivoRepository.findByTenantIdOrderByUltimoUsoDesc(tenantId).stream()
            .map(d -> DispositivoInfo.builder()
                .id(d.getId())
                .nombre(d.getNombre())
                .plataforma(d.getPlataforma() != null ? d.getPlataforma().name() : null)
                .ultimoUso(d.getUltimoUso())
                .build())
            .collect(Collectors.toList());
    }

    /**
     * Desconecta (desactiva) un dispositivo.
     */
    public void desconectarDispositivo(Long dispositivoId) {
        Dispositivo dispositivo = dispositivoRepository.findById(dispositivoId)
            .orElseThrow(() -> new NotFoundException("Dispositivo no encontrado"));
        
        dispositivo.desactivar();
        dispositivoRepository.save(dispositivo);
        
        log.info("Dispositivo {} desconectado", dispositivoId);
    }

    // ==================== Helpers ====================

    private Plataforma parsePlataforma(String plataformaStr) {
        if (plataformaStr == null || plataformaStr.isBlank()) {
            return Plataforma.WEB;
        }
        try {
            return Plataforma.valueOf(plataformaStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Plataforma.WEB;
        }
    }
}
