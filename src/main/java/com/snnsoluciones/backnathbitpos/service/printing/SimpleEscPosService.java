package com.snnsoluciones.backnathbitpos.service.printing;

import com.snnsoluciones.backnathbitpos.entity.Factura;
import com.snnsoluciones.backnathbitpos.entity.FacturaDetalle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Servicio simplificado para generar comandos ESC/POS
 * Sin usar la librería escpos-coffee para evitar problemas de dependencias
 */
@Slf4j
@Service
public class SimpleEscPosService {

    private static final int ANCHO_80MM = 48;
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // Comandos ESC/POS básicos
    private static final byte[] ESC_INIT = {0x1B, 0x40}; // Initialize printer
    private static final byte[] ESC_ALIGN_CENTER = {0x1B, 0x61, 0x01}; // Center align
    private static final byte[] ESC_ALIGN_LEFT = {0x1B, 0x61, 0x00}; // Left align
    private static final byte[] ESC_BOLD_ON = {0x1B, 0x45, 0x01}; // Bold on
    private static final byte[] ESC_BOLD_OFF = {0x1B, 0x45, 0x00}; // Bold off
    private static final byte[] ESC_DOUBLE_HEIGHT_ON = {0x1D, 0x21, 0x11}; // Double height
    private static final byte[] ESC_NORMAL_SIZE = {0x1D, 0x21, 0x00}; // Normal size
    private static final byte[] ESC_CUT_PAPER = {0x1D, 0x56, 0x41}; // Cut paper
    private static final byte[] ESC_LINE_FEED = {0x0A}; // Line feed

    public String generarComandosBase64(Factura factura) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Inicializar impresora
            out.write(ESC_INIT);

            // Imprimir contenido
            imprimirEncabezado(out, factura);
            imprimirDatosFactura(out, factura);
            imprimirDetalles(out, factura);
            imprimirTotales(out, factura);
            imprimirPie(out, factura);

            // Alimentar papel y cortar
            for (int i = 0; i < 5; i++) {
                out.write(ESC_LINE_FEED);
            }
            out.write(ESC_CUT_PAPER);

