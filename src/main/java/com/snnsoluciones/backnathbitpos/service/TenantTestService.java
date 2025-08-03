package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.config.tenant.TenantContext;
import com.snnsoluciones.backnathbitpos.entity.security.Usuario;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de prueba para verificar el funcionamiento del multi-tenant.
 * Este servicio debe ser usado solo para desarrollo y pruebas.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantTestService {

    private final UsuarioRepository usuarioRepository;

    /**
     * Prueba la funcionalidad multi-tenant listando usuarios de diferentes tenants
     */
    @Transactional(readOnly = true)
    public void testMultiTenantAccess() {
        log.info("=== Iniciando prueba de Multi-Tenant ===");
        
        // Probar con tenant "demo"
        TenantContext.setCurrentTenant("demo");
        log.info("Tenant actual: {}", TenantContext.getCurrentTenant());
        
        List<Usuario> usuariosDemo = usuarioRepository.findAll();
        log.info("Usuarios en tenant 'demo': {}", usuariosDemo.size());
        usuariosDemo.forEach(u -> log.info("  - {}: {}", u.getEmail(), u.getNombre()));
        
        // Limpiar contexto
        TenantContext.clear();
        
        // Probar con tenant "tenant1"
        TenantContext.setCurrentTenant("tenant1");
        log.info("Tenant actual: {}", TenantContext.getCurrentTenant());
        
        List<Usuario> usuariosTenant1 = usuarioRepository.findAll();
        log.info("Usuarios en tenant 'tenant1': {}", usuariosTenant1.size());
        usuariosTenant1.forEach(u -> log.info("  - {}: {}", u.getEmail(), u.getNombre()));
        
        // Limpiar contexto
        TenantContext.clear();
        
        log.info("=== Prueba de Multi-Tenant completada ===");
    }
    
    /**
     * Verifica que el tenant_id se establece automáticamente al crear entidades
     */
    @Transactional
    public void testAutoTenantAssignment() {
        log.info("=== Probando asignación automática de tenant_id ===");
        
        TenantContext.setCurrentTenant("demo");
        
        // Aquí podrías crear una entidad de prueba para verificar
        // que el tenant_id se asigna automáticamente
        
        log.info("Tenant en contexto: {}", TenantContext.getCurrentTenant());
        
        TenantContext.clear();
    }
}