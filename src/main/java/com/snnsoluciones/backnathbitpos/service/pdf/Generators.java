package com.snnsoluciones.backnathbitpos.service.pdf;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.snnsoluciones.backnathbitpos.entity.Factura;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class Generators {

  public byte[] generarCodigoQRDetallado(Factura factura) {
    try {
      // Formato alternativo con más información
      StringBuilder qrContent = new StringBuilder();

      // Opción 2: Con información adicional (si lo requiere tu sistema)

      qrContent.append("FACTURA ELECTRONICA\n");
      qrContent.append("Clave: ").append(factura.getClave()).append("\n");
      qrContent.append("Emisor: ").append(factura.getSucursal().getEmpresa().getNombreComercial())
          .append("\n");
      qrContent.append("Total: ").append(factura.getMoneda().getCodigo()).append(" ")
          .append(factura.getTotalComprobante()).append("\n");
      qrContent.append(
              "Consultar en: https://www.hacienda.go.cr/ATV/ComprobanteElectronico/ConsultaPublica/Consulta?clave=")
          .append(factura.getClave());

      QRCodeWriter qrCodeWriter = new QRCodeWriter();

      Map<EncodeHintType, Object> hints = new HashMap<>();
      hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
      hints.put(EncodeHintType.MARGIN, 1);
      hints.put(EncodeHintType.ERROR_CORRECTION,
          ErrorCorrectionLevel.M); // Mayor corrección de errores

      BitMatrix bitMatrix = qrCodeWriter.encode(
          qrContent.toString(),
          BarcodeFormat.QR_CODE,
          250, // Más grande para más información
          250,
          hints
      );

      BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ImageIO.write(qrImage, "PNG", baos);

      return baos.toByteArray();

    } catch (Exception e) {
      log.error("Error generando código QR detallado: {}", e.getMessage());
      return new byte[0];
    }
  }

}