            // Convertir a Base64
            return Base64.getEncoder().encodeToString(out.toByteArray());

        } catch (Exception e) {
            log.error("Error generando comandos ESC/POS", e);
            throw new RuntimeException("Error al generar comandos de impresión: " + e.getMessage());
        }
    }

    private void imprimirEncabezado(ByteArrayOutputStream out, Factura factura) throws IOException {
        var empresa = factura.getSucursal().getEmpresa();
        var sucursal = factura.getSucursal();

        // Centrar y poner en negritas el nombre
        out.write(ESC_ALIGN_CENTER);
        out.write(ESC_DOUBLE_HEIGHT_ON);
        out.write(ESC_BOLD_ON);
        escribirLinea(out, empresa.getNombreComercial());
        out.write(ESC_NORMAL_SIZE);
        out.write(ESC_BOLD_OFF);
        String direccionCompleta = null;
        if(sucursal.getProvincia() != null) {
            direccionCompleta = sucursal.getProvincia().getProvincia().concat(" ,").concat(
                sucursal.getCanton().getCanton() != null ? sucursal.getCanton().getCanton() : ""
            );
        } else {
            direccionCompleta = empresa.getProvincia().getProvincia().concat(" ,").concat(
                empresa.getCanton().getCanton() != null ? empresa.getCanton().getCanton() : ""
            );
        }
        // Datos de la empresa
        escribirLinea(out, empresa.getNombreRazonSocial());
        escribirLinea(out, empresa.getTipoIdentificacion().getDescripcion() + ": " + empresa.getIdentificacion());
        escribirLinea(out, direccionCompleta);
        escribirLinea(out, "Tel: " + empresa.getTelefono());
        escribirLinea(out, empresa.getEmail());

        // Línea divisoria
        out.write(ESC_ALIGN_LEFT);
        escribirLinea(out, "=".repeat(ANCHO_80MM));
    }

    private void imprimirDatosFactura(ByteArrayOutputStream out, Factura factura) throws IOException {
        // Tipo de documento
        out.write(ESC_ALIGN_CENTER);
        out.write(ESC_BOLD_ON);
        escribirLinea(out, getTipoDocumentoNombre(factura));
        out.write(ESC_BOLD_OFF);
        out.write(ESC_ALIGN_LEFT);

        // Datos del documento
        escribirLinea(out, "Consecutivo: " + factura.getConsecutivo());
        if (factura.getClave() != null) {
            escribirLinea(out, "Clave: " + factura.getClave());
        }
        escribirLinea(out, "Fecha: " + factura.getFechaEmision());

        // Cliente si existe
        if (factura.getCliente() != null || factura.getNombreReceptor() != null) {
            escribirLinea(out, "-".repeat(ANCHO_80MM));
            escribirLinea(out, "CLIENTE: " + (factura.getNombreReceptor() != null ?
                factura.getNombreReceptor() : "Cliente General"));
            if (factura.getCliente() != null && factura.getCliente().getNumeroIdentificacion() != null) {
                escribirLinea(out, "ID: " + factura.getCliente().getNumeroIdentificacion());
            }
        }

        escribirLinea(out, "=".repeat(ANCHO_80MM));
    }

    private void imprimirDetalles(ByteArrayOutputStream out, Factura factura) throws IOException {
        // Encabezado
        out.write(ESC_BOLD_ON);
        escribirLinea(out, "CANT  DESCRIPCION              P.UNIT    TOTAL");
        out.write(ESC_BOLD_OFF);
        escribirLinea(out, "-".repeat(ANCHO_80MM));

        // Detalles
        for (FacturaDetalle detalle : factura.getDetalles()) {
            // Línea 1: Cantidad y descripción
            String cant = String.format("%4.0f", detalle.getCantidad());
            String desc = truncar(detalle.getDescripcionPersonalizada(), 28);
            escribirLinea(out, cant + "  " + desc);

            // Línea 2: Precio unitario y total
            String pUnit = DECIMAL_FORMAT.format(detalle.getPrecioUnitario());
            String total = DECIMAL_FORMAT.format(detalle.getMontoTotal());
            escribirLinea(out, formatearLineaPrecio(pUnit, total));

            // Descuento si existe
            if (detalle.getMontoDescuento() != null &&
                detalle.getMontoDescuento().compareTo(BigDecimal.ZERO) > 0) {
                escribirLinea(out, "      Descuento: -" +
                    DECIMAL_FORMAT.format(detalle.getMontoDescuento()));
            }
        }

        escribirLinea(out, "-".repeat(ANCHO_80MM));
    }

    private void imprimirTotales(ByteArrayOutputStream out, Factura factura) throws IOException {
        // Subtotal
        imprimirLineaTotal(out, "Subtotal:", factura.getTotalVentaNeta());

        // Descuentos
        if (factura.getTotalDescuentos().compareTo(BigDecimal.ZERO) > 0) {
            imprimirLineaTotal(out, "Descuentos:", factura.getTotalDescuentos().negate());
        }

        // Impuestos
        if (factura.getTotalImpuesto().compareTo(BigDecimal.ZERO) > 0) {
            imprimirLineaTotal(out, "IVA:", factura.getTotalImpuesto());
        }

        // Total
        escribirLinea(out, "=".repeat(ANCHO_80MM));
        out.write(ESC_BOLD_ON);
        out.write(ESC_DOUBLE_HEIGHT_ON);
        imprimirLineaTotal(out, "TOTAL:", factura.getTotalComprobante());
        out.write(ESC_NORMAL_SIZE);
        out.write(ESC_BOLD_OFF);
        escribirLinea(out, "=".repeat(ANCHO_80MM));

        // Medios de pago
        escribirLinea(out, "");
        escribirLinea(out, "FORMA DE PAGO:");
        factura.getMediosPago().forEach(medio -> {
            try {
                imprimirLineaTotal(out, medio.getMedioPago().getDescripcion() + ":",
                    medio.getMonto());
            } catch (IOException e) {
                log.error("Error imprimiendo medio de pago", e);
            }
        });

        // Vuelto si existe
        if (factura.getVuelto() != null && factura.getVuelto().compareTo(BigDecimal.ZERO) > 0) {
            escribirLinea(out, "-".repeat(ANCHO_80MM));
            out.write(ESC_BOLD_ON);
            imprimirLineaTotal(out, "VUELTO:", factura.getVuelto());
            out.write(ESC_BOLD_OFF);
        }
    }

    private void imprimirPie(ByteArrayOutputStream out, Factura factura) throws IOException {
        escribirLinea(out, "=".repeat(ANCHO_80MM));

        // Mensaje de agradecimiento
        out.write(ESC_ALIGN_CENTER);
        out.write(ESC_BOLD_ON);
        escribirLinea(out, "¡GRACIAS POR SU COMPRA!");
        out.write(ESC_BOLD_OFF);

        // Info del sistema
        escribirLinea(out, "NathBit POS v1.0");

        // Usuario y terminal
        String usuario = factura.getCajero() != null ?
            factura.getCajero().getNombre() : "Sistema";
        String terminal = factura.getTerminal() != null ?
            factura.getTerminal().getNumeroTerminal() : "T001";
        escribirLinea(out, "Cajero: " + usuario + " | Terminal: " + terminal);

        out.write(ESC_ALIGN_LEFT);
    }

    // Métodos auxiliares

    private void escribirLinea(ByteArrayOutputStream out, String texto) throws IOException {
        out.write(texto.getBytes(StandardCharsets.UTF_8));
        out.write(ESC_LINE_FEED);
    }

    private void imprimirLineaTotal(ByteArrayOutputStream out, String etiqueta, BigDecimal valor)
        throws IOException {
        String valorStr = DECIMAL_FORMAT.format(valor);
        int espacios = ANCHO_80MM - etiqueta.length() - valorStr.length();
        String linea = etiqueta + " ".repeat(Math.max(espacios, 1)) + valorStr;
        escribirLinea(out, linea);
    }

    private String formatearLineaPrecio(String pUnit, String total) {
        return String.format("      %28s %10s",
            alinearDerecha(pUnit, 8),
            alinearDerecha(total, 10));
    }

    private String truncar(String texto, int longitud) {
        if (texto == null) return "";
        return texto.length() > longitud ? texto.substring(0, longitud) : texto;
    }

    private String alinearDerecha(String texto, int ancho) {
        if (texto.length() >= ancho) return texto.substring(0, ancho);
        return " ".repeat(ancho - texto.length()) + texto;
    }

    private String getTipoDocumentoNombre(Factura factura) {
        return switch (factura.getTipoDocumento()) {
            case TIQUETE_INTERNO -> "TIQUETE INTERNO";
            case FACTURA_INTERNA -> "FACTURA INTERNA";
            case TIQUETE_ELECTRONICO -> "TIQUETE ELECTRÓNICO";
            case FACTURA_ELECTRONICA -> "FACTURA ELECTRÓNICA";
            default -> factura.getTipoDocumento().getDescripcion();
        };
    }
}