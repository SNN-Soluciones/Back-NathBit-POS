package com.snnsoluciones.backnathbitpos.service.metrics;

import com.snnsoluciones.backnathbitpos.dto.metrics.MetricaDashboardDTO;
import com.snnsoluciones.backnathbitpos.dto.metrics.MetricaMensualDTO;
import com.snnsoluciones.backnathbitpos.entity.MetricasVentasMensuales;
import com.snnsoluciones.backnathbitpos.repository.MetricaMensualRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true) // Solo lectura!
public class MetricaMensualService {

    private final MetricaMensualRepository metricaRepository;

    /**
     * Obtener métricas para el dashboard de empresa
     */
    public MetricaDashboardDTO obtenerMetricasDashboard(Long empresaId) {
        LocalDate hoy = LocalDate.now();
        Integer anioActual = hoy.getYear();
        Integer mesActual = hoy.getMonthValue();
        
        // Obtener métrica consolidada de la empresa
        MetricasVentasMensuales metrica = metricaRepository
            .findByEmpresaConsolidada(empresaId, anioActual, mesActual)
            .orElse(new MetricasVentasMensuales()); // Si no existe, retornar vacío
        
        return MetricaDashboardDTO.builder()
            .ventasMesActual(metrica.getVentasTotales())
            .facturasMesActual(
                metrica.getCantidadFacturasMh() + 
                metrica.getCantidadFacturasInternas()
            )
            .ventasMH(metrica.getVentasMh())
            .ventasInternas(metrica.getVentasInternas())
            // TODO: Para ventas del día, necesitaremos otra tabla o vista
            .ventasHoy(BigDecimal.ZERO)
            .facturasHoy(0)
            .build();
    }

    /**
     * Obtener métricas para el dashboard de sucursal específica
     */
    public MetricaDashboardDTO obtenerMetricasDashboardSucursal(Long sucursalId) {
        LocalDate hoy = LocalDate.now();
        Integer anioActual = hoy.getYear();
        Integer mesActual = hoy.getMonthValue();
        
        MetricasVentasMensuales metrica = metricaRepository
            .findBySucursal(sucursalId, anioActual, mesActual)
            .orElse(new MetricasVentasMensuales());
        
        return MetricaDashboardDTO.builder()
            .ventasMesActual(metrica.getVentasTotales())
            .facturasMesActual(
                metrica.getCantidadFacturasMh() + 
                metrica.getCantidadFacturasInternas()
            )
            .ventasMH(metrica.getVentasMh())
            .ventasInternas(metrica.getVentasInternas())
            .ventasHoy(BigDecimal.ZERO)
            .facturasHoy(0)
            .build();
    }

    /**
     * Obtener datos para el reporte D104
     */
    public Map<String, Object> obtenerDatosD104(Long empresaId, Integer anio, Integer mes) {
        Map<String, Object> datos = metricaRepository.obtenerDatosD104(empresaId, anio, mes);
        
        if (datos == null || datos.isEmpty()) {
            log.warn("No se encontraron datos D104 para empresa {} en {}/{}", 
                    empresaId, mes, anio);
            // Retornar estructura vacía con todos los campos en cero
            return crearD104Vacio();
        }
        
        return datos;
    }

    /**
     * Obtener histórico anual de una empresa
     */
    public List<MetricaMensualDTO> obtenerHistoricoAnual(Long empresaId, Integer anio) {
        return metricaRepository
            .findHistoricoAnualEmpresa(empresaId, anio)
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * Obtener métricas de todas las sucursales para un período
     */
    public List<MetricaMensualDTO> obtenerMetricasPorSucursales(Long empresaId, Integer anio, Integer mes) {
        return metricaRepository
            .findAllSucursalesByEmpresaAndPeriodo(empresaId, anio, mes)
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * Convertir entidad a DTO
     */
    private MetricaMensualDTO toDTO(MetricasVentasMensuales metrica) {
        return MetricaMensualDTO.builder()
            .id(metrica.getId())
            .empresaId(metrica.getEmpresa().getId())
            .empresaNombre(metrica.getEmpresa().getNombreComercial())
            .sucursalId(metrica.getSucursal() != null ? metrica.getSucursal().getId() : null)
            .sucursalNombre(metrica.getSucursal() != null ? metrica.getSucursal().getNombre() : "Consolidado")
            .numeroSucursal(metrica.getSucursal() != null ? Integer.valueOf(
                metrica.getSucursal().getNumeroSucursal()) : null)
            .anio(metrica.getAnio())
            .mes(metrica.getMes())
            .ventasMh(metrica.getVentasMh())
            .ventasInternas(metrica.getVentasInternas())
            .ventasTotales(metrica.getVentasTotales())
            .ventasServicios(metrica.getVentasServicios())
            .ventasMercancias(metrica.getVentasMercancias())
            .notasCreditoTotal(metrica.getNotasCreditoTotal())
            .anulacionesTotal(metrica.getAnulacionesTotal())
            .descuentosTotal(metrica.getDescuentosTotal())
            .impuestoTotal(metrica.getImpuestoTotal())
            .impuestoIva13(metrica.getImpuestoIva13())
            .impuestoIva4(metrica.getImpuestoIva4())
            .impuestoIva2(metrica.getImpuestoIva2())
            .impuestoIva1(metrica.getImpuestoIva1())
            .exentoTotal(metrica.getExentoTotal())
            .exoneradoTotal(metrica.getExoneradoTotal())
            .cantidadFacturasMh(metrica.getCantidadFacturasMh())
            .cantidadFacturasInternas(metrica.getCantidadFacturasInternas())
            .cantidadNotasCredito(metrica.getCantidadNotasCredito())
            .cantidadAnulaciones(metrica.getCantidadAnulaciones())
            .ventasNetas(
                metrica.getVentasTotales()
                    .subtract(metrica.getNotasCreditoTotal())
                    .subtract(metrica.getDescuentosTotal())
            )
            .baseGravable(
                metrica.getVentasTotales()
                    .subtract(metrica.getNotasCreditoTotal())
                    .subtract(metrica.getDescuentosTotal())
                    .subtract(metrica.getExentoTotal())
                    .subtract(metrica.getExoneradoTotal())
            )
            .createdAt(metrica.getCreatedAt())
            .updatedAt(metrica.getUpdatedAt())
            .build();
    }

    /**
     * Crear estructura D104 vacía
     */
    private Map<String, Object> crearD104Vacio() {
        return Map.of(
            "ventasBrutas", BigDecimal.ZERO,
            "ventasServicios", BigDecimal.ZERO,
            "ventasMercancias", BigDecimal.ZERO,
            "notasCredito", BigDecimal.ZERO,
            "descuentos", BigDecimal.ZERO,
            "ventasNetas", BigDecimal.ZERO,
            "ventasExentas", BigDecimal.ZERO,
            "ventasExoneradas", BigDecimal.ZERO,
            "ventasGravadas", BigDecimal.ZERO,
            "totalIVA", BigDecimal.ZERO
        );
    }
}