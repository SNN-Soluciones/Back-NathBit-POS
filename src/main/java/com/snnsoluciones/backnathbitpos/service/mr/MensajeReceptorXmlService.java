package com.snnsoluciones.backnathbitpos.service.mr;

import com.snnsoluciones.backnathbitpos.entity.Compra;
import com.snnsoluciones.backnathbitpos.entity.MensajeReceptorBitacora;
import com.snnsoluciones.backnathbitpos.repository.MensajeReceptorBitacoraRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MensajeReceptorXmlService {

  private final MensajeReceptorBitacoraRepository bitacoraRepository;

  private static final DateTimeFormatter FECHA_HACIENDA_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

  /**
   * Construye el XML del Mensaje Receptor según especificación de Hacienda v4.3
   */
  public byte[] buildMensajeReceptorXml(Compra compra, MensajeReceptorBitacora bitacora) {
    log.info("Construyendo XML Mensaje Receptor para clave: {}", bitacora.getClave());

    // Validaciones
    if (bitacora.getTipoMensaje() == null) {
      throw new IllegalArgumentException("Tipo de mensaje no puede ser null");
    }

    if (("06".equals(bitacora.getTipoMensaje()) || "07".equals(bitacora.getTipoMensaje()))
        && (bitacora.getJustificacion() == null || bitacora.getJustificacion().trim().isEmpty())) {
      throw new IllegalArgumentException(
          "La justificación es obligatoria para mensajes tipo 06 (Parcial) y 07 (Rechazo)"
      );
    }

    // Construir XML
    StringBuilder xml = new StringBuilder();
    xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    xml.append(
        "<MensajeReceptor xmlns=\"https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.3/mensajeReceptor\" ");
    xml.append("xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" ");
    xml.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n");

    // 1. Clave del documento recibido
    xml.append("  <Clave>").append(bitacora.getClave()).append("</Clave>\n");

    // 2. Número de cédula del EMISOR ORIGINAL (el proveedor)
    xml.append("  <NumeroCedulaEmisor>")
        .append(compra.getProveedor().getNumeroIdentificacion())
        .append("</NumeroCedulaEmisor>\n");

    // 3. Fecha de emisión del mensaje receptor (AHORA con zona -06:00)
    OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.of("-06:00"));
    xml.append("  <FechaEmisionDoc>")
        .append(ahora.format(FECHA_HACIENDA_FORMATTER))
        .append("</FechaEmisionDoc>\n");

    // 4. Tipo de mensaje (1 = acepta, 2 = parcial, 3 = rechazo)
    String mensaje = switch (bitacora.getTipoMensaje()) {
      case "05" -> "1";  // Aceptación
      case "06" -> "2";  // Aceptación Parcial
      case "07" -> "3";  // Rechazo
      default -> throw new IllegalArgumentException(
          "Tipo de mensaje inválido: " + bitacora.getTipoMensaje()
      );
    };
    xml.append("  <Mensaje>").append(mensaje).append("</Mensaje>\n");

    // 5. Detalle del mensaje (opcional si acepta total, obligatorio si rechaza o parcial)
    if (bitacora.getJustificacion() != null && !bitacora.getJustificacion().trim().isEmpty()) {
      String detalleMensaje = bitacora.getJustificacion().trim();

      // Validar longitud según especificación (mínimo 5, máximo 160)
      if (detalleMensaje.length() < 5) {
        detalleMensaje = detalleMensaje + " - Mensaje de confirmación";
      }
      if (detalleMensaje.length() > 160) {
        detalleMensaje = detalleMensaje.substring(0, 160);
      }

      xml.append("  <DetalleMensaje>")
          .append(escaparXml(detalleMensaje))
          .append("</DetalleMensaje>\n");
    }

    // 6. Monto total de impuesto aceptado (SOLO para tipo 06 - Aceptación Parcial)
    if ("06".equals(bitacora.getTipoMensaje()) && compra.getMontoImpuestoAceptado() != null) {
      xml.append("  <MontoTotalImpuesto>")
          .append(compra.getMontoImpuestoAceptado().toString())
          .append("</MontoTotalImpuesto>\n");
    }

    // 7. Monto total de la factura aceptado (SOLO para tipo 06)
    if ("06".equals(bitacora.getTipoMensaje()) && compra.getTotalComprobante() != null) {
      xml.append("  <TotalFactura>")
          .append(compra.getTotalComprobante().toString())
          .append("</TotalFactura>\n");
    }

    // 8. Número de cédula del RECEPTOR ORIGINAL (nosotros - la empresa)
    xml.append("  <NumeroCedulaReceptor>")
        .append(compra.getEmpresa().getIdentificacion())
        .append("</NumeroCedulaReceptor>\n");

    // 9. Consecutivo del mensaje receptor
    xml.append("  <NumeroConsecutivoReceptor>")
        .append(bitacora.getConsecutivo())
        .append("</NumeroConsecutivoReceptor>\n");

    xml.append("</MensajeReceptor>");

    String xmlString = xml.toString();
    log.debug("XML Mensaje Receptor generado:\n{}", xmlString);

    return xmlString.getBytes(StandardCharsets.UTF_8);
  }

  /**
   * Genera el consecutivo del Mensaje Receptor Formato: 506DDDMMAAA000000001 - 506: Código país
   * Costa Rica - DDD: Día (3 dígitos) - MM: Mes (2 dígitos) - AAA: Año (3 últimos dígitos) -
   * 000000001: Secuencial (9 dígitos)
   */
  public String generarConsecutivoMR(Long sucursalId) {
    LocalDateTime ahora = LocalDateTime.now();

    // Componentes de fecha
    String codigoPais = "506";
    String dia = String.format("%03d", ahora.getDayOfYear()); // Día del año (001-366)
    String mes = String.format("%02d", ahora.getMonthValue()); // Mes (01-12)
    String anio = String.format("%03d", ahora.getYear() % 1000); // Últimos 3 dígitos del año

    // Obtener último consecutivo de la empresa
    Optional<MensajeReceptorBitacora> ultimoConsecutivo = bitacoraRepository.findUltimoConsecutivoBySucursal(
        sucursalId);

    int secuencial = 1;

    if (ultimoConsecutivo.isPresent()) {
      String ultimo = ultimoConsecutivo.get().getConsecutivo();

      // Validar que tenga el formato correcto (mínimo 20 caracteres)
      if (ultimo.length() >= 20) {
        // Extraer la fecha del último consecutivo
        String ultimoDia = ultimo.substring(3, 6);
        String ultimoMes = ultimo.substring(6, 8);
        String ultimoAnio = ultimo.substring(8, 11);

        // Si es el mismo día, incrementar el secuencial
        if (dia.equals(ultimoDia) && mes.equals(ultimoMes) && anio.equals(ultimoAnio)) {
          String secuencialStr = ultimo.substring(11);
          try {
            secuencial = Integer.parseInt(secuencialStr) + 1;
          } catch (NumberFormatException e) {
            log.warn("Error parseando secuencial del consecutivo: {}", ultimo);
          }
        }
      }
    }

    // Construir consecutivo completo
    String consecutivo = String.format("%s%s%s%s%09d",
        codigoPais, dia, mes, anio, secuencial);

    log.info("Consecutivo MR generado: {} para empresa: {}", consecutivo, sucursalId);

    return consecutivo;
  }

  /**
   * Escapa caracteres especiales XML
   */
  private String escaparXml(String texto) {
    if (texto == null) {
      return "";
    }
    return texto
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;");
  }

  /**
   * Valida que el XML generado cumpla con los requisitos mínimos
   */
  public boolean validarXmlMR(byte[] xmlBytes) {
    String xml = new String(xmlBytes, StandardCharsets.UTF_8);

    // Validaciones básicas
    boolean tieneClave = xml.contains("<Clave>");
    boolean tieneEmisor = xml.contains("<NumeroCedulaEmisor>");
    boolean tieneFecha = xml.contains("<FechaEmisionDoc>");
    boolean tieneMensaje = xml.contains("<Mensaje>");
    boolean tieneReceptor = xml.contains("<NumeroCedulaReceptor>");
    boolean tieneConsecutivo = xml.contains("<NumeroConsecutivoReceptor>");

    return tieneClave && tieneEmisor && tieneFecha &&
        tieneMensaje && tieneReceptor && tieneConsecutivo;
  }
}