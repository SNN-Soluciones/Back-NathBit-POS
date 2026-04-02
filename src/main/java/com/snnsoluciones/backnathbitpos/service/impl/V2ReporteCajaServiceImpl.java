// src/main/java/com/snnsoluciones/backnathbitpos/service/impl/V2ReporteCajaServiceImpl.java

package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.v2sesion.V2OpcionesReporteDTO;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.repository.*;
import com.snnsoluciones.backnathbitpos.service.V2ReporteCajaService;
import com.snnsoluciones.backnathbitpos.service.impl.V2SesionCajaServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class V2ReporteCajaServiceImpl implements V2ReporteCajaService {

    private final V2SesionCajaRepository      sesionRepo;
    private final V2TurnoCajeroRepository     turnoRepo;
    private final V2MovimientoCajaRepository  movimientoRepo;
    private final V2CierreDatafonoRepository  datafonoRepo;
    private final V2SesionPlataformaRepository plataformaRepo;
    private final V2SesionCajaServiceImpl     sesionService;
    private final V2TurnoDenominacionRepository denominacionRepo;
    private final FacturaRepository facturaRepo;
    private final FacturaInternaRepository facturaInternaRepo;

    private static final DateTimeFormatter DTF =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private String fmt(BigDecimal v) {
        if (v == null) v = BigDecimal.ZERO;
        NumberFormat nf = NumberFormat.getInstance(new Locale("es", "CR"));
        nf.setMinimumFractionDigits(0);
        nf.setMaximumFractionDigits(0);
        return "&#8353;" + nf.format(v);
    }

    private String fecha(java.time.LocalDateTime ldt) {
        return ldt != null ? DTF.format(ldt) : "-";
    }

    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private String dif(BigDecimal v) {
        if (v == null) v = BigDecimal.ZERO;
        String color = v.compareTo(BigDecimal.ZERO) >= 0 ? "#16a34a" : "#dc2626";
        String signo = v.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        return "<span style='color:" + color + "'>" + signo + fmt(v) + "</span>";
    }

    // =========================================================
    // CSS COMPARTIDO — compatible térmica + carta
    // =========================================================
    private String css() {
        return """
            <style>
            @media print {
              body { margin: 0; }
              @page { margin: 8mm; }
            }
            * { box-sizing: border-box; margin: 0; padding: 0; }
            body {
              font-family: Arial, Helvetica, sans-serif;
              font-size: 12px;
              color: #1a1a1a;
              background: #fff;
              padding: 16px;
              max-width: 100%;
            }
            .header { text-align: center; border-bottom: 2px solid #1a1a1a; padding-bottom: 10px; margin-bottom: 14px; }
            .header h1 { font-size: 15px; font-weight: 700; letter-spacing: 1px; text-transform: uppercase; }
            .header h2 { font-size: 12px; font-weight: 400; margin-top: 3px; }
            .meta { font-size: 10px; color: #555; margin-top: 5px; line-height: 1.6; }
            .stitle {
              font-size: 10px; font-weight: 700; letter-spacing: 1px;
              text-transform: uppercase;
              border-top: 1.5px solid #1a1a1a;
              border-bottom: 1px solid #1a1a1a;
              padding: 3px 0; margin: 12px 0 6px;
            }
            .row { display: flex; justify-content: space-between; padding: 2px 0; border-bottom: 0.5px dashed #ddd; font-size: 11px; }
            .row.total { font-weight: 700; font-size: 12px; border-top: 1.5px solid #1a1a1a; border-bottom: 1.5px solid #1a1a1a; padding: 4px 0; margin-top: 3px; border-style: solid; }
            .turno-card { border: 1px solid #ccc; border-radius: 3px; padding: 8px; margin-bottom: 8px; }
            .turno-nombre { font-weight: 700; font-size: 12px; margin-bottom: 6px; }
            .turno-meta { font-size: 10px; color: #666; margin-bottom: 6px; }
            .dash { border-top: 0.5px dashed #ccc; margin: 6px 0; }
            .totales-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 6px; margin-top: 6px; }
            .total-box { border: 1px solid #ccc; border-radius: 3px; padding: 6px; text-align: center; }
            .total-box .lbl { font-size: 9px; color: #666; }
            .total-box .val { font-size: 13px; font-weight: 700; margin-top: 2px; }
            .ok-box { border: 1px solid #86efac; border-radius: 3px; padding: 5px 10px; font-size: 10px; color: #15803d; text-align: center; margin-top: 10px; }
            .footer { text-align: center; font-size: 10px; color: #888; margin-top: 14px; border-top: 0.5px solid #ccc; padding-top: 8px; }
            </style>
            """;
    }

    // =========================================================
    // REPORTE DE TURNO INDIVIDUAL
    // =========================================================

    @Override
    public String generarHtmlTurno(Long turnoId, V2OpcionesReporteDTO opciones) {
        V2TurnoCajero turno = turnoRepo.findById(turnoId)
            .orElseThrow(() -> new RuntimeException("Turno no encontrado"));

        V2SesionCaja sesion = turno.getSesion();
        String cajero   = turno.getUsuario().getNombre() + " " + turno.getUsuario().getApellidos();
        String empresa  = sesion.getTerminal().getSucursal().getEmpresa().getNombreComercial();
        String sucursal = sesion.getTerminal().getSucursal().getNombre();
        String terminal = sesion.getTerminal().getNombre();

        BigDecimal ef   = sesionService.calcularVentasTurno(turnoId, "EFECTIVO");
        BigDecimal tc   = sesionService.calcularVentasTurno(turnoId, "TARJETA");
        BigDecimal sin  = sesionService.calcularVentasTurno(turnoId, "SINPE");
        BigDecimal tb   = sesionService.calcularVentasTurno(turnoId, "TRANSFERENCIA");
        BigDecimal cred = sesionService.calcularVentasTurno(turnoId, "CREDITO");
        BigDecimal totalVentas = ef.add(tc).add(sin).add(tb).add(cred);

        BigDecimal entradas    = nvl(movimientoRepo.sumEntradasEfectivoByTurnoId(turnoId));
        BigDecimal salidas     = nvl(movimientoRepo.sumSalidasByTurnoId(turnoId));
        BigDecimal montoEsperado = nvl(turno.getFondoInicio()).add(ef).add(entradas).subtract(salidas);

        List<V2CierreDatafono> datafonos = datafonoRepo.findByTurnoId(turnoId);
        boolean cerrado = turno.isCerrado();

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang='es'><head><meta charset='UTF-8'>")
            .append("<meta name='viewport' content='width=device-width,initial-scale=1'>")
            .append("<title>Arqueo de Turno</title>")
            .append(css())
            .append("</head><body>");

        // Header
        sb.append("<div class='header'>")
            .append("<h1>Arqueo de Turno</h1>")
            .append("<h2>").append(empresa).append(" — ").append(sucursal).append("</h2>")
            .append("<div class='meta'>")
            .append("Terminal: ").append(terminal).append("<br>")
            .append("Cajero: <strong>").append(cajero).append("</strong><br>")
            .append("Turno #").append(turnoId)
            .append(" &nbsp;|&nbsp; ").append(fecha(turno.getFechaInicio()))
            .append(" &rarr; ").append(fecha(turno.getFechaFin()))
            .append("</div></div>");

        // Efectivo en caja
        sb.append("<div class='stitle'>Efectivo en caja</div>")
            .append(row("Fondo al entrar:", fmt(turno.getFondoInicio())))
            .append(row("+ Ventas efectivo:", fmt(ef)))
            .append(row("+ Entradas adicionales:", fmt(entradas)))
            .append(row("- Salidas / vales:", fmt(salidas)))
            .append(rowTotal("Total esperado:", fmt(montoEsperado)));

        if (cerrado) {
            sb.append(row("Monto contado:", fmt(turno.getMontoContado())))
                .append(rowDif("Diferencia:", dif(turno.getDifEfectivo())));
        }

        // Ventas del turno
        sb.append("<div class='stitle'>Ventas del turno</div>")
            .append(row("Efectivo:", fmt(ef)))
            .append(row("Tarjeta:", fmt(tc)))
            .append(row("SINPE:", fmt(sin)))
            .append(row("Transferencia:", fmt(tb)));

        if (Boolean.TRUE.equals(opciones.getIncluirVentasCredito()) && cred.compareTo(BigDecimal.ZERO) > 0) {
            sb.append(row("Crédito:", fmt(cred)));
        }

        sb.append(rowTotal("Total ventas:", fmt(totalVentas)));

        if (Boolean.TRUE.equals(opciones.getIncluirFacturas())) {
            List<Factura> facturas        = facturaRepo.findByV2TurnoId(turnoId);
            List<FacturaInterna> internas = facturaInternaRepo.findByV2TurnoId(turnoId);

            if (!facturas.isEmpty() || !internas.isEmpty()) {
                sb.append("<div class='stitle'>Resumen de facturas</div>");

                if (!facturas.isEmpty()) {
                    sb.append("<div style='font-size:10px;font-weight:700;padding:3px 0'>Electrónicas / Tiquetes</div>");
                    for (Factura f : facturas) {
                        String cliente = f.getNombreReceptor() != null
                            ? f.getNombreReceptor() : "Consumidor Final";
                        String simbolos = simbolosPago(f.getMediosPago(), false);
                        sb.append(row(
                            f.getConsecutivo() + " — " + cliente + simbolos,
                            fmt(f.getTotalComprobante())
                        ));
                    }

                }

                if (!internas.isEmpty()) {
                    sb.append("<div style='font-size:10px;font-weight:700;padding:3px 0;margin-top:4px'>Internas</div>");
                    for (FacturaInterna fi : internas) {
                        String cliente = fi.getNombreCliente() != null
                            ? fi.getNombreCliente() : "Consumidor Final";
                        String simbolos = simbolosPago(fi.getMediosPago(), true);
                        sb.append(row(
                            fi.getNumero() + " — " + cliente + simbolos,
                            fmt(fi.getTotal())
                        ));
                    }
                }

                int totalDocs = facturas.size() + internas.size();
                sb.append(rowTotal("Total: " + totalDocs + " documentos", fmt(totalVentas)));
            }
        }

        // Distribución (solo si cerrado)
        if (cerrado) {
            sb.append("<div class='stitle'>Distribución de efectivo</div>")
                .append(row("Monto contado:", fmt(turno.getMontoContado())))
                .append(row("Retiro:", fmt(turno.getMontoRetirado())))
                .append(rowTotal("Fondo dejado en caja:", fmt(turno.getFondoCaja())));
        }

        // Denominaciones (opcional)
        if (Boolean.TRUE.equals(opciones.getIncluirDenominaciones())) {
            List<V2TurnoDenominacion> denoms = denominacionRepo.findByTurnoIdOrderByValorDesc(turnoId);
            if (!denoms.isEmpty()) {
                sb.append("<div class='stitle'>Denominaciones</div>");
                BigDecimal totBilletes = BigDecimal.ZERO;
                BigDecimal totMonedas  = BigDecimal.ZERO;
                for (V2TurnoDenominacion d : denoms) {
                    BigDecimal sub = BigDecimal.valueOf((long) d.getValor() * d.getCantidad());
                    sb.append(row("&#8353;" + String.format("%,d", d.getValor())
                        + " &times; " + d.getCantidad() + ":", fmt(sub)));
                    if ("BILLETE".equals(d.getTipo())) totBilletes = totBilletes.add(sub);
                    else totMonedas = totMonedas.add(sub);
                }
                sb.append(row("Billetes:", fmt(totBilletes)))
                    .append(row("Monedas:", fmt(totMonedas)))
                    .append(rowTotal("Total denominaciones:", fmt(totBilletes.add(totMonedas))));
            }
        }

        // Datafonos (opcional)
        if (Boolean.TRUE.equals(opciones.getIncluirDatafonos()) && !datafonos.isEmpty()) {
            BigDecimal totDf = BigDecimal.ZERO;
            sb.append("<div class='stitle'>Datafonos declarados</div>");
            for (V2CierreDatafono d : datafonos) {
                sb.append(row(d.getDatafono() + ":", fmt(d.getMonto())));
                totDf = totDf.add(nvl(d.getMonto()));
            }
            sb.append(rowTotal("Total datafonos:", fmt(totDf)));
        }

        // Movimientos (opcional)
        if (Boolean.TRUE.equals(opciones.getIncluirMovimientos())) {
            List<V2MovimientoCaja> movs = movimientoRepo.findByTurnoIdOrderByFechaHoraDesc(turnoId);
            if (!movs.isEmpty()) {
                sb.append("<div class='stitle'>Movimientos de caja</div>");
                for (V2MovimientoCaja m : movs) {
                    String montoStr = m.esEntrada()
                        ? "<span style='color:#16a34a'>+" + fmt(m.getMonto()) + "</span>"
                        : "<span style='color:#dc2626'>-" + fmt(m.getMonto()) + "</span>";
                    String concepto = m.getConcepto() != null ? m.getConcepto() : m.getTipo();
                    sb.append(rowDif(concepto, montoStr));
                }
            }
        }

        // Observaciones
        if (turno.getObservacionesCierre() != null && !turno.getObservacionesCierre().isBlank()) {
            sb.append("<div class='stitle'>Observaciones</div>")
                .append("<div style='font-size:11px;padding:4px 0'>")
                .append(turno.getObservacionesCierre())
                .append("</div>");
        }

        if (cerrado) {
            sb.append("<div class='ok-box'>Arqueo procesado correctamente</div>");
        }

        sb.append("<div class='footer'>")
            .append("Impreso: ").append(fecha(java.time.LocalDateTime.now()))
            .append(" &nbsp;|&nbsp; NathBit POS")
            .append("</div>");

        sb.append("</body></html>");
        return sb.toString();
    }

    @Override
    public String generarHtmlSesion(Long sesionId, V2OpcionesReporteDTO opciones) {
        V2SesionCaja sesion = sesionRepo.findById(sesionId)
            .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

        String empresa  = sesion.getTerminal().getSucursal().getEmpresa().getNombreComercial();
        String sucursal = sesion.getTerminal().getSucursal().getNombre();
        String terminal = sesion.getTerminal().getNombre();

        List<V2TurnoCajero>          turnos       = turnoRepo.findBySesionId(sesionId);
        List<V2SesionPlataforma>     plataformas  = plataformaRepo.findBySesionId(sesionId);
        List<V2MovimientoCaja>       movimientos  = movimientoRepo.findBySesionIdOrderByFechaHoraDesc(sesionId);

        // Totales generales
        BigDecimal totEf   = BigDecimal.ZERO;
        BigDecimal totTc   = BigDecimal.ZERO;
        BigDecimal totSin  = BigDecimal.ZERO;
        BigDecimal totTb   = BigDecimal.ZERO;
        BigDecimal totCred = BigDecimal.ZERO;

        for (V2TurnoCajero t : turnos) {
            if (t.isCerrado()) {
                totEf   = totEf.add(nvl(t.getVentasEfectivo()));
                totTc   = totTc.add(nvl(t.getVentasTarjeta()));
                totSin  = totSin.add(nvl(t.getVentasSinpe()));
                totTb   = totTb.add(nvl(t.getVentasTransferencia()));
                totCred = totCred.add(nvl(t.getVentasCredito()));
            } else {
                totEf   = totEf.add(sesionService.calcularVentasTurno(t.getId(), "EFECTIVO"));
                totTc   = totTc.add(sesionService.calcularVentasTurno(t.getId(), "TARJETA"));
                totSin  = totSin.add(sesionService.calcularVentasTurno(t.getId(), "SINPE"));
                totTb   = totTb.add(sesionService.calcularVentasTurno(t.getId(), "TRANSFERENCIA"));
                totCred = totCred.add(sesionService.calcularVentasTurno(t.getId(), "CREDITO"));
            }
        }

        BigDecimal totVentas  = totEf.add(totTc).add(totSin).add(totTb).add(totCred);
        BigDecimal totEntradas = nvl(movimientoRepo.sumEntradasEfectivoBySesionId(sesionId));
        BigDecimal totSalidas  = nvl(movimientoRepo.sumSalidasBySesionId(sesionId));

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang='es'><head><meta charset='UTF-8'>")
            .append("<meta name='viewport' content='width=device-width,initial-scale=1'>")
            .append("<title>Reporte de Sesión</title>")
            .append(css())
            .append("</head><body>");

        // Header
        sb.append("<div class='header'>")
            .append("<h1>Reporte de Sesión de Caja</h1>")
            .append("<h2>").append(empresa).append(" — ").append(sucursal).append("</h2>")
            .append("<div class='meta'>")
            .append("Terminal: ").append(terminal)
            .append(" &nbsp;|&nbsp; Sesión #").append(sesionId).append("<br>")
            .append("Apertura: ").append(fecha(sesion.getFechaApertura()))
            .append(" &nbsp;|&nbsp; Cierre: ").append(fecha(sesion.getFechaCierre())).append("<br>")
            .append("Modo gaveta: ").append(sesion.getModoGaveta())
            .append(" &nbsp;|&nbsp; Fondo inicial: ").append(fmt(sesion.getMontoInicial()))
            .append("</div></div>");

        // Turnos
        sb.append("<div class='stitle'>Turnos (").append(turnos.size()).append(")</div>");

        for (V2TurnoCajero t : turnos) {
            String cajeroNombre = t.getUsuario().getNombre() + " " + t.getUsuario().getApellidos();
            boolean cerrado = t.isCerrado();

            BigDecimal ef   = cerrado ? nvl(t.getVentasEfectivo())      : sesionService.calcularVentasTurno(t.getId(), "EFECTIVO");
            BigDecimal tc   = cerrado ? nvl(t.getVentasTarjeta())       : sesionService.calcularVentasTurno(t.getId(), "TARJETA");
            BigDecimal sin  = cerrado ? nvl(t.getVentasSinpe())         : sesionService.calcularVentasTurno(t.getId(), "SINPE");
            BigDecimal tb   = cerrado ? nvl(t.getVentasTransferencia()) : sesionService.calcularVentasTurno(t.getId(), "TRANSFERENCIA");
            BigDecimal cred = cerrado ? nvl(t.getVentasCredito())       : sesionService.calcularVentasTurno(t.getId(), "CREDITO");
            BigDecimal total = ef.add(tc).add(sin).add(tb).add(cred);

            String estadoColor = cerrado ? "#1a1a1a" : "#92400e";
            String estadoLabel = cerrado ? "CERRADO" : "ACTIVO";

            sb.append("<div class='turno-card'>")
                .append("<div class='turno-nombre'>").append(cajeroNombre)
                .append(" <span style='font-size:9px;border:1px solid ").append(estadoColor)
                .append(";color:").append(estadoColor)
                .append(";padding:1px 6px;border-radius:8px'>").append(estadoLabel).append("</span>")
                .append("</div>")
                .append("<div class='turno-meta'>")
                .append("Turno #").append(t.getId())
                .append(" &nbsp;|&nbsp; ").append(fecha(t.getFechaInicio()))
                .append(" &rarr; ").append(fecha(t.getFechaFin()))
                .append("</div>")
                .append(row("Fondo al entrar:", fmt(t.getFondoInicio())))
                .append(row("Efectivo:", fmt(ef)))
                .append(row("Tarjeta:", fmt(tc)))
                .append(row("SINPE:", fmt(sin)))
                .append(row("Transferencia:", fmt(tb)));

            if (Boolean.TRUE.equals(opciones.getIncluirVentasCredito()) && cred.compareTo(BigDecimal.ZERO) > 0) {
                sb.append(row("Crédito:", fmt(cred)));
            }

            sb.append(rowTotal("Total ventas:", fmt(total)));

            if (cerrado) {
                sb.append("<div class='dash'></div>")
                    .append(row("Monto esperado:", fmt(t.getMontoEsperado())))
                    .append(row("Monto contado:", fmt(t.getMontoContado())))
                    .append(rowDif("Diferencia efectivo:", dif(t.getDifEfectivo())))
                    .append(row("Retiro:", fmt(t.getMontoRetirado())))
                    .append(rowTotal("Fondo dejado:", fmt(t.getFondoCaja())));
            }

            // Denominaciones por turno (opcional)
            if (Boolean.TRUE.equals(opciones.getIncluirDenominaciones()) && cerrado) {
                List<V2TurnoDenominacion> denoms = denominacionRepo.findByTurnoIdOrderByValorDesc(t.getId());
                if (!denoms.isEmpty()) {
                    sb.append("<div style='font-size:10px;font-weight:700;padding:4px 0;border-top:0.5px dashed #ccc;margin-top:4px'>Denominaciones</div>");
                    for (V2TurnoDenominacion d : denoms) {
                        BigDecimal sub = BigDecimal.valueOf((long) d.getValor() * d.getCantidad());
                        sb.append(row("&#8353;" + String.format("%,d", d.getValor())
                            + " &times; " + d.getCantidad() + ":", fmt(sub)));
                    }
                }
            }

            // Datafonos por turno (opcional)
            if (Boolean.TRUE.equals(opciones.getIncluirDatafonos())) {
                List<V2CierreDatafono> dfs = datafonoRepo.findByTurnoId(t.getId());
                if (!dfs.isEmpty()) {
                    BigDecimal totDf = BigDecimal.ZERO;
                    sb.append("<div style='font-size:10px;font-weight:700;padding:4px 0;border-top:0.5px dashed #ccc;margin-top:4px'>Datafonos</div>");
                    for (V2CierreDatafono d : dfs) {
                        sb.append(row(d.getDatafono() + ":", fmt(d.getMonto())));
                        totDf = totDf.add(nvl(d.getMonto()));
                    }
                    sb.append(rowTotal("Total datafonos:", fmt(totDf)));
                }
            }

            sb.append("</div>");
        }

        // Totales generales
        sb.append("<div class='stitle'>Totales generales</div>")
            .append("<div class='totales-grid'>")
            .append("<div class='total-box'><div class='lbl'>Efectivo</div><div class='val'>").append(fmt(totEf)).append("</div></div>")
            .append("<div class='total-box'><div class='lbl'>Tarjeta</div><div class='val'>").append(fmt(totTc)).append("</div></div>")
            .append("<div class='total-box'><div class='lbl'>SINPE</div><div class='val'>").append(fmt(totSin)).append("</div></div>")
            .append("<div class='total-box'><div class='lbl'>Transferencia</div><div class='val'>").append(fmt(totTb)).append("</div></div>")
            .append("</div>");

        if (Boolean.TRUE.equals(opciones.getIncluirFacturas())) {
            List<Factura> facturas        = facturaRepo.findByV2SesionId(sesionId);
            List<FacturaInterna> internas = facturaInternaRepo.findByV2SesionId(sesionId);

            log.info("Facturas v2 sesión {}: electrónicas={} internas={}",
                sesionId, facturas.size(), internas.size()); // ← agregar esto

            if (!facturas.isEmpty() || !internas.isEmpty()) {
                sb.append("<div class='stitle'>Resumen de facturas</div>");

                // Facturas electrónicas
                if (!facturas.isEmpty()) {
                    sb.append("<div style='font-size:10px;font-weight:700;padding:3px 0'>Electrónicas / Tiquetes</div>");
                    for (Factura f : facturas) {
                        String cliente = f.getNombreReceptor() != null
                            ? f.getNombreReceptor() : "Consumidor Final";
                        String simbolos = simbolosPago(f.getMediosPago(), false);
                        sb.append(row(
                            f.getConsecutivo() + " — " + cliente + simbolos,
                            fmt(f.getTotalComprobante())
                        ));
                    }

                }

                // Facturas internas
                if (!internas.isEmpty()) {
                    sb.append("<div style='font-size:10px;font-weight:700;padding:3px 0;margin-top:4px'>Internas</div>");
                    for (FacturaInterna fi : internas) {
                        String cliente = fi.getNombreCliente() != null
                            ? fi.getNombreCliente() : "Consumidor Final";
                        String simbolos = simbolosPago(fi.getMediosPago(), true);
                        sb.append(row(
                            fi.getNumero() + " — " + cliente + simbolos,
                            fmt(fi.getTotal())
                        ));
                    }
                }

                int totalDocs = facturas.size() + internas.size();
                sb.append(rowTotal("Total documentos: " + totalDocs, fmt(totVentas)));
            }
        }



        // Crédito en totales (opcional)
        if (Boolean.TRUE.equals(opciones.getIncluirVentasCredito()) && totCred.compareTo(BigDecimal.ZERO) > 0) {
            sb.append("<div style='margin-top:6px'>")
                .append(row("Crédito total sesión:", fmt(totCred)))
                .append("</div>");
        }

        sb.append("<div style='margin-top:6px'>")
            .append(row("+ Entradas adicionales:",
                "<span style='color:#16a34a'>+" + fmt(totEntradas) + "</span>"))
            .append(row("- Salidas / retiros:",
                "<span style='color:#dc2626'>-" + fmt(totSalidas) + "</span>"))
            .append(rowTotal("Total ventas sesión:", fmt(totVentas)))
            .append("</div>");

        // Plataformas (opcional)
        if (Boolean.TRUE.equals(opciones.getIncluirPlataformas()) && !plataformas.isEmpty()) {
            BigDecimal totPlat = BigDecimal.ZERO;
            sb.append("<div class='stitle'>Plataformas digitales</div>");
            for (V2SesionPlataforma p : plataformas) {
                sb.append(row(p.getPlataformaNombre() + " (" + p.getCantidadPedidos() + " pedidos):",
                    fmt(p.getTotalVentas())));
                totPlat = totPlat.add(nvl(p.getTotalVentas()));
            }
            sb.append(rowTotal("Total plataformas:", fmt(totPlat)));
        }

        // Movimientos (opcional)
        if (Boolean.TRUE.equals(opciones.getIncluirMovimientos()) && !movimientos.isEmpty()) {
            sb.append("<div class='stitle'>Movimientos de caja</div>");
            for (V2MovimientoCaja m : movimientos) {
                String cajero   = m.getUsuario().getNombre() + " " + m.getUsuario().getApellidos();
                String concepto = m.getConcepto() != null ? m.getConcepto() : m.getTipo();
                String montoStr = m.esEntrada()
                    ? "<span style='color:#16a34a'>+" + fmt(m.getMonto()) + "</span>"
                    : "<span style='color:#dc2626'>-" + fmt(m.getMonto()) + "</span>";
                sb.append("<div class='row'>")
                    .append("<span>").append(concepto).append(" — ").append(cajero).append("</span>")
                    .append("<span>").append(montoStr).append("</span>")
                    .append("</div>");
            }
            sb.append(row("Total entradas:", "<span style='color:#16a34a'>+" + fmt(totEntradas) + "</span>"))
                .append(rowTotal("Total salidas:", "<span style='color:#dc2626'>-" + fmt(totSalidas) + "</span>"));
        }

        // Footer
        sb.append("<div class='footer'>")
            .append("Impreso: ").append(fecha(java.time.LocalDateTime.now()))
            .append(" &nbsp;|&nbsp; NathBit POS")
            .append("</div>");

        sb.append("</body></html>");
        return sb.toString();
    }

    // =========================================================
    // HELPERS DE FILAS
    // =========================================================

    private String row(String label, String value) {
        return "<div class='row'><span>" + label + "</span><span>" + value + "</span></div>";
    }

    private String rowTotal(String label, String value) {
        return "<div class='row total'><span>" + label + "</span><span>" + value + "</span></div>";
    }

    private String rowDif(String label, String value) {
        return "<div class='row' style='font-weight:700'><span>" + label + "</span><span>" + value + "</span></div>";
    }

    private String simbolosPago(List<?> mediosPago, boolean esInterna) {
        if (mediosPago == null || mediosPago.isEmpty()) return "";
        StringBuilder s = new StringBuilder();
        for (Object mp : mediosPago) {
            String tipo = esInterna
                ? ((com.snnsoluciones.backnathbitpos.entity.FacturaInternaMedioPago) mp).getTipo()
                : ((com.snnsoluciones.backnathbitpos.entity.FacturaMedioPago) mp).getMedioPago().name();
            if (tipo == null) continue;
            String t = tipo.toUpperCase();
            if (t.contains("EFECTIVO"))       s.append("(E)");
            else if (t.contains("TARJETA"))   s.append("(TC)");
            else if (t.contains("SINPE"))     s.append("(S)");
            else if (t.contains("TRANSFER"))  s.append("(TB)");
            else if (t.contains("PLATAFORM")) s.append("(P)");
            else if (t.contains("CREDITO"))   s.append("(C)");
            else                              s.append("(O)");
        }
        return s.length() > 0 ? " <span style='font-size:9px;color:#666'>" + s + "</span>" : "";
    }
}