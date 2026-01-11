package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.dashboard.*;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.exception.UnauthorizedException;
import com.snnsoluciones.backnathbitpos.repository.DashboardRepository;
import com.snnsoluciones.backnathbitpos.repository.EmpresaRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioEmpresaRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import com.snnsoluciones.backnathbitpos.service.DashboardAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Override
    @Transactional(readOnly = true)
    public DashboardEmpresaDetalladoResponse obtenerDashboardEmpresaDetallado(Long usuarioId, Long empresaId) {
        log.info("Obteniendo dashboard detallado - Usuario: {}, Empresa: {}", usuarioId, empresaId);

        // 1. Validar acceso del usuario a la empresa
        validarAccesoEmpresa(usuarioId, empresaId);

        // 2. Obtener datos básicos de la empresa
        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

        // 3. Calcular fechas necesarias
        LocalDate hoy = LocalDate.now();
        LocalDate inicioSemana = hoy.minusDays(7);
        LocalDate inicioMes = hoy.withDayOfMonth(1);

        // 4. Obtener todas las métricas
        MetricasDTO metricas = obtenerMetricas(empresaId, hoy, inicioSemana, inicioMes);
        List<SucursalMetricasDTO> sucursales = obtenerMetricasSucursales(empresaId, hoy);
        List<VentaDiariaDTO> ventasPorDia = obtenerVentasPorDia(empresaId, inicioSemana);
        List<ProductoTopDTO> topProductos = obtenerTopProductos(empresaId, hoy);
        List<UsuarioSimpleDTO> usuarios = obtenerUsuarios(empresaId);
        List<CajaAbiertaDTO> cajas = obtenerCajasAbiertas(empresaId);

        // 5. Construir response
        DashboardEmpresaDetalladoResponse response = DashboardEmpresaDetalladoResponse.builder()
            .empresa(EmpresaBasicaDTO.builder()
                .id(empresa.getId())
                .nombreComercial(empresa.getNombreComercial())
                .identificacion(empresa.getIdentificacion())
                .build())
            .metricas(metricas)
            .sucursales(sucursales)
            .ventasPorDia(ventasPorDia)
            .topProductos(topProductos)
            .usuarios(usuarios)
            .cajas(cajas)
            .build();

        log.info("Dashboard detallado generado - {} sucursales, {} usuarios, {} cajas abiertas",
            sucursales.size(), usuarios.size(), cajas.size());

        return response;
    }

    // ==================== MÉTODOS PRIVADOS ====================

    /**
     * Obtiene las empresas según el rol del usuario
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

    /**
     * Valida que el usuario tenga acceso a la empresa
     */
    private void validarAccesoEmpresa(Long usuarioId, Long empresaId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        RolNombre rol = usuario.getRol();

        // ROOT y SOPORTE tienen acceso a todo
        if (rol == RolNombre.ROOT || rol == RolNombre.SOPORTE) {
            return;
        }

        // SUPER_ADMIN solo a sus empresas
        if (rol == RolNombre.SUPER_ADMIN) {
            boolean tieneAcceso = usuarioEmpresaRepository.existsByUsuarioIdAndEmpresaId(usuarioId, empresaId);
            if (!tieneAcceso) {
                log.warn("Usuario {} intentó acceder a empresa {} sin permisos", usuarioId, empresaId);
                throw new UnauthorizedException("No tienes acceso a esta empresa");
            }
            return;
        }

        // Otros roles no tienen acceso
        throw new UnauthorizedException("Rol no autorizado para acceder al dashboard admin");
    }

    /**
     * Obtiene métricas generales (ventas, cajas, usuarios)
     */
    private MetricasDTO obtenerMetricas(Long empresaId, LocalDate hoy, LocalDate inicioSemana, LocalDate inicioMes) {
        // Query que retorna [ventasHoy, ventasSemana, ventasMes]
        Object[] ventasData = dashboardRepository.calcularMetricasVentas(empresaId, hoy, inicioSemana, inicioMes);

        Long cajasAbiertas = dashboardRepository.contarCajasAbiertas(empresaId);
        Long pdvsActivos = dashboardRepository.contarPdvsActivos(empresaId);
        Long usuariosActivos = dashboardRepository.contarUsuariosActivos(empresaId);

        return MetricasDTO.builder()
            .ventasHoy((BigDecimal) ventasData[0])
            .ventasSemana((BigDecimal) ventasData[1])
            .ventasMes((BigDecimal) ventasData[2])
            .cajasAbiertas(cajasAbiertas)
            .pdvsActivos(pdvsActivos)
            .usuariosActivos(usuariosActivos)
            .build();
    }

    /**
     * Obtiene métricas por sucursal
     */
    private List<SucursalMetricasDTO> obtenerMetricasSucursales(Long empresaId, LocalDate hoy) {
        List<Object[]> resultados = dashboardRepository.obtenerMetricasSucursales(empresaId, hoy);

        return resultados.stream()
            .map(obj -> SucursalMetricasDTO.builder()
                .id((Long) obj[0])
                .nombre((String) obj[1])
                .ventasHoy((BigDecimal) obj[2])
                .cajasAbiertas((Long) obj[3])
                .pdvsActivos((Long) obj[4])
                .build())
            .collect(Collectors.toList());
    }

    /**
     * Obtiene ventas de los últimos 7 días
     */
    private List<VentaDiariaDTO> obtenerVentasPorDia(Long empresaId, LocalDate inicioSemana) {
        List<Object[]> resultados = dashboardRepository.obtenerVentasPorDia(empresaId, inicioSemana);

        return resultados.stream()
            .map(obj -> VentaDiariaDTO.builder()
                .fecha((LocalDate) obj[0])
                .monto((BigDecimal) obj[1])
                .build())
            .collect(Collectors.toList());
    }

    /**
     * Obtiene top 5 productos más vendidos hoy
     */
    private List<ProductoTopDTO> obtenerTopProductos(Long empresaId, LocalDate hoy) {
        List<Object[]> resultados = dashboardRepository.obtenerTopProductosHoy(
            empresaId,
            hoy,
            PageRequest.of(0, 5)
        );

        return resultados.stream()
            .map(obj -> ProductoTopDTO.builder()
                .nombre((String) obj[0])
                .cantidad((BigDecimal) obj[1])
                .monto((BigDecimal) obj[2])
                .build())
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de usuarios de la empresa
     */
    private List<UsuarioSimpleDTO> obtenerUsuarios(Long empresaId) {
        List<Usuario> usuarios = usuarioRepository.findByEmpresaId(empresaId);

        return usuarios.stream()
            .map(u -> UsuarioSimpleDTO.builder()
                .id(u.getId())
                .nombre(u.getNombre() + " " + u.getApellidos())
                .usuario(u.getEmail())
                .rol(u.getRol().name())
                .activo(u.getActivo())
                .build())
            .collect(Collectors.toList());
    }

    /**
     * Obtiene cajas abiertas con detalles
     */
    private List<CajaAbiertaDTO> obtenerCajasAbiertas(Long empresaId) {
        List<Object[]> resultados = dashboardRepository.listarCajasAbiertas(empresaId);

        return resultados.stream()
            .map(obj -> CajaAbiertaDTO.builder()
                .id((Long) obj[0])
                .sucursalNombre((String) obj[1])
                .usuario((String) obj[2] + " " + (String) obj[3])
                .montoInicial((BigDecimal) obj[4])
                .horaApertura((LocalDateTime) obj[5])
                .build())
            .collect(Collectors.toList());
    }
}