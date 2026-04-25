package com.snnsoluciones.backnathbitpos.service.kiosko;

import com.snnsoluciones.backnathbitpos.dto.kiosko.KioscoConfigDTO;
import com.snnsoluciones.backnathbitpos.dto.kiosko.KioskoInitResponse;
import com.snnsoluciones.backnathbitpos.dto.kiosko.KioskoInitResponse.*;
import com.snnsoluciones.backnathbitpos.dto.kiosko.KioskoOrdenDTOs.CrearOrdenKioskoRequest;
import com.snnsoluciones.backnathbitpos.dto.kiosko.KioskoOrdenDTOs.ItemKioskoRequest;
import com.snnsoluciones.backnathbitpos.dto.kiosko.KioskoOrdenDTOs.ItemOrdenKioskoResponse;
import com.snnsoluciones.backnathbitpos.dto.kiosko.KioskoOrdenDTOs.OrdenKioskoResponse;
import com.snnsoluciones.backnathbitpos.dto.kiosko.KioskoOrdenDTOs.OrdenPendientePagoResponse;
import com.snnsoluciones.backnathbitpos.entity.global.Dispositivo;
import com.snnsoluciones.backnathbitpos.entity.global.Tenant;
import com.snnsoluciones.backnathbitpos.exception.BadRequestException;
import com.snnsoluciones.backnathbitpos.exception.NotFoundException;
import com.snnsoluciones.backnathbitpos.exception.UnauthorizedException;
import com.snnsoluciones.backnathbitpos.repository.KioscoConfigRepository;
import com.snnsoluciones.backnathbitpos.repository.global.DispositivoRepository;
import com.snnsoluciones.backnathbitpos.repository.global.TenantRepository;
import com.snnsoluciones.backnathbitpos.service.V2SesionCajaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class KioskoService {

  private final DispositivoRepository dispositivoRepository;
  private final V2SesionCajaService sesionCajaService;
  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;
  private final TenantRepository tenantRepository;
  private final KioscoConfigRepository kioscoConfigRepository;

  @Transactional
  public KioskoInitResponse init(String deviceToken) {
    log.info("Kiosko init — deviceToken: {}...", deviceToken.substring(0, 8));

    // 1. Validar dispositivo
    Dispositivo dispositivo = dispositivoRepository.findByTokenAndActivoTrueWithTenant(deviceToken)
        .orElseThrow(() -> new UnauthorizedException("Dispositivo no autorizado"));

    // 2. Validar que es tipo KIOSKO
    if (!"KIOSKO".equals(dispositivo.getTipo())) {
      throw new BadRequestException("Este dispositivo no está configurado como kiosko");
    }

    Tenant tenant = dispositivo.getTenant();
    if (!tenant.estaActivo()) {
      throw new BadRequestException("La empresa no está activa");
    }

    // 3. Validar que tiene terminal asignada
    if (dispositivo.getTerminalId() == null) {
      throw new BadRequestException(
          "El kiosko no tiene terminal asignada. Configurar desde el panel.");
    }

    // 4. Registrar uso
    dispositivo.registrarUso();
    dispositivoRepository.save(dispositivo);

    // 5. Abrir/recuperar sesión de caja autónoma
    var sesionResponse = sesionCajaService.abrirSesionKiosko(dispositivo.getTerminalId());

    // 6. Cargar configuración del kiosko
    KioskoConfig config = parsearConfig(dispositivo.getConfig(), tenant.getSchemaName(),
        dispositivo.getSucursalId());

    // 7. Cargar catálogo de productos
    List<CategoriaKiosko> categorias = cargarCatalogo(tenant.getSchemaName(),
        dispositivo.getSucursalId());

    log.info("Kiosko init OK — sesion={} categorias={}", sesionResponse.getSesionId(),
        categorias.size());

    // 8. Cargar branding de la sucursal
    KioscoConfigDTO branding = kioscoConfigRepository
        .findBySucursalId(dispositivo.getSucursalId())
        .map(KioscoConfigDTO::from)
        .orElseGet(() -> KioscoConfigDTO.builder()
            .sucursalId(dispositivo.getSucursalId())
            .templateId("CLEAN_LIGHT")
            .colorPrimary("#1F4E79")
            .colorSecondary("#2E75B6")
            .colorBackground("#FFFFFF")
            .colorSurface("#F4F4F4")
            .colorTextPrimary("#222222")
            .colorTextSecondary("#888888")
            .colorAccent("#FFD60A")
            .colorSuccess("#2EC4B6")
            .colorDanger("#E71D36")
            .textoBienvenida("¡Bienvenido! Toca para comenzar")
            .tiempoInactividad(60)
            .mostrarPrecios(true)
            .requierePagoEnCaja(true)
            .build()
        );

    return KioskoInitResponse.builder()
        .dispositivoId(dispositivo.getId())
        .dispositivoNombre(dispositivo.getNombre())
        .sucursalId(dispositivo.getSucursalId())
        .sucursalNombre(dispositivo.getSucursalNombre())
        .sesionId(sesionResponse.getSesionId())
        .terminalId(dispositivo.getTerminalId())
        .config(config)
        .branding(branding)
        .categorias(categorias)
        .build();
  }

  @Transactional
  public OrdenKioskoResponse crearOrden(String deviceToken, CrearOrdenKioskoRequest request) {
    log.info("Kiosko crear orden — metodoPago={} tipoConsumo={}", request.getMetodoPago(),
        request.getTipoConsumo());

    // 1. Validar dispositivo
    Dispositivo dispositivo = dispositivoRepository.findByTokenAndActivoTrueWithTenant(deviceToken)
        .orElseThrow(() -> new UnauthorizedException("Dispositivo no autorizado"));

      if (!"KIOSKO".equals(dispositivo.getTipo())) {
          throw new BadRequestException("Dispositivo no es kiosko");
      }

    Tenant tenant = dispositivo.getTenant();
    String schemaName = tenant.getSchemaName();

    // 2. Verificar config del kiosko (pausado?)
    KioskoConfig config = parsearConfig(dispositivo.getConfig(), schemaName,
        dispositivo.getSucursalId());
      if (config.isPausado()) {
          throw new BadRequestException("Kiosko pausado temporalmente");
      }

    // 3. Validar método de pago permitido
    if (!"CAJA".equals(request.getMetodoPago()) &&
        !config.getMetodosPago().contains(request.getMetodoPago())) {
      throw new BadRequestException("Método de pago no habilitado en este kiosko");
    }

    // 4. Obtener sesión activa del kiosko
    var sesionResponse = sesionCajaService.abrirSesionKiosko(dispositivo.getTerminalId());

    // 5. Calcular número display (secuencial del día, simple)
    int numeroDisplay = obtenerSiguienteNumeroDisplay(schemaName, dispositivo.getSucursalId());

    // 6. Construir items via JDBC (los productos están en el schema del tenant)
    BigDecimal subtotal = BigDecimal.ZERO;
    BigDecimal totalImpuesto = BigDecimal.ZERO;
    List<ItemOrdenKioskoResponse> itemsResponse = new ArrayList<>();

    for (ItemKioskoRequest itemReq : request.getItems()) {
      Map<String, Object> producto = obtenerProducto(schemaName, itemReq.getProductoId());
        if (producto == null) {
            throw new NotFoundException("Producto " + itemReq.getProductoId() + " no encontrado");
        }

      BigDecimal precio = (BigDecimal) producto.get("precio_venta");
      BigDecimal subtotalItem = precio.multiply(BigDecimal.valueOf(itemReq.getCantidad()));

      // TODO: calcular impuesto según producto
      subtotal = subtotal.add(subtotalItem);

      itemsResponse.add(ItemOrdenKioskoResponse.builder()
          .id(itemReq.getProductoId())
          .nombreProducto((String) producto.get("nombre"))
          .cantidad(itemReq.getCantidad())
          .precioUnitario(precio)
          .total(subtotalItem)
          .notas(itemReq.getNotas())
          .build());
    }

    BigDecimal total = subtotal.add(totalImpuesto);

    // 7. Determinar estado según método de pago
    String estado = "CAJA".equals(request.getMetodoPago())
        ? "PENDIENTE_PAGO"
        : "EN_PREPARACION";

    // 8. Insertar orden via JDBC en el schema del tenant
    String numeroOrden = generarNumeroOrdenKiosko(
        numeroDisplay);

    Long ordenId = jdbcTemplate.queryForObject(
        String.format("""
            INSERT INTO %s.ordenes
              (numero, sucursal_id, mesero_id, estado, nombre_cliente,
               subtotal, total_descuento, total_impuesto, total_servicio, total,
               numero_personas, porcentaje_servicio,
               observaciones, fecha_creacion, fecha_actualizacion)
            VALUES (?, ?, NULL, ?, ?, ?, 0, ?, 0, ?, 1, 0, ?, NOW(), NOW())
            RETURNING id
            """, schemaName),
        Long.class,
        numeroOrden,
        dispositivo.getSucursalId(),
        estado,
        request.getNombreCliente(),
        subtotal,
        totalImpuesto,
        total,
        "KIOSKO|" + request.getTipoConsumo() + "|" + request.getMetodoPago()
    );

    // 9. Insertar items
    for (ItemKioskoRequest itemReq : request.getItems()) {
      Map<String, Object> producto = obtenerProducto(schemaName, itemReq.getProductoId());
      BigDecimal precio = (BigDecimal) producto.get("precio_venta");
      BigDecimal subtotalItem = precio.multiply(BigDecimal.valueOf(itemReq.getCantidad()));

      jdbcTemplate.update(
          String.format("""
              INSERT INTO %s.orden_items
                (orden_id, producto_id, cantidad, precio_unitario,
                 subtotal, total_descuento, total_impuesto, total,
                 notas, enviado_cocina, preparado, entregado,
                 tarifa_impuesto, porcentaje_descuento, estado_pago,
                 fecha_creacion)
              VALUES (?, ?, ?, ?, ?, 0, 0, ?, ?, false, false, false, 0, 0, 'PENDIENTE', NOW())
              """, schemaName),
          ordenId, itemReq.getProductoId(), itemReq.getCantidad(),
          precio, subtotalItem, subtotalItem, itemReq.getNotas()
      );
    }

    // 10. Si paga directo (no en caja), enviar a cocina automáticamente
    if (!"CAJA".equals(request.getMetodoPago())) {
      jdbcTemplate.update(
          String.format("""
              UPDATE %s.orden_items
              SET enviado_cocina = true, fecha_envio_cocina = NOW()
              WHERE orden_id = ?
              """, schemaName),
          ordenId
      );
    }

    log.info("Orden kiosko {} creada — estado={} display=#{}", numeroOrden, estado, numeroDisplay);

    return OrdenKioskoResponse.builder()
        .ordenId(ordenId)
        .numeroOrden(numeroOrden)
        .numeroDisplay(numeroDisplay)
        .subtotal(subtotal)
        .totalImpuesto(totalImpuesto)
        .total(total)
        .descuentoAplicado(BigDecimal.ZERO)
        .estado(estado)
        .metodoPago(request.getMetodoPago())
        .tipoConsumo(request.getTipoConsumo())
        .nombreCliente(request.getNombreCliente())
        .tipoDocumento("CAJA".equals(request.getMetodoPago()) ? "PENDIENTE" :
            config.isOfrecerFacturaElectronica() ? "ELECTRONICA" : "INTERNA")
        .tiempoEstimadoMinutos(10) // TODO: calcular según carga de cocina
        .creadaEn(LocalDateTime.now())
        .items(itemsResponse)
        .build();
  }

  @Transactional(readOnly = true)
  public List<OrdenPendientePagoResponse> getOrdenesPendientesPago(String schemaName,
      Long sucursalId) {
    String sql = String.format("""
        SELECT o.id, o.numero, o.nombre_cliente, o.observaciones,
               o.total, o.fecha_creacion, s.nombre AS sucursal_nombre
        FROM %s.ordenes o
        JOIN %s.sucursales s ON s.id = o.sucursal_id
        WHERE o.sucursal_id = ?
          AND o.estado = 'PENDIENTE_PAGO'
        ORDER BY o.fecha_creacion ASC
        """, schemaName, schemaName);

    List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, sucursalId);

    return rows.stream().map(row -> {
      Long ordenId = ((Number) row.get("id")).longValue();
      String observaciones = (String) row.get("observaciones"); // "KIOSKO|AQUI|CAJA"
      String tipoConsumo = "AQUI";
      int numeroDisplay = 0;

      // Parsear observaciones del kiosko
      if (observaciones != null && observaciones.startsWith("KIOSKO|")) {
        String[] parts = observaciones.split("\\|");
          if (parts.length >= 2) {
              tipoConsumo = parts[1];
          }
        // Extraer número display del número de orden
        try {
          String num = ((String) row.get("numero"));
          numeroDisplay = Integer.parseInt(num.substring(num.lastIndexOf("-") + 1));
        } catch (Exception ignored) {
        }
      }

      LocalDateTime creadaEn = (LocalDateTime) row.get("fecha_creacion");
      long minutosEspera = java.time.temporal.ChronoUnit.MINUTES.between(creadaEn,
          LocalDateTime.now());

      // Obtener items de la orden
      List<ItemOrdenKioskoResponse> items = getItemsOrden(schemaName, ordenId);

      return OrdenPendientePagoResponse.builder()
          .ordenId(ordenId)
          .numeroOrden((String) row.get("numero"))
          .numeroDisplay(numeroDisplay)
          .nombreCliente((String) row.get("nombre_cliente"))
          .tipoConsumo(tipoConsumo)
          .total((BigDecimal) row.get("total"))
          .creadaEn(creadaEn)
          .minutosEspera(minutosEspera)
          .sucursalNombre((String) row.get("sucursal_nombre"))
          .items(items)
          .build();
    }).collect(java.util.stream.Collectors.toList());
  }

  // ── Helpers privados ──────────────────────────────────────────────────

  public Dispositivo validarDispositivo(String deviceToken) {
    return dispositivoRepository.findByTokenAndActivoTrueWithTenant(deviceToken)
        .orElseThrow(() -> new UnauthorizedException("Dispositivo no autorizado"));
  }

  // 2 — getEstadoOrden()
  @Transactional(readOnly = true)
  public Map<String, Object> getEstadoOrden(String schemaName, Long ordenId) {
    try {
      return jdbcTemplate.queryForMap(
          String.format("""
              SELECT id, numero, estado, nombre_cliente, total, fecha_creacion
              FROM %s.ordenes WHERE id = ?
              """, schemaName),
          ordenId);
    } catch (Exception e) {
      throw new NotFoundException("Orden no encontrada");
    }
  }

  // 3 — getSchemaNamePorEmpresa()
  @Transactional(readOnly = true)
  public String getSchemaNamePorEmpresa(Long empresaId) {
    return tenantRepository.findByEmpresaLegacyId(empresaId)
        .map(t -> t.getSchemaName())
        .orElseThrow(() -> new NotFoundException("Tenant no encontrado para empresa " + empresaId));
  }

  private int obtenerSiguienteNumeroDisplay(String schemaName, Long sucursalId) {
    // Número del día (1-999), se resetea a medianoche operativa (4am)
    try {
      Integer max = jdbcTemplate.queryForObject(
          String.format("""
              SELECT COALESCE(MAX(
                  CAST(SUBSTRING(numero FROM '-(\\d+)$') AS INTEGER)
              ), 0)
              FROM %s.ordenes
              WHERE sucursal_id = ?
                AND fecha_creacion >= (CURRENT_DATE + INTERVAL '4 hours')
                AND estado != 'ANULADA'
              """, schemaName),
          Integer.class, sucursalId);
      return (max != null ? max : 0) + 1;
    } catch (Exception e) {
      log.warn("Error obteniendo número display: {}", e.getMessage());
      return 1;
    }
  }

  private String generarNumeroOrdenKiosko(int display) {
    java.time.LocalDate hoy = java.time.LocalDate.now();
    return String.format("ORD-%02d%02d%02d-%03d",
        hoy.getDayOfMonth(), hoy.getMonthValue(), hoy.getYear() % 100, display);
  }

  private Map<String, Object> obtenerProducto(String schemaName, Long productoId) {
    try {
      return jdbcTemplate.queryForMap(
          String.format(
              "SELECT id, nombre, precio_venta FROM %s.productos WHERE id = ? AND activo = true",
              schemaName),
          productoId);
    } catch (Exception e) {
      return null;
    }
  }

  private List<ItemOrdenKioskoResponse> getItemsOrden(String schemaName, Long ordenId) {
    String sql = String.format("""
        SELECT oi.id, p.nombre, oi.cantidad, oi.precio_unitario, oi.total, oi.notas
        FROM %s.orden_items oi
        JOIN %s.productos p ON p.id = oi.producto_id
        WHERE oi.orden_id = ?
        """, schemaName, schemaName);
    try {
      return jdbcTemplate.query(sql, (rs, rn) -> ItemOrdenKioskoResponse.builder()
          .id(rs.getLong("id"))
          .nombreProducto(rs.getString("nombre"))
          .cantidad(rs.getInt("cantidad"))
          .precioUnitario(rs.getBigDecimal("precio_unitario"))
          .total(rs.getBigDecimal("total"))
          .notas(rs.getString("notas"))
          .build(), ordenId);
    } catch (Exception e) {
      return List.of();
    }
  }

  // ── Helpers ───────────────────────────────────────────────────────────

  private KioskoConfig parsearConfig(String configJson, String schemaName, Long sucursalId) {
    // Config por defecto
    KioskoConfig config = KioskoConfig.builder()
        .permitePagoDirecto(true)
        .metodosPago(List.of("TARJETA", "SINPE"))
        .aceptaEfectivo(false)
        .modosConsumo(List.of("AQUI", "LLEVAR"))
        .pausado(false)
        .maxItemsOrden(20)
        .mostrarTiempoEspera(true)
        .ofrecerFacturaElectronica(verificarHacienda(schemaName, sucursalId))
        .build();

    // Si tiene config JSON personalizada, mergear
    if (configJson != null && !configJson.isBlank() && !configJson.equals("{}")) {
      try {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = objectMapper.readValue(configJson, Map.class);

          if (map.containsKey("permitePagoDirecto")) {
              config.setPermitePagoDirecto((Boolean) map.get("permitePagoDirecto"));
          }
          if (map.containsKey("metodosPago")) {
              config.setMetodosPago((List<String>) map.get("metodosPago"));
          }
          if (map.containsKey("aceptaEfectivo")) {
              config.setAceptaEfectivo((Boolean) map.get("aceptaEfectivo"));
          }
          if (map.containsKey("pausado")) {
              config.setPausado((Boolean) map.get("pausado"));
          }
          if (map.containsKey("maxItemsOrden")) {
              config.setMaxItemsOrden((Integer) map.get("maxItemsOrden"));
          }

      } catch (Exception e) {
        log.warn("Error parseando config kiosko: {}", e.getMessage());
      }
    }

    return config;
  }

  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
  private boolean verificarHacienda(String schemaName, Long sucursalId) {
    try {
      String sql = String.format("""
            SELECT e.requiere_hacienda
            FROM %s.sucursales s
            JOIN %s.empresas e ON e.id = s.empresa_id
            WHERE s.id = ?
            """, schemaName, schemaName);
      Boolean requiere = jdbcTemplate.queryForObject(sql, Boolean.class, sucursalId);
      return Boolean.TRUE.equals(requiere);
    } catch (Exception e) {
      log.warn("Error verificando Hacienda para sucursal {}: {}", sucursalId, e.getMessage());
      return false;
    }
  }

  private List<CategoriaKiosko> cargarCatalogo(String schemaName, Long sucursalId) {
    // Cargar categorías activas
    String sqlCategorias = String.format("""
        SELECT id, nombre
        FROM %s.categorias_producto
        WHERE activo = true
        ORDER BY nombre
        """, schemaName);

    try {
      List<Map<String, Object>> cats = jdbcTemplate.queryForList(sqlCategorias);
      List<CategoriaKiosko> resultado = new ArrayList<>();

      for (Map<String, Object> cat : cats) {
        Long catId = ((Number) cat.get("id")).longValue();
        String catNombre = (String) cat.get("nombre");

        List<ProductoKiosko> productos = cargarProductosCategoria(schemaName, catId);
          if (productos.isEmpty()) {
              continue; // no mostrar categorías vacías
          }

        resultado.add(CategoriaKiosko.builder()
            .id(catId)
            .nombre(catNombre)
            .imagen(null) // TODO: agregar imagen de categoría
            .productos(productos)
            .build());
      }

      return resultado;
    } catch (Exception e) {
      log.warn("Error cargando catálogo kiosko: {}", e.getMessage());
      return List.of();
    }
  }

  private List<ProductoKiosko> cargarProductosCategoria(String schemaName, Long categoriaId) {
    String sql = String.format("""
        SELECT DISTINCT p.id, p.nombre, p.descripcion, p.precio_venta,
               p.tipo, p.activo, p.imagen_url
        FROM %s.productos p
        JOIN %s.producto_categoria pc ON pc.producto_id = p.id
        WHERE pc.categoria_id = ?
          AND p.activo = true
          AND p.tipo IN ('VENTA', 'COMBO', 'COMPUESTO')
        ORDER BY p.nombre
        """, schemaName, schemaName);

    try {
      return jdbcTemplate.query(sql, (rs, rn) -> ProductoKiosko.builder()
          .id(rs.getLong("id"))
          .nombre(rs.getString("nombre"))
          .descripcion(rs.getString("descripcion"))
          .imagen(rs.getString("imagen_url"))
          .precio(rs.getBigDecimal("precio_venta"))
          .disponible(rs.getBoolean("activo"))
          .tipo(rs.getString("tipo"))
          .tieneOpciones("COMPUESTO".equals(rs.getString("tipo")))
          .build(), categoriaId);
    } catch (Exception e) {
      log.warn("Error cargando productos de categoría {}: {}", categoriaId, e.getMessage());
      return List.of();
    }
  }
}