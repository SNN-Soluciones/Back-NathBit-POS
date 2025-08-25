package com.snnsoluciones.backnathbitpos.util;

import com.snnsoluciones.backnathbitpos.entity.ActividadEconomica;
import com.snnsoluciones.backnathbitpos.entity.Cliente;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.EmpresaActividad;
import com.snnsoluciones.backnathbitpos.entity.Factura;
import com.snnsoluciones.backnathbitpos.entity.FacturaDetalle;
import com.snnsoluciones.backnathbitpos.entity.FacturaDetalleImpuesto;
import com.snnsoluciones.backnathbitpos.entity.FacturaMedioPago;
import com.snnsoluciones.backnathbitpos.enums.mh.CondicionVenta;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento;
import com.snnsoluciones.backnathbitpos.repository.EmpresaConfigHaciendaRepository;
import com.snnsoluciones.backnathbitpos.repository.FacturaRepository;
import java.io.StringWriter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Service
@Slf4j
@RequiredArgsConstructor
public class FacturaXMLGeneratorService {
    
    private final FacturaRepository facturaRepository;
    private final EmpresaConfigHaciendaRepository configRepository;
    
    /**
     * Genera el XML de la factura según estructura 4.4
     */
    public String generarXML(Long facturaId) {
        log.info("Generando XML para factura ID: {}", facturaId);
        
        Factura factura = facturaRepository.findById(facturaId)
            .orElseThrow(() -> new RuntimeException("Factura no encontrada: " + facturaId));
        
        try {
            // Crear documento XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            
            // Namespace de Hacienda 4.4
            String namespace = "https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/facturaElectronica";
            
            // Elemento raíz según tipo documento
            Element root = crearElementoRaiz(doc, factura, namespace);
            doc.appendChild(root);
            
            // 1. Clave
            agregarElemento(doc, root, "Clave", factura.getClave());
            
            // 2. CodigoActividad
            String codigoActividad = factura.getSucursal().getEmpresa()
                .getActividades().stream()
                .filter(EmpresaActividad::getEsPrincipal)
                .findFirst()
                .map(ea -> ea.getActividad().getCodigo())
                .orElseThrow(() -> new RuntimeException("No hay actividad económica principal configurada"));

            agregarElemento(doc, root, "CodigoActividad", codigoActividad);
            
            agregarElemento(doc, root, "CodigoActividad", codigoActividad);
            
            // 3. NumeroConsecutivo
            agregarElemento(doc, root, "NumeroConsecutivo", factura.getConsecutivo());
            
            // 4. FechaEmision
            agregarElemento(doc, root, "FechaEmision", factura.getFechaEmision());
            
            // 5. Emisor
            agregarEmisor(doc, root, factura);
            
            // 6. Receptor
            agregarReceptor(doc, root, factura);
            
            // 7. CondicionVenta
            agregarElemento(doc, root, "CondicionVenta", 
                factura.getCondicionVenta().getCodigo());
            
            // 8. PlazoCredito (si aplica)
            if (factura.getCondicionVenta() == CondicionVenta.CREDITO &&
                factura.getPlazoCredito() != null) {
                agregarElemento(doc, root, "PlazoCredito", 
                    factura.getPlazoCredito().toString());
            }
            
            // 9. MedioPago
            agregarMediosPago(doc, root, factura);
            
            // 10. DetalleServicio
            agregarDetalles(doc, root, factura);
            
            // 11. ResumenFactura
            agregarResumen(doc, root, factura);
            
            // Convertir a String
            return documentToString(doc);
            
        } catch (Exception e) {
            log.error("Error generando XML: {}", e.getMessage(), e);
            throw new RuntimeException("Error generando XML", e);
        }
    }
    
    private Element crearElementoRaiz(Document doc, Factura factura, String namespace) {
        String rootName = switch (factura.getTipoDocumento()) {
            case FACTURA_ELECTRONICA -> "FacturaElectronica";
            case TIQUETE_ELECTRONICO -> "TiqueteElectronico";
            case NOTA_CREDITO -> "NotaCreditoElectronica";
            case NOTA_DEBITO -> "NotaDebitoElectronica";
            default -> throw new RuntimeException("Tipo documento no soportado");
        };
        
        Element root = doc.createElementNS(namespace, rootName);
        root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        root.setAttribute("xmlns:xsd", "http://www.w3.org/2001/XMLSchema");
        
        return root;
    }
    
