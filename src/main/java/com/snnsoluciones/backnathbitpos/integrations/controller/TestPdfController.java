package com.snnsoluciones.backnathbitpos.integrations.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.snnsoluciones.backnathbitpos.service.pdf.PdfGeneratorService;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/test/pdf")
@RequiredArgsConstructor
public class TestPdfController {

    private final PdfGeneratorService pdfGeneratorService;

    @GetMapping("/generar-factura-test")
    public ResponseEntity<byte[]> generarFacturaTest() {
        try {
            // 1. PARÁMETROS (los $P del reporte)
            Map<String, Object> parametros = new HashMap<>();
            
            // Datos del emisor
            parametros.put("emisorNombre", "SUPER TIENDA COSTA RICA S.A.");
            parametros.put("emisorIdentificacion", "Cédula Jurídica: 3-101-123456");
            parametros.put("emisorTelefono", "(506) 2222-3333");
            parametros.put("emisorEmail", "facturacion@supertienda.cr");
            parametros.put("emisorDireccion", "San José, Costa Rica, Avenida Central");
            
            // Datos del documento
            parametros.put("tipoDocumento", "FACTURA ELECTRÓNICA");
            parametros.put("numeroConsecutivo", "00100001010000000001");
            parametros.put("claveNumerica", "50631012500310112345600100001010000000001199999999");
            parametros.put("fechaEmision", new Date().toString());
            
            // Datos del cliente
            parametros.put("clienteNombre", "JUAN PÉREZ RODRÍGUEZ");
            parametros.put("clienteIdentificacion", "Cédula: 1-1234-5678");
            parametros.put("clienteTelefono", "(506) 8888-9999");
            parametros.put("clienteEmail", "juan@email.com");
            parametros.put("clienteDireccion", "Heredia, San Rafael");
            
            // Condiciones
            parametros.put("condicionVenta", "Contado");
            parametros.put("medioPago", "Efectivo");
            parametros.put("moneda", "CRC");
            parametros.put("tipoCambio", new BigDecimal("1"));
            
            // Totales
            parametros.put("subtotalGravado", new BigDecimal("10000.00"));
            parametros.put("subtotalExento", new BigDecimal("2000.00"));
            parametros.put("subtotalExonerado", new BigDecimal("0.00"));
            parametros.put("totalDescuentos", new BigDecimal("1000.00"));
            parametros.put("totalImpuesto", new BigDecimal("1430.00"));
            parametros.put("totalOtrosCargos", new BigDecimal("500.00"));
            parametros.put("totalComprobante", new BigDecimal("12930.00"));
            
            // QR dummy (solo para prueba)
            parametros.put("codigoQR", generarQRDummy());
            
            // Observaciones
            parametros.put("observaciones", "Gracias por su compra!");
            
            // 2. DATOS DE LOS ITEMS (los $F del reporte)
            List<Map<String, Object>> items = new ArrayList<>();
            
            // Item 1
            Map<String, Object> item1 = new HashMap<>();
            item1.put("linea", 1);
            item1.put("codigo", "PROD-001");
            item1.put("descripcion", "Laptop Dell Inspiron 15");
            item1.put("cantidad", new BigDecimal("1"));
            item1.put("unidad", "Unid");
            item1.put("precioUnitario", new BigDecimal("5000.00"));
            item1.put("descuento", new BigDecimal("500.00"));
            item1.put("subtotal", new BigDecimal("4500.00"));
            item1.put("impuesto", new BigDecimal("585.00"));
            item1.put("totalLinea", new BigDecimal("5085.00"));
            items.add(item1);
            
            // Item 2
            Map<String, Object> item2 = new HashMap<>();
            item2.put("linea", 2);
            item2.put("codigo", "PROD-002");
            item2.put("descripcion", "Mouse inalámbrico Logitech");
            item2.put("cantidad", new BigDecimal("2"));
            item2.put("unidad", "Unid");
            item2.put("precioUnitario", new BigDecimal("2500.00"));
            item2.put("descuento", new BigDecimal("500.00"));
            item2.put("subtotal", new BigDecimal("4500.00"));
            item2.put("impuesto", new BigDecimal("585.00"));
            item2.put("totalLinea", new BigDecimal("5085.00"));
            items.add(item2);
            
            // Item 3 (exento)
            Map<String, Object> item3 = new HashMap<>();
            item3.put("linea", 3);
            item3.put("codigo", "SERV-001");
            item3.put("descripcion", "Servicio de instalación");
            item3.put("cantidad", new BigDecimal("1"));
            item3.put("unidad", "Serv");
            item3.put("precioUnitario", new BigDecimal("2000.00"));
            item3.put("descuento", new BigDecimal("0.00"));
            item3.put("subtotal", new BigDecimal("2000.00"));
            item3.put("impuesto", new BigDecimal("0.00"));
            item3.put("totalLinea", new BigDecimal("2000.00"));
            items.add(item3);
            
            // 3. GENERAR PDF
            byte[] pdfBytes = pdfGeneratorService.generarPdf(
                "factura_electronica",  // nombre del archivo sin .jasper
                parametros,             // todos los $P
                items                   // todos los $F (los items)
            );
            
            // 4. DEVOLVER PDF
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", "factura_test.pdf");
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
                
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/generar-tiquete-test")
    public ResponseEntity<byte[]> generarTiqueteTest() {
        try {
            // 1. PARÁMETROS DEL TIQUETE
            Map<String, Object> parametros = new HashMap<>();

            // Datos del emisor
            parametros.put("emisorNombre", "INVERSIONES JR DE ZAGALA VIEJA SOCIEDAD ANONIMA");
            parametros.put("emisorIdentificacion", "3101752961");
            parametros.put("emisorTelefono", "(506) 26391292");
            parametros.put("emisorEmail", "RESTAURANTEVISTAALMARO7@GMAIL.COM");
            parametros.put("emisorDireccion", "PUNTARENAS, PUNTARENAS, PITAHAYA, CEBADILLA, KM 137 RUTA INTERAMERICANA");

            // Datos del documento
            parametros.put("tipoDocumento", "TIQUETE ELECTRONICO");
            parametros.put("fechaEmision", "26/08/2025 - 11:30:06");
            parametros.put("numeroConsecutivo", "00200001040000059459");
            parametros.put("claveNumerica", "50626082500310175296100200001040000059459106402334");
            parametros.put("numeroInterno", "183887");

            // Cliente genérico para tiquete
            parametros.put("clienteNombre", "Cliente");

            // Condiciones
            parametros.put("condicionVenta", "CONTADO");
            parametros.put("medioPago", "EFECTIVO");
            parametros.put("moneda", "CRC");
            parametros.put("tipoCambio", new BigDecimal("1.0"));

            // Totales
            parametros.put("totalVentas", new BigDecimal("6106.19"));
            parametros.put("totalExonerado", new BigDecimal("0.00"));
            parametros.put("totalDescuentos", new BigDecimal("0.00"));
            parametros.put("totalVentaNeta", new BigDecimal("6106.19"));
            parametros.put("totalImpuestos", new BigDecimal("793.81"));
            parametros.put("totalOtrosCargos", new BigDecimal("0.00"));
            parametros.put("totalComprobante", new BigDecimal("6900.00"));

            // Pago y vuelto
            parametros.put("montoPago", new BigDecimal("7000.00"));
            parametros.put("montoVuelto", new BigDecimal("100.00"));

            // Otros
            parametros.put("atendidoPor", "LETICIA CORTES GARCIA");
            parametros.put("observaciones", "");

            // QR Code
            parametros.put("codigoQR", generarQRDummy());

            // 2. DATOS DE LOS ITEMS (formato simplificado para tiquete)
            List<Map<String, Object>> items = new ArrayList<>();

            // Item 1
            Map<String, Object> item1 = new HashMap<>();
            item1.put("descripcion", "EMPAQUE PARA COMIDA, , UNID");
            item1.put("cantidad", new BigDecimal("1.00"));
            item1.put("precioUnitario", new BigDecimal("176.99"));
            item1.put("totalLinea", new BigDecimal("176.99"));
            items.add(item1);

            // Item 2
            Map<String, Object> item2 = new HashMap<>();
            item2.put("descripcion", "COCA PEQUEÑA, , UNID");
            item2.put("cantidad", new BigDecimal("1.00"));
            item2.put("precioUnitario", new BigDecimal("796.46"));
            item2.put("totalLinea", new BigDecimal("796.46"));
            items.add(item2);

            // Item 3
            Map<String, Object> item3 = new HashMap<>();
            item3.put("descripcion", "PESCADO, , UNID");
            item3.put("cantidad", new BigDecimal("1.00"));
            item3.put("precioUnitario", new BigDecimal("1592.92"));
            item3.put("totalLinea", new BigDecimal("1592.92"));
            items.add(item3);

            // Item 4
            Map<String, Object> item4 = new HashMap<>();
            item4.put("descripcion", "ARROZ CON CAMARONES, , UNID");
            item4.put("cantidad", new BigDecimal("1.00"));
            item4.put("precioUnitario", new BigDecimal("3539.82"));
            item4.put("totalLinea", new BigDecimal("3539.82"));
            items.add(item4);

            // 3. GENERAR PDF
            byte[] pdfBytes = pdfGeneratorService.generarPdf(
                "factura_electronica_80mm",  // nombre del archivo sin .jasper
                parametros,             // todos los parámetros
                items                   // los items
            );

            // 4. DEVOLVER PDF
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", "tiquete_test.pdf");

            return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
    
    private byte[] generarQRDummy() {
        try {
            String contenido = "https://www.hacienda.go.cr/ATV/ComprobanteElectronico/ConsultaPublica/Consulta?clave=50626082500310175296100200001040000059459106402334";

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix bitMatrix = qrCodeWriter.encode(
                contenido,
                BarcodeFormat.QR_CODE,
                100,  // Más pequeño para tiquete
                100,
                hints
            );

            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "PNG", baos);

            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }
}