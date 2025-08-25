package com.snnsoluciones.backnathbitpos.scheduler;

import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.mh.CondicionVenta;
import com.snnsoluciones.backnathbitpos.enums.mh.ProveedorSistema;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumentoExoneracion;
import com.snnsoluciones.backnathbitpos.repository.EmpresaConfigHaciendaRepository;
import com.snnsoluciones.backnathbitpos.repository.FacturaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Slf4j
@RequiredArgsConstructor
public class FacturaXMLGeneratorService {

  private final FacturaRepository facturaRepository;
  private final EmpresaConfigHaciendaRepository configRepository;

  // ============================
  // Helpers de formato/porcentajes
  // ============================
  private static String fmtMonto(BigDecimal v) { // 5 decimales fijos
    return v == null ? null : v.setScale(5, RoundingMode.HALF_UP).toPlainString();
  }

  private static String fmtCantidad(BigDecimal v) { // 3 decimales fijos
    return v == null ? null : v.setScale(3, RoundingMode.HALF_UP).toPlainString();
  }

  private static String fmtPorcentaje(BigDecimal v) { // 2 decimales fijos
    return v == null ? null : v.setScale(2, RoundingMode.HALF_UP).toPlainString();
  }

  private static BigDecimal asPercent(BigDecimal tarifa) {
    if (tarifa == null) {
      return null;
    }
    // Si viene como fracción (<=1), conviértela a porcentaje
    return tarifa.compareTo(BigDecimal.ONE) <= 0
        ? tarifa.multiply(new BigDecimal("100"))
        : tarifa;
  }

