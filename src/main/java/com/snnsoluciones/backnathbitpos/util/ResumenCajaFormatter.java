package com.snnsoluciones.backnathbitpos.util;

import com.snnsoluciones.backnathbitpos.dto.sesiones.ResumenCajaDetalladoDTO;
import com.snnsoluciones.backnathbitpos.entity.SesionCaja;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ResumenCajaFormatter {

  private static final Locale LOCALE_CR = new Locale("es", "CR");
  private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  private static String crc(BigDecimal n) {
    if (n == null) n = BigDecimal.ZERO;
    NumberFormat nf = NumberFormat.getCurrencyInstance(LOCALE_CR);
    // Forzar el símbolo ₡ (algunos JDK ponen "CRC")
    String v = nf.format(n);
    if (!v.contains("₡")) v = v.replace("CRC", "₡").replace("CRC ", "₡");
    return v;
  }

  private static String safe(String s) { return (s == null || s.isBlank()) ? "-" : s; }

  /** Texto plano listo para pegar/mandar */
  public static String toTexto(SesionCaja sesion, ResumenCajaDetalladoDTO r) {
    StringBuilder sb = new StringBuilder();

    sb.append("🧾 RESUMEN DE CAJA\n");
    sb.append("----------------------------------------\n");
    sb.append("Fecha: ").append(sesion.getFechaHoraCierre() != null ? DT.format(sesion.getFechaHoraCierre()) : "-").append("\n");
    sb.append("Cajero: ").append(safe(sesion.getUsuario().getNombre())).append(" ").append(safe(sesion.getUsuario().getApellidos())).append("\n");
    sb.append("Terminal: ").append(safe(sesion.getTerminal().getNombre())).append("\n");
    sb.append("\n");

    sb.append("💰 Totales principales\n");
    sb.append(String.format(" - Monto inicial:       %s%n", crc(r.getMontoInicial())));
    sb.append(String.format(" - Ventas efectivo:     %s%n", crc(r.getVentasEfectivo())));
    sb.append(String.format(" - Ventas tarjeta:      %s%n", crc(r.getVentasTarjeta())));
    sb.append(String.format(" - Transferencia:       %s%n", crc(r.getVentasTransferencia())));
    sb.append(String.format(" - SINPE:               %s%n", crc(r.getVentasOtros())));
    sb.append(String.format(" - Entradas adicionales:%s%n", crc(nvl(r.getEntradasAdicionales()))));
    sb.append(String.format(" - Vales:               -%s%n", crc(nvl(r.getVales()))));
    sb.append(String.format(" - Depósitos:           -%s%n", crc(nvl(r.getDepositos()))));
    sb.append(String.format(" = Monto esperado:      %s%n", crc(r.getMontoEsperado())));
    sb.append(String.format(" = Monto de cierre:     %s%n", crc(nvl(r.getMontoCierre()))));
    BigDecimal diferencia = nvl(r.getMontoCierre()).subtract(nvl(r.getMontoEsperado()));
    sb.append(String.format(" = Diferencia:          %s%n", pref(crc(diferencia), diferencia)));
    // Fondo de caja si lo guardaste en Sesion
    if (sesion.getFondoCaja() != null) {
      sb.append(String.format(" - Fondo para mañana:   %s%n", crc(sesion.getFondoCaja())));
    }
    sb.append("\n");

    sb.append("📦 Documentos emitidos\n");
    sb.append(String.format(" - Facturas: %d  Total: %s%n", nvl(r.getCantidadFacturas()), crc(nvl(r.getTotalFacturas()))));
    sb.append(String.format(" - Tiquetes: %d  Total: %s%n", nvl(r.getCantidadTiquetes()), crc(nvl(r.getTotalTiquetes()))));
    sb.append(String.format(" - Ventas internas: %d  Total: %s%n", nvl(r.getCantidadVentasInternas()), crc(nvl(r.getTotalVentasInternas()))));
    sb.append(String.format(" - Notas de crédito: %d  Total: -%s%n", nvl(r.getCantidadNotasCredito()), crc(nvl(r.getTotalNotasCredito()))));
    sb.append(String.format(" = Total docs (sin NC): %s%n",
        crc(nvl(r.getTotalFacturas()).add(nvl(r.getTotalTiquetes())).add(nvl(r.getTotalVentasInternas())))));
    sb.append("\n");

    if (r.getVentasPlataformas() != null && !r.getVentasPlataformas().isEmpty()) {
      sb.append("🛒 Plataformas digitales\n");
      r.getVentasPlataformas().forEach(p ->
          sb.append(String.format(" - %s (%s): %d trx  %s%n",
              safe(p.getPlataformaNombre()),
              safe(p.getPlataformaCodigo()),
              p.getCantidadTransacciones(),
              crc(nvl(p.getTotalVentas())))));
      sb.append("\n");
    }

    if (sesion.getObservacionesCierre() != null && !sesion.getObservacionesCierre().isBlank()) {
      sb.append("💬 Observaciones\n");
      sb.append(sesion.getObservacionesCierre()).append("\n\n");
    }

    sb.append("Generado por NathBit POS\n");
    return sb.toString();
  }

  /** HTML básico (sin CSS pesado) para email/pantalla */
  public static String toHtml(SesionCaja sesion, ResumenCajaDetalladoDTO r) {
    String fecha = sesion.getFechaHoraCierre() != null ? DT.format(sesion.getFechaHoraCierre()) : "-";
    BigDecimal diferencia = nvl(r.getMontoCierre()).subtract(nvl(r.getMontoEsperado()));

    StringBuilder h = new StringBuilder();
    h.append("<div style='font-family:Arial,Helvetica,sans-serif;font-size:14px;color:#222'>");
    h.append("<h2>🧾 Resumen de Caja</h2>");
    h.append("<p><b>Fecha:</b> ").append(fecha)
     .append(" &nbsp; <b>Cajero:</b> ").append(safe(sesion.getUsuario().getNombre())).append(" ").append(safe(sesion.getUsuario().getApellidos()))
     .append(" &nbsp; <b>Terminal:</b> ").append(safe(sesion.getTerminal().getNombre()))
     .append("</p>");

    h.append(table(
        new String[][]{
            {"Monto inicial", crc(r.getMontoInicial())},
            {"Ventas efectivo", crc(r.getVentasEfectivo())},
            {"Ventas tarjeta", crc(r.getVentasTarjeta())},
            {"Transferencia", crc(r.getVentasTransferencia())},
            {"SINPE", crc(r.getVentasOtros())},
            {"Entradas adicionales", crc(nvl(r.getEntradasAdicionales()))},
            {"Vales", "-" + crc(nvl(r.getVales()))},
            {"Depósitos", "-" + crc(nvl(r.getDepositos()))},
            {"<b>Monto esperado</b>", "<b>" + crc(r.getMontoEsperado()) + "</b>"},
            {"<b>Monto de cierre</b>", "<b>" + crc(nvl(r.getMontoCierre())) + "</b>"},
            {"<b>Diferencia</b>", "<b>" + pref(crc(diferencia), diferencia) + "</b>"},
            (sesion.getFondoCaja() != null) ? new String[]{"Fondo para mañana", crc(sesion.getFondoCaja())} : null
        }
    ));

    h.append("<h3>📦 Documentos emitidos</h3>");
    h.append(table(
        new String[][]{
            {"Facturas (cant)", String.valueOf(nvl(r.getCantidadFacturas()))},
            {"Facturas (monto)", crc(nvl(r.getTotalFacturas()))},
            {"Tiquetes (cant)", String.valueOf(nvl(r.getCantidadTiquetes()))},
            {"Tiquetes (monto)", crc(nvl(r.getTotalTiquetes()))},
            {"Ventas internas (cant)", String.valueOf(nvl(r.getCantidadVentasInternas()))},
            {"Ventas internas (monto)", crc(nvl(r.getTotalVentasInternas()))},
            {"Notas de crédito (cant)", String.valueOf(nvl(r.getCantidadNotasCredito()))},
            {"Notas de crédito (monto)", "-" + crc(nvl(r.getTotalNotasCredito()))}
        }
    ));

    if (r.getVentasPlataformas() != null && !r.getVentasPlataformas().isEmpty()) {
      h.append("<h3>🛒 Plataformas digitales</h3>");
      String[][] rows = r.getVentasPlataformas().stream()
          .map(p -> new String[]{
              safe(p.getPlataformaNombre()) + " (" + safe(p.getPlataformaCodigo()) + ")",
              p.getCantidadTransacciones() + " trx — " + crc(nvl(p.getTotalVentas()))
          })
          .toArray(String[][]::new);
      h.append(table(rows));
    }

    if (sesion.getObservacionesCierre() != null && !sesion.getObservacionesCierre().isBlank()) {
      h.append("<h3>💬 Observaciones</h3>");
      h.append("<div style='background:#FFF8D8;border-left:4px solid #FFC107;padding:8px;border-radius:6px;'>")
       .append(escapeHtml(sesion.getObservacionesCierre())).append("</div>");
    }

    h.append("<p style='color:#777;font-size:12px;margin-top:16px;'>Generado por NathBit POS</p>");
    h.append("</div>");
    return h.toString();
  }

  // ---------- helpers ----------
  private static BigDecimal nvl(BigDecimal n) { return n == null ? BigDecimal.ZERO : n; }
  private static int nvl(Integer n) { return n == null ? 0 : n; }

  private static String pref(String monto, BigDecimal val) {
    int cmp = val.compareTo(BigDecimal.ZERO);
    return (cmp > 0 ? "+" : (cmp < 0 ? "-" : "")) + monto.replace("₡-", "₡").replace("-₡", "₡");
  }

  private static String table(String[][] rows) {
    StringBuilder t = new StringBuilder();
    t.append("<table cellpadding='6' cellspacing='0' style='border-collapse:collapse;width:100%;font-size:14px'>");
    for (String[] row : rows) {
      if (row == null) continue;
      t.append("<tr>")
       .append("<td style='border-bottom:1px solid #eee'>").append(row[0]).append("</td>")
       .append("<td style='border-bottom:1px solid #eee;text-align:right'><b>").append(row[1]).append("</b></td>")
       .append("</tr>");
    }
    t.append("</table>");
    return t.toString();
  }

  private static String escapeHtml(String s) {
    if (s == null) return "";
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }
}