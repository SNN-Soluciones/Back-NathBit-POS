package com.snnsoluciones.backnathbitpos.service.auth.multitenant;

import com.snnsoluciones.backnathbitpos.dto.auth.multitenant.AuthMultitenantDTOs.*;
import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.entity.Terminal;
import com.snnsoluciones.backnathbitpos.entity.global.*;
import com.snnsoluciones.backnathbitpos.entity.global.Dispositivo.Plataforma;
import com.snnsoluciones.backnathbitpos.exception.BadRequestException;
import com.snnsoluciones.backnathbitpos.exception.NotFoundException;
import com.snnsoluciones.backnathbitpos.exception.UnauthorizedException;
import com.snnsoluciones.backnathbitpos.repository.SucursalRepository;
import com.snnsoluciones.backnathbitpos.repository.TerminalRepository;
import com.snnsoluciones.backnathbitpos.repository.global.*;
import com.snnsoluciones.backnathbitpos.service.EmailService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final JdbcTemplate jdbcTemplate;
    private final SuperAdminTenantRepository superAdminTenantRepository;
    private final TerminalRepository terminalRepository;
    private final SucursalRepository sucursalRepository;

    /**
     * Máximo de códigos activos por tenant
     */
    private static final int MAX_CODIGOS_ACTIVOS_POR_TENANT = 10;

    @Transactional
    public VerificarCodigoResponse registrarConCredenciales(
        RegistrarDispositivoConCredencialesRequest request,
        Long usuarioGlobalId,
        String ipCliente,
        String userAgent) {

        log.info("Registro con credenciales - tenantId={}, sucursalId={}, dispositivo={}",
            request.getTenantId(), request.getSucursalId(), request.getNombreDispositivo());

        // 1. Buscar tenant
        Tenant tenant = tenantRepository.findById(request.getTenantId())
            .orElseThrow(() -> new NotFoundException("Empresa no encontrada"));

        if (!tenant.estaActivo()) {
            throw new BadRequestException("La empresa no está activa");
        }

        // 2. Verificar que el usuario tiene acceso a este tenant
        boolean tieneAcceso = superAdminTenantRepository
            .existsActiveByUsuarioIdAndTenantId(usuarioGlobalId, request.getTenantId());

        // ROOT y SOPORTE tienen acceso a todos
        UsuarioGlobal usuarioGlobal = usuarioGlobalRepository.findById(usuarioGlobalId)
            .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));

        if (!tieneAcceso && !usuarioGlobal.esRolSistema()) {
            throw new UnauthorizedException("No tiene acceso a esta empresa");
        }

        // 3. Verificar que la sucursal existe en el schema
        String sucursalNombre = obtenerNombreSucursal(tenant.getSchemaName(), request.getSucursalId());
        if (sucursalNombre == null) {
            throw new BadRequestException("Sucursal no encontrada");
        }

        // 4. Crear dispositivo directamente (sin OTP — ya se autenticó con credenciales)
        Plataforma plataforma = parsePlataforma(request.getPlataforma());

        Dispositivo dispositivo = Dispositivo.crear(
            tenant,
            request.getNombreDispositivo(),
            plataforma,
            userAgent,
            ipCliente
        );
        dispositivo.setSucursalId(request.getSucursalId());
        dispositivo.setSucursalNombre(sucursalNombre);
        dispositivo.setTipo(request.getTipo() != null ? request.getTipo() : "PDV");
        dispositivo.setTerminalId(request.getTerminalId());
        dispositivo = dispositivoRepository.save(dispositivo);

        log.info("Dispositivo {} registrado con credenciales para tenant {} en sucursal {}",
            dispositivo.getNombre(), tenant.getCodigo(), sucursalNombre);

        // 5. Retornar mismo formato que verificarCodigo para consistencia
        return VerificarCodigoResponse.builder()
            .deviceToken(dispositivo.getToken())
            .tenant(TenantResumen.builder()
                .id(tenant.getId())
                .codigo(tenant.getCodigo())
                .nombre(tenant.getNombre())
                .build())
            .sucursal(SucursalResumen.builder()
                .id(request.getSucursalId())
                .nombre(sucursalNombre)
                .build())
            .dispositivo(DispositivoInfo.builder()
                .id(dispositivo.getId())
                .nombre(dispositivo.getNombre())
                .plataforma(plataforma != null ? plataforma.name() : null)
                .build())
            .build();
    }

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
            List<SucursalResumen> sucursales = obtenerSucursalesDeSchema(tenant.getSchemaName());
            return LoginEmpresaResponse.builder()
                .tenant(tenantResumen)
                .sucursales(sucursales)
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
        log.info("Solicitando código OTP para tenant={}, dispositivo={}, sucursal={}",
            request.getTenantCodigo(), request.getNombreDispositivo(), request.getSucursalId());

        // Buscar tenant
        Tenant tenant = tenantRepository.findByCodigoIgnoreCase(request.getTenantCodigo())
            .orElseThrow(() -> new NotFoundException("Empresa no encontrada"));

        if (!tenant.estaActivo()) {
            throw new BadRequestException("La empresa no está activa");
        }

        // Validar que la sucursal existe
        String sucursalNombre = obtenerNombreSucursal(tenant.getSchemaName(), request.getSucursalId());
        if (sucursalNombre == null) {
            throw new BadRequestException("Sucursal no encontrada");
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

        // Crear nuevo código CON SUCURSAL
        CodigoRegistro codigo = CodigoRegistro.crear(
            tenant,
            request.getNombreDispositivo(),
            ipCliente,
            userAgent
        );
        codigo.setSucursalId(request.getSucursalId());
        codigo.setSucursalNombre(sucursalNombre);
        codigo = codigoRegistroRepository.save(codigo);

        // Obtener emails de SUPER_ADMINs del tenant
        List<UsuarioGlobal> admins = usuarioGlobalRepository.findByTenantId(tenant.getId());

        if (admins.isEmpty()) {
            log.warn("No hay SUPER_ADMINs para el tenant {}", tenant.getCodigo());
            admins = usuarioGlobalRepository.findPropietariosByTenantId(tenant.getId());
        }

        if (admins.isEmpty()) {
            throw new BadRequestException("No hay administradores configurados para esta empresa");
        }

        // Enviar email a cada admin (incluir info de sucursal)
        for (UsuarioGlobal admin : admins) {
            try {
                emailService.enviarCodigoRegistroDispositivo(
                    admin.getEmail(),
                    admin.getNombre(),
                    tenant.getNombre(),
                    request.getNombreDispositivo() + " - " + sucursalNombre,  // Incluir sucursal
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
            .intentosRestantes(3)
            .build();
    }

    /**
     * Obtiene las sucursales de un tenant para selección.
     */
    @Transactional(readOnly = true)
    public ListaSucursalesResponse obtenerSucursalesTenant(String codigoTenant) {
        log.info("Obteniendo sucursales para tenant: {}", codigoTenant);

        Tenant tenant = tenantRepository.findByCodigoIgnoreCase(codigoTenant)
            .orElseThrow(() -> new NotFoundException("Empresa no encontrada"));

        if (!tenant.estaActivo()) {
            throw new BadRequestException("La empresa no está activa");
        }

        // Consultar sucursales del schema del tenant
        String schemaName = tenant.getSchemaName();
        List<SucursalResumen> sucursales = obtenerSucursalesDelSchema(schemaName);

        return ListaSucursalesResponse.builder()
            .tenant(TenantResumen.builder()
                .id(tenant.getId())
                .codigo(tenant.getCodigo())
                .nombre(tenant.getNombre())
                .build())
            .sucursales(sucursales)
            .build();
    }

    /**
     * Obtiene sucursales directamente del schema del tenant
     */
    private List<SucursalResumen> obtenerSucursalesDelSchema(String schemaName) {
        String sql = String.format(
            "SELECT id, nombre, numero_sucursal FROM %s.sucursales WHERE activa = true ORDER BY nombre",
            schemaName
        );

        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            return rows.stream()
                .map(row -> SucursalResumen.builder()
                    .id(((Number) row.get("id")).longValue())
                    .nombre((String) row.get("nombre"))
                    .numeroSucursal(row.get("numero_sucursal") != null ? row.get("numero_sucursal").toString() : "001")
                    .build())
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error obteniendo sucursales del schema {}: {}", schemaName, e.getMessage());
            return List.of();
        }
    }

    /**
     * Obtiene el nombre de una sucursal
     */
    private String obtenerNombreSucursal(String schemaName, Long sucursalId) {
        String sql = String.format(
            "SELECT nombre FROM %s.sucursales WHERE id = ?",
            schemaName
        );

        try {
            return jdbcTemplate.queryForObject(sql, String.class, sucursalId);
        } catch (Exception e) {
            log.warn("No se encontró sucursal {} en schema {}", sucursalId, schemaName);
            return null;
        }
    }

    /**
     * Verifica el código OTP y registra el dispositivo.
     */
    public VerificarCodigoResponse verificarCodigo(VerificarCodigoRequest request,
        String ipCliente,
        String userAgent) {
        log.info("Verificando código OTP para tenant={}, dispositivo={}, sucursal={}",
            request.getTenantCodigo(), request.getNombreDispositivo(), request.getSucursalId());

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

        // Verificar que coincide la sucursal
        if (!codigo.getSucursalId().equals(request.getSucursalId())) {
            throw new BadRequestException("El código no corresponde a esta sucursal");
        }

        // Marcar código como usado
        codigo.marcarComoUsado();
        codigoRegistroRepository.save(codigo);

        // Determinar plataforma y tipo
        Plataforma plataforma = parsePlataforma(request.getPlataforma());
        String tipo = request.getTipo() != null ? request.getTipo() : "PDV";

        // Crear dispositivo
        Dispositivo dispositivo = Dispositivo.crear(
            tenant,
            request.getNombreDispositivo(),
            plataforma,
            userAgent,
            ipCliente
        );
        dispositivo.setSucursalId(request.getSucursalId());
        dispositivo.setSucursalNombre(codigo.getSucursalNombre());
        dispositivo.setTipo(tipo);

        // Si es KIOSKO → asignar o crear terminal automáticamente
        // Si es PDV → usar la terminalId que manda el frontend (puede ser null)
        if ("KIOSKO".equals(tipo)) {
            Terminal terminal = asignarOCrearTerminalKiosko(
                tenant.getSchemaName(), request.getSucursalId()
            );
            dispositivo.setTerminalId(terminal.getId());
        } else {
            dispositivo.setTerminalId(request.getTerminalId());
        }

        // Guardar dispositivo — solo una vez
        dispositivo = dispositivoRepository.save(dispositivo);

        // Si es KIOSKO → linkear dispositivoId en la terminal
        if ("KIOSKO".equals(tipo)) {
            final Dispositivo dispositivoFinal = dispositivo; // ← agregar esta línea
            terminalRepository.findById(dispositivo.getTerminalId())
                .ifPresent(t -> {
                    t.setDispositivoId(dispositivoFinal.getId()); // ← usar dispositivoFinal
                    terminalRepository.save(t);
                });
        }

        log.info("Dispositivo {} tipo={} registrado para tenant {} en sucursal {}",
            dispositivo.getNombre(), tipo, tenant.getCodigo(), codigo.getSucursalNombre());

        return VerificarCodigoResponse.builder()
            .deviceToken(dispositivo.getToken())
            .tenant(TenantResumen.builder()
                .id(tenant.getId())
                .codigo(tenant.getCodigo())
                .nombre(tenant.getNombre())
                .build())
            .sucursal(SucursalResumen.builder()
                .id(codigo.getSucursalId())
                .nombre(codigo.getSucursalNombre())
                .build())
            .dispositivo(DispositivoInfo.builder()
                .id(dispositivo.getId())
                .nombre(dispositivo.getNombre())
                .plataforma(plataforma != null ? plataforma.name() : null)
                .sucursal(SucursalResumen.builder()
                    .id(codigo.getSucursalId())
                    .nombre(codigo.getSucursalNombre())
                    .build())
                .build())
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

    public List<SucursalResumen> obtenerSucursalesDeSchema(String schemaName) {
        String sql = String.format("""
            SELECT id, nombre, numero_sucursal
            FROM %s.sucursales
            WHERE activa = true
            ORDER BY nombre
            """, schemaName);
        try {
            return jdbcTemplate.query(sql, (rs, rn) ->
                SucursalResumen.builder()
                    .id(rs.getLong("id"))
                    .nombre(rs.getString("nombre"))
                    .numeroSucursal(rs.getString("numero_sucursal"))
                    .build()
            );
        } catch (Exception e) {
            log.warn("Error obteniendo sucursales del schema {}: {}", schemaName, e.getMessage());
            return List.of();
        }
    }

    public Terminal asignarOCrearTerminalKiosko(String schemaName, Long sucursalId) {
        // Buscar terminal KIOSKO disponible via JDBC
        String sqlBuscar = String.format("""
        SELECT id, nombre, numero_terminal
        FROM %s.terminales
        WHERE sucursal_id = ?
          AND tipo = 'KIOSKO'
          AND dispositivo_id IS NULL
          AND activa = true
        LIMIT 1
        """, schemaName);

        List<Map<String, Object>> disponibles = jdbcTemplate.queryForList(sqlBuscar, sucursalId);

        if (!disponibles.isEmpty()) {
            Long terminalId = ((Number) disponibles.get(0).get("id")).longValue();
            log.info("Terminal KIOSKO disponible: {}", terminalId);
            // Retornar un Terminal dummy con solo el id — suficiente para setear terminalId
            Terminal t = new Terminal();
            t.setId(terminalId);
            return t;
        }

        // No hay → crear nueva via JDBC
        String sqlMaxNumero = String.format("""
        SELECT COALESCE(MAX(CAST(numero_terminal AS INTEGER)), 0)
        FROM %s.terminales
        WHERE sucursal_id = ?
        """, schemaName);

        Integer maxNumero = jdbcTemplate.queryForObject(sqlMaxNumero, Integer.class, sucursalId);
        String siguienteNumero = String.format("%05d", (maxNumero != null ? maxNumero : 0) + 1);

        String sqlCrear = String.format("""
        INSERT INTO %s.terminales
          (sucursal_id, numero_terminal, nombre, tipo, activa,
           consecutivo_factura_electronica, consecutivo_tiquete_electronico,
           consecutivo_nota_credito, consecutivo_nota_debito,
           consecutivo_factura_compra, consecutivo_factura_exportacion,
           consecutivo_mensaje_receptor, consecutivo_recibo_pago,
           consecutivo_tiquete_interno, consecutivo_factura_interna,
           consecutivo_proforma, consecutivo_orden_pedido,
           imprimir_automatico, created_at, updated_at)
        VALUES (?, ?, ?, 'KIOSKO', true, 0,0,0,0,0,0,0,0,0,0,0,0, false, NOW(), NOW())
        RETURNING id
        """, schemaName);

        Long nuevoId = jdbcTemplate.queryForObject(sqlCrear, Long.class,
            sucursalId, siguienteNumero, "Kiosko " + siguienteNumero);

        log.info("Terminal KIOSKO creada via JDBC: id={}", nuevoId);

        Terminal t = new Terminal();
        t.setId(nuevoId);
        return t;
    }

}
