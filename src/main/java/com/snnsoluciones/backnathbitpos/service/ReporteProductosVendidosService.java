package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.reporte.ProductoVendidoDTO;
import com.snnsoluciones.backnathbitpos.dto.reporte.ReporteProductosVendidosRequest;
import com.snnsoluciones.backnathbitpos.dto.reporte.ReporteProductosVendidosResponse;
import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.MetricaProductoVendidoRepository;
import com.snnsoluciones.backnathbitpos.repository.SucursalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReporteProductosVendidosService {

  private final MetricaProductoVendidoRepository metricaRepository;
  private final SucursalRepository sucursalRepository;
  private final JdbcTemplate jdbcTemplate;

  /**
   * Top N productos más vendidos en un rango
   */
  /**
   * Top N productos más vendidos en un rango - VERSIÓN OPTIMIZADA
   */
  @Transactional(readOnly = true)
  public List<ProductoVendidoDTO> getTopProductos(
      Long sucursalId, LocalDate fechaDesde, LocalDate fechaHasta, Integer top) {

    String sql = """
        SELECT 
            p.id,
            p.codigo_interno as codigo,
            p.nombre,
            STRING_AGG(DISTINCT c.nombre, ', ') as categorias,
            SUM(m.cantidad_vendida) as total_vendido
        FROM metricas_productos_vendidos m
        JOIN productos p ON p.id = m.producto_id
        LEFT JOIN producto_categoria pc ON pc.producto_id = p.id
        LEFT JOIN categorias_producto c ON c.id = pc.categoria_id
        WHERE m.sucursal_id = ?
          AND m.fecha BETWEEN ? AND ?
        GROUP BY p.id, p.codigo_interno, p.nombre
        ORDER BY total_vendido DESC
        LIMIT ?
        """;

    List<ProductoVendidoDTO> productos = jdbcTemplate.query(
        sql,
        (rs, rowNum) -> ProductoVendidoDTO.builder()
            .productoId(rs.getLong("id"))
            .codigoProducto(rs.getString("codigo"))
            .nombreProducto(rs.getString("nombre"))
            .categoria(rs.getString("categorias"))
            .cantidadVendida(rs.getBigDecimal("total_vendido"))
            .posicion(rowNum + 1)
            .build(),
        sucursalId, fechaDesde, fechaHasta, top
    );

    // Calcular porcentajes
    calcularPorcentajes(productos);

    return productos;
  }

  /**
   * Reporte mensual con desglose
   */
  @Transactional(readOnly = true)
  public ReporteProductosVendidosResponse generarReporteMensual(
      ReporteProductosVendidosRequest request) {

    Sucursal sucursal = sucursalRepository.findById(request.getSucursalId())
        .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));

    // Top productos del período completo
    List<ProductoVendidoDTO> topProductos = getTopProductos(
        request.getSucursalId(),
        request.getFechaDesde(),
        request.getFechaHasta(),
        request.getTopProductos()
    );

    // Desglose por mes
    Map<String, List<ProductoVendidoDTO>> productosPorMes = new LinkedHashMap<>();
    Map<String, BigDecimal> totalesPorMes = new LinkedHashMap<>();

    if (Boolean.TRUE.equals(request.getAgruparPorMes())) {
      YearMonth inicio = YearMonth.from(request.getFechaDesde());
      YearMonth fin = YearMonth.from(request.getFechaHasta());

      for (YearMonth mes = inicio; !mes.isAfter(fin); mes = mes.plusMonths(1)) {
        LocalDate primerDia = mes.atDay(1);
        LocalDate ultimoDia = mes.atEndOfMonth();

        String claveMes = mes.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        List<ProductoVendidoDTO> productosDelMes = getTopProductos(
            request.getSucursalId(),
            primerDia,
            ultimoDia,
            request.getTopProductos()
        );

        BigDecimal totalMes = productosDelMes.stream()
            .map(ProductoVendidoDTO::getCantidadVendida)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        productosPorMes.put(claveMes, productosDelMes);
        totalesPorMes.put(claveMes, totalMes);
      }
    }

    // Totales generales
    BigDecimal totalGeneral = topProductos.stream()
        .map(ProductoVendidoDTO::getCantidadVendida)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    return ReporteProductosVendidosResponse.builder()
        .empresaNombre(sucursal.getEmpresa().getNombreRazonSocial())
        .sucursalNombre(sucursal.getNombre())
        .fechaDesde(request.getFechaDesde())
        .fechaHasta(request.getFechaHasta())
        .totalProductosVendidos(totalGeneral)
        .cantidadProductosDistintos(topProductos.size())
        .topProductos(topProductos)
        .productosPorMes(productosPorMes)
        .totalesPorMes(totalesPorMes)
        .build();
  }

  /**
   * Productos vendidos hoy
   */
  @Transactional(readOnly = true)
  public List<ProductoVendidoDTO> getProductosVendidosHoy(Long sucursalId, Integer top) {
    LocalDate hoy = LocalDate.now();
    return getTopProductos(sucursalId, hoy, hoy, top);
  }

  /**
   * Detalle de un producto específico
   */
  /**
   * Detalle de un producto específico
   */
  @Transactional(readOnly = true)
  public ProductoVendidoDTO getDetalleProducto(
      Long productoId, Long sucursalId, LocalDate fechaDesde, LocalDate fechaHasta) {

    String sql = """
        SELECT 
            p.id,
            p.codigo_interno as codigo,
            p.nombre,
            STRING_AGG(DISTINCT c.nombre, ', ') as categorias,
            COALESCE(SUM(m.cantidad_vendida), 0) as total_vendido
        FROM productos p
        LEFT JOIN metricas_productos_vendidos m 
            ON m.producto_id = p.id 
            AND m.sucursal_id = ? 
            AND m.fecha BETWEEN ? AND ?
        LEFT JOIN producto_categoria pc ON pc.producto_id = p.id
        LEFT JOIN categorias_producto c ON c.id = pc.categoria_id
        WHERE p.id = ?
        GROUP BY p.id, p.codigo_interno, p.nombre
        """;

    return jdbcTemplate.queryForObject(
        sql,
        (rs, rowNum) -> ProductoVendidoDTO.builder()
            .productoId(rs.getLong("id"))
            .codigoProducto(rs.getString("codigo"))
            .nombreProducto(rs.getString("nombre"))
            .categoria(rs.getString("categorias"))
            .cantidadVendida(rs.getBigDecimal("total_vendido"))
            .build(),
        sucursalId, fechaDesde, fechaHasta, productoId
    );
  }

  /**
   * Comparativa: Mes actual vs Mes anterior
   */
  @Transactional(readOnly = true)
  public Map<String, Object> getComparativaMensual(Long sucursalId, Integer top) {
    LocalDate hoy = LocalDate.now();
    YearMonth mesActual = YearMonth.from(hoy);
    YearMonth mesAnterior = mesActual.minusMonths(1);

    List<ProductoVendidoDTO> productosActual = getTopProductos(
        sucursalId,
        mesActual.atDay(1),
        mesActual.atEndOfMonth(),
        top
    );

    List<ProductoVendidoDTO> productosAnterior = getTopProductos(
        sucursalId,
        mesAnterior.atDay(1),
        mesAnterior.atEndOfMonth(),
        top
    );

    BigDecimal totalActual = productosActual.stream()
        .map(ProductoVendidoDTO::getCantidadVendida)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal totalAnterior = productosAnterior.stream()
        .map(ProductoVendidoDTO::getCantidadVendida)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal diferencia = totalActual.subtract(totalAnterior);
    BigDecimal porcentajeCambio = totalAnterior.compareTo(BigDecimal.ZERO) > 0
        ? diferencia.divide(totalAnterior, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
        : BigDecimal.ZERO;

    Map<String, Object> resultado = new HashMap<>();
    resultado.put("mesActual", mesActual.toString());
    resultado.put("mesAnterior", mesAnterior.toString());
    resultado.put("productosActual", productosActual);
    resultado.put("productosAnterior", productosAnterior);
    resultado.put("totalActual", totalActual);
    resultado.put("totalAnterior", totalAnterior);
    resultado.put("diferencia", diferencia);
    resultado.put("porcentajeCambio", porcentajeCambio);

    return resultado;
  }

  // ===== MÉTODOS HELPER =====

  private void calcularPorcentajes(List<ProductoVendidoDTO> productos) {
    if (productos == null || productos.isEmpty()) {
      return;
    }

    BigDecimal total = productos.stream()
        .map(ProductoVendidoDTO::getCantidadVendida)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    if (total.compareTo(BigDecimal.ZERO) > 0) {
      productos.forEach(p -> {
        BigDecimal porcentaje = p.getCantidadVendida()
            .divide(total, 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"))
            .setScale(2, RoundingMode.HALF_UP);
        p.setPorcentaje(porcentaje);
      });
    }
  }
}