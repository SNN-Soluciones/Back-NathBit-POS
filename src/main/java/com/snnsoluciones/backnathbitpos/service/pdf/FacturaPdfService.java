package com.snnsoluciones.backnathbitpos.service.pdf;

import com.snnsoluciones.backnathbitpos.entity.Cliente;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.Factura;
import com.snnsoluciones.backnathbitpos.entity.FacturaDetalle;
import com.snnsoluciones.backnathbitpos.enums.facturacion.EstadoFactura;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento;
import com.snnsoluciones.backnathbitpos.repository.FacturaRepository;
import com.snnsoluciones.backnathbitpos.service.FacturaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.*;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

@Slf4j
@Service
@RequiredArgsConstructor
public class FacturaPdfService {

    private final PdfGeneratorService pdfGeneratorService;
    private final FacturaService facturaService;

    /**
     * Genera PDF de factura usando solo la clave numérica
     */
    @Transactional(readOnly = true)
    public byte[] generarFacturaPorClave(String claveNumerica) {
        log.info("Generando PDF para clave: {}", claveNumerica);
        
        // Buscar documento por clave
        Factura factura = facturaService.buscarPorClave(claveNumerica)
            .orElseThrow(() -> new RuntimeException("Documento no encontrado con clave: " + claveNumerica));
        
        return generarFactura(factura);
    }

    /**
     * Genera PDF de factura desde el documento
     */
    public byte[] generarFactura(Factura factura) {
        try {
            // Determinar plantilla según tipo de documento
            String plantilla = obtenerPlantilla(factura.getTipoDocumento());
            
            // Preparar parámetros
            Map<String, Object> parametros = prepararParametros(factura);
            
            // Preparar datos de items
            List<Map<String, Object>> items = prepararItems(factura);
            
            // Generar PDF
            return pdfGeneratorService.generarPdf(plantilla, parametros, items);
            
        } catch (Exception e) {
            log.error("Error generando factura PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar factura PDF", e);
        }
    }

    /**
     * Prepara todos los parámetros para el reporte
     */
    private Map<String, Object> prepararParametros(Factura factura) {
        Map<String, Object> params = new HashMap<>();
        
        // Datos del emisor
        Empresa empresa = factura.getSucursal().getEmpresa();
        params.put("emisorNombre", empresa.getNombreComercial() != null ? 
            empresa.getNombreComercial() : empresa.getNombreRazonSocial());
        params.put("emisorIdentificacion", empresa.getTipoIdentificacion() + ": " + empresa.getIdentificacion());
        params.put("emisorTelefono", empresa.getTelefono());
        params.put("emisorEmail", empresa.getEmail());
        params.put("emisorDireccion", construirDireccion(empresa));
        
        // Logo de la empresa (si existe)
        if (empresa.getLogoUrl() != null) {
            params.put("logoUrl", empresa.getLogoUrl());
        }
        
        // Datos del documento
        params.put("tipoDocumento", obtenerNombreTipoDocumento(factura.getTipoDocumento()));
        params.put("numeroConsecutivo", factura.getConsecutivo());
        params.put("claveNumerica", factura.getClave());
        params.put("fechaEmision", factura.getFechaEmision());
        
        // Datos del cliente
        if (factura.getCliente() != null) {
            Cliente cliente = factura.getCliente();
            params.put("clienteNombre", cliente.getRazonSocial());
            params.put("clienteIdentificacion", cliente.getTipoIdentificacion() + ": " + cliente.getNumeroIdentificacion());
            params.put("clienteTelefono", cliente.getTelefonoNumero());
            params.put("clienteEmail", cliente.getEmails());
            params.put("clienteDireccion", cliente.getUbicacion());
        } else {
            // Cliente genérico
            params.put("clienteNombre", "CLIENTE CONTADO");
            params.put("clienteIdentificacion", "");
        }
        
        // Condiciones de venta
        params.put("condicionVenta", factura.getCondicionVenta().getDescripcion());
        params.put("plazoCredito", factura.getPlazoCredito() != null ?
            factura.getPlazoCredito() + " días" : "");
        params.put("medioPago", factura.getMediosPago());
        
        // Montos
        params.put("moneda", factura.getMoneda().getCodigo());
        params.put("tipoCambio", factura.getTipoCambio());
        
        // Subtotales
        params.put("subtotalGravado", factura.getTotalServiciosGravados());
        params.put("subtotalExento", factura.getTotalServiciosExentos());
        params.put("subtotalExonerado", factura.getTotalServiciosExonerados());
        
        // Descuentos
        params.put("totalDescuentos", factura.getTotalDescuentos());
        
        // Impuestos
        params.put("totalImpuesto", factura.getTotalImpuesto());
        params.put("totalIVADevuelto", factura.getTotalIVADevuelto());
        
        // Otros cargos
        params.put("totalOtrosCargos", factura.getTotalOtrosCargos());
        
        // Total
        params.put("totalVenta", factura.getTotalVenta());
        params.put("totalComprobante", factura.getTotalComprobante());
        
        // Generar código QR
        params.put("codigoQR", generarCodigoQR(factura));
        
        // Notas
        params.put("observaciones", factura.getObservaciones());

        // Estado del documento
        if (factura.getEstado() == EstadoFactura.ANULADA) {
            params.put("marcaAgua", "DOCUMENTO ANULADO");
        }
        
        return params;
    }

