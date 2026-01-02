package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.factura.FacturaEscposResponse;
import com.snnsoluciones.backnathbitpos.dto.factura.FacturaListaResponse;
import com.snnsoluciones.backnathbitpos.dto.factura.FacturaResponse;
import com.snnsoluciones.backnathbitpos.dto.factura.FacturaResponse.*;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.facturacion.EstadoFactura;
import com.snnsoluciones.backnathbitpos.enums.mh.*;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Builder para construir responses de factura con TODOS los detalles (null-safe)
 */
@Component
public class FacturaResponseBuilder {

  // ===== Helpers de null-safety / defaults =====

  private static BigDecimal nz(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }

  private static String s(String v) { return v != null ? v : ""; }

  private static <T> List<T> list(List<T> v) { return v != null ? v : Collections.emptyList(); }

  private static <E extends Enum<E>> E parseEnum(Class<E> type, String name, E fallback) {
    if (name == null || name.isBlank()) return fallback;
    try { return Enum.valueOf(type, name); } catch (Exception ex) { return fallback; }
  }

  private static <T> T firstNonNull(T v, T fallback) { return v != null ? v : fallback; }

  // ====== API ======

  /**
   * Construir response completo de factura (fail-fast en campos obligatorios)
   */
  public FacturaResponse construirResponse(Factura factura) {
    if (factura == null) throw new IllegalArgumentException("Factura no puede ser null");

    // Validaciones mínimas obligatorias (fail-fast con mensaje claro)
    Sucursal sucursal = requireNonNull(factura.getSucursal(), "Sucursal en factura es requerida");
    Empresa empresa = requireNonNull(sucursal.getEmpresa(), "Empresa en Sucursal es requerida");

    // Datos “opcionales” tomados con defaults
    Terminal terminal = sucursal.getTerminales() != null && !sucursal.getTerminales().isEmpty()
        ? sucursal.getTerminales().get(0) : factura.getTerminal(); // preferir explícito si lo trae la factura
    String terminalNombre = terminal != null ? s(terminal.getNombre()) : "";
    String sucursalNombre = s(sucursal.getNombre());
    String cajeroNombre = factura.getCajero() != null ? s(factura.getCajero().getNombre()) : "";

    return FacturaResponse.builder()
        // ===== IDENTIFICADORES =====
        .id(factura.getId())
        .clave(s(factura.getClave()))
        .consecutivo(s(factura.getConsecutivo()))
        .tipoDocumento(firstNonNull(factura.getTipoDocumento(), TipoDocumento.FACTURA_ELECTRONICA))
        .estado(firstNonNull(factura.getEstado(), EstadoFactura.GENERADA))

        // ===== EMISOR =====
        .emisor(construirEmisor(factura, empresa, sucursal))

        // ===== RECEPTOR =====
        .receptor(construirReceptor(factura))

        // ===== DATOS COMERCIALES =====
        .condicionVenta(firstNonNull(factura.getCondicionVenta(), CondicionVenta.CONTADO))
        .plazoCredito(Objects.nonNull(factura.getPlazoCredito())? factura.getPlazoCredito() : 0)
        .moneda(firstNonNull(factura.getMoneda(), Moneda.CRC))
        .tipoCambio(nz(factura.getTipoCambio()))
        .fechaEmision(factura.getFechaEmision())
        .situacionComprobante(firstNonNull(factura.getSituacion(), SituacionDocumento.NORMAL))
        .observaciones(s(factura.getObservaciones()))

        // ===== DETALLES =====
        .detalles(construirDetalles(list(factura.getDetalles())))

        // ===== OTROS CARGOS =====
        .otrosCargos(construirOtrosCargos(list(factura.getOtrosCargos())))

        // ===== REFERENCIAS =====
        .referencias(construirReferencias(List.of(factura))) // mantiene tu comportamiento original

        // ===== RESUMEN =====
        .resumen(construirResumen(factura))

        // ===== MEDIOS DE PAGO =====
        .mediosPago(construirMediosPago(list(factura.getMediosPago())))

        // ===== METADATA SISTEMA =====
        .sucursalNombre(sucursalNombre)
        .terminalNombre(terminalNombre)
        .cajeroNombre(cajeroNombre)
        .sesionCajaId(factura.getSesionCaja() != null ? factura.getSesionCaja().getId() : null)

        // ===== HACIENDA =====
        .mensajeHacienda("Por ahora nada")
        .xmlFirmado("Por ahora nada")
        .xmlRespuesta("Por ahora nada")

        .build();
  }

