package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.dashboard.DashboardAdminResponse;
import com.snnsoluciones.backnathbitpos.dto.dashboard.EmpresaResumenDashboard;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.DashboardRepository;
import com.snnsoluciones.backnathbitpos.repository.EmpresaRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioEmpresaRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import com.snnsoluciones.backnathbitpos.service.DashboardAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de Dashboard Administrativo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardAdminServiceImpl implements DashboardAdminService {
    
    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final UsuarioEmpresaRepository usuarioEmpresaRepository;
    private final DashboardRepository dashboardRepository;
    
    @Override
    @Transactional(readOnly = true)
    public DashboardAdminResponse obtenerDashboardAdmin(Long usuarioId) {
        log.info("Obteniendo dashboard admin para usuario: {}", usuarioId);
        
        // 1. Validar que el usuario existe
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        
        // 2. Obtener empresas según el rol del usuario
        List<Empresa> empresas = obtenerEmpresasSegunRol(usuario);
        
        if (empresas.isEmpty()) {
            log.warn("Usuario {} no tiene empresas asignadas", usuarioId);
            return DashboardAdminResponse.builder()
                .empresas(new ArrayList<>())
                .build();
        }
        
        // 3. Obtener IDs de empresas
        List<Long> empresasIds = empresas.stream()
            .map(Empresa::getId)
            .collect(Collectors.toList());
        
        // 4. Calcular ventas de hoy para todas las empresas (bulk query - eficiente)
        List<Object[]> ventasResultados = dashboardRepository.calcularVentasHoyPorEmpresas(empresasIds);
        
        // 5. Convertir resultados a Map para lookup rápido
        Map<Long, BigDecimal> ventasPorEmpresa = ventasResultados.stream()
            .collect(Collectors.toMap(
                obj -> (Long) obj[0],           // empresaId
                obj -> (BigDecimal) obj[1]      // totalVentas
            ));
        
        // 6. Construir lista de DTOs
        List<EmpresaResumenDashboard> empresasResumen = empresas.stream()
            .map(empresa -> EmpresaResumenDashboard.builder()
                .id(empresa.getId())
                .nombreComercial(empresa.getNombreComercial())
                .identificacion(empresa.getIdentificacion())
                .ventasHoy(ventasPorEmpresa.getOrDefault(empresa.getId(), BigDecimal.ZERO))
                .build())
            .collect(Collectors.toList());
        
        log.info("Dashboard admin generado con {} empresas", empresasResumen.size());
        
        return DashboardAdminResponse.builder()
            .empresas(empresasResumen)
            .build();
    }
    
    /**
     * Obtiene las empresas según el rol del usuario
     * - ROOT/SOPORTE: Todas las empresas
     * - SUPER_ADMIN: Solo empresas asignadas
     */
    private List<Empresa> obtenerEmpresasSegunRol(Usuario usuario) {
        RolNombre rol = usuario.getRol();
        
        // ROOT y SOPORTE ven todas las empresas
        if (rol == RolNombre.ROOT || rol == RolNombre.SOPORTE) {
            log.debug("Usuario {} con rol {} - acceso a todas las empresas", usuario.getId(), rol);
            return empresaRepository.findAll();
        }
        
        // SUPER_ADMIN solo ve sus empresas asignadas
        if (rol == RolNombre.SUPER_ADMIN) {
            log.debug("Usuario {} con rol SUPER_ADMIN - empresas asignadas", usuario.getId());
            return usuarioEmpresaRepository.findEmpresasByUsuarioId(usuario.getId());
        }
        
        // Otros roles no tienen acceso al dashboard admin
        log.warn("Usuario {} con rol {} intentó acceder al dashboard admin", usuario.getId(), rol);
        return new ArrayList<>();
    }
}