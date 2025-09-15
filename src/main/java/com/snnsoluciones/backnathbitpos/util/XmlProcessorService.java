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

      detalle.setNumeroLinea(parseInt(getTextContent(linea, "NumeroLinea"), i + 1));

      // Código
      NodeList codigos = linea.getElementsByTagName("Codigo");
      if (codigos.getLength() > 0) {
        Element codigo = (Element) codigos.item(0);
        detalle.setCodigo(getTextContent(codigo, "Codigo"));
      }

      // Código CABYS
      NodeList codigosCabys = linea.getElementsByTagName("CodigoComercial");
      if (codigosCabys.getLength() > 0) {
        Element codigoCabys = (Element) codigosCabys.item(0);
        detalle.setCodigoCabys(getTextContent(codigoCabys, "Codigo"));
      }

      detalle.setCantidad(parseBigDecimal(getTextContent(linea, "Cantidad"), BigDecimal.ONE));
      detalle.setUnidadMedida(getTextContent(linea, "UnidadMedida"));
      detalle.setDetalle(getTextContent(linea, "Detalle"));
      detalle.setPrecioUnitario(
          parseBigDecimal(getTextContent(linea, "PrecioUnitario"), BigDecimal.ZERO));
      detalle.setMontoTotal(
          parseBigDecimal(getTextContent(linea, "MontoTotal"), BigDecimal.ZERO));

      // Descuento
      Element descuento = (Element) linea.getElementsByTagName("Descuento").item(0);
      if (descuento != null) {
        detalle.setMontoDescuento(
            parseBigDecimal(getTextContent(descuento, "MontoDescuento"), BigDecimal.ZERO));
        detalle.setNaturalezaDescuento(getTextContent(descuento, "NaturalezaDescuento"));
      } else {
        detalle.setMontoDescuento(BigDecimal.ZERO);
      }

      detalle.setSubTotal(parseBigDecimal(getTextContent(linea, "SubTotal"), BigDecimal.ZERO));

      // Impuesto
      Element impuesto = (Element) linea.getElementsByTagName("Impuesto").item(0);
      if (impuesto != null) {
        // Crear lista de impuestos
        List<FacturaXmlDto.ImpuestoDto> impuestos = new ArrayList<>();
        FacturaXmlDto.ImpuestoDto impuestoDto = new FacturaXmlDto.ImpuestoDto();

        impuestoDto.setCodigo(getTextContent(impuesto, "Codigo"));
        impuestoDto.setCodigoTarifa(getTextContent(impuesto, "CodigoTarifa"));
        impuestoDto.setTarifa(parseBigDecimal(getTextContent(impuesto, "Tarifa"), BigDecimal.ZERO));
        impuestoDto.setMonto(parseBigDecimal(getTextContent(impuesto, "Monto"), BigDecimal.ZERO));

        impuestos.add(impuestoDto);
        detalle.setImpuestos(impuestos);

        // Exoneración
        Element exoneracion = (Element) impuesto.getElementsByTagName("Exoneracion").item(0);
        if (exoneracion != null) {
          FacturaXmlDto.ExoneracionDto exoneracionDto = new FacturaXmlDto.ExoneracionDto();
          exoneracionDto.setTipoDocumento(getTextContent(exoneracion, "TipoDocumento"));
          exoneracionDto.setNumeroDocumento(getTextContent(exoneracion, "NumeroDocumento"));
          exoneracionDto.setNombreInstitucion(getTextContent(exoneracion, "NombreInstitucion"));

          String fechaExoneracion = getTextContent(exoneracion, "FechaEmision");
          if (fechaExoneracion != null) {
            exoneracionDto.setFechaEmision(
                LocalDateTime.parse(fechaExoneracion, DateTimeFormatter.ISO_DATE_TIME));
          }

          exoneracionDto.setPorcentajeExoneracion(
              parseBigDecimal(getTextContent(exoneracion, "PorcentajeExoneracion"), BigDecimal.ZERO));
          exoneracionDto.setMontoExoneracion(
              parseBigDecimal(getTextContent(exoneracion, "MontoExoneracion"), BigDecimal.ZERO));

          detalle.setExoneracion(exoneracionDto);
        }
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
      // Crear ResumenFacturaDto
      FacturaXmlDto.ResumenFacturaDto resumenFactura = new FacturaXmlDto.ResumenFacturaDto();

      // Códigos de moneda y tipo de cambio
      Element codigoTipoMoneda = (Element) resumen.getElementsByTagName("CodigoTipoMoneda").item(0);
      if (codigoTipoMoneda != null) {
        factura.setCodigoMoneda(getTextContent(codigoTipoMoneda, "CodigoMoneda"));
        factura.setTipoCambio(
            parseBigDecimal(getTextContent(codigoTipoMoneda, "TipoCambio"), BigDecimal.ONE));
      }

      // Llenar todos los totales en ResumenFacturaDto
      resumenFactura.setTotalServiciosGravados(
          parseBigDecimal(getTextContent(resumen, "TotalServGravados"), BigDecimal.ZERO));
      resumenFactura.setTotalServiciosExentos(
          parseBigDecimal(getTextContent(resumen, "TotalServExentos"), BigDecimal.ZERO));
      resumenFactura.setTotalServiciosExonerados(
          parseBigDecimal(getTextContent(resumen, "TotalServExonerado"), BigDecimal.ZERO));
      resumenFactura.setTotalMercanciasGravadas(
          parseBigDecimal(getTextContent(resumen, "TotalMercGravadas"), BigDecimal.ZERO));
      resumenFactura.setTotalMercanciasExentas(
          parseBigDecimal(getTextContent(resumen, "TotalMercExentas"), BigDecimal.ZERO));
      resumenFactura.setTotalMercanciasExoneradas(
          parseBigDecimal(getTextContent(resumen, "TotalMercExonerada"), BigDecimal.ZERO));
      resumenFactura.setTotalGravado(
          parseBigDecimal(getTextContent(resumen, "TotalGravado"), BigDecimal.ZERO));
      resumenFactura.setTotalExento(
          parseBigDecimal(getTextContent(resumen, "TotalExento"), BigDecimal.ZERO));
      resumenFactura.setTotalExonerado(
          parseBigDecimal(getTextContent(resumen, "TotalExonerado"), BigDecimal.ZERO));
      resumenFactura.setTotalVenta(
          parseBigDecimal(getTextContent(resumen, "TotalVenta"), BigDecimal.ZERO));
      resumenFactura.setTotalDescuentos(
          parseBigDecimal(getTextContent(resumen, "TotalDescuentos"), BigDecimal.ZERO));
      resumenFactura.setTotalVentaNeta(
          parseBigDecimal(getTextContent(resumen, "TotalVentaNeta"), BigDecimal.ZERO));
      resumenFactura.setTotalImpuesto(
          parseBigDecimal(getTextContent(resumen, "TotalImpuesto"), BigDecimal.ZERO));
      resumenFactura.setTotalIVADevuelto(
          parseBigDecimal(getTextContent(resumen, "TotalIVADevuelto"), BigDecimal.ZERO));
      resumenFactura.setTotalOtrosCargos(
          parseBigDecimal(getTextContent(resumen, "TotalOtrosCargos"), BigDecimal.ZERO));
      resumenFactura.setTotalComprobante(
          parseBigDecimal(getTextContent(resumen, "TotalComprobante"), BigDecimal.ZERO));

      // Asignar el resumen
      factura.setResumenFactura(resumenFactura);

      // También llenar los campos antiguos para compatibilidad
      factura.setTotalGravado(resumenFactura.getTotalGravado());
      factura.setTotalExento(resumenFactura.getTotalExento());
      factura.setTotalExonerado(resumenFactura.getTotalExonerado());
      factura.setTotalVenta(resumenFactura.getTotalVenta());
      factura.setTotalDescuentos(resumenFactura.getTotalDescuentos());
      factura.setTotalVentaNeta(resumenFactura.getTotalVentaNeta());
      factura.setTotalImpuesto(resumenFactura.getTotalImpuesto());
      factura.setTotalOtrosCargos(resumenFactura.getTotalOtrosCargos());
      factura.setTotalComprobante(resumenFactura.getTotalComprobante());
    }

    // Guardar XML original
    factura.setXmlOriginal(xmlDecodificado);

    return factura;
  }

  private boolean esBase64(String str) {
    try {
      Base64.getDecoder().decode(str);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
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

  private BigDecimal parseBigDecimal(String value, BigDecimal defaultValue) {
    if (value == null || value.trim().isEmpty()) {
      return defaultValue;
    }
    try {
      return new BigDecimal(value.trim());
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  private Integer parseInt(String value, Integer defaultValue) {
    if (value == null || value.trim().isEmpty()) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }
}