  /**
   * Construir datos del emisor (null-safe)
   */
  private EmisorDto construirEmisor(Factura factura, Empresa empresa, Sucursal sucursal) {
    return EmisorDto.builder()
        .nombre(firstNonNull(empresa.getNombreComercial(), s(empresa.getNombreRazonSocial())))
        .tipoIdentificacion(empresa.getTipoIdentificacion())
        .numeroIdentificacion(s(empresa.getIdentificacion()))
        .ubicacion(construirUbicacionSucursal(sucursal))
        .telefono(TelefonoDto.builder()
            .codigoPais("506")
            .numTelefono(s(sucursal.getTelefono()))
            .build())
        .correoElectronico(s(sucursal.getEmail()))
        .codigoActividad( "No lo ocupamos")
        .proveedorSistemas(s("NO lo ocupamos"))
        .build();
  }

  /**
   * Construir datos del receptor (null-safe)
   */
  private ReceptorDto construirReceptor(Factura factura) {
    Cliente cliente = factura.getCliente();

    ReceptorDto.ReceptorDtoBuilder builder = ReceptorDto.builder()
        .correoElectronico(s(factura.getEmailReceptor()))
        .codigoActividadReceptor(s(factura.getActividadReceptor()));

    if (cliente != null) {
      builder.clienteId(cliente.getId())
          .nombre(s(cliente.getRazonSocial()))
          .tipoIdentificacion(cliente.getTipoIdentificacion())
          .numeroIdentificacion(s(cliente.getNumeroIdentificacion()));

      // Ubicación del cliente si existe
      ClienteUbicacion ub = cliente.getUbicacion();
      if (ub != null) {
        builder.ubicacion(UbicacionDto.builder()
            .provincia(ub.getProvincia() != null ? s(ub.getProvincia().getProvincia()) : "")
            .canton(ub.getCanton() != null ? s(ub.getCanton().getCanton()) : "")
            .distrito(ub.getDistrito() != null ? s(ub.getDistrito().getDistrito()) : "")
            .barrio(ub.getBarrio() != null ? s(ub.getBarrio().getBarrio()) : "")
            .otrasSenas(s(ub.getOtrasSenas()))
            .build());
      }

      // Teléfono del cliente si existe
      if (cliente.getTelefonoNumero() != null || cliente.getTelefonoCodigoPais() != null) {
        builder.telefono(TelefonoDto.builder()
            .codigoPais(s(firstNonNull(cliente.getTelefonoCodigoPais(), "506")))
            .numTelefono(s(cliente.getTelefonoNumero()))
            .build());
      }
    } else {
      // Cliente genérico
      builder.nombre("")
          .tipoIdentificacion(null)
          .numeroIdentificacion("");
    }

    return builder.build();
  }

  /**
   * Construir ubicación Sucursal (null-safe)
   * SIEMPRE retorna un objeto UbicacionDto (aunque sea vacío)
   */
  private UbicacionDto construirUbicacionSucursal(Sucursal sucursal) {
    // Si no hay sucursal, retornar ubicación vacía
    if (sucursal == null) {
      return UbicacionDto.builder()
          .provincia("")
          .canton("")
          .distrito("")
          .barrio("")
          .otrasSenas("")
          .build();
    }

    // Si no hay provincia, retornar ubicación vacía
    if (sucursal.getProvincia() == null) {
      return UbicacionDto.builder()
          .provincia("")
          .canton("")
          .distrito("")
          .barrio("")
          .otrasSenas("")
          .build();
    }

    // Si hay provincia, construir ubicación normalmente
    return UbicacionDto.builder()
        .provincia(s(sucursal.getProvincia().getProvincia()))
        .canton(sucursal.getCanton() != null ? s(sucursal.getCanton().getCanton()) : "")
        .distrito(sucursal.getDistrito() != null ? s(sucursal.getDistrito().getDistrito()) : "")
        .barrio(sucursal.getBarrio() != null ? s(sucursal.getBarrio().getBarrio()) : "")
        .otrasSenas(s(sucursal.getOtrasSenas()))
        .build();
  }