  @Transactional(readOnly = true)
  public String generarXML(Long facturaId) {
    log.info("Generando XML para factura ID: {}", facturaId);

    Factura factura = facturaRepository.findByIdWithRelaciones(facturaId)
        .orElseThrow(() -> new RuntimeException("Factura no encontrada: " + facturaId));

    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document doc = builder.newDocument();

      String namespace = "https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/facturaElectronica";
      Element root = crearElementoRaiz(doc, factura, namespace);
      doc.appendChild(root);

      agregarElemento(doc, root, "Clave", factura.getClave());

      String codigoActividad = factura.getSucursal().getEmpresa()
          .getActividades().stream()
          .filter(EmpresaActividad::getEsPrincipal)
          .findFirst()
          .map(ea -> ea.getActividad().getCodigo())
          .orElseThrow(
              () -> new RuntimeException("No hay actividad económica principal configurada"));
      agregarElemento(doc, root, "CodigoActividad", codigoActividad);

      agregarElemento(doc, root, "NumeroConsecutivo", factura.getConsecutivo());
      agregarElemento(doc, root, "FechaEmision", factura.getFechaEmision().toString());

      // Emisor
      agregarEmisor(doc, root, factura);

      // Proveedor de sistemas (4.4)
      agregarProveedorSistema(doc, root);

      // Receptor (si corresponde)
      if (factura.getCliente() != null
          || factura.getTipoDocumento() == TipoDocumento.FACTURA_ELECTRONICA) {
        agregarReceptor(doc, root, factura);
      }

      agregarElemento(doc, root, "CondicionVenta", factura.getCondicionVenta().getCodigo());

      if (factura.getCondicionVenta() == CondicionVenta.CREDITO
          && factura.getPlazoCredito() != null) {
        agregarElemento(doc, root, "PlazoCredito", factura.getPlazoCredito().toString());
      }

      // Detalles
      agregarDetalles(doc, root, factura);

      // Otros Cargos
      if (!factura.getOtrosCargos().isEmpty()) {
        agregarOtrosCargos(doc, root, factura);
      }

      // Resumen
      agregarResumen(doc, root, factura);

      return documentToString(doc);

    } catch (Exception e) {
      log.error("Error generando XML: {}", e.getMessage(), e);
      throw new RuntimeException("Error generando XML", e);
    }
  }

  private Element crearElementoRaiz(Document doc, Factura factura,
      String /*ignored*/ namespaceArg) {
    String rootName;
    String ns;

    switch (factura.getTipoDocumento()) {
      case FACTURA_ELECTRONICA -> {
        rootName = "FacturaElectronica";
        ns = "https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/facturaElectronica";
      }
      case TIQUETE_ELECTRONICO -> {
        rootName = "TiqueteElectronico";
        ns = "https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/tiqueteElectronico";
      }
      case NOTA_CREDITO -> {
        rootName = "NotaCreditoElectronica";
        ns = "https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/notaCreditoElectronica";
      }
      case NOTA_DEBITO -> {
        rootName = "NotaDebitoElectronica";
        ns = "https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/notaDebitoElectronica";
      }
      case FACTURA_COMPRA -> {
        rootName = "FacturaElectronicaCompra";
        ns = "https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/facturaElectronicaCompra";
      }
      case FACTURA_EXPORTACION -> {
        rootName = "FacturaElectronicaExportacion";
        ns = "https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/facturaElectronicaExportacion";
      }
      case RECIBO_PAGO -> {
        rootName = "ReciboElectronicoPago";
        ns = "https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/reciboElectronicoPago";
      }

      // No usados en DGT - fallbacks
      case TIQUETE_INTERNO -> {
        rootName = "TiqueteElectronico";
        ns = "https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/tiqueteElectronico";
        log.warn(
            "TipoDocumento TIQUETE_INTERNO no soportado en DGT; usando TiqueteElectronico como fallback.");
      }
      case FACTURA_INTERNA, PROFORMA, ORDEN_PEDIDO -> {
        rootName = "FacturaElectronica";
        ns = "https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/facturaElectronica";
        log.warn("TipoDocumento {} no soportado en DGT; usando FacturaElectronica como fallback.",
            factura.getTipoDocumento());
      }
      default -> throw new IllegalArgumentException(
          "Tipo documento no soportado: " + factura.getTipoDocumento());
    }

    Element root = doc.createElementNS(ns, rootName);
    root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
    root.setAttribute("xmlns:xsd", "http://www.w3.org/2001/XMLSchema");
    return root;
  }

  private void agregarEmisor(Document doc, Element root, Factura factura) {
    Element emisor = doc.createElement("Emisor");
    root.appendChild(emisor);

    Empresa empresa = factura.getSucursal().getEmpresa();

    agregarElemento(doc, emisor, "Nombre", empresa.getNombreComercial());

    Element identificacion = doc.createElement("Identificacion");
    emisor.appendChild(identificacion);
    agregarElemento(doc, identificacion, "Tipo", empresa.getTipoIdentificacion().getCodigo());
    agregarElemento(doc, identificacion, "Numero", empresa.getIdentificacion());

    // Ubicación OBLIGATORIA en 4.4
    Element ubicacion = doc.createElement("Ubicacion");
    int added = 0;
    if (empresa.getProvincia() != null) {
      agregarElemento(doc, ubicacion, "Provincia",
          String.valueOf(empresa.getProvincia().getCodigo()));
      added++;
    }
    if (empresa.getCanton() != null) {
      agregarElemento(doc, ubicacion, "Canton",
          String.format("%02d", empresa.getCanton().getCodigo()));
      added++;
    }
    if (empresa.getDistrito() != null) {
      agregarElemento(doc, ubicacion, "Distrito",
          String.format("%02d", empresa.getDistrito().getCodigo()));
      added++;
    }
    if (empresa.getBarrio() != null) {
      agregarElemento(doc, ubicacion, "Barrio",
          String.format("%02d", empresa.getBarrio().getCodigo()));
    }
    if (empresa.getOtrasSenas() != null && !empresa.getOtrasSenas().isBlank()) {
      agregarElemento(doc, ubicacion, "OtrasSenas", empresa.getOtrasSenas());
      added++;
    }

    if (added < 4) {
      throw new RuntimeException(
          "Emisor/Ubicación incompleta: Provincia, Cantón, Distrito y OtrasSenas son obligatorios en 4.4");
    }
    emisor.appendChild(ubicacion);

    if (empresa.getTelefono() != null && !empresa.getTelefono().isBlank()) {
      Element tel = doc.createElement("Telefono");
      emisor.appendChild(tel);
      agregarElemento(doc, tel, "CodigoPais", "506");
      agregarElemento(doc, tel, "NumTelefono", empresa.getTelefono().replaceAll("[^0-9]", ""));
    }

    if (empresa.getEmail() != null && !empresa.getEmail().isBlank()) {
      agregarElemento(doc, emisor, "CorreoElectronico", empresa.getEmail());
    }
  }

  private void agregarReceptor(Document doc, Element root, Factura factura) {
    if (factura.getTipoDocumento() == TipoDocumento.TIQUETE_ELECTRONICO
        && factura.getCliente() == null) {
      return;
    }
    Cliente cliente = factura.getCliente();
    if (cliente == null) {
      return;
    }

    Element receptor = doc.createElement("Receptor");
    root.appendChild(receptor);

    if (cliente.getRazonSocial() != null && !cliente.getRazonSocial().isBlank()) {
      agregarElemento(doc, receptor, "Nombre", cliente.getRazonSocial());
    }

    if (cliente.getTipoIdentificacion() != null && cliente.getNumeroIdentificacion() != null) {
      Element identificacion = doc.createElement("Identificacion");
      receptor.appendChild(identificacion);
      agregarElemento(doc, identificacion, "Tipo", cliente.getTipoIdentificacion().getCodigo());
      agregarElemento(doc, identificacion, "Numero", cliente.getNumeroIdentificacion());
    }

    if (cliente.getEmails() != null && !cliente.getEmails().isBlank()) {
      agregarElemento(doc, receptor, "CorreoElectronico", cliente.getEmails());
    }
  }

  private void agregarProveedorSistema(Document doc, Element root) {
    Element proveedor = doc.createElement("Proveedor");
    root.appendChild(proveedor);

    ProveedorSistema ps = ProveedorSistema.SNN_SOLUCIONES;
    agregarElemento(doc, proveedor, "Identificacion", ps.getIdentificacion());
    agregarElemento(doc, proveedor, "Nombre", ps.getNombre());
    agregarElemento(doc, proveedor, "CorreoElectronico", ps.getEmail());
  }

  private void agregarDetalles(Document doc, Element root, Factura factura) {
    Element detalleServicio = doc.createElement("DetalleServicio");
    root.appendChild(detalleServicio);

    int numeroLinea = 1;
    for (FacturaDetalle detalle : factura.getDetalles()) {
      Element lineaDetalle = doc.createElement("LineaDetalle");
      detalleServicio.appendChild(lineaDetalle);

      agregarElemento(doc, lineaDetalle, "NumeroLinea", String.valueOf(numeroLinea++));
      Element codigo = doc.createElement("Codigo");
      lineaDetalle.appendChild(codigo);
      agregarElemento(doc, codigo, "Tipo", "04");
      agregarElemento(doc, codigo, "Codigo", detalle.getProducto().getCodigoInterno());

      agregarElemento(doc, lineaDetalle, "Cantidad", fmtCantidad(detalle.getCantidad()));
      agregarElemento(doc, lineaDetalle, "UnidadMedida",
          detalle.getProducto().getUnidadMedida().getCodigo());
      agregarElemento(doc, lineaDetalle, "Detalle", detalle.getProducto().getNombre());
      agregarElemento(doc, lineaDetalle, "PrecioUnitario", fmtMonto(detalle.getPrecioUnitario()));
      agregarElemento(doc, lineaDetalle, "MontoTotal", fmtMonto(detalle.getMontoTotal()));
      agregarElemento(doc, lineaDetalle, "SubTotal", fmtMonto(detalle.getSubtotal()));

      if (!detalle.getImpuestos().isEmpty()) {
        for (FacturaDetalleImpuesto imp : detalle.getImpuestos()) {
          Element impuestoElement = doc.createElement("Impuesto");
          lineaDetalle.appendChild(impuestoElement);

          agregarElemento(doc, impuestoElement, "Codigo", imp.getCodigoImpuesto());
          agregarElemento(doc, impuestoElement, "CodigoTarifaIVA", imp.getCodigoTarifaIVA());

          // Tarifa SIEMPRE en porcentaje (13.00, 2.00, 0.50...)
          BigDecimal tarifaPct = asPercent(imp.getTarifa());
          agregarElemento(doc, impuestoElement, "Tarifa", fmtPorcentaje(tarifaPct));

          agregarElemento(doc, impuestoElement, "Monto", fmtMonto(imp.getMontoImpuesto()));

          if (Boolean.TRUE.equals(imp.getTieneExoneracion())) {
            // TarifaExonerada = puntos de tarifa (ej.: 13, 9, 0.5)
            BigDecimal tarifaExon =
                imp.getTarifaExonerada() != null ? imp.getTarifaExonerada() : BigDecimal.ZERO;

            // Base imponible (usa tu campo si lo tienes; aquí SubTotal)
            BigDecimal base = detalle.getSubtotal();

            // Monto exonerado = base * (TarifaExonerada/100), limitado al impuesto bruto
            BigDecimal montoExonCalc = base.multiply(tarifaExon)
                .divide(new BigDecimal("100"), 5, RoundingMode.HALF_UP);

            BigDecimal montoImpuestoBruto =
                imp.getMontoImpuesto() != null ? imp.getMontoImpuesto() : BigDecimal.ZERO;
            BigDecimal montoExoneracion = montoExonCalc.min(montoImpuestoBruto);

            // TipoDocumentoEX1 = 04 (FRANQUICIA) según tu enum
            String tipoEx1 = TipoDocumentoExoneracion.FRANQUICIA.getCodigo();
            String fechaEX = imp.getFechaEmisionExoneracion(); // ideal: RFC3339 "YYYY-MM-DDThh:mm:ss-06:00"

            agregarExoneracionEnImpuesto(
                doc,
                impuestoElement,
                tipoEx1,                                 // TipoDocumentoEX1 (código)
                imp.getTipoDocumentoExoneracionOTRO(),   // si usas 99
                imp.getNumeroDocumentoExoneracion(),
                imp.getNombreInstitucion(),
                imp.getNombreInstitucionOtros(),
                fechaEX,
                fmtPorcentaje(tarifaExon),               // TarifaExonerada (2 dec)
                fmtMonto(montoExoneracion),              // MontoExoneracion (5 dec)
                imp.getArticuloExoneracion(),
                imp.getIncisoExoneracion()
            );
          }
        }
      }

      agregarElemento(doc, lineaDetalle, "MontoTotalLinea", fmtMonto(detalle.getMontoTotalLinea()));
    }
  }

  private void agregarOtrosCargos(Document doc, Element root, Factura factura) {
    for (OtroCargo cargo : factura.getOtrosCargos()) {
      Element otros = doc.createElement("OtrosCargos");
      root.appendChild(otros);

      // Si tienes el código de catálogo de otros cargos, inclúyelo:
      if (cargo.getTipoDocumentoOTROS() != null) {
        agregarElemento(doc, otros, "TipoDocumento", cargo.getTipoDocumentoOTROS()); // p.ej. "06"
      }
      agregarElemento(doc, otros, "Detalle", cargo.getNombreCargo());

      if (cargo.getPorcentaje() != null) {
        // Si guardas fracción (0.10), conviértela a 10.00
        BigDecimal pct = asPercent(cargo.getPorcentaje());
        agregarElemento(doc, otros, "Porcentaje", fmtPorcentaje(pct));
      }

      agregarElemento(doc, otros, "MontoCargo", fmtMonto(cargo.getMontoCargo()));
    }
  }

  private void agregarResumen(Document doc, Element root, Factura factura) {
    Element resumen = doc.createElement("ResumenFactura");
    root.appendChild(resumen);

    Element moneda = doc.createElement("CodigoTipoMoneda");
    resumen.appendChild(moneda);
    agregarElemento(doc, moneda, "CodigoMoneda", factura.getMoneda().getCodigo());
    agregarElemento(doc, moneda, "TipoCambio", "1");

    agregarElemento(doc, resumen, "TotalServGravados",
        fmtMonto(factura.getTotalServiciosGravados()));
    agregarElemento(doc, resumen, "TotalServExentos", fmtMonto(factura.getTotalServiciosExentos()));
    agregarElemento(doc, resumen, "TotalMercanciasGravadas",
        fmtMonto(factura.getTotalMercanciasGravadas()));
    agregarElemento(doc, resumen, "TotalMercanciasExentas",
        fmtMonto(factura.getTotalMercanciasExentas()));
    agregarElemento(doc, resumen, "TotalGravado", fmtMonto(factura.getTotalGravado()));
    agregarElemento(doc, resumen, "TotalExento", fmtMonto(factura.getTotalExento()));
    agregarElemento(doc, resumen, "TotalExonerado", fmtMonto(factura.getTotalExonerado()));
    agregarElemento(doc, resumen, "TotalVenta", fmtMonto(factura.getTotalVenta()));
    agregarElemento(doc, resumen, "TotalDescuentos", fmtMonto(factura.getTotalDescuentos()));
    agregarElemento(doc, resumen, "TotalVentaNeta", fmtMonto(factura.getTotalVentaNeta()));
    agregarElemento(doc, resumen, "TotalImpuesto", fmtMonto(factura.getTotalImpuesto()));

    if (!factura.getOtrosCargos().isEmpty()) {
      BigDecimal totalOtros = factura.getOtrosCargos().stream()
          .map(OtroCargo::getMontoCargo)
          .reduce(BigDecimal.ZERO, BigDecimal::add);
      agregarElemento(doc, resumen, "TotalOtrosCargos", fmtMonto(totalOtros));
    }

    // Desglose por impuesto (tu entidad)
    agregarDesgloseImpuestos(doc, resumen, factura.getResumenImpuestos());

    agregarElemento(doc, resumen, "TotalComprobante", fmtMonto(factura.getTotalComprobante()));

    // Medios de pago en Resumen (4.4)
    for (FacturaMedioPago mp : factura.getMediosPago()) {
      Element medio = doc.createElement("MedioPago");
      resumen.appendChild(medio);
      agregarElemento(doc, medio, "TipoMedioPago", mp.getMedioPago().getCodigo());
      if (factura.getMediosPago().size() > 1 && mp.getMonto() != null) {
        agregarElemento(doc, medio, "TotalMedioPago", fmtMonto(mp.getMonto()));
      }
    }
  }

  private void agregarElemento(Document doc, Element parent, String name, String value) {
    if (value != null) {
      Element element = doc.createElement(name);
      element.setTextContent(value);
      parent.appendChild(element);
    }
  }

  private String documentToString(Document doc) throws Exception {
    TransformerFactory tf = TransformerFactory.newInstance();
    Transformer transformer = tf.newTransformer();
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    StringWriter writer = new StringWriter();
    transformer.transform(new DOMSource(doc), new StreamResult(writer));
    return writer.toString();
  }

  private void agregarDesgloseImpuestos(Document doc, Element resumen,
      List<FacturaResumenImpuesto> lista) {
    if (lista == null || lista.isEmpty()) {
      return;
    }

    for (FacturaResumenImpuesto r : lista) {
      Element des = doc.createElement("TotalDesgloseImpuesto");
      resumen.appendChild(des);

      agregarElemento(doc, des, "Codigo", r.getCodigoImpuesto());
      if (r.getCodigoTarifaIVA() != null) {
        agregarElemento(doc, des, "CodigoTarifaIVA", r.getCodigoTarifaIVA());
      }

      if (r.getTotalBaseImponible() != null) {
        agregarElemento(doc, des, "TotalBaseImponible", fmtMonto(r.getTotalBaseImponible()));
      }
      agregarElemento(doc, des, "TotalMontoImpuesto", fmtMonto(r.getTotalMontoImpuesto()));
      agregarElemento(doc, des, "TotalMontoExoneracion",
          fmtMonto(r.getTotalMontoExoneracion() == null ? BigDecimal.ZERO
              : r.getTotalMontoExoneracion()));
      agregarElemento(doc, des, "TotalImpuestoNeto", fmtMonto(r.getTotalImpuestoNeto()));
      agregarElemento(doc, des, "CantidadLineas", String.valueOf(r.getCantidadLineas()));
    }
  }

  private void agregarExoneracionEnImpuesto(
      Document doc, Element impuestoElement,
      String tipoDocumentoEX1, String tipoDocumentoOTRO,
      String numeroDocumento, String nombreInstitucion, String nombreInstitucionOtros,
      String fechaEmisionRFC3339, String tarifaExonerada, String montoExoneracion,
      String articulo, String inciso
  ) {
    Element ex = doc.createElement("Exoneracion");
    impuestoElement.appendChild(ex);

    agregarElemento(doc, ex, "TipoDocumentoEX1", tipoDocumentoEX1);
    if (tipoDocumentoOTRO != null) {
      agregarElemento(doc, ex, "TipoDocumentoOTRO", tipoDocumentoOTRO);
    }

    agregarElemento(doc, ex, "NumeroDocumento", numeroDocumento);

    if (nombreInstitucion != null) {
      agregarElemento(doc, ex, "NombreInstitucion", nombreInstitucion);
    } else if (nombreInstitucionOtros != null) {
      agregarElemento(doc, ex, "NombreInstitucionOtros", nombreInstitucionOtros);
    }

    agregarElemento(doc, ex, "FechaEmisionEX", fechaEmisionRFC3339);
    agregarElemento(doc, ex, "TarifaExonerada", tarifaExonerada);     // puntos de tarifa, 2 dec
    agregarElemento(doc, ex, "MontoExoneracion", montoExoneracion);   // 5 dec

    if (articulo != null) {
      agregarElemento(doc, ex, "Articulo", articulo);
    }
    if (inciso != null) {
      agregarElemento(doc, ex, "Inciso", inciso);
    }
  }
}