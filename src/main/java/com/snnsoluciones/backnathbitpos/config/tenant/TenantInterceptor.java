package com.snnsoluciones.backnathbitpos.config.tenant;

import com.snnsoluciones.backnathbitpos.entity.global.Dispositivo;
import com.snnsoluciones.backnathbitpos.entity.global.Tenant;
import com.snnsoluciones.backnathbitpos.repository.global.DispositivoRepository;
import com.snnsoluciones.backnathbitpos.repository.global.TenantRepository;
import com.snnsoluciones.backnathbitpos.security.ContextoUsuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.Optional;

/**
 * TenantInterceptor - Interceptor que extrae el tenant de cada request.
 * 
 * Fuentes de tenant (en orden de prioridad):
 * 1. Header X-Device-Token → Busca dispositivo → Obtiene tenant
 * 2. Header X-Tenant-Code → Busca tenant por código
 * 3. JWT (empresaId del sistema legacy) → Mapea a tenant
 * 4. Default: Schema public (sin tenant)
 * 
 * Este interceptor se ejecuta DESPUÉS del JwtAuthenticationFilter.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TenantInterceptor implements HandlerInterceptor {

    private final DispositivoRepository dispositivoRepository;
    private final TenantRepository tenantRepository;

    /**
     * Header para el token del dispositivo
     */
    public static final String HEADER_DEVICE_TOKEN = "X-Device-Token";

    /**
     * Header para el código del tenant (alternativo)
     */
    public static final String HEADER_TENANT_CODE = "X-Tenant-Code";

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, 
                            @NonNull HttpServletResponse response, 
                            @NonNull Object handler) {
        
        // Limpiar contexto previo (por si acaso)
        TenantContext.clear();

        // Intentar obtener tenant en orden de prioridad
        boolean tenantEstablecido = false;

        // 1. Intentar por Device Token
        if (!tenantEstablecido) {
            tenantEstablecido = trySetTenantFromDeviceToken(request);
        }

        // 2. Intentar por Header X-Tenant-Code
        if (!tenantEstablecido) {
            tenantEstablecido = trySetTenantFromHeader(request);
        }

        // 3. Intentar por JWT del sistema legacy (empresaId)
        if (!tenantEstablecido) {
            tenantEstablecido = trySetTenantFromJwtLegacy();
        }

        // Si no se encontró tenant, se usa el schema public por defecto
        if (!tenantEstablecido) {
            log.debug("No se encontró tenant, usando schema public");
            TenantContext.setCurrentTenant(TenantContext.DEFAULT_SCHEMA);
        }

        return true;
    }

    @Override
    public void postHandle(@NonNull HttpServletRequest request, 
                          @NonNull HttpServletResponse response, 
                          @NonNull Object handler,
                          ModelAndView modelAndView) {
        // No hacemos nada aquí
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, 
                               @NonNull HttpServletResponse response, 
                               @NonNull Object handler, 
                               Exception ex) {
        // IMPORTANTE: Limpiar el contexto al finalizar el request
        TenantContext.clear();
    }

    // ==================== Métodos privados ====================

    /**
     * Intenta establecer el tenant desde el Device Token
     */
    private boolean trySetTenantFromDeviceToken(HttpServletRequest request) {
        String deviceToken = request.getHeader(HEADER_DEVICE_TOKEN);
        
        if (!StringUtils.hasText(deviceToken)) {
            return false;
        }

        log.debug("Device token encontrado: {}...", deviceToken.substring(0, Math.min(10, deviceToken.length())));

        Optional<Dispositivo> dispositivoOpt = dispositivoRepository.findByTokenWithTenant(deviceToken);
        
        if (dispositivoOpt.isEmpty()) {
            log.warn("Device token no válido o dispositivo inactivo");
            return false;
        }

        Dispositivo dispositivo = dispositivoOpt.get();
        Tenant tenant = dispositivo.getTenant();

        if (tenant == null || !tenant.estaActivo()) {
            log.warn("Tenant del dispositivo no válido o inactivo");
            return false;
        }

        // Establecer contexto
        TenantContext.setTenant(tenant.getId(), tenant.getSchemaName());
        
        // Actualizar último uso del dispositivo (async sería mejor)
        dispositivo.registrarUso();
        // No guardamos aquí para no afectar performance, 
        // se puede hacer en un job async

        log.debug("Tenant establecido desde device token: {} ({})", tenant.getCodigo(), tenant.getSchemaName());
        return true;
    }

    /**
     * Intenta establecer el tenant desde el header X-Tenant-Code
     */
    private boolean trySetTenantFromHeader(HttpServletRequest request) {
        String tenantCode = request.getHeader(HEADER_TENANT_CODE);
        
        if (!StringUtils.hasText(tenantCode)) {
            return false;
        }

        log.debug("Tenant code en header: {}", tenantCode);

        Optional<Tenant> tenantOpt = tenantRepository.findByCodigoIgnoreCase(tenantCode);
        
        if (tenantOpt.isEmpty()) {
            log.warn("Código de tenant no encontrado: {}", tenantCode);
            return false;
        }

        Tenant tenant = tenantOpt.get();
        
        if (!tenant.estaActivo()) {
            log.warn("Tenant inactivo: {}", tenantCode);
            return false;
        }

        TenantContext.setTenant(tenant.getId(), tenant.getSchemaName());
        log.debug("Tenant establecido desde header: {} ({})", tenant.getCodigo(), tenant.getSchemaName());
        return true;
    }

    /**
     * Intenta establecer el tenant desde el JWT del sistema legacy.
     * Esto permite compatibilidad con el sistema actual mientras se migra.
     */
    private boolean trySetTenantFromJwtLegacy() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        Object principal = auth.getPrincipal();
        
        if (!(principal instanceof ContextoUsuario)) {
            return false;
        }

        ContextoUsuario contexto = (ContextoUsuario) principal;
        Long empresaId = contexto.getEmpresaId();
        
        if (empresaId == null) {
            return false;
        }

        // Buscar tenant por empresa legacy
        Optional<Tenant> tenantOpt = tenantRepository.findByEmpresaLegacyId(empresaId);
        
        if (tenantOpt.isEmpty()) {
            // Empresa no migrada aún, usar schema public (sistema legacy)
            log.debug("Empresa {} no migrada a tenant, usando sistema legacy", empresaId);
            return false;
        }

        Tenant tenant = tenantOpt.get();
        
        if (!tenant.estaActivo()) {
            log.warn("Tenant de empresa {} está inactivo", empresaId);
            return false;
        }

        TenantContext.setTenant(tenant.getId(), tenant.getSchemaName());
        log.debug("Tenant establecido desde JWT legacy: empresaId={} -> {} ({})", 
                  empresaId, tenant.getCodigo(), tenant.getSchemaName());
        return true;
    }
}