  /**
   * Construir detalles (null-safe + orden por número de línea)
   */
  private List<DetalleFacturaDto> construirDetalles(List<FacturaDetalle> detalles) {
    if (detalles.isEmpty()) return Collections.emptyList();

    return detalles.stream()
        .filter(Objects::nonNull)
        .sorted(Comparator.comparing(fd -> firstNonNull(fd.getNumeroLinea(), 0)))
        .map(this::construirDetalle)
        .collect(Collectors.toList());
  }

  /**
   * Construir un detalle (null-safe)
   */
  private DetalleFacturaDto construirDetalle(FacturaDetalle detalle) {
    if (detalle == null) return null;

    Producto producto = detalle.getProducto();

    String unidadStr = detalle.getUnidadMedida();
    UnidadMedida unidad = parseEnum(UnidadMedida.class, unidadStr, UnidadMedida.UNIDAD);

    String productoNombre = (producto != null) ? s(producto.getNombre()) : s(detalle.getDetalle());
    Long productoId = (producto != null) ? producto.getId() : null;

    return DetalleFacturaDto.builder()
        .numeroLinea(firstNonNull(detalle.getNumeroLinea(), 0))
        .codigoCABYS(s(detalle.getCodigoCabys()))
        .codigoComercial(construirCodigoComercial(producto))
        .cantidad(nz(detalle.getCantidad()))
        .unidadMedida(unidad)
        .detalle(s(detalle.getDetalle()))
        .precioUnitario(nz(detalle.getPrecioUnitario()))
        .montoTotal(nz(detalle.getMontoTotal()))
        .subTotal(nz(detalle.getSubtotal()))
        .baseImponible(nz(detalle.getSubtotal()))
        .descuentos(construirDescuentos(list(detalle.getDescuentos())))
        .montoTotalLinea(nz(detalle.getMontoTotalLinea()))
        .impuestos(construirImpuestos(list(detalle.getImpuestos())))
        .esServicio(Boolean.TRUE.equals(detalle.getEsServicio()))
        .impuestoAsumidoEmisorFabrica(BigDecimal.ZERO)
        .impuestoNeto(nz(detalle.getMontoImpuesto()))
        .productoId(productoId)
        .productoNombre(productoNombre)
        .seleccionado(false)
        .build();
  }

  /**
   * Construir código comercial (null-safe)
   */
  private CodigoComercialDto construirCodigoComercial(Producto producto) {
    if (producto == null) return null;

    String interno = s(producto.getCodigoInterno());
    String barras  = s(producto.getCodigoBarras());

    if (interno.isBlank() && barras.isBlank()) return null;

    return CodigoComercialDto.builder()
        .tipo("Código uso Interno")
        .codigo(interno.isBlank() ? barras : interno)
        .build();
  }

  /**
   * Construir descuentos (null-safe)
   */
  private List<DescuentoDto> construirDescuentos(List<FacturaDescuento> descuentos) {
    if (descuentos.isEmpty()) return Collections.emptyList();

    return descuentos.stream()
        .filter(Objects::nonNull)
        .map(desc -> DescuentoDto.builder()
            .codigoDescuento(s(desc.getCodigoDescuento()))
            .descripcion(s(desc.getNaturalezaDescuento()))
            .porcentaje(nz(desc.getPorcentaje()))
            .montoDescuento(nz(desc.getMontoDescuento()))
            .orden(firstNonNull(desc.getOrden(), 0))
            .build())
        .collect(Collectors.toList());
  }

  /**
   * Construir impuestos (null-safe)
   */
  private List<ImpuestoDto> construirImpuestos(List<FacturaDetalleImpuesto> impuestos) {
    if (impuestos.isEmpty()) return Collections.emptyList();

    return impuestos.stream()
        .filter(Objects::nonNull)
        .map(imp -> ImpuestoDto.builder()
            .codigo(TipoImpuesto.fromCodigo(s(imp.getCodigoImpuesto())))
            .codigoTarifaIVA(CodigoTarifaIVA.fromCodigo(s(imp.getCodigoTarifaIVA())))
            .tarifa(nz(imp.getTarifa()))
            .monto(nz(imp.getMontoImpuesto()))
            .exoneracion(construirExoneracion(imp))
            .build())
        .collect(Collectors.toList());
  }

