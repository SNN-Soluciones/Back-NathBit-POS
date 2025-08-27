package com.snnsoluciones.backnathbitpos.service.pdf;

import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.mh.CondicionVenta;
import com.snnsoluciones.backnathbitpos.enums.mh.MedioPago;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoIdentificacion;
import com.snnsoluciones.backnathbitpos.repository.FacturaRepository;
import com.snnsoluciones.backnathbitpos.service.EmpresaService;
import com.snnsoluciones.backnathbitpos.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para mapear datos de factura a parámetros de JasperReports Siguiendo formato de facturas
 * de Costa Rica
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FacturaPdfMapperService {

  private final FacturaRepository facturaRepository;
  private final StorageService storageService;
  private final EmpresaService empresaService;
  private final Generators generators;

  private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");
  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(
      "dd/MM/yyyy - hh:mm:ss a");
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  @Transactional(readOnly = true)
  public Map<String, Object> mapearFacturaAParametros(String clave, boolean esTicket) {
    log.info("Mapeando factura con clave: {} para formato: {}", clave, esTicket ? "80mm" : "carta");

    // Buscar factura por clave
    Factura factura = facturaRepository.findByClave(clave)
        .orElseThrow(() -> new RuntimeException("Factura no encontrada con clave: " + clave));

    Map<String, Object> params = new HashMap<>();

    // Mapear datos del emisor (empresa/sucursal)
    mapearDatosEmisor(params, factura);

    // Mapear datos del receptor (cliente)
    mapearDatosReceptor(params, factura);

    // Mapear datos del documento
    mapearDatosDocumento(params, factura);

    // Mapear líneas de detalle
    mapearDetalles(params, factura, esTicket);

    // Mapear totales
    mapearTotales(params, factura);

    // Mapear información de exoneración si existe
    mapearExoneraciones(params, factura);

    // Mapear otros cargos si existen
    mapearOtrosCargos(params, factura);

    // Mapear referencias si existen
    mapearReferencias(params, factura);

    // Información adicional
    mapearInformacionAdicional(params, factura);

    // Logo de la empresa (desde S3)
    cargarLogoEmpresa(params, empresaService.buscarPorId(factura.getSucursal().getEmpresa().getId()));

    return params;
  }

  private void mapearDatosEmisor(Map<String, Object> params, Factura factura) {
    Sucursal sucursal = factura.getSucursal();
    Empresa empresa = empresaService.buscarPorId(sucursal.getEmpresa().getId());

    // Nombre comercial o razón social
    params.put("emisor_nombre", empresa.getNombreComercial() != null ?
        empresa.getNombreComercial() : empresa.getNombreRazonSocial());

    // Identificación
    params.put("emisor_identificacion", formatearIdentificacion(
        empresa.getTipoIdentificacion(), empresa.getIdentificacion()));

    // Contacto
    params.put("emisor_telefono", sucursal.getTelefono() != null ?
        sucursal.getTelefono() : empresa.getTelefono());
    params.put("emisor_correo", sucursal.getEmail() != null ?
        sucursal.getEmail() : empresa.getEmail());

    // Dirección completa
    String direccion = construirDireccionCompleta(sucursal, empresa);
    params.put("emisor_direccion", direccion);

    // Para formato ticket (80mm) - versión corta
    if (sucursal.getOtrasSenas() != null) {
      params.put("emisor_direccion_corta", sucursal.getOtrasSenas());
    }
  }

  private void mapearDatosReceptor(Map<String, Object> params, Factura factura) {
    Cliente cliente = factura.getCliente();

    if (cliente != null) {
      String direccion = construirDireccionCompletaCliente(factura.getCliente().getUbicacion());
      params.put("receptor_nombre", cliente.getRazonSocial());
      params.put("receptor_identificacion", formatearIdentificacion(
          cliente.getTipoIdentificacion(), cliente.getNumeroIdentificacion()));
      params.put("receptor_correo", cliente.getEmails() != null ? cliente.getEmails() : "");
      params.put("receptor_telefono",
          cliente.getTelefonoNumero() != null ? cliente.getTelefonoNumero() : "");
      params.put("receptor_direccion", Objects.nonNull(direccion) ? direccion : "");
    } else {
      // Cliente genérico
      params.put("receptor_nombre", "");
      params.put("receptor_identificacion", "");
      params.put("receptor_correo", "");
      params.put("receptor_telefono", "");
      params.put("receptor_direccion", "");
    }
  }

  private void mapearDatosDocumento(Map<String, Object> params, Factura factura) {
    // Clave y consecutivo
    params.put("clave", factura.getClave());
    params.put("consecutivo", factura.getConsecutivo());
    params.put("numero_interno", factura.getId().toString());

    // Tipo de documento
    params.put("tipo_documento", obtenerNombreTipoDocumento(factura.getTipoDocumento()));

    // Fecha y hora
    String fechaEmision = factura.getFechaEmision();
    params.put("fecha_emision", fechaEmision.substring(0, 10));

    // Condiciones comerciales
    params.put("condicion_venta", traducirCondicionVenta(factura.getCondicionVenta()));

    // CORRECCIÓN: Mapear múltiples medios de pago
    mapearMediosPago(params, factura);

    params.put("tipo_cambio", formatearMoneda(factura.getTipoCambio()));

    // Plazo crédito si aplica
    if (factura.getPlazoCredito() != null && factura.getPlazoCredito() > 0) {
      params.put("plazo_credito", factura.getPlazoCredito() + " días");
    }

    // Datos para código QR
    params.put("qr_data", factura.getClave());

    try {
      byte[] qrImage = generators.generarCodigoQRDetallado(factura);
      params.put("qr_image", qrImage);
    } catch (Exception e) {
      log.error("Error generando código QR: {}", e.getMessage());
      params.put("qr_image", null);
    }

    // Observaciones
    params.put("observaciones",
        factura.getObservaciones() != null ? factura.getObservaciones() : "");

    // Usuario que atendió
    if (factura.getSesionCaja() != null && factura.getSesionCaja().getUsuario() != null) {
      params.put("atendido_por", factura.getSesionCaja().getUsuario().getNombre());
    }
  }

  private void mapearMediosPago(Map<String, Object> params, Factura factura) {
    List<FacturaMedioPago> mediosPago = factura.getMediosPago();

    if (mediosPago == null || mediosPago.isEmpty()) {
      params.put("medio_pago", "Efectivo");
      return;
    }

    // Etiqueta compacta (si hay varios, concatenados)
    String mediosConcatenados = mediosPago.stream()
        .map(this::labelMedioPago)   // <-- usa enum
        .distinct()
        .collect(Collectors.joining(" / "));
    params.put("medio_pago", mediosConcatenados);

    // (Opcional) detalle por medio con monto
    List<Map<String, Object>> detallesMediosPago = new ArrayList<>();
    for (FacturaMedioPago mp : mediosPago) {
      Map<String, Object> row = new HashMap<>();
      row.put("tipo",  labelMedioPago(mp));
      row.put("monto", formatearMoneda(mp.getMonto()));
      detallesMediosPago.add(row);
    }
    params.put("detalle_medios_pago", detallesMediosPago);
  }

  private void mapearDetalles(Map<String, Object> params, Factura factura, boolean esTicket) {
    List<Map<String, Object>> detalles = new ArrayList<>();

    for (FacturaDetalle detalle : factura.getDetalles()) {
      Map<String, Object> item = new HashMap<>();

      // Datos básicos
      item.put("codigo", detalle.getProducto().getCodigoInterno());
      item.put("descripcion", esTicket ?
          truncarTexto(detalle.getDetalle(), 30) : detalle.getDetalle());
      item.put("cantidad", formatearCantidad(detalle.getCantidad()));
      item.put("unidad", detalle.getUnidadMedida());
      item.put("precio_unitario", formatearMoneda(detalle.getPrecioUnitario()));
      item.put("monto_total", formatearMoneda(detalle.getMontoTotal()));

      // Descuento si existe
      if (detalle.getMontoDescuento() != null
          && detalle.getMontoDescuento().compareTo(BigDecimal.ZERO) > 0) {
        item.put("descuento", formatearMoneda(detalle.getMontoDescuento()));
        item.put("tiene_descuento", true);
      } else {
        item.put("descuento", "0.00");
        item.put("tiene_descuento", false);
      }

      // Subtotal (total - descuento)
      item.put("subtotal", formatearMoneda(detalle.getSubtotal()));

      // Impuesto total de la línea
      BigDecimal impuestoTotal = detalle.getImpuestos().stream()
          .map(FacturaDetalleImpuesto::getMontoImpuesto)
          .reduce(BigDecimal.ZERO, BigDecimal::add);
      item.put("impuesto", formatearMoneda(impuestoTotal));

      // CORRECCIÓN: Verificar si hay exoneración en los impuestos de esta línea
      boolean tieneExoneracion = detalle.getImpuestos().stream()
          .anyMatch(imp -> imp.getTieneExoneracion() != null && imp.getTieneExoneracion()
              && imp.getMontoExoneracion() != null
              && imp.getMontoExoneracion().compareTo(BigDecimal.ZERO) > 0);

      if (tieneExoneracion) {
        item.put("tiene_exoneracion", true);
        // Sumar todos los montos exonerados de esta línea
        BigDecimal montoExoneradoLinea = detalle.getImpuestos().stream()
            .filter(imp -> imp.getMontoExoneracion() != null)
            .map(FacturaDetalleImpuesto::getMontoExoneracion)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        item.put("monto_exonerado", formatearMoneda(montoExoneradoLinea));
      } else {
        item.put("tiene_exoneracion", false);
        item.put("monto_exonerado", "0.00");
      }

      detalles.add(item);
    }

    params.put("detalles", detalles);
  }

  private void mapearTotales(Map<String, Object> params, Factura factura) {
    // Totales principales
    params.put("total_venta", formatearMoneda(factura.getTotalVenta()));
    params.put("total_descuentos", formatearMoneda(factura.getTotalDescuentos()));
    params.put("total_venta_neta", formatearMoneda(factura.getTotalVentaNeta()));
    params.put("total_impuesto", formatearMoneda(factura.getTotalImpuesto()));
    params.put("total_otros_cargos", formatearMoneda(factura.getTotalOtrosCargos()));
    params.put("total_comprobante", formatearMoneda(factura.getTotalComprobante()));

    // Totales por tipo de venta
    params.put("total_gravado", formatearMoneda(factura.getTotalGravado()));
    params.put("total_exento", formatearMoneda(factura.getTotalExento()));
    params.put("total_exonerado", formatearMoneda(factura.getTotalExonerado()));

    // Información de pago (para tickets)
    if (factura.getMediosPago() != null && !factura.getMediosPago().isEmpty()) {
      FacturaMedioPago primerPago = factura.getMediosPago().get(0);
      params.put("pago_con", formatearMoneda(primerPago.getMonto()));

      // Calcular vuelto si es efectivo
      if ("01".equals(primerPago.getMedioPago())) { // 01 = Efectivo
        BigDecimal vuelto = primerPago.getMonto().subtract(factura.getTotalComprobante());
        params.put("vuelto", formatearMoneda(vuelto.max(BigDecimal.ZERO)));
      } else {
        params.put("vuelto", "0.00");
      }
    }

    // Moneda
    params.put("moneda", factura.getMoneda().getCodigo());
    params.put("simbolo_moneda", "CRC".equals(factura.getMoneda().getCodigo()) ? "₡" : "$");
  }

  private void mapearExoneraciones(Map<String, Object> params, Factura factura) {
    List<Map<String, Object>> exoneraciones = new ArrayList<>();
    Map<String, Map<String, Object>> exoneracionesAgrupadas = new HashMap<>();

    // CORRECCIÓN: Recorrer todos los detalles e impuestos
    for (FacturaDetalle detalle : factura.getDetalles()) {
      for (FacturaDetalleImpuesto impuesto : detalle.getImpuestos()) {
        // Verificar si este impuesto tiene exoneración
        if (impuesto.getTieneExoneracion() != null && impuesto.getTieneExoneracion()
            && impuesto.getMontoExoneracion() != null
            && impuesto.getMontoExoneracion().compareTo(BigDecimal.ZERO) > 0) {

          String numeroDocumento = impuesto.getNumeroDocumentoExoneracion();

          // Verificar si ya existe esta exoneración
          if (exoneracionesAgrupadas.containsKey(numeroDocumento)) {
            // Sumar al monto existente
            Map<String, Object> exonExistente = exoneracionesAgrupadas.get(numeroDocumento);
            BigDecimal montoActual = new BigDecimal(
                exonExistente.get("monto_exonerado_raw").toString());
            BigDecimal nuevoMonto = montoActual.add(impuesto.getMontoExoneracion());
            exonExistente.put("monto_exonerado_raw", nuevoMonto);
            exonExistente.put("monto_exonerado", formatearMoneda(nuevoMonto));
          } else {
            // Crear nueva entrada de exoneración
            Map<String, Object> exon = new HashMap<>();
            exon.put("tipo", traducirTipoExoneracion(impuesto.getTipoDocumentoExoneracion()));
            exon.put("numero_documento", numeroDocumento);
            exon.put("institucion", impuesto.getNombreInstitucion() != null ?
                impuesto.getNombreInstitucion() : "");
            exon.put("fecha_emision", impuesto.getFechaEmisionExoneracion() != null ?
                impuesto.getFechaEmisionExoneracion() : "");
            exon.put("monto_exonerado_raw", impuesto.getMontoExoneracion());
            exon.put("monto_exonerado", formatearMoneda(impuesto.getMontoExoneracion()));

            // Calcular porcentaje si está disponible
            if (impuesto.getTarifaExonerada() != null) {
              exon.put("porcentaje", impuesto.getTarifaExonerada() + "%");
            } else {
              exon.put("porcentaje", "");
            }

            exoneracionesAgrupadas.put(numeroDocumento, exon);
          }
        }
      }
    }

    // Convertir el mapa a lista
    exoneraciones.addAll(exoneracionesAgrupadas.values());

    params.put("exoneraciones", exoneraciones);
    params.put("tiene_exoneraciones", !exoneraciones.isEmpty());
  }

  private void mapearOtrosCargos(Map<String, Object> params, Factura factura) {
    List<Map<String, Object>> otrosCargos = new ArrayList<>();

    if (factura.getOtrosCargos() != null) {
      for (OtroCargo cargo : factura.getOtrosCargos()) {
        Map<String, Object> item = new HashMap<>();
        item.put("tipo", traducirTipoOtroCargo(cargo.getTipoDocumentoOTROS()));
        item.put("detalle", cargo.getNombreCargo() != null ? cargo.getNombreCargo() : "");
        item.put("porcentaje", cargo.getPorcentaje() != null ?
            cargo.getPorcentaje() + "%" : "");
        item.put("monto", formatearMoneda(cargo.getMontoCargo()));
        otrosCargos.add(item);
      }
    }

    params.put("otros_cargos", otrosCargos);
    params.put("tiene_otros_cargos", !otrosCargos.isEmpty());
  }

  private void mapearReferencias(Map<String, Object> params, Factura factura) {
    List<Map<String, Object>> referencias = new ArrayList<>();

    if (factura.getFacturaReferencia() != null) {
      Factura ref = facturaRepository.findById(factura.getFacturaReferencia().getId()).orElse(null);
      if (ref != null) {
        Map<String, Object> item = new HashMap<>();
        item.put("tipo_doc", traducirTipoDocumento(ref.getTipoDocumento().getDescripcion()));
        item.put("clave", ref.getClave());
        item.put("fecha", ref.getFechaEmision());
        item.put("razon", ref.getRazonReferencia());
        referencias.add(item);
      }
    }

    params.put("referencias", referencias);
    params.put("tiene_referencias", !referencias.isEmpty());
  }

  private void mapearInformacionAdicional(Map<String, Object> params, Factura factura) {
    // Mensaje según estado
    String mensajeEstado = "";
    switch (factura.getEstado()) {
      case ACEPTADA:
        mensajeEstado = "mediante resolución N° DGT-R-033-2019 del 20/06/2019";
        break;
      case RECHAZADA:
        mensajeEstado = "DOCUMENTO RECHAZADO POR HACIENDA";
        break;
      case ERROR:
        mensajeEstado = "DOCUMENTO PENDIENTE DE ENVÍO";
        break;
      default:
        mensajeEstado = "";
    }
    params.put("mensaje_hacienda", mensajeEstado);

    // Terminal y sucursal
    if (factura.getTerminal() != null) {
      params.put("terminal", factura.getTerminal().getNombre());
    }
    params.put("sucursal", factura.getSucursal().getNombre());
  }

  private void cargarLogoEmpresa(Map<String, Object> params, Empresa empresa) {
    try {
      if (empresa.getLogoUrl() != null && !empresa.getLogoUrl().isEmpty()) {
        // CORRECCIÓN: Usar downloadFileAsBytes que retorna byte[]
        byte[] logoBytes = storageService.downloadFileAsBytes(empresa.getLogoUrl());
        params.put("logo_empresa", logoBytes);
        params.put("tiene_logo", true);
      } else {
        params.put("tiene_logo", false);
      }
    } catch (Exception e) {
      log.error("Error cargando logo de empresa: {}", e.getMessage());
      params.put("tiene_logo", false);
    }
  }

  // ========== MÉTODOS AUXILIARES ==========

  private String formatearIdentificacion(TipoIdentificacion tipo, String numero) {
    if (tipo == null || numero == null) {
      return "";
    }
    return tipo.getDescripcion() + ": " + numero;
  }

  private String construirDireccionCompletaCliente(ClienteUbicacion clienteUbicacion) {
    if (clienteUbicacion == null) {
      return "";
    }

    List<String> partes = new ArrayList<>();

    String provincia = clienteUbicacion.getProvincia() != null ?
        clienteUbicacion.getProvincia().getProvincia() : "";
    String canton = clienteUbicacion.getCanton() != null ?
        clienteUbicacion.getCanton().getCanton() : "";
    String distrito = clienteUbicacion.getDistrito() != null ?
        clienteUbicacion.getDistrito().getDistrito() : "";
    String direccion = clienteUbicacion.getOtrasSenas();

    if (!provincia.isEmpty()) {
      partes.add(provincia);
    }
    if (!canton.isEmpty()) {
      partes.add(canton);
    }
    if (!distrito.isEmpty()) {
      partes.add(distrito);
    }
    if (!direccion.isEmpty()) {
      partes.add(direccion);
    }

    return String.join(", ", partes);
  }

  private String construirDireccionCompleta(Sucursal sucursal, Empresa empresa) {
    List<String> partes = new ArrayList<>();

    // Usar datos de la sucursal primero, si no están, usar de empresa
    String provincia = sucursal.getProvincia() != null ?
        sucursal.getProvincia().getProvincia() :
        (empresa.getProvincia() != null ? empresa.getProvincia().getProvincia() : "");

    String canton = sucursal.getCanton() != null ?
        sucursal.getCanton().getCanton() :
        (empresa.getCanton() != null ? empresa.getCanton().getCanton() : "");

    String distrito = sucursal.getDistrito() != null ?
        sucursal.getDistrito().getDistrito() :
        (empresa.getDistrito() != null ? empresa.getDistrito().getDistrito() : "");

    String direccion = sucursal.getOtrasSenas() != null ?
        sucursal.getOtrasSenas() :
        (empresa.getOtrasSenas() != null ? empresa.getOtrasSenas() : "");

    if (!provincia.isEmpty()) {
      partes.add(provincia);
    }
    if (!canton.isEmpty()) {
      partes.add(canton);
    }
    if (!distrito.isEmpty()) {
      partes.add(distrito);
    }
    if (!direccion.isEmpty()) {
      partes.add(direccion);
    }

    return String.join(", ", partes);
  }

  private String formatearMoneda(BigDecimal valor) {
    if (valor == null) {
      return "0.00";
    }
    return DECIMAL_FORMAT.format(valor);
  }

  private String formatearCantidad(BigDecimal cantidad) {
    if (cantidad == null) {
      return "1";
    }
    // Si es entero, mostrar sin decimales
    if (cantidad.stripTrailingZeros().scale() <= 0) {
      return cantidad.intValue() + "";
    }
    return cantidad.stripTrailingZeros().toPlainString();
  }

  private String truncarTexto(String texto, int maxLength) {
    if (texto == null || texto.length() <= maxLength) {
      return texto;
    }
    return texto.substring(0, maxLength - 3) + "...";
  }

  private String obtenerNombreTipoDocumento(TipoDocumento tipo) {
    switch (tipo) {
      case FACTURA_ELECTRONICA:
        return "Factura Electrónica";
      case TIQUETE_ELECTRONICO:
        return "Tiquete Electrónico";
      case NOTA_CREDITO:
        return "Nota de Crédito Electrónica";
      case NOTA_DEBITO:
        return "Nota de Débito Electrónica";
      default:
        return tipo.name();
    }
  }

  private String traducirCondicionVenta(CondicionVenta condicion) {
    if (condicion == null) {
      return "CONTADO";
    }
    return switch (condicion) {
      case CONTADO -> "CONTADO";
      case CREDITO -> "CRÉDITO";
      case CONSIGNACION -> "CONSIGNACIÓN";
      case APARTADO -> "APARTADO";
      case ARRENDAMIENTO_OPCION_COMPRA -> "ARRENDAMIENTO CON OPCIÓN DE COMPRA";
      case ARRENDAMIENTO_FINANCIERO_V2 -> "ARRENDAMIENTO EN FUNCIÓN FINANCIERA";
      case COBRO_FAVOR_TERCERO -> "COBRO A FAVOR DE UN TERCERO";
      case SERVICIOS_PRESTADOS_ESTADO -> "SERVICIOS PRESTADOS AL ESTADO";
      case PAGO_SERVICIOS_ESTADO -> "PAGO DE SERVICIOS AL ESTADO";
      case VENTA_CREDITO_IVA_90_DIAS -> "VENTA CRÉDITO IVA 90 DÍAS";
      case OTROS -> "OTROS";
      default -> condicion.name();
    };
  }

  private String traducirMedioPago(String codigo) {
    if (codigo == null) {
      return "EFECTIVO";
    }
    return switch (codigo) {
      case "01" -> "EFECTIVO";
      case "02" -> "TARJETA";
      case "03" -> "CHEQUE";
      case "04" -> "TRANSFERENCIA";
      case "05" -> "RECAUDADO POR TERCEROS";
      case "99" -> "OTROS";
      default -> "OTROS";
    };
  }

  private String traducirTipoExoneracion(String tipo) {
    if (tipo == null) {
      return "";
    }
    return switch (tipo) {
      case "01" -> "Compras Autorizadas";
      case "02" -> "Ventas exentas a diplomáticos";
      case "03" -> "Orden de compra (Instituciones Públicas y otros)";
      case "04" -> "Exenciones Dirección General de Hacienda";
      case "05" -> "Transitorio V";
      case "99" -> "Otros";
      default -> tipo;
    };
  }

  private String traducirTipoOtroCargo(String tipo) {
    if (tipo == null) {
      return "";
    }
    return switch (tipo) {
      case "01" -> "Gastos de envío";
      case "02" -> "Gastos de transporte";
      case "03" -> "Gastos de instalación";
      case "04" -> "Impuesto al banano";
      case "05" -> "Impuesto INDER";
      case "06" -> "Impuesto a la madera";
      case "07" -> "Recargo";
      case "08" -> "Propina";
      case "99" -> "Otros cargos";
      default -> tipo;
    };
  }

  private String traducirTipoDocumento(String tipo) {
    if (tipo == null) {
      return "";
    }
    return switch (tipo) {
      case "01" -> "Factura electrónica";
      case "02" -> "Nota de débito";
      case "03" -> "Nota de crédito";
      case "04" -> "Tiquete electrónico";
      case "05" -> "Nota de despacho";
      case "06" -> "Contrato";
      case "07" -> "Procedimiento";
      case "08" -> "Comprobante emitido en el exterior";
      case "09" -> "Factura de exportación";
      case "99" -> "Otros";
      default -> tipo;
    };
  }

  // --- Helpers de mapeo ---

  private String labelMedioPago(FacturaMedioPago mp) {
    if (mp == null || mp.getMedioPago() == null) return "Efectivo";

    MedioPago medio = mp.getMedioPago(); // ¡Usa el enum, no la descripción!
    switch (medio) {
      case EFECTIVO:             return "Efectivo";
      case TARJETA:              return "Tarjeta";
      case CHEQUE:               return "Cheque";
      case TRANSFERENCIA:        return "Transferencia / Depósito";
      case RECAUDADO_TERCEROS:   return "Recaudado por terceros";
      case SINPE_MOVIL:          return "SINPE Móvil";
      case PLATAFORMA_DIGITAL:   return "Plataforma Digital";
      case OTROS:
      default:                   return "Otros";
    }
  }

  /** Por si algún día te llega algo “legacy” como texto: nombre, código o descripción */
  private MedioPago normalizarMedioPago(Object raw) {
    if (raw == null) return null;
    if (raw instanceof MedioPago) return (MedioPago) raw;

    String s = raw.toString().trim();

    // ¿Viene como código "06", "02"...?
    if (s.matches("\\d{2}")) {
      try { return MedioPago.fromCodigo(s); } catch (Exception ignored) {}
    }

    // ¿Viene como nombre del enum?
    String enumLike = s.toUpperCase().replace(' ', '_');
    try { return MedioPago.valueOf(enumLike); } catch (Exception ignored) {}

    // ¿Viene como descripción “humana”? Normalizamos acentos y variantes comunes:
    String desc = s.toUpperCase()
        .replace("Á","A").replace("É","E").replace("Í","I").replace("Ó","O").replace("Ú","U");

    if (desc.contains("SINPE"))             return MedioPago.SINPE_MOVIL;
    if (desc.contains("PLATAFORMA DIGITAL"))return MedioPago.PLATAFORMA_DIGITAL;
    if (desc.contains("EFECT"))             return MedioPago.EFECTIVO;
    if (desc.contains("TARJ"))              return MedioPago.TARJETA;
    if (desc.contains("CHEQ"))              return MedioPago.CHEQUE;
    if (desc.contains("TRANS") || desc.contains("DEPOS")) return MedioPago.TRANSFERENCIA;
    if (desc.contains("TERCER"))            return MedioPago.RECAUDADO_TERCEROS;

    return MedioPago.OTROS;
  }
}