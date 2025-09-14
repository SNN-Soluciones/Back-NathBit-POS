package com.snnsoluciones.backnathbitpos.util;

import com.snnsoluciones.backnathbitpos.dto.compra.FacturaXmlDto;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class XmlProcessorService {

  public FacturaXmlDto procesarFacturaXml(String xmlContent) throws Exception {
    // Si viene en Base64, decodificar
    String xmlDecodificado = xmlContent;
    if (esBase64(xmlContent)) {
      xmlDecodificado = new String(Base64.getDecoder().decode(xmlContent), StandardCharsets.UTF_8);
    }

    // Parsear XML
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(
        new ByteArrayInputStream(xmlDecodificado.getBytes(StandardCharsets.UTF_8)));

    FacturaXmlDto factura = new FacturaXmlDto();

    // Extraer clave
    factura.setClave(getTextContent(doc, "Clave"));

    // Extraer número consecutivo
    factura.setNumeroConsecutivo(getTextContent(doc, "NumeroConsecutivo"));

    // Extraer fecha de emisión
    String fechaEmisionStr = getTextContent(doc, "FechaEmision");
    if (fechaEmisionStr != null) {
      factura.setFechaEmision(
          LocalDateTime.parse(fechaEmisionStr, DateTimeFormatter.ISO_DATE_TIME));
    }

    // Extraer datos del emisor
    Element emisor = (Element) doc.getElementsByTagName("Emisor").item(0);
    if (emisor != null) {
      FacturaXmlDto.EmisorDto emisorDto = new FacturaXmlDto.EmisorDto();
      emisorDto.setNombre(getTextContent(emisor, "Nombre"));

      Element identificacion = (Element) emisor.getElementsByTagName("Identificacion").item(0);
      if (identificacion != null) {
        emisorDto.setTipoIdentificacion(getTextContent(identificacion, "Tipo"));
        emisorDto.setNumeroIdentificacion(getTextContent(identificacion, "Numero"));
      }

      emisorDto.setNombreComercial(getTextContent(emisor, "NombreComercial"));
      emisorDto.setCorreoElectronico(getTextContent(emisor, "CorreoElectronico"));

      // Teléfono
      Element telefono = (Element) emisor.getElementsByTagName("Telefono").item(0);
      if (telefono != null) {
        emisorDto.setTelefono(getTextContent(telefono, "NumTelefono"));
      }

      factura.setEmisor(emisorDto);
    }

    // Extraer condición de venta
    factura.setCondicionVenta(getTextContent(doc, "CondicionVenta"));

    // Extraer plazo de crédito
    String plazoStr = getTextContent(doc, "PlazoCredito");
    if (plazoStr != null && !plazoStr.trim().isEmpty()) {
      try {
        factura.setPlazoCredito(Integer.parseInt(plazoStr.trim()));
      } catch (NumberFormatException e) {
        // Si no es un número válido, dejar null
      }
    }

    // Extraer medio de pago
    NodeList mediosPago = doc.getElementsByTagName("MedioPago");
    if (mediosPago.getLength() > 0) {
      Element medioPago = (Element) mediosPago.item(0);
      String tipoMedioPago = getTextContent(medioPago, "TipoMedioPago");
      if (tipoMedioPago != null) {
        factura.setMedioPago(tipoMedioPago);
      }
    }

    // Extraer detalles
    NodeList lineasDetalle = doc.getElementsByTagName("LineaDetalle");
    List<FacturaXmlDto.DetalleDto> detalles = new ArrayList<>();

    for (int i = 0; i < lineasDetalle.getLength(); i++) {
      Element linea = (Element) lineasDetalle.item(i);
      FacturaXmlDto.DetalleDto detalle = new FacturaXmlDto.DetalleDto();

      // NumeroLinea
      String numeroLinea = getTextContent(linea, "NumeroLinea");
      if (numeroLinea != null && !numeroLinea.trim().isEmpty()) {
        detalle.setNumeroLinea(Integer.parseInt(numeroLinea.trim()));
      }

      // Código del producto
      Element codigoComercial = (Element) linea.getElementsByTagName("CodigoComercial").item(0);
      if (codigoComercial != null) {
        detalle.setCodigo(getTextContent(codigoComercial, "Codigo"));
        // El código CABYS está en el tag CodigoCABYS, no en CodigoComercial
      }
      detalle.setCodigoCabys(getTextContent(linea, "CodigoCABYS"));

      // Cantidad
      detalle.setCantidad(parseBigDecimal(getTextContent(linea, "Cantidad"), BigDecimal.ZERO));

      // Unidad de medida
      detalle.setUnidadMedida(getTextContent(linea, "UnidadMedida"));

      // Descripción
      detalle.setDetalle(getTextContent(linea, "Detalle"));

      // Precio unitario
      detalle.setPrecioUnitario(
          parseBigDecimal(getTextContent(linea, "PrecioUnitario"), BigDecimal.ZERO));

      // Monto total
      detalle.setMontoTotal(parseBigDecimal(getTextContent(linea, "MontoTotal"), BigDecimal.ZERO));

      // Descuento
      Element descuento = (Element) linea.getElementsByTagName("Descuento").item(0);
      if (descuento != null) {
        detalle.setMontoDescuento(
            parseBigDecimal(getTextContent(descuento, "MontoDescuento"), BigDecimal.ZERO));
        detalle.setNaturalezaDescuento(getTextContent(descuento, "NaturalezaDescuento"));
      }

      // SubTotal
      detalle.setSubTotal(parseBigDecimal(getTextContent(linea, "SubTotal"), BigDecimal.ZERO));

      // Impuesto
      Element impuesto = (Element) linea.getElementsByTagName("Impuesto").item(0);
      if (impuesto != null) {
        detalle.setCodigoImpuesto(getTextContent(impuesto, "Codigo"));
        detalle.setCodigoTarifa(getTextContent(impuesto, "CodigoTarifaIVA"));
        detalle.setTarifa(parseBigDecimal(getTextContent(impuesto, "Tarifa"), BigDecimal.ZERO));
        detalle.setMontoImpuesto(
            parseBigDecimal(getTextContent(impuesto, "Monto"), BigDecimal.ZERO));

        // Exoneración
        Element exoneracion = (Element) impuesto.getElementsByTagName("Exoneracion").item(0);
        if (exoneracion != null) {
          detalle.setTieneExoneracion(true);
          detalle.setMontoExoneracion(
              parseBigDecimal(getTextContent(exoneracion, "MontoExoneracion"), BigDecimal.ZERO));
        }
      } else {
        // No hay impuesto (producto exento)
        detalle.setMontoImpuesto(BigDecimal.ZERO);
      }

      // MontoTotalLinea
      detalle.setMontoTotalLinea(
          parseBigDecimal(getTextContent(linea, "MontoTotalLinea"), BigDecimal.ZERO));

      detalles.add(detalle);
    }

    factura.setDetalles(detalles);

    // Extraer resumen
    Element resumen = (Element) doc.getElementsByTagName("ResumenFactura").item(0);
    if (resumen != null) {
      // Códigos de moneda y tipo de cambio
      Element codigoTipoMoneda = (Element) resumen.getElementsByTagName("CodigoTipoMoneda").item(0);
      if (codigoTipoMoneda != null) {
        factura.setCodigoMoneda(getTextContent(codigoTipoMoneda, "CodigoMoneda"));
        factura.setTipoCambio(
            parseBigDecimal(getTextContent(codigoTipoMoneda, "TipoCambio"), BigDecimal.ONE));
      }

      // Totales por tipo
      factura.setTotalServiciosGravados(
          parseBigDecimal(getTextContent(resumen, "TotalServGravados"), BigDecimal.ZERO));
      factura.setTotalServiciosExentos(
          parseBigDecimal(getTextContent(resumen, "TotalServExentos"), BigDecimal.ZERO));
      factura.setTotalServiciosExonerados(
          parseBigDecimal(getTextContent(resumen, "TotalServExonerado"), BigDecimal.ZERO));
      factura.setTotalMercanciasGravadas(
          parseBigDecimal(getTextContent(resumen, "TotalMercanciasGravadas"), BigDecimal.ZERO));
      factura.setTotalMercanciasExentas(
          parseBigDecimal(getTextContent(resumen, "TotalMercanciasExentas"), BigDecimal.ZERO));
      factura.setTotalMercanciasExoneradas(
          parseBigDecimal(getTextContent(resumen, "TotalMercExonerada"), BigDecimal.ZERO));

      // Totales generales
      factura.setTotalGravado(
          parseBigDecimal(getTextContent(resumen, "TotalGravado"), BigDecimal.ZERO));
      factura.setTotalExento(
          parseBigDecimal(getTextContent(resumen, "TotalExento"), BigDecimal.ZERO));
      factura.setTotalExonerado(
          parseBigDecimal(getTextContent(resumen, "TotalExonerado"), BigDecimal.ZERO));
      factura.setTotalVenta(
          parseBigDecimal(getTextContent(resumen, "TotalVenta"), BigDecimal.ZERO));
      factura.setTotalDescuentos(
          parseBigDecimal(getTextContent(resumen, "TotalDescuentos"), BigDecimal.ZERO));
      factura.setTotalVentaNeta(
          parseBigDecimal(getTextContent(resumen, "TotalVentaNeta"), BigDecimal.ZERO));
      factura.setTotalImpuesto(
          parseBigDecimal(getTextContent(resumen, "TotalImpuesto"), BigDecimal.ZERO));
      factura.setTotalOtrosCargos(
          parseBigDecimal(getTextContent(resumen, "TotalOtrosCargos"), BigDecimal.ZERO));
      factura.setTotalComprobante(
          parseBigDecimal(getTextContent(resumen, "TotalComprobante"), BigDecimal.ZERO));
    }

    // Guardar XML original
    factura.setXmlOriginal(xmlDecodificado);

    return factura;
  }

  /**
   * Método helper para parsear BigDecimal con manejo de errores
   */
  private BigDecimal parseBigDecimal(String value, BigDecimal defaultValue) {
    if (value == null || value.trim().isEmpty()) {
      return defaultValue;
    }

    try {
      // Limpiar el valor: quitar espacios y reemplazar coma por punto si es necesario
      String cleanValue = value.trim().replace(",", ".");
      return new BigDecimal(cleanValue);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  private String getTextContent(Element parent, String tagName) {
    NodeList nodes = parent.getElementsByTagName(tagName);
    if (nodes.getLength() > 0) {
      return nodes.item(0).getTextContent();
    }
    return null;
  }

  private String getTextContent(Document doc, String tagName) {
    NodeList nodes = doc.getElementsByTagName(tagName);
    if (nodes.getLength() > 0) {
      return nodes.item(0).getTextContent();
    }
    return null;
  }

  private String getTextContent(Element parent, String tagName, String defaultValue) {
    String value = getTextContent(parent, tagName);
    return value != null ? value : defaultValue;
  }

  private boolean esBase64(String str) {
    try {
      Base64.getDecoder().decode(str);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}