  /**
   * Construir exoneración (null-safe)
   */
  private ExoneracionDto construirExoneracion(FacturaDetalleImpuesto imp) {
    if (imp == null) return null;

    String tipoDocExStr = imp.getTipoDocumentoExoneracion();
    TipoDocumentoExoneracion tipoDocEx = parseEnum(
        TipoDocumentoExoneracion.class, tipoDocExStr, null);

    if (tipoDocEx == null
        && s(imp.getNumeroDocumentoExoneracion()).isBlank()
        && imp.getFechaEmisionExoneracion() == null
        && nz(imp.getTarifaExonerada()).compareTo(BigDecimal.ZERO) == 0
        && nz(imp.getMontoExoneracion()).compareTo(BigDecimal.ZERO) == 0) {
      // No hay datos relevantes; no devolver nodo vacío
      return null;
    }

    return ExoneracionDto.builder()
        .tipoDocumentoEX(tipoDocEx.name())
        .numeroDocumentoEX(s(imp.getNumeroDocumentoExoneracion()))
        .institucionOtorgante(s(imp.getNombreInstitucion()))
        .fechaEmisionExoneracion(imp.getFechaEmisionExoneracion())
        .porcentajeExonerado(nz(imp.getTarifaExonerada()))
        .montoExoneracion(nz(imp.getMontoExoneracion()))
        .articulo(imp.getArticuloExoneracion())
        .inciso(imp.getIncisoExoneracion())
        .build();
  }

  /**
   * Construir otros cargos (null-safe)
   */
  private List<OtroCargoDto> construirOtrosCargos(List<OtroCargo> otrosCargos) {
    if (otrosCargos.isEmpty()) return Collections.emptyList();

    return otrosCargos.stream()
        .filter(Objects::nonNull)
        .map(cargo -> OtroCargoDto.builder()
            .tipoDocumentoOC(firstNonNull(cargo.getTipoDocumentoOC(), "Otros"))
            .detalle(s(cargo.getNombreCargo()))
            .nombreCargo(s(cargo.getNombreCargo()))
            .porcentaje(nz(cargo.getPorcentaje()))
            .montoCargo(nz(cargo.getMontoCargo()))
            .numeroLinea(firstNonNull(cargo.getNumeroLinea(), 0))
            .build())
        .collect(Collectors.toList());
  }

  /**
   * Construir referencias (null-safe)
   */
  private List<InformacionReferenciaDto> construirReferencias(List<Factura> referencias) {
    if (referencias == null || referencias.isEmpty()) return Collections.emptyList();

    return referencias.stream()
        .filter(Objects::nonNull)
        .map(ref -> InformacionReferenciaDto.builder()
            .tipoDoc(firstNonNull(ref.getTipoDocumento(), TipoDocumento.FACTURA_ELECTRONICA))
            .numero(s(ref.getNumeroReferencia()))
            .fechaEmision(ref.getFechaEmision())
            .codigo(parseEnum(CodigoReferencia.class, ref.getCodigoReferencia(), CodigoReferencia.OTROS))
            .razon(s(ref.getRazonReferencia()))
            .build())
        .collect(Collectors.toList());
  }

  /**
   * Construir resumen (null-safe)
   */
  private ResumenFacturaDto construirResumen(Factura factura) {
    return ResumenFacturaDto.builder()
        // Totales por tipo
        .totalServGravados(nz(factura.getTotalServiciosGravados()))
        .totalServExentos(nz(factura.getTotalServiciosExentos()))
        .totalServExonerado(nz(factura.getTotalServiciosExonerados()))
        .totalServNoSujeto(nz(factura.getTotalServiciosNoSujetos()))
        .totalMercanciasGravadas(nz(factura.getTotalMercanciasGravadas()))
        .totalMercanciasExentas(nz(factura.getTotalMercanciasExentas()))
        .totalMercExonerada(nz(factura.getTotalMercanciasExoneradas()))
        .totalMercNoSujeta(nz(factura.getTotalMercanciasNoSujetas()))

        // Totales generales
        .totalGravado(nz(factura.getTotalGravado()))
        .totalExento(nz(factura.getTotalExento()))
        .totalExonerado(nz(factura.getTotalExonerado()))
        .totalNoSujeto(nz(factura.getTotalNoSujeto()))
        .totalVenta(nz(factura.getTotalVenta()))
        .totalDescuentos(nz(factura.getTotalDescuentos()))
        .totalVentaNeta(nz(factura.getTotalVentaNeta()))
        .totalImpuesto(nz(factura.getTotalImpuesto()))
        .totalImpAsumEmisorFabrica(BigDecimal.ZERO)
        .totalIVADevuelto(nz(factura.getTotalIVADevuelto()))
        .totalOtrosCargos(nz(factura.getTotalOtrosCargos()))
        .totalComprobante(nz(factura.getTotalComprobante()))

        // Desglose de impuestos
        .totalDesgloseImpuesto(construirDesgloseImpuestos(factura))
        .build();
  }