    /**
     * Prepara los items del documento
     */
    private List<Map<String, Object>> prepararItems(Factura factura) {
        List<Map<String, Object>> items = new ArrayList<>();
        
        for (FacturaDetalle detalle : factura.getDetalles()) {
            Map<String, Object> item = new HashMap<>();
            
            item.put("linea", detalle.getNumeroLinea());
            item.put("codigo", detalle.getProducto().getCodigoInterno());
            item.put("descripcion", detalle.getProducto().getDescripcion());
            item.put("cantidad", detalle.getCantidad());
            item.put("unidad", detalle.getUnidadMedida());
            item.put("precioUnitario", detalle.getPrecioUnitario());
            
            // Descuentos
            BigDecimal descuento = detalle.getMontoDescuento() != null ? 
                detalle.getMontoDescuento() : BigDecimal.ZERO;
            item.put("descuento", descuento);
            
            // Subtotal
            item.put("subtotal", detalle.getSubtotal());
            
            // Impuesto
            BigDecimal impuesto = detalle.getMontoImpuesto() != null ?
                detalle.getMontoImpuesto() : BigDecimal.ZERO;
            item.put("impuesto", impuesto);
            
            // Total línea
            item.put("totalLinea", detalle.getMontoTotalLinea());
            
            items.add(item);
        }
        
        return items;
    }

    /**
     * Genera código QR con información de la factura
     */
    private byte[] generarCodigoQR(Factura factura) {
        try {
            // Contenido del QR (puedes personalizarlo)
            String contenido = String.format(
                "https://www.hacienda.go.cr/consultapublicadocumento?clave=%s",
                factura.getClave()
            );
            
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(
                contenido, 
                BarcodeFormat.QR_CODE, 
                200, 
                200
            );
            
            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "PNG", baos);
            
            return baos.toByteArray();
            
        } catch (Exception e) {
            log.error("Error generando código QR: {}", e.getMessage());
            return new byte[0];
        }
    }

    /**
     * Determina la plantilla según el tipo de documento
     */
    private String obtenerPlantilla(TipoDocumento tipo) {
      return switch (tipo) {
        case FACTURA_ELECTRONICA, FACTURA_INTERNA -> "factura_electronica";
        case TIQUETE_ELECTRONICO, TIQUETE_INTERNO -> "tiquete_electronico";
        case NOTA_CREDITO -> "nota_credito";
        case NOTA_DEBITO -> "nota_debito";
        default -> "factura_electronica";
      };
    }

    /**
     * Utilidades auxiliares
     */
    private String construirDireccion(Empresa empresa) {
        return String.format("%s, %s, %s, %s",
            empresa.getOtrasSenas(),
            empresa.getDistrito() != null ? empresa.getDistrito().getDistrito() : "",
            empresa.getCanton() != null ? empresa.getCanton().getCanton() : "",
            empresa.getProvincia() != null ? empresa.getProvincia().getProvincia() : ""
        );
    }

    private String obtenerNombreTipoDocumento(TipoDocumento tipo) {
      return switch (tipo) {
        case FACTURA_ELECTRONICA -> "FACTURA ELECTRÓNICA";
        case TIQUETE_ELECTRONICO -> "TIQUETE ELECTRÓNICO";
        case NOTA_CREDITO -> "NOTA DE CRÉDITO ELECTRÓNICA";
        case NOTA_DEBITO -> "NOTA DE DÉBITO ELECTRÓNICA";
        case FACTURA_INTERNA -> "FACTURA";
        case TIQUETE_INTERNO -> "TIQUETE";
        default -> tipo.toString();
      };
    }
}