package com.snnsoluciones.backnathbitpos.service.mr;

import com.snnsoluciones.backnathbitpos.entity.Compra;
import com.snnsoluciones.backnathbitpos.entity.MensajeReceptorBitacora;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MensajeReceptorXmlService {

  public byte[] buildMensajeReceptorXml(Compra compra, MensajeReceptorBitacora bitacora) {
    // TODO: Construir el XML MR de acuerdo a especificación de MH:
    // - Clave de la factura recibida (compra.getClaveHacienda())
    // - Consecutivo de receptor (bitacora.getConsecutivo())
    // - Tipo (05/06/07), justificación si 06/07, montos parciales si 06
    // - Emisor/Receptor invertidos respecto a la factura del proveedor
    // - Fecha con zona -06:00
    // Aquí dejo un XML de plantilla mínima (ajústalo a tu esquema XSD real):
    String xml = """
        <MensajeReceptor xmlns="https://tribunet.hacienda.go.cr/docs/esquemas/2017/v4.3/mensajeReceptor">
          <Clave>%s</Clave>
          <NumeroCedulaEmisor>%s</NumeroCedulaEmisor>
          <FechaEmisionDoc>%s</FechaEmisionDoc>
          <Mensaje>%s</Mensaje>
          %s
          <DetalleMensaje>%s</DetalleMensaje>
          <NumeroCedulaReceptor>%s</NumeroCedulaReceptor>
          <ConsecutivoReceptor>%s</ConsecutivoReceptor>
        </MensajeReceptor>
        """.formatted(
        compra.getClaveHacienda(),
        compra.getProveedor().getNumeroIdentificacion(),
        compra.getFechaEmision().toString(), // ajusta a formato requerido
        bitacora.getTipoMensaje(),      // "05"/"06"/"07"
        compra.getMontoImpuestoAceptado(),
        bitacora.getJustificacion() == null ? "" : bitacora.getJustificacion(),
        compra.getEmpresa().getIdentificacion(),
        bitacora.getConsecutivo()
    );

    return xml.getBytes(StandardCharsets.UTF_8);
  }
}