  /**
   * Construir desglose de impuestos
   * (Si aún no agregas el agrupado, devolvemos vacío para evitar NPE)
   */
  private List<TotalDesgloseImpuestoDto> construirDesgloseImpuestos(Factura factura) {
    if (factura == null) return Collections.emptyList();

    // 1) Preferir los totales ya consolidados en la entidad resumen
    List<FacturaResumenImpuesto> resumen = factura.getResumenImpuestos();
    if (resumen != null && !resumen.isEmpty()) {
      return resumen.stream()
          .map(ri -> TotalDesgloseImpuestoDto.builder()
              .codigo(toTipoImpuesto(ri.getCodigoImpuesto()))                // ej: "01" -> TipoImpuesto.IVA
              .codigoTarifaIVA(toCodigoTarifaIva(ri.getCodigoImpuesto(),     // solo aplica para IVA ("01")
                  ri.getCodigoTarifaIVA()))   // ej: "08" -> TARIFA_GENERAL_13 (según tu enum)
              .totalMontoImpuesto(safeScale(
                  defaultIfNull(ri.getTotalImpuestoNeto(), ri.getTotalMontoImpuesto())
              ))
              .build())
          .toList();
    }

    // 2) Fallback: agrupar desde detalles (si no hubieras llenado resumenImpuestos aún)
    // Nota: si todavía no tienes entidades de detalle aquí, puedes omitir este bloque.
    //       Lo dejo de referencia por si decides usarlo luego.
    if (factura.getDetalles() != null && !factura.getDetalles().isEmpty()) {
      record Clave(String codImp, String codTarifa) {}
      Map<Clave, BigDecimal> acumulado = new HashMap<>();

      factura.getDetalles().forEach(d -> {
        if (d.getImpuestos() == null) return;
        d.getImpuestos().forEach(imp -> {
          String codImp = toCodigoImpuestoString(imp.getCodigoImpuesto());       // normaliza a "01","02", etc.
          String codTarifa = toCodigoTarifaString(imp.getCodigoTarifaIVA()); // "08", etc., si aplica
          BigDecimal neto = defaultIfNull(imp.getImpuestoNeto(), imp.getImpuestoNeto()); // neto = monto - exoneración

          Clave k = new Clave(codImp, codTarifa);
          acumulado.merge(k, defaultIfZero(neto), BigDecimal::add);
        });
      });

      return acumulado.entrySet().stream()
          .map(e -> TotalDesgloseImpuestoDto.builder()
              .codigo(toTipoImpuesto(e.getKey().codImp()))
              .codigoTarifaIVA(toCodigoTarifaIva(e.getKey().codImp(), e.getKey().codTarifa()))
              .totalMontoImpuesto(safeScale(e.getValue()))
              .build())
          .toList();
    }

    // Si no hay nada, devolvemos vacío
    return Collections.emptyList();
  }

  /**
   * Construir medios de pago (null-safe)
   */
  private List<MedioPagoDto> construirMediosPago(List<FacturaMedioPago> mediosPago) {
    if (mediosPago.isEmpty()) return Collections.emptyList();

    return mediosPago.stream()
        .filter(Objects::nonNull)
        .map(mp -> {
          com.snnsoluciones.backnathbitpos.enums.mh.MedioPago tipo =
              firstNonNull(mp.getMedioPago(), com.snnsoluciones.backnathbitpos.enums.mh.MedioPago.EFECTIVO);
          return MedioPagoDto.builder()
              .tipoMedioPago(tipo)
              .totalMedioPago(nz(mp.getMonto()))
              .referencia(s(mp.getReferencia()))
              .descripcion(obtenerDescripcionMedioPago(tipo))
              .build();
        })
        .collect(Collectors.toList());
  }

