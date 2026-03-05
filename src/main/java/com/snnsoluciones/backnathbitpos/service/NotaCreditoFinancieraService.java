package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.factura.*;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.mh.Moneda;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Genera una Nota de Crédito Financiera (código referencia 09) por cada abono
 * registrado contra una Factura Electrónica a crédito.
 * Solo aplica cuando la CuentaPorCobrar apunta a una Factura Electrónica.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotaCreditoFinancieraService {

    private static final int    SCALE   = 5;
    private static final String VERSION = "4.4";

    private final FacturaService facturaService;

    /**
     * Punto de entrada. Llamar desde PagoService después de persistir el Pago.
     */
    public void emitirPorAbono(Pago pago) {

        CuentaPorCobrar cuenta = pago.getCuentaPorCobrar();

        // Solo facturas electrónicas van a Hacienda
        if (cuenta.getFactura() == null) {
            log.debug("Pago {} corresponde a factura interna, no genera NC financiera",
                pago.getId());
            return;
        }

        Factura original = cuenta.getFactura();

        // Solo facturas aceptadas por Hacienda
        if (original.getClave() == null || original.getClave().isBlank()) {
            log.warn("Factura {} no tiene clave, no se puede emitir NC financiera",
                original.getId());
            return;
        }

        try {
            CrearFacturaRequest req = construirRequest(pago, original);
            Factura nc = facturaService.crear(req);
            log.info("NC financiera {} emitida para pago {} (abono ₡{})",
                nc.getClave(), pago.getId(), pago.getMonto());

        } catch (Exception e) {
            // No romper el flujo del pago si falla la NC
            log.error("Error emitiendo NC financiera para pago {}: {}",
                pago.getId(), e.getMessage(), e);
        }
    }

    // =========================================================
    // Construcción del request
    // =========================================================

    private CrearFacturaRequest construirRequest(Pago pago, Factura original) {

        BigDecimal montoAbono     = pago.getMonto();
        BigDecimal totalOriginal  = original.getTotalComprobante();

        // Proporción del abono respecto al total de la factura original
        BigDecimal proporcion = montoAbono.divide(totalOriginal, 10, RoundingMode.HALF_UP);

        // IVA proporcional al abono
        BigDecimal ivaNC  = original.getTotalImpuesto()
                                    .multiply(proporcion)
                                    .setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal baseNC = montoAbono.subtract(ivaNC)
                                      .setScale(SCALE, RoundingMode.HALF_UP);

        // Fecha en zona CR
        String fechaCR = ZonedDateTime.now(ZoneId.of("America/Costa_Rica"))
                                      .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        CrearFacturaRequest req = new CrearFacturaRequest();

        // ── Tipo y contexto ──────────────────────────────────────────────
        req.setTipoDocumento(TipoDocumento.NOTA_CREDITO);
        req.setUsuarioId(pago.getCajero().getId());
        req.setSesionCajaId(
            original.getSesionCaja() != null ? original.getSesionCaja().getId() : null);
        req.setTerminalId(
            original.getTerminal() != null ? original.getTerminal().getId() : null);

        // ── Cliente ──────────────────────────────────────────────────────
        req.setClienteId(
            original.getCliente() != null ? original.getCliente().getId() : null);
        req.setNombreReceptor(original.getNombreReceptor());
        req.setEmailReceptor(original.getEmailReceptor());
        req.setActividadReceptor(original.getActividadReceptor());

        // ── Datos comerciales ────────────────────────────────────────────
        req.setCondicionVenta("01");   // CONTADO: la NC financiera siempre es contado
        req.setPlazoCredito(null);
        req.setMoneda(original.getMoneda() != null ? original.getMoneda() : Moneda.CRC);
        req.setTipoCambio(
            original.getTipoCambio() != null ? original.getTipoCambio() : BigDecimal.ONE);
        req.setSituacionComprobante("NORMAL");
        req.setObservaciones("NC Financiera - Abono a factura " + original.getConsecutivo());
        req.setVersionCatalogos(VERSION);
        req.setVuelto(null);

        // ── Descuento global: ninguno ─────────────────────────────────────
        req.setDescuentoGlobalPorcentaje(null);
        req.setMontoDescuentoGlobal(null);
        req.setMotivoDescuentoGlobal(null);

        // ── Referencia a la factura original (código 09) ─────────────────
        InformacionReferenciaDto ref = new InformacionReferenciaDto();
        ref.setTipoDoc(original.getTipoDocumento().getCodigo());
        ref.setNumero(original.getClave());
        ref.setFechaEmision(original.getFechaEmision());
        ref.setCodigo("09");   // Nota de crédito financiera
        ref.setRazon("Abono recibido - Recibo " + pago.getNumeroRecibo());
        req.setInformacionReferencia(List.of(ref));
        req.setFacturaReferenciaId(original.getId());

        // ── Detalle: una línea usando el primer producto de la original ───
        req.setDetalles(construirDetalle(original, baseNC, ivaNC));

        // ── Otros cargos: ninguno ─────────────────────────────────────────
        req.setOtrosCargos(new ArrayList<>());

        // ── Medio de pago: espejo del pago ────────────────────────────────
        MedioPagoRequest mp = new MedioPagoRequest();
        mp.setMedioPago(pago.getMedioPago().getCodigo());
        mp.setMonto(montoAbono);
        mp.setReferencia(pago.getReferencia());
        req.setMediosPago(List.of(mp));

        // ── Totales ───────────────────────────────────────────────────────
        asignarTotales(req, original, baseNC, ivaNC, montoAbono, proporcion);

        return req;
    }

    // ─── Detalle proporcional ──────────────────────────────────────────────

    private List<DetalleFacturaRequest> construirDetalle(
            Factura original, BigDecimal baseNC, BigDecimal ivaNC) {

        // Tomamos el primer detalle de la factura original como referencia
        FacturaDetalle lineaRef = original.getDetalles().isEmpty()
                ? null : original.getDetalles().get(0);

        DetalleFacturaRequest det = new DetalleFacturaRequest();
        det.setNumeroLinea(1);
        det.setCantidad(BigDecimal.ONE);
        det.setPrecioUnitario(baseNC);
        det.setUnidadMedida("Sp");  // Servicio profesional — genérico para NC financiera

        // Producto y CABYS de la línea original
        if (lineaRef != null) {
            det.setProductoId(lineaRef.getProducto().getId());
            det.setCodigoCabys(
                lineaRef.getCodigoCabys() != null ? lineaRef.getCodigoCabys() : "0000000000000");
            det.setEsServicio(lineaRef.getEsServicio());
        } else {
            // Fallback: sin producto de referencia
            det.setProductoId(null);
            det.setCodigoCabys("0000000000000");
            det.setEsServicio(true);
        }

        det.setDescripcionPersonalizada(
            "Abono a crédito - Factura " + original.getConsecutivo());
        det.setAplicaImpuestoServicio(false);
        det.setMontoTotal(baseNC);
        det.setMontoDescuento(BigDecimal.ZERO);
        det.setSubtotal(baseNC);
        det.setMontoImpuesto(ivaNC);
        det.setMontoTotalLinea(baseNC.add(ivaNC).setScale(SCALE, RoundingMode.HALF_UP));
        det.setDescuentos(new ArrayList<>());

        // Impuesto proporcional
        det.setImpuestos(construirImpuesto(original, baseNC, ivaNC));

        return List.of(det);
    }

    private List<ImpuestoLineaRequest> construirImpuesto(
            Factura original, BigDecimal baseNC, BigDecimal ivaNC) {

        // Leer código de tarifa IVA de la primera línea original
        String codigoTarifa = "08"; // default: 13%
        BigDecimal tarifa   = new BigDecimal("13.00");

        if (!original.getDetalles().isEmpty()) {
            FacturaDetalle primeraLinea = original.getDetalles().get(0);
            if (!primeraLinea.getImpuestos().isEmpty()) {
                FacturaDetalleImpuesto imp = primeraLinea.getImpuestos().get(0);
                if (imp.getCodigoTarifaIVA() != null) codigoTarifa = imp.getCodigoTarifaIVA();
                if (imp.getTarifa() != null)           tarifa       = imp.getTarifa();
            }
        }

        ImpuestoLineaRequest iva = new ImpuestoLineaRequest();
        iva.setCodigoImpuesto("01");         // IVA
        iva.setCodigoTarifaIVA(codigoTarifa);
        iva.setTarifa(tarifa);
        iva.setBaseImponible(baseNC);
        iva.setMontoImpuesto(ivaNC);
        iva.setTieneExoneracion(false);
        iva.setMontoExoneracion(BigDecimal.ZERO);
        iva.setImpuestoNeto(ivaNC);

        return List.of(iva);
    }

    // ─── Totales ───────────────────────────────────────────────────────────

    private void asignarTotales(CrearFacturaRequest req, Factura original,
                                BigDecimal baseNC, BigDecimal ivaNC,
                                BigDecimal montoAbono, BigDecimal proporcion) {

        boolean esServicio = !original.getDetalles().isEmpty()
                             && original.getDetalles().get(0).getEsServicio();

        BigDecimal cero = BigDecimal.ZERO;

        req.setTotalServiciosGravados(     esServicio  ? baseNC : cero);
        req.setTotalServiciosExentos(      cero);
        req.setTotalServiciosExonerados(   cero);
        req.setTotalServiciosNoSujetos(    cero);
        req.setTotalMercanciasGravadas(   !esServicio  ? baseNC : cero);
        req.setTotalMercanciasExentas(     cero);
        req.setTotalMercanciasExoneradas(  cero);
        req.setTotalMercanciasNoSujetas(   cero);
        req.setTotalGravado(               baseNC);
        req.setTotalExento(                cero);
        req.setTotalExonerado(             cero);
        req.setTotalNoSujeto(              cero);
        req.setTotalVenta(                 baseNC);
        req.setTotalDescuentos(            cero);
        req.setTotalVentaNeta(             baseNC);
        req.setTotalImpuesto(              ivaNC);
        req.setTotalIVADevuelto(           cero);
        req.setTotalOtrosCargos(           cero);
        req.setTotalComprobante(
            baseNC.add(ivaNC).setScale(SCALE, RoundingMode.HALF_UP));

        // Resumen de impuestos
        ResumenImpuestoRequest resumen = ResumenImpuestoRequest.builder()
            .codigoImpuesto("01")
            .codigoTarifaIVA(obtenerCodigoTarifa(original))
            .totalBaseImponible(baseNC)
            .totalMontoImpuesto(ivaNC)
            .totalMontoExoneracion(cero)
            .totalImpuestoNeto(ivaNC)
            .cantidadLineas(1)
            .build();

        req.setResumenImpuestos(List.of(resumen));
    }

    private String obtenerCodigoTarifa(Factura original) {
        if (!original.getDetalles().isEmpty()) {
            FacturaDetalle det = original.getDetalles().get(0);
            if (!det.getImpuestos().isEmpty()
                && det.getImpuestos().get(0).getCodigoTarifaIVA() != null) {
                return det.getImpuestos().get(0).getCodigoTarifaIVA();
            }
        }
        return "08"; // 13% por defecto
    }
}