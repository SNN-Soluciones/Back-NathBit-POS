package com.snnsoluciones.backnathbitpos.scheduler;

import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.factura.EstadoFacturaRecepcion;
import com.snnsoluciones.backnathbitpos.enums.mh.*;
import com.snnsoluciones.backnathbitpos.service.EmailService;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FacturaRecepcionXMLParserService {

    private final EmailService emailService;

    /**
     * Parsear XML de factura recibida
     */
    public FacturaRecepcion parsearXML(
            String xmlContent,
            Empresa empresa,
            Sucursal sucursal) {

        try {
            log.info("Iniciando parseo de XML para empresa: {}, sucursal: {}", 
                empresa.getId(), sucursal.getId());

            // 1. Parsear XML a Document
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlContent)));
            doc.getDocumentElement().normalize();

            // 2. Validar contra XSD (opcional pero recomendado)
            try {
                validarContraXSD(doc, xmlContent);
            } catch (Exception e) {
                log.warn("Advertencia en validación XSD: {}", e.getMessage());
                // Continuamos aunque falle validación
            }

            // 3. Crear entidad principal
            FacturaRecepcion factura = new FacturaRecepcion();
            factura.setEmpresa(empresa);
            factura.setSucursal(sucursal);
            factura.setEstadoInterno(EstadoFacturaRecepcion.PENDIENTE_DECISION);

            // 4. Parsear elementos del XML
            Element root = doc.getDocumentElement();
            String rootName = root.getLocalName();

            // Determinar tipo de documento por el root element
            TipoDocumento tipoDoc = determinarTipoDocumento(rootName);
            factura.setTipoDocumento(tipoDoc);

            // Elementos comunes
            factura.setClave(getElementValue(root, "Clave"));
            factura.setNumeroConsecutivo(getElementValue(root, "NumeroConsecutivo"));
            factura.setCodigoActividad(getElementValue(root, "CodigoActividadEmisor"));
            factura.setFechaEmision(parseDateTime(getElementValue(root, "FechaEmision")));

            // Emisor (proveedor)
            parsearEmisor(root, factura);

            // Receptor (nosotros)
            parsearReceptor(root, factura);

            // Condición de venta
            String condVenta = getElementValue(root, "CondicionVenta");
            if (condVenta != null) {
                factura.setCondicionVenta(condVenta);
            }

            String plazo = getElementValue(root, "PlazoCredito");
            if (plazo != null) {
                factura.setPlazoCredito(Integer.parseInt(plazo));
            }

            // Detalles de líneas
            parsearDetalles(root, factura);

            // Otros cargos
            parsearOtrosCargos(root, factura);

            // Resumen
            parsearResumen(root, factura);

            // Medios de pago
            parsearMediosPago(root, factura);

            // Referencias (para NC/ND)
            if (tipoDoc == TipoDocumento.NOTA_CREDITO || tipoDoc == TipoDocumento.NOTA_DEBITO) {
                parsearReferencias(root, factura);
            }

            log.info("XML parseado exitosamente. Clave: {}", factura.getClave());
            return factura;

        } catch (Exception e) {
            log.error("Error parseando XML: {}", e.getMessage(), e);
            
            // Enviar email de error
            enviarEmailError(empresa, xmlContent, e);
            
            throw new RuntimeException("Error al parsear XML: " + e.getMessage(), e);
        }
    }

    /**
     * Determinar tipo de documento por el nombre del root element
     */
    private TipoDocumento determinarTipoDocumento(String rootName) {
        return switch (rootName) {
            case "FacturaElectronica" -> TipoDocumento.FACTURA_ELECTRONICA;
            case "NotaCreditoElectronica" -> TipoDocumento.NOTA_CREDITO;
            case "NotaDebitoElectronica" -> TipoDocumento.NOTA_DEBITO;
            case "TiqueteElectronico" -> TipoDocumento.TIQUETE_ELECTRONICO;
            case "FacturaElectronicaCompra" -> TipoDocumento.FACTURA_COMPRA;
            case "FacturaElectronicaExportacion" -> TipoDocumento.FACTURA_EXPORTACION;
            default -> throw new IllegalArgumentException("Tipo de documento no reconocido: " + rootName);
        };
    }

    /**
     * Parsear datos del emisor (proveedor)
     */
    private void parsearEmisor(Element root, FacturaRecepcion factura) {
        Element emisor = getFirstElement(root, "Emisor");
        if (emisor == null) return;

        factura.setProveedorNombre(getElementValue(emisor, "Nombre"));
        factura.setProveedorNombreComercial(getElementValue(emisor, "NombreComercial"));

        Element identificacion = getFirstElement(emisor, "Identificacion");
        if (identificacion != null) {
            String tipo = getElementValue(identificacion, "Tipo");
            String numero = getElementValue(identificacion, "Numero");
            
            if (tipo != null) {
                factura.setProveedorTipoIdentificacion(TipoIdentificacion.porCodigo(tipo));
            }
            factura.setProveedorIdentificacion(numero);
        }

        // Ubicación
        Element ubicacion = getFirstElement(emisor, "Ubicacion");
        if (ubicacion != null) {
            factura.setProveedorProvincia(getElementValue(ubicacion, "Provincia"));
            factura.setProveedorCanton(getElementValue(ubicacion, "Canton"));
            factura.setProveedorDistrito(getElementValue(ubicacion, "Distrito"));
            factura.setProveedorBarrio(getElementValue(ubicacion, "Barrio"));
            factura.setProveedorOtrasSenas(getElementValue(ubicacion, "OtrasSenas"));
        }

        // Teléfono
        Element telefono = getFirstElement(emisor, "Telefono");
        if (telefono != null) {
            String codigoPais = getElementValue(telefono, "CodigoPais");
            String numTel = getElementValue(telefono, "NumTelefono");
            factura.setProveedorTelefono(codigoPais != null && numTel != null 
                ? codigoPais + numTel : numTel);
        }

        factura.setProveedorCorreo(getElementValue(emisor, "CorreoElectronico"));
    }

    /**
     * Parsear datos del receptor (nosotros)
     */
    private void parsearReceptor(Element root, FacturaRecepcion factura) {
        Element receptor = getFirstElement(root, "Receptor");
        if (receptor == null) return;

        factura.setReceptorNombre(getElementValue(receptor, "Nombre"));
        factura.setReceptorNombreComercial(getElementValue(receptor, "NombreComercial"));

        Element identificacion = getFirstElement(receptor, "Identificacion");
        if (identificacion != null) {
            String tipo = getElementValue(identificacion, "Tipo");
            String numero = getElementValue(identificacion, "Numero");
            
            if (tipo != null) {
                factura.setReceptorTipoIdentificacion(TipoIdentificacion.porCodigo(tipo));
            }
            factura.setReceptorIdentificacion(numero);
        }

        factura.setReceptorCorreo(getElementValue(receptor, "CorreoElectronico"));
    }

    private void parsearDetalles(Element root, FacturaRecepcion factura) {
        Element detalleServicio = getFirstElement(root, "DetalleServicio");
        if (detalleServicio == null) return;

        NodeList lineas = detalleServicio.getElementsByTagName("LineaDetalle");
        List<FacturaRecepcionDetalle> detalles = new ArrayList<>();

        for (int i = 0; i < lineas.getLength(); i++) {
            Element linea = (Element) lineas.item(i);

            // 👇 CORREGIR AQUÍ
            String detalleTexto = getElementValue(linea, "Detalle");

            FacturaRecepcionDetalle detalle = FacturaRecepcionDetalle.builder()
                .facturaRecepcion(factura)
                .numeroLinea(Integer.parseInt(
                    Objects.requireNonNull(getElementValue(linea, "NumeroLinea"))))
                .codigoCabys(getElementValue(linea, "CodigoCABYS"))
                .descripcion(detalleTexto)  // 👈 ESTE es el NOT NULL
                .detalle(detalleTexto)      // 👈 También guardarlo aquí (opcional)
                .cantidad(parseBigDecimal(getElementValue(linea, "Cantidad")))
                .unidadMedida(UnidadMedida.fromCodigo(getElementValue(linea, "UnidadMedida")))
                .unidadMedidaComercial(getElementValue(linea, "UnidadMedidaComercial"))
                .precioUnitario(parseBigDecimal(getElementValue(linea, "PrecioUnitario")))
                .montoTotal(parseBigDecimal(getElementValue(linea, "MontoTotal")))
                .subtotal(parseBigDecimal(getElementValue(linea, "SubTotal")))
                .montoTotalLinea(parseBigDecimal(getElementValue(linea, "MontoTotalLinea")))
                .build();

            // Código comercial
            Element codigoComercial = getFirstElement(linea, "CodigoComercial");
            if (codigoComercial != null) {
                detalle.setTipoCodigoComercial(getElementValue(codigoComercial, "Tipo"));
                detalle.setCodigoComercial(getElementValue(codigoComercial, "Codigo"));
            }

            // Descuentos
            parsearDescuentos(linea, detalle);

            // Impuestos
            parsearImpuestosDetalle(linea, detalle);

            detalles.add(detalle);
        }

        factura.setDetalles(detalles);
    }

    /**
     * Parsear descuentos de una línea
     */
    private void parsearDescuentos(Element lineaDetalle, FacturaRecepcionDetalle detalle) {
        NodeList descuentos = lineaDetalle.getElementsByTagName("Descuento");
        List<FacturaRecepcionDescuento> listaDescuentos = new ArrayList<>();

        for (int i = 0; i < descuentos.getLength(); i++) {
            Element desc = (Element) descuentos.item(i);
            
            FacturaRecepcionDescuento descuento = FacturaRecepcionDescuento.builder()
                .facturaRecepcionDetalle(detalle)
                .codigoDescuento(getElementValue(desc, "CodigoDescuento"))
                .naturalezaDescuento(getElementValue(desc, "NaturalezaDescuento"))
                .montoDescuento(parseBigDecimal(getElementValue(desc, "MontoDescuento")))
                .orden(i + 1)
                .build();

            listaDescuentos.add(descuento);
        }

        detalle.setDescuentos(listaDescuentos);
    }

    /**
     * Parsear impuestos de una línea
     */
    private void parsearImpuestosDetalle(Element lineaDetalle, FacturaRecepcionDetalle detalle) {
        NodeList impuestos = lineaDetalle.getElementsByTagName("Impuesto");
        List<FacturaRecepcionDetalleImpuesto> listaImpuestos = new ArrayList<>();

        for (int i = 0; i < impuestos.getLength(); i++) {
            Element imp = (Element) impuestos.item(i);
            
            FacturaRecepcionDetalleImpuesto impuesto = FacturaRecepcionDetalleImpuesto.builder()
                .facturaRecepcionDetalle(detalle)
                .codigoImpuesto(getElementValue(imp, "Codigo"))
                .codigoTarifa(getElementValue(imp, "CodigoTarifaIVA"))
                .tarifa(parseBigDecimal(getElementValue(imp, "Tarifa")))
                .monto(parseBigDecimal(getElementValue(imp, "Monto")))
                .montoExoneracion(parseBigDecimal(getElementValue(imp, "MontoExoneracion")))
                .impuestoNeto(parseBigDecimal(getElementValue(imp, "ImpuestoNeto")))
                .build();

            // Exoneración (si existe)
            Element exoneracion = getFirstElement(imp, "Exoneracion");
            if (exoneracion != null) {
                // Guardar como JSON string
                String exonJson = String.format(
                    "{\"tipoDocumento\":\"%s\",\"numeroDocumento\":\"%s\",\"institucion\":\"%s\",\"fechaEmision\":\"%s\",\"porcentaje\":\"%s\"}",
                    getElementValue(exoneracion, "TipoDocumentoEX1"),
                    getElementValue(exoneracion, "NumeroDocumento"),
                    getElementValue(exoneracion, "NombreInstitucion"),
                    getElementValue(exoneracion, "FechaEmisionEX"),
                    getElementValue(exoneracion, "TarifaExonerada")
                );
                impuesto.setExoneracion(exonJson);
            }

            listaImpuestos.add(impuesto);
        }

        detalle.setImpuestos(listaImpuestos);
    }

    /**
     * Parsear otros cargos
     */
    private void parsearOtrosCargos(Element root, FacturaRecepcion factura) {
        NodeList otrosCargos = root.getElementsByTagName("OtrosCargos");
        List<FacturaRecepcionOtroCargo> listaCargos = new ArrayList<>();

        for (int i = 0; i < otrosCargos.getLength(); i++) {
            Element cargo = (Element) otrosCargos.item(i);
            
            FacturaRecepcionOtroCargo otroCargo = FacturaRecepcionOtroCargo.builder()
                .facturaRecepcion(factura)
                .tipoDocumentoOC(getElementValue(cargo, "TipoDocumento"))
                .terceroNumeroIdentificacion(getElementValue(cargo, "NumeroIdentidadTercero"))
                .terceroNombre(getElementValue(cargo, "NombreTercero"))
                .detalle(getElementValue(cargo, "Detalle"))
                .porcentaje(parseBigDecimal(getElementValue(cargo, "Porcentaje")))
                .montoCargo(parseBigDecimal(getElementValue(cargo, "MontoCargo")))
                .build();

            listaCargos.add(otroCargo);
        }

        factura.setOtrosCargos(listaCargos);
    }

    /**
     * Parsear resumen de factura
     */
    private void parsearResumen(Element root, FacturaRecepcion factura) {
        Element resumen = getFirstElement(root, "ResumenFactura");
        if (resumen == null) return;

        // Moneda
        Element moneda = getFirstElement(resumen, "CodigoTipoMoneda");
        if (moneda != null) {
            String codMoneda = getElementValue(moneda, "CodigoMoneda");
            if (codMoneda != null) {
                factura.setMoneda(Moneda.fromCodigo(codMoneda));
            }
            factura.setTipoCambio(parseBigDecimal(getElementValue(moneda, "TipoCambio")));
        }

        // Totales servicios
        factura.setTotalServGravados(parseBigDecimal(getElementValue(resumen, "TotalServGravados")));
        factura.setTotalServExentos(parseBigDecimal(getElementValue(resumen, "TotalServExentos")));
        factura.setTotalServExonerado(parseBigDecimal(getElementValue(resumen, "TotalServExonerado")));
        factura.setTotalServNoSujeto(parseBigDecimal(getElementValue(resumen, "TotalServNoSujeto")));

        // Totales mercancías
        factura.setTotalMercGravada(parseBigDecimal(getElementValue(resumen, "TotalMercanciasGravadas")));
        factura.setTotalMercExenta(parseBigDecimal(getElementValue(resumen, "TotalMercanciasExentas")));
        factura.setTotalMercExonerada(parseBigDecimal(getElementValue(resumen, "TotalMercExonerada")));
        factura.setTotalMercNoSujeta(parseBigDecimal(getElementValue(resumen, "TotalMercNoSujeta")));

        // Totales generales
        factura.setTotalGravado(parseBigDecimal(getElementValue(resumen, "TotalGravado")));
        factura.setTotalExento(parseBigDecimal(getElementValue(resumen, "TotalExento")));
        factura.setTotalExonerado(parseBigDecimal(getElementValue(resumen, "TotalExonerado")));
        factura.setTotalVenta(parseBigDecimal(getElementValue(resumen, "TotalVenta")));
        factura.setTotalDescuentos(parseBigDecimal(getElementValue(resumen, "TotalDescuentos")));
        factura.setTotalVentaNeta(parseBigDecimal(getElementValue(resumen, "TotalVentaNeta")));
        factura.setTotalImpuesto(parseBigDecimal(getElementValue(resumen, "TotalImpuesto")));
        factura.setTotalIVADevuelto(parseBigDecimal(getElementValue(resumen, "TotalIVADevuelto")));
        factura.setTotalOtrosCargos(parseBigDecimal(getElementValue(resumen, "TotalOtrosCargos")));
        factura.setTotalComprobante(parseBigDecimal(getElementValue(resumen, "TotalComprobante")));
    }

    /**
     * Parsear medios de pago
     */
    private void parsearMediosPago(Element root, FacturaRecepcion factura) {
        Element resumen = getFirstElement(root, "ResumenFactura");
        if (resumen == null) return;

        NodeList mediosPago = resumen.getElementsByTagName("MedioPago");
        List<FacturaRecepcionMedioPago> listaMedios = new ArrayList<>();

        for (int i = 0; i < mediosPago.getLength(); i++) {
            Element medio = (Element) mediosPago.item(i);
            
            FacturaRecepcionMedioPago medioPago = FacturaRecepcionMedioPago.builder()
                .facturaRecepcion(factura)
                .medioPago(MedioPago.fromCodigo(getElementValue(medio, "TipoMedioPago")))
                .medioPagoOtro(getElementValue(medio, "MedioPagoOtros"))
                .monto(parseBigDecimal(getElementValue(medio, "TotalMedioPago")))
                .build();

            listaMedios.add(medioPago);
        }

        factura.setMediosPago(listaMedios);
    }

    /**
     * Parsear referencias (NC/ND)
     */
    private void parsearReferencias(Element root, FacturaRecepcion factura) {
        NodeList referencias = root.getElementsByTagName("InformacionReferencia");
        List<FacturaRecepcionReferencia> listaRefs = new ArrayList<>();

        for (int i = 0; i < referencias.getLength(); i++) {
            Element ref = (Element) referencias.item(i);
            
            FacturaRecepcionReferencia referencia = FacturaRecepcionReferencia.builder()
                .facturaRecepcion(factura)
                .tipoDocReferencia(TipoDocumento.fromCodigo(getElementValue(ref, "TipoDocIR")))
                .numeroDocumentoReferencia(getElementValue(ref, "Numero"))
                .fechaEmisionReferencia(String.valueOf(parseDateTime(getElementValue(ref, "FechaEmisionIR"))))
                .codigoReferencia(getElementValue(ref, "Codigo"))
                .razonReferencia(getElementValue(ref, "Razon"))
                .build();

            listaRefs.add(referencia);
        }

        factura.setReferencias(listaRefs);
    }

    /**
     * Validar XML contra XSD de Hacienda
     */
    private void validarContraXSD(Document doc, String xmlContent) throws Exception {
        // Determinar el XSD según el tipo de documento
        String tipoDoc = doc.getDocumentElement().getLocalName();
        String xsdUrl = getXSDUrl(tipoDoc);

        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = factory.newSchema(new StreamSource(xsdUrl));
        Validator validator = schema.newValidator();
        validator.validate(new StreamSource(new StringReader(xmlContent)));
        
        log.info("XML validado exitosamente contra XSD: {}", xsdUrl);
    }

    /**
     * Obtener URL del XSD según tipo de documento
     */
    private String getXSDUrl(String tipoDocumento) {
        String baseUrl = "https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/";
        return switch (tipoDocumento) {
            case "FacturaElectronica" -> baseUrl + "facturaElectronica_V.4.4.xsd";
            case "NotaCreditoElectronica" -> baseUrl + "NotaCreditoElectronica_V4.4.xsd";
            case "NotaDebitoElectronica" -> baseUrl + "NotaDebitoElectronica_V4.4.xsd";
            case "TiqueteElectronico" -> baseUrl + "TiqueteElectronico_V4.4.xsd";
            default -> baseUrl + "facturaElectronica_V.4.4.xsd";
        };
    }

    /**
     * Enviar email de error
     */
    private void enviarEmailError(Empresa empresa, String xmlContent, Exception error) {
        try {
            String emailDestino = empresa.getEmailNotificacion();
            if (emailDestino == null || emailDestino.isBlank()) {
                log.warn("No hay email de notificaciones configurado para empresa: {}", empresa.getId());
                return;
            }

            String asunto = "Error al procesar factura electrónica";
            String mensaje = String.format("""
                Se produjo un error al procesar una factura electrónica recibida:
                
                Empresa: %s
                Error: %s
                
                Por favor revise el XML adjunto y contacte a soporte si el problema persiste.
                """, 
                empresa.getNombreRazonSocial(),
                error.getMessage()
            );

            emailService.enviarEmailSimple(emailDestino, asunto, mensaje);
            log.info("Email de error enviado a: {}", emailDestino);
            
        } catch (Exception e) {
            log.error("Error enviando email de notificación: {}", e.getMessage());
        }
    }

    // ==================== HELPERS ====================

    private String getElementValue(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            Element element = (Element) nodeList.item(0);
            return element.getTextContent();
        }
        return null;
    }

    private Element getFirstElement(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return (Element) nodeList.item(0);
        }
        return null;
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Error parseando BigDecimal: {}", value);
            return BigDecimal.ZERO;
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            // Parsear formato ISO con zona horaria
            ZonedDateTime zdt = ZonedDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME);
            return zdt.toLocalDateTime();
        } catch (Exception e) {
            log.warn("Error parseando fecha: {}", value);
            return null;
        }
    }
}