  /**
   * Obtener descripción del medio de pago
   */
  private String obtenerDescripcionMedioPago(com.snnsoluciones.backnathbitpos.enums.mh.MedioPago medioPago) {
    if (medioPago == null) return "Otros";
    return switch (medioPago) {
      case EFECTIVO -> "Efectivo";
      case TARJETA -> "Tarjeta";
      case CHEQUE -> "Cheque";
      case TRANSFERENCIA -> "Transferencia o depósito";
      case RECAUDADO_TERCEROS -> "Recaudado por terceros";
      default -> "Otros";
    };
  }

  /**
   * Construir response para lista (null-safe)
   */
  public FacturaListaResponse construirListaResponse(Factura factura) {
    if (factura == null) throw new IllegalArgumentException("Factura no puede ser null");

    String tipoDoc = firstNonNull(factura.getTipoDocumento(), TipoDocumento.FACTURA_ELECTRONICA).name();
    String clienteNombre = (factura.getCliente() != null)
        ? s(factura.getCliente().getRazonSocial())
        : "Consumidor final";
    String estado = firstNonNull(factura.getEstado(), EstadoFactura.GENERADA).name();

    return FacturaListaResponse.builder()
        .id(factura.getId())
        .consecutivo(s(factura.getConsecutivo()))
        .fechaEmision(factura.getFechaEmision())
        .tipoDocumento(tipoDoc)
        .clienteNombre(clienteNombre)
        .estado(estado)
        .moneda(firstNonNull(factura.getMoneda(), Moneda.CRC))
        .total(nz(factura.getTotalComprobante()))
        .build();
  }


  /* =================== helpers =================== */

  private static BigDecimal safeScale(BigDecimal v) {
    return v == null ? BigDecimal.ZERO : v.setScale(5, RoundingMode.HALF_UP);
  }
  private static BigDecimal defaultIfNull(BigDecimal a, BigDecimal b) {
    return a != null ? a : (b != null ? b : BigDecimal.ZERO);
  }
  private static BigDecimal defaultIfZero(BigDecimal a) {
    return (a == null) ? BigDecimal.ZERO : a;
  }

  /** Convierte "01" -> TipoImpuesto.IVA, etc. Ajusta a tu enum real */
  private static TipoImpuesto toTipoImpuesto(String codigo) {
    if (codigo == null) return null;
    // si tienes TipoImpuesto.fromCodigo("01"), usa eso:
    try {
      return TipoImpuesto.fromCodigo(codigo);
    } catch (Exception ignore) {
      // fallback por nombre
      return enumByName(TipoImpuesto.class, codigo);
    }
  }

  /** Para IVA ("01") convierte "08" -> CodigoTarifaIVA.TARIFA_GENERAL_13 (según tu enum); para otros impuestos retorna null */
  private static CodigoTarifaIVA toCodigoTarifaIva(String codigoImpuesto, String codigoTarifa) {
    if (!"01".equals(codigoImpuesto)) return null; // solo IVA lleva código de tarifa
    if (codigoTarifa == null) return null;
    try {
      return CodigoTarifaIVA.fromCodigo(codigoTarifa);
    } catch (Exception ignore) {
      return enumByName(CodigoTarifaIVA.class, codigoTarifa);
    }
  }

  /** Normaliza desde tus clases de detalle (ajusta a tus getters reales) */
  private static String toCodigoImpuestoString(Object cod) {
    if (cod == null) return null;
    if (cod instanceof String s) return s;
    // si tu clase tiene getCodigo() o es un enum con getCodigo()
    try { return (String) cod.getClass().getMethod("getCodigo").invoke(cod); }
    catch (Exception e) { return cod.toString(); }
  }
  private static String toCodigoTarifaString(Object codTarifa) {
    if (codTarifa == null) return null;
    if (codTarifa instanceof String s) return s;
    try { return (String) codTarifa.getClass().getMethod("getCodigo").invoke(codTarifa); }
    catch (Exception e) { return codTarifa.toString(); }
  }

  private static <E extends Enum<E>> E enumByName(Class<E> e, String name) {
    if (name == null) return null;
    try { return Enum.valueOf(e, name); }
    catch (Exception ex) { return null; }
  }
}