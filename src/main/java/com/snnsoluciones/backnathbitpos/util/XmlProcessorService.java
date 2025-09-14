package com.snnsoluciones.backnathbitpos.util;

import com.snnsoluciones.backnathbitpos.dto.compra.FacturaXmlDto;
import java.util.Objects;
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
        Document doc = builder.parse(new ByteArrayInputStream(xmlDecodificado.getBytes(StandardCharsets.UTF_8)));
        
        FacturaXmlDto factura = new FacturaXmlDto();
        
        // Extraer clave
        factura.setClave(getTextContent(doc, "Clave"));
        
        // Extraer número consecutivo
        factura.setNumeroConsecutivo(getTextContent(doc, "NumeroConsecutivo"));
        
        // Extraer fecha de emisión
        String fechaEmisionStr = getTextContent(doc, "FechaEmision");
        if (fechaEmisionStr != null) {
            factura.setFechaEmision(LocalDateTime.parse(fechaEmisionStr, DateTimeFormatter.ISO_DATE_TIME));
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
        if (plazoStr != null) {
            factura.setPlazoCredito(Integer.parseInt(plazoStr));
        }
        
        // Extraer medio de pago
        NodeList mediosPago = doc.getElementsByTagName("MedioPago");
        if (mediosPago.getLength() > 0) {
            factura.setMedioPago(mediosPago.item(0).getTextContent());
        }
        
        // Extraer detalles
        NodeList lineasDetalle = doc.getElementsByTagName("LineaDetalle");
        List<FacturaXmlDto.DetalleDto> detalles = new ArrayList<>();
        
        for (int i = 0; i < lineasDetalle.getLength(); i++) {
            Element linea = (Element) lineasDetalle.item(i);
            FacturaXmlDto.DetalleDto detalle = new FacturaXmlDto.DetalleDto();
            
            detalle.setNumeroLinea(Integer.parseInt(
                Objects.requireNonNull(getTextContent(linea, "NumeroLinea"))));
            detalle.setCodigo(getTextContent(linea, "Codigo"));
            detalle.setCodigoCabys(getTextContent(linea, "CodigoComercial"));
            detalle.setCantidad(new BigDecimal(
                Objects.requireNonNull(getTextContent(linea, "Cantidad"))));
            detalle.setUnidadMedida(getTextContent(linea, "UnidadMedida"));
            detalle.setDetalle(getTextContent(linea, "Detalle"));
            detalle.setPrecioUnitario(new BigDecimal(
                Objects.requireNonNull(getTextContent(linea, "PrecioUnitario"))));
            detalle.setMontoTotal(new BigDecimal(
                Objects.requireNonNull(getTextContent(linea, "MontoTotal"))));
            
            // Descuento
            Element descuento = (Element) linea.getElementsByTagName("Descuento").item(0);
            if (descuento != null) {
                detalle.setMontoDescuento(new BigDecimal(
                    Objects.requireNonNull(getTextContent(descuento, "MontoDescuento"))));
                detalle.setNaturalezaDescuento(getTextContent(descuento, "NaturalezaDescuento"));
            }
            
            detalle.setSubTotal(new BigDecimal(
                Objects.requireNonNull(getTextContent(linea, "SubTotal"))));
            
            // Impuesto
            Element impuesto = (Element) linea.getElementsByTagName("Impuesto").item(0);
            if (impuesto != null) {
                detalle.setCodigoImpuesto(getTextContent(impuesto, "Codigo"));
                detalle.setCodigoTarifa(getTextContent(impuesto, "CodigoTarifa"));
                detalle.setTarifa(new BigDecimal(
                    Objects.requireNonNull(getTextContent(impuesto, "Tarifa"))));
                detalle.setMontoImpuesto(new BigDecimal(
                    Objects.requireNonNull(getTextContent(impuesto, "Monto"))));
                
                // Exoneración
                Element exoneracion = (Element) impuesto.getElementsByTagName("Exoneracion").item(0);
                if (exoneracion != null) {
                    detalle.setTieneExoneracion(true);
                    detalle.setMontoExoneracion(new BigDecimal(
                        Objects.requireNonNull(getTextContent(exoneracion, "MontoExoneracion"))));
                }
            }
            
            detalle.setMontoTotalLinea(new BigDecimal(
                Objects.requireNonNull(getTextContent(linea, "MontoTotalLinea"))));
            
            detalles.add(detalle);
        }
        
        factura.setDetalles(detalles);
        
        // Extraer resumen
        Element resumen = (Element) doc.getElementsByTagName("ResumenFactura").item(0);
        if (resumen != null) {
            // Códigos de moneda y tipo de cambio
            factura.setCodigoMoneda(getTextContent(resumen, "CodigoTipoMoneda", "CodigoMoneda"));
            String tipoCambioStr = getTextContent(resumen, "TipoCambio");
            if (tipoCambioStr != null) {
                factura.setTipoCambio(new BigDecimal(tipoCambioStr));
            }
            
            // Totales por tipo
            String totalServGravados = getTextContent(resumen, "TotalServGravados");
            if (totalServGravados != null) {
                factura.setTotalServiciosGravados(new BigDecimal(totalServGravados));
            }
            
            String totalServExentos = getTextContent(resumen, "TotalServExentos");
            if (totalServExentos != null) {
                factura.setTotalServiciosExentos(new BigDecimal(totalServExentos));
            }
            
            String totalMercGravadas = getTextContent(resumen, "TotalMercanciasGravadas");
            if (totalMercGravadas != null) {
                factura.setTotalMercanciasGravadas(new BigDecimal(totalMercGravadas));
            }
            
            String totalMercExentas = getTextContent(resumen, "TotalMercanciasExentas");
            if (totalMercExentas != null) {
                factura.setTotalMercanciasExentas(new BigDecimal(totalMercExentas));
            }
            
            // Totales generales
            factura.setTotalGravado(new BigDecimal(getTextContent(resumen, "TotalGravado", "0")));
            factura.setTotalExento(new BigDecimal(getTextContent(resumen, "TotalExento", "0")));
            factura.setTotalExonerado(new BigDecimal(getTextContent(resumen, "TotalExonerado", "0")));
            factura.setTotalVenta(new BigDecimal(
                Objects.requireNonNull(getTextContent(resumen, "TotalVenta"))));
            factura.setTotalDescuentos(new BigDecimal(getTextContent(resumen, "TotalDescuentos", "0")));
            factura.setTotalVentaNeta(new BigDecimal(
                Objects.requireNonNull(getTextContent(resumen, "TotalVentaNeta"))));
            factura.setTotalImpuesto(new BigDecimal(getTextContent(resumen, "TotalImpuesto", "0")));
            
            String totalOtrosCargos = getTextContent(resumen, "TotalOtrosCargos");
            if (totalOtrosCargos != null) {
                factura.setTotalOtrosCargos(new BigDecimal(totalOtrosCargos));
            }
            
            factura.setTotalComprobante(new BigDecimal(
                Objects.requireNonNull(getTextContent(resumen, "TotalComprobante"))));
        }
        
        // Guardar XML original
        factura.setXmlOriginal(xmlDecodificado);
        
        return factura;
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