    private void agregarEmisor(Document doc, Element root, Factura factura) {
        Element emisor = doc.createElement("Emisor");
        root.appendChild(emisor);
        
        Empresa empresa = factura.getSucursal().getEmpresa();
        
        agregarElemento(doc, emisor, "Nombre", empresa.getNombreComercial());
        
        // Identificación
        Element identificacion = doc.createElement("Identificacion");
        emisor.appendChild(identificacion);
        agregarElemento(doc, identificacion, "Tipo", empresa.getTipoIdentificacion().getCodigo());
        agregarElemento(doc, identificacion, "Numero", empresa.getIdentificacion());
        
        // Ubicación
        agregarUbicacion(doc, emisor, empresa);
        
        // Teléfono
        if (empresa.getTelefono() != null) {
            Element telefono = doc.createElement("Telefono");
            emisor.appendChild(telefono);
            agregarElemento(doc, telefono, "CodigoPais", "506");
            agregarElemento(doc, telefono, "NumTelefono", 
                empresa.getTelefono().replaceAll("[^0-9]", ""));
        }
        
        // Email
        agregarElemento(doc, emisor, "CorreoElectronico", empresa.getEmail());
    }
    
    private void agregarReceptor(Document doc, Element root, Factura factura) {
        Element receptor = doc.createElement("Receptor");
        root.appendChild(receptor);
        
        Cliente cliente = factura.getCliente();
        
        // Si es tiquete y no hay cliente, es consumidor final
        if (cliente == null && factura.getTipoDocumento() == TipoDocumento.TIQUETE_ELECTRONICO) {
            return; // Receptor vacío para consumidor final
        }
        
        agregarElemento(doc, receptor, "Nombre", cliente.getRazonSocial());
        
        // Identificación (si tiene)
        if (cliente.getTipoIdentificacion() != null) {
            Element identificacion = doc.createElement("Identificacion");
            receptor.appendChild(identificacion);
            agregarElemento(doc, identificacion, "Tipo", 
                cliente.getTipoIdentificacion().getCodigo());
            agregarElemento(doc, identificacion, "Numero", 
                cliente.getNumeroIdentificacion());
        }
        
        // Email (si tiene)
        if (cliente.getEmails() != null) {
            agregarElemento(doc, receptor, "CorreoElectronico", cliente.getEmails());
        }
    }
    
    // Métodos auxiliares...
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

    // Agregar estos métodos en FacturaXMLGeneratorService

    private void agregarUbicacion(Document doc, Element parent, Empresa empresa) {
        Element ubicacion = doc.createElement("Ubicacion");
        parent.appendChild(ubicacion);

        // Provincia - debe ser el código de 1 dígito
        if (empresa.getProvincia() != null) {
            agregarElemento(doc, ubicacion, "Provincia",
                String.valueOf(empresa.getProvincia().getCodigo()));
        }

        // Cantón - debe ser de 2 dígitos con ceros a la izquierda
        if (empresa.getCanton() != null) {
            agregarElemento(doc, ubicacion, "Canton",
                String.format("%02d", empresa.getCanton().getCodigo()));
        }

        // Distrito - debe ser de 2 dígitos con ceros a la izquierda
        if (empresa.getDistrito() != null) {
            agregarElemento(doc, ubicacion, "Distrito",
                String.format("%02d", empresa.getDistrito().getCodigo()));
        }

        // Barrio - es opcional, si existe debe ser el nombre del barrio (texto)
        if (empresa.getBarrio() != null) {
            agregarElemento(doc, ubicacion, "Barrio",
                empresa.getBarrio().getBarrio());
        }

        // Otras señas
        if (empresa.getOtrasSenas() != null) {
            agregarElemento(doc, ubicacion, "OtrasSenas",
                empresa.getOtrasSenas());
        }
    }

    private void agregarMediosPago(Document doc, Element root, Factura factura) {
        for (FacturaMedioPago medioPago : factura.getMediosPago()) {
            agregarElemento(doc, root, "MedioPago",
                medioPago.getMedioPago().getCodigo());
        }
    }

    private void agregarDetalles(Document doc, Element root, Factura factura) {
        Element detalleServicio = doc.createElement("DetalleServicio");
        root.appendChild(detalleServicio);

        int numeroLinea = 1;
        for (FacturaDetalle detalle : factura.getDetalles()) {
            Element lineaDetalle = doc.createElement("LineaDetalle");
            detalleServicio.appendChild(lineaDetalle);

            // Número de línea
            agregarElemento(doc, lineaDetalle, "NumeroLinea", String.valueOf(numeroLinea++));

            // Código
            Element codigo = doc.createElement("Codigo");
            lineaDetalle.appendChild(codigo);
            agregarElemento(doc, codigo, "Tipo", "04"); // Código interno
            agregarElemento(doc, codigo, "Codigo", detalle.getProducto().getCodigoInterno());

            // Cantidad
            agregarElemento(doc, lineaDetalle, "Cantidad",
                detalle.getCantidad().stripTrailingZeros().toPlainString());

            // Unidad de medida
            agregarElemento(doc, lineaDetalle, "UnidadMedida",
                detalle.getProducto().getUnidadMedida().getCodigo());

            // Detalle
            agregarElemento(doc, lineaDetalle, "Detalle", detalle.getDetalle());

            // Precio unitario
            agregarElemento(doc, lineaDetalle, "PrecioUnitario",
                detalle.getPrecioUnitario().stripTrailingZeros().toPlainString());

            // Monto total
            agregarElemento(doc, lineaDetalle, "MontoTotal",
                detalle.getMontoTotal().stripTrailingZeros().toPlainString());

            // Subtotal
            agregarElemento(doc, lineaDetalle, "SubTotal",
                detalle.getSubtotal().stripTrailingZeros().toPlainString());

            // Impuestos
            if (!detalle.getImpuestos().isEmpty()) {
                for (FacturaDetalleImpuesto impuesto : detalle.getImpuestos()) {
                    Element impuestoElement = doc.createElement("Impuesto");
                    lineaDetalle.appendChild(impuestoElement);

                    agregarElemento(doc, impuestoElement, "Codigo",
                        impuesto.getCodigoImpuesto());
                    agregarElemento(doc, impuestoElement, "CodigoTarifa",
                        impuesto.getCodigoTarifaIVA());
                    agregarElemento(doc, impuestoElement, "Tarifa",
                        impuesto.getTarifa().stripTrailingZeros().toPlainString());
                    agregarElemento(doc, impuestoElement, "Monto",
                        impuesto.getMontoImpuesto().stripTrailingZeros().toPlainString());
                }
            }

            // Impuesto neto
            agregarElemento(doc, lineaDetalle, "ImpuestoNeto",
                detalle.getMontoImpuesto().stripTrailingZeros().toPlainString());

            // Monto total línea
            agregarElemento(doc, lineaDetalle, "MontoTotalLinea",
                detalle.getMontoTotalLinea().stripTrailingZeros().toPlainString());
        }
    }

    private void agregarResumen(Document doc, Element root, Factura factura) {
        Element resumen = doc.createElement("ResumenFactura");
        root.appendChild(resumen);

        // Código de moneda
        Element codigoTipoMoneda = doc.createElement("CodigoTipoMoneda");
        resumen.appendChild(codigoTipoMoneda);
        agregarElemento(doc, codigoTipoMoneda, "CodigoMoneda",
            factura.getMoneda().getCodigo());
        agregarElemento(doc, codigoTipoMoneda, "TipoCambio",
            factura.getTipoCambio().stripTrailingZeros().toPlainString());

        // Totales de servicios
        agregarElemento(doc, resumen, "TotalServGravados",
            factura.getTotalServiciosGravados().stripTrailingZeros().toPlainString());
        agregarElemento(doc, resumen, "TotalServExentos",
            factura.getTotalServiciosExentos().stripTrailingZeros().toPlainString());
        agregarElemento(doc, resumen, "TotalServExonerado",
            factura.getTotalServiciosExonerados().stripTrailingZeros().toPlainString());

        // Totales de mercancías
        agregarElemento(doc, resumen, "TotalMercanciasGravadas",
            factura.getTotalMercanciasGravadas().stripTrailingZeros().toPlainString());
        agregarElemento(doc, resumen, "TotalMercanciasExentas",
            factura.getTotalMercanciasExentas().stripTrailingZeros().toPlainString());
        agregarElemento(doc, resumen, "TotalMercExonerada",
            factura.getTotalMercanciasExoneradas().stripTrailingZeros().toPlainString());

        // Totales generales
        agregarElemento(doc, resumen, "TotalGravado",
            factura.getTotalGravado().stripTrailingZeros().toPlainString());
        agregarElemento(doc, resumen, "TotalExento",
            factura.getTotalExento().stripTrailingZeros().toPlainString());
        agregarElemento(doc, resumen, "TotalExonerado",
            factura.getTotalExonerado().stripTrailingZeros().toPlainString());

        // Totales de venta
        agregarElemento(doc, resumen, "TotalVenta",
            factura.getTotalVenta().stripTrailingZeros().toPlainString());
        agregarElemento(doc, resumen, "TotalDescuentos",
            factura.getTotalDescuentos().stripTrailingZeros().toPlainString());
        agregarElemento(doc, resumen, "TotalVentaNeta",
            factura.getTotalVentaNeta().stripTrailingZeros().toPlainString());

        // Impuestos
        agregarElemento(doc, resumen, "TotalImpuesto",
            factura.getTotalImpuesto().stripTrailingZeros().toPlainString());

        // Total del comprobante
        agregarElemento(doc, resumen, "TotalComprobante",
            factura.getTotalComprobante().stripTrailingZeros().toPlainString());
    }
}