package com.snnsoluciones.backnathbitpos.scheduler;

import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.factura.EstadoFacturaRecepcion;
import com.snnsoluciones.backnathbitpos.enums.mh.*;
import com.snnsoluciones.backnathbitpos.service.EmailService;
import java.io.InputStream;
import java.net.URL;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
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

    private static final ZoneId ZONA_CR = ZoneId.of("America/Costa_Rica");


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

//            // 2. Validar contra XSD (opcional pero recomendado)
//            try {
//                validarContraXSD(doc, xmlContent);
//            } catch (Exception e) {
//                log.warn("Advertencia en validación XSD: {}", e.getMessage());
//                // Continuamos aunque falle validación
//            }

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

        for (int i = 0; i < referencias.getLength(); i++) {
            Element ref = (Element) referencias.item(i);

            // Algunos emisores usan "TipoDocumento" en vez de "TipoDoc"
            String tipoDocTxt = getAny(ref, "TipoDoc", "TipoDocumento", "TipoDocReferencia");

            // Si no viene, asumimos que referencia una FE (ajusta si prefieres otra política)
            com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento tipoRef =
                (tipoDocTxt != null) ? com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento.fromCodigo(tipoDocTxt)
                    : com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento.FACTURA_ELECTRONICA;

            if (tipoRef == null) {
                // Último salvavidas: evita NULL en DB
                log.warn("TipoDoc ausente o no reconocido en InformacionReferencia. Usando FACTURA_ELECTRONICA por defecto.");
                tipoRef = com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento.FACTURA_ELECTRONICA;
            }

            // FechaEmision puede venir vacía: no la fuerces si no está
            String fechaRefTxt = getAny(ref, "FechaEmision", "FechaEmisionReferencia");
            String fechaRefNormalizada = null;
            if (fechaRefTxt != null && !fechaRefTxt.isBlank()) {
                var ldt = parseDateTime(fechaRefTxt); // tu parser flexible
                fechaRefNormalizada = (ldt != null) ? ldt.toString() : null;
            }

            FacturaRecepcionReferencia r = FacturaRecepcionReferencia.builder()
                .tipoDocReferencia(tipoRef)                                 // 🔴 NUNCA NULL
                .numeroDocumentoReferencia(getElementValue(ref, "Numero"))
                .fechaEmisionReferencia(fechaRefNormalizada)                // puede ser null
                .codigoReferencia(getElementValue(ref, "Codigo"))           // 99 en tu caso
                .razonReferencia(getElementValue(ref, "Razon"))
                .build();

            // usa el helper del aggregate para asegurar numero_linea y relación
            factura.addReferencia(r); // ✅ setea numeroLinea = size()+1
        }
    }

    private String getAny(Element parent, String... tagNames) {
        for (String t : tagNames) {
            String v = getElementValue(parent, t);
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }


    /**
     * Validar XML contra XSD de Hacienda
     */
    private void validarContraXSD(Document doc, String xmlContent) throws Exception {
        // Asegúrate de haber parseado el Document con namespaceAware=true
        String tipoDoc = doc.getDocumentElement().getLocalName();

        String xsdUrl = getXSDUrl(tipoDoc);

        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        // 2) Permitir cargar XSD externos por HTTPS
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "https");
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "https");

        // 3) Pasar systemId correcto para que resuelva includes relativos
        try (InputStream in = new URL(xsdUrl).openStream()) {
            StreamSource source = new StreamSource(in, xsdUrl); // nota el segundo argumento (systemId)
            Schema schema = factory.newSchema(source);
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new StringReader(xmlContent)));
        }

        log.info("XML validado exitosamente contra XSD: {}", xsdUrl);
    }

    /** Map correcto de URLs 4.4 (ojo mayúsculas y nombres) */
    private String getXSDUrl(String tipoDocumento) {
        String base = "https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/";
        return switch (tipoDocumento) {
            case "FacturaElectronica"        -> base + "FacturaElectronica_V4.4.xsd";
            case "NotaCreditoElectronica"    -> base + "NotaCreditoElectronica_V4.4.xsd";
            case "NotaDebitoElectronica"     -> base + "NotaDebitoElectronica_V4.4.xsd";
            case "TiqueteElectronico"        -> base + "TiqueteElectronico_V4.4.xsd";
            case "MensajeReceptor"           -> base + "MensajeReceptor_V4.4.xsd"; // útil si validas MR
            // agrega otros que uses: FacturaElectronicaCompra, etc., con el nombre exacto
            default -> base + "FacturaElectronica_V4.4.xsd";
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
        if (value == null || value.isBlank()) return null;

        // 1) Intento con offset/Z (ej: 2025-09-13T14:45:50-06:00 o ...Z)
        try {
            OffsetDateTime odt = OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            // Normaliza a la zona CR
            return odt.atZoneSameInstant(ZONA_CR).toLocalDateTime();
        } catch (DateTimeParseException ignore) {}

        // 2) Sin offset (ej: 2025-09-13T14:45:50 o con fracciones 2025-09-13T14:45:50.123)
        DateTimeFormatter flexSinOffset = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral('T')
            .appendPattern("HH:mm")
            .optionalStart().appendPattern(":ss").optionalEnd()
            .optionalStart().appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true).optionalEnd()
            .toFormatter();

        LocalDateTime ldt = LocalDateTime.parse(value, flexSinOffset);
        // Interpretamos que el emisor emitió en hora local CR (ajusta si prefieres otra política)
        return ldt;
    }
}