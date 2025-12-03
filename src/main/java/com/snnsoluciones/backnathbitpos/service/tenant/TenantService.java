package com.snnsoluciones.backnathbitpos.service.tenant;

import com.snnsoluciones.backnathbitpos.config.tenant.TenantContext;
import com.snnsoluciones.backnathbitpos.entity.global.SuperAdminTenant;
import com.snnsoluciones.backnathbitpos.entity.global.Tenant;
import com.snnsoluciones.backnathbitpos.entity.global.UsuarioGlobal;
import com.snnsoluciones.backnathbitpos.exception.BadRequestException;
import com.snnsoluciones.backnathbitpos.exception.NotFoundException;
import com.snnsoluciones.backnathbitpos.repository.global.SuperAdminTenantRepository;
import com.snnsoluciones.backnathbitpos.repository.global.TenantRepository;
import com.snnsoluciones.backnathbitpos.repository.global.UsuarioGlobalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * TenantService - Servicio para gestión de tenants.
 * 
 * Responsabilidades:
 * - CRUD de tenants
 * - Creación de schemas en PostgreSQL
 * - Generación de códigos de tenant
 * - Asignación de SUPER_ADMINs a tenants
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TenantService {

    private final TenantRepository tenantRepository;
    private final UsuarioGlobalRepository usuarioGlobalRepository;
    private final SuperAdminTenantRepository superAdminTenantRepository;
    private final JdbcTemplate jdbcTemplate;

    // ==================== CRUD Básico ====================

    /**
     * Crea un nuevo tenant con su schema en PostgreSQL
     */
    public Tenant crearTenant(String nombre, String codigo, Long empresaLegacyId) {
        log.info("Creando tenant: nombre={}, codigo={}", nombre, codigo);

        // Validar código
        String codigoNormalizado = normalizarCodigo(codigo);
        validarCodigoDisponible(codigoNormalizado);

        // Crear tenant
        String schemaName = TenantContext.TENANT_SCHEMA_PREFIX + codigoNormalizado;
        
        Tenant tenant = Tenant.builder()
            .codigo(codigoNormalizado)
            .nombre(nombre)
            .schemaName(schemaName)
            .empresaLegacyId(empresaLegacyId)
            .activo(true)
            .build();

        tenant = tenantRepository.save(tenant);
        log.info("Tenant creado con ID: {}", tenant.getId());

        // Crear schema en PostgreSQL
        crearSchemaEnBD(schemaName);

        return tenant;
    }

    /**
     * Busca un tenant por ID
     */
    @Transactional(readOnly = true)
    public Tenant buscarPorId(Long id) {
        return tenantRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Tenant no encontrado: " + id));
    }

    /**
     * Busca un tenant por código
     */
    @Transactional(readOnly = true)
    public Optional<Tenant> buscarPorCodigo(String codigo) {
        return tenantRepository.findByCodigoIgnoreCase(codigo);
    }

    /**
     * Busca un tenant por empresa legacy
     */
    @Transactional(readOnly = true)
    public Optional<Tenant> buscarPorEmpresaLegacy(Long empresaId) {
        return tenantRepository.findByEmpresaLegacyId(empresaId);
    }

    /**
     * Lista todos los tenants
     */
    @Transactional(readOnly = true)
    public List<Tenant> listarTodos() {
        return tenantRepository.findAllByOrderByNombreAsc();
    }

    /**
     * Lista tenants activos
     */
    @Transactional(readOnly = true)
    public List<Tenant> listarActivos() {
        return tenantRepository.findByActivoTrueOrderByNombreAsc();
    }

    /**
     * Actualiza un tenant
     */
    public Tenant actualizar(Long id, String nombre) {
        Tenant tenant = buscarPorId(id);
        tenant.setNombre(nombre);
        return tenantRepository.save(tenant);
    }

    /**
     * Activa un tenant
     */
    public void activar(Long id) {
        Tenant tenant = buscarPorId(id);
        tenant.setActivo(true);
        tenantRepository.save(tenant);
        log.info("Tenant {} activado", id);
    }

    /**
     * Desactiva un tenant
     */
    public void desactivar(Long id) {
        Tenant tenant = buscarPorId(id);
        tenant.setActivo(false);
        tenantRepository.save(tenant);
        log.info("Tenant {} desactivado", id);
    }

    // ==================== Asignación de SUPER_ADMINs ====================

    /**
     * Asigna un SUPER_ADMIN como propietario del tenant
     */
    public SuperAdminTenant asignarPropietario(Long tenantId, Long usuarioId) {
        Tenant tenant = buscarPorId(tenantId);
        UsuarioGlobal usuario = usuarioGlobalRepository.findById(usuarioId)
            .orElseThrow(() -> new NotFoundException("Usuario no encontrado: " + usuarioId));

        // Validar que sea SUPER_ADMIN
        if (!usuario.esSuperAdmin()) {
            throw new BadRequestException("Solo usuarios SUPER_ADMIN pueden ser propietarios de tenants");
        }

        // Verificar si ya existe la relación
        Optional<SuperAdminTenant> existente = superAdminTenantRepository
            .findByUsuarioIdAndTenantId(usuarioId, tenantId);
        
        if (existente.isPresent()) {
            SuperAdminTenant sat = existente.get();
            sat.setEsPropietario(true);
            sat.setActivo(true);
            return superAdminTenantRepository.save(sat);
        }

        // Crear nueva relación
        SuperAdminTenant relacion = SuperAdminTenant.crearPropietario(usuario, tenant);
        return superAdminTenantRepository.save(relacion);
    }

    /**
     * Asigna un SUPER_ADMIN como colaborador del tenant
     */
    public SuperAdminTenant asignarColaborador(Long tenantId, Long usuarioId) {
        Tenant tenant = buscarPorId(tenantId);
        UsuarioGlobal usuario = usuarioGlobalRepository.findById(usuarioId)
            .orElseThrow(() -> new NotFoundException("Usuario no encontrado: " + usuarioId));

        if (!usuario.esSuperAdmin()) {
            throw new BadRequestException("Solo usuarios SUPER_ADMIN pueden ser colaboradores de tenants");
        }

        Optional<SuperAdminTenant> existente = superAdminTenantRepository
            .findByUsuarioIdAndTenantId(usuarioId, tenantId);
        
        if (existente.isPresent()) {
            SuperAdminTenant sat = existente.get();
            sat.setActivo(true);
            return superAdminTenantRepository.save(sat);
        }

        SuperAdminTenant relacion = SuperAdminTenant.crearColaborador(usuario, tenant);
        return superAdminTenantRepository.save(relacion);
    }

    /**
     * Desasigna un usuario del tenant
     */
    public void desasignarUsuario(Long tenantId, Long usuarioId) {
        SuperAdminTenant relacion = superAdminTenantRepository
            .findByUsuarioIdAndTenantId(usuarioId, tenantId)
            .orElseThrow(() -> new NotFoundException("Relación no encontrada"));
        
        relacion.setActivo(false);
        superAdminTenantRepository.save(relacion);
        log.info("Usuario {} desasignado del tenant {}", usuarioId, tenantId);
    }

    /**
     * Lista tenants de un usuario SUPER_ADMIN
     */
    @Transactional(readOnly = true)
    public List<Tenant> listarTenantsDeUsuario(Long usuarioId) {
        return tenantRepository.findByUsuarioGlobalId(usuarioId);
    }

    /**
     * Verifica si un usuario tiene acceso a un tenant
     */
    @Transactional(readOnly = true)
    public boolean usuarioTieneAcceso(Long usuarioId, Long tenantId) {
        // ROOT y SOPORTE tienen acceso a todos
        UsuarioGlobal usuario = usuarioGlobalRepository.findById(usuarioId).orElse(null);
        if (usuario != null && usuario.esRolSistema()) {
            return true;
        }
        
        return superAdminTenantRepository.existsActiveByUsuarioIdAndTenantId(usuarioId, tenantId);
    }

    // ==================== Gestión de Schema ====================

    /**
     * Crea el schema en PostgreSQL para el tenant
     */
    private void crearSchemaEnBD(String schemaName) {
        log.info("Creando schema en PostgreSQL: {}", schemaName);
        
        try {
            // Crear schema
            jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);
            log.info("Schema {} creado exitosamente", schemaName);
            
        } catch (Exception e) {
            log.error("Error al crear schema {}: {}", schemaName, e.getMessage());
            throw new RuntimeException("Error al crear schema: " + e.getMessage(), e);
        }
    }

    /**
     * Inicializa las tablas del tenant copiando el template
     * NOTA: Este método debe llamarse después de crearTenant y ejecutar el script SQL del template
     */
    public void inicializarTablasTenant(String schemaName) {
        log.info("Inicializando tablas para schema: {}", schemaName);
        
        // TODO: Ejecutar script SQL del template
        // Por ahora, esto se haría manualmente o con Flyway/Liquibase
        
        log.warn("inicializarTablasTenant() requiere ejecución manual del script SQL template");
    }

    /**
     * Verifica si existe el schema en PostgreSQL
     */
    @Transactional(readOnly = true)
    public boolean existeSchema(String schemaName) {
        String sql = "SELECT EXISTS(SELECT 1 FROM information_schema.schemata WHERE schema_name = ?)";
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, schemaName));
    }

    // ==================== Generación de código ====================

    /**
     * Genera un código de tenant a partir del nombre/razón social
     */
    public String generarCodigo(String nombre) {
        String codigo = normalizarCodigo(nombre);
        
        // Si ya existe, agregar sufijo numérico
        String codigoFinal = codigo;
        int contador = 1;
        while (tenantRepository.existsByCodigo(codigoFinal)) {
            codigoFinal = codigo + "_" + contador;
            contador++;
        }
        
        return codigoFinal;
    }

    /**
     * Normaliza un texto para usar como código de tenant
     */
    private String normalizarCodigo(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new BadRequestException("El código no puede estar vacío");
        }

        // Normalizar (quitar acentos)
        String normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        normalizado = pattern.matcher(normalizado).replaceAll("");

        // Convertir a minúsculas
        normalizado = normalizado.toLowerCase();

        // Reemplazar espacios por guiones bajos
        normalizado = normalizado.replaceAll("\\s+", "_");

        // Eliminar caracteres especiales (solo permitir a-z, 0-9, _)
        normalizado = normalizado.replaceAll("[^a-z0-9_]", "");

        // Eliminar guiones bajos consecutivos
        normalizado = normalizado.replaceAll("_+", "_");

        // Eliminar guiones bajos al inicio y final
        normalizado = normalizado.replaceAll("^_|_$", "");

        // Limitar longitud
        if (normalizado.length() > 40) {
            normalizado = normalizado.substring(0, 40);
        }

        if (normalizado.isBlank()) {
            throw new BadRequestException("El código generado está vacío");
        }

        return normalizado;
    }

    /**
     * Valida que el código esté disponible
     */
    private void validarCodigoDisponible(String codigo) {
        if (tenantRepository.existsByCodigo(codigo)) {
            throw new BadRequestException("El código de tenant ya existe: " + codigo);
        }
        
        String schemaName = TenantContext.TENANT_SCHEMA_PREFIX + codigo;
        if (tenantRepository.existsBySchemaName(schemaName)) {
            throw new BadRequestException("El schema ya existe: " + schemaName);
        }
    }
}