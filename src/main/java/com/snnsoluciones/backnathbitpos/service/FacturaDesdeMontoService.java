package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.factura.CrearFacturaRequest;
import com.snnsoluciones.backnathbitpos.dto.factura.DetalleFacturaRequest;
import com.snnsoluciones.backnathbitpos.dto.factura.ImpuestoLineaRequest;
import com.snnsoluciones.backnathbitpos.dto.factura.MedioPagoRequest;
import com.snnsoluciones.backnathbitpos.dto.factura.ResumenImpuestoRequest;
import com.snnsoluciones.backnathbitpos.dto.pago.FacturaDesdeMontoRequest;
import com.snnsoluciones.backnathbitpos.dto.pago.FacturaDesdeMontoResponse;
import com.snnsoluciones.backnathbitpos.dto.pago.PagoRequest;
import com.snnsoluciones.backnathbitpos.entity.Factura;
import com.snnsoluciones.backnathbitpos.entity.Producto;
import com.snnsoluciones.backnathbitpos.enums.mh.CodigoTarifaIVA;
import com.snnsoluciones.backnathbitpos.enums.mh.CondicionVenta;
import com.snnsoluciones.backnathbitpos.enums.mh.Moneda;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoImpuesto;
import com.snnsoluciones.backnathbitpos.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FacturaDesdeMontoService {

    private final FacturaService facturaService;
    private final ProductoRepository productoRepository;

    // ===== Config MVP =====
    private static final BigDecimal MIN_TICKET = new BigDecimal("1500");
    private static final BigDecimal MAX_TICKET = new BigDecimal("7500");
    private static final BigDecimal MARGEN = new BigDecimal("100000"); // ±100 mil
    private static final BigDecimal IVA_PORC = new BigDecimal("13");    // 13 (%)
    private static final BigDecimal IVA_FACTOR = new BigDecimal("0.13"); // 0.13
    private static final int SCALE_MONEDA = 2;

    // ===== Cabecera por defecto (colocá tus valores reales) =====
    private static final TipoDocumento TIPO_DOC = TipoDocumento.TIQUETE_ELECTRONICO;
    private static final Moneda MONEDA = Moneda.CRC;
    private static final BigDecimal TIPO_CAMBIO = BigDecimal.ONE; // CRC
    private static final String CONDICION_VENTA = CondicionVenta.CONTADO.name();
    private static final String SITUACION_COMPROBANTE_NORMAL = "1"; // según tu catálogo
    private static final String VERSION_CATALOGOS = "MH-4.4-2025-08-21";

    // Si preferís inyectarlos, cambiá estos getters por servicio/config
    private Long getTerminalIdDefault() { return 1L; }        // TODO: tu terminal
    private Long getSesionCajaIdDefault() { return 3L; }      // TODO: tu sesión de caja vigente
    private Long getUsuarioIdDefault() { return 6L; }         // TODO: usuario que ejecuta

    @Transactional
    public FacturaDesdeMontoResponse generar(FacturaDesdeMontoRequest request) {
        BigDecimal objetivo = request.getMontoTotal().setScale(SCALE_MONEDA, RoundingMode.HALF_UP);
        BigDecimal acumulado = BigDecimal.ZERO;

        List<FacturaDesdeMontoResponse.DocumentoGenerado> docs = new ArrayList<>();
        Random rnd = new Random();

        // Productos disponibles para “asociar” de forma aleatoria
        List<Producto> productos = productoRepository.findAll();
        if (productos.isEmpty()) {
            throw new IllegalStateException("No hay productos disponibles para generar tiquetes.");
        }

        // Cola de pagos a prorratear por ticket
        List<MedioPagoRequest> pagosPendientes = clonarPagos(request.getPagos());

        while (acumulado.compareTo(objetivo.add(MARGEN)) < 0) {
            // ¿ya estamos dentro del margen? salir
            BigDecimal faltante = objetivo.subtract(acumulado);
            if (faltante.abs().compareTo(MARGEN) <= 0) break;

            // monto aleatorio sugerido para este ticket
            BigDecimal ticketMonto = MIN_TICKET.add(
                BigDecimal.valueOf(rnd.nextDouble())
                    .multiply(MAX_TICKET.subtract(MIN_TICKET))
            ).setScale(SCALE_MONEDA, RoundingMode.HALF_UP);

            // ajustar si nos pasamos
            if (ticketMonto.compareTo(faltante) > 0) ticketMonto = faltante;

            // Elegimos 1 producto random (KISS). Precio de línea = ticketMonto (base sin IVA).
            Producto prod = productos.get(rnd.nextInt(productos.size()));

            // Armar el CrearFacturaRequest completo
            CrearFacturaRequest cfReq = buildCrearFacturaRequest(prod, ticketMonto);

            // Prorratear medios de pago para este ticket por el totalComprobante calculado
            distribuirPagosParaTicket(cfReq, pagosPendientes);

            // Crear la factura con el service fiscal ya existente
            Factura factura = facturaService.crear(cfReq);

            // Resumen para devolver
            FacturaDesdeMontoResponse.DocumentoGenerado doc = new FacturaDesdeMontoResponse.DocumentoGenerado();
            doc.setClave(factura.getClave());
            doc.setTotal(factura.getTotalComprobante());
            doc.setEstado(factura.getEstado().name());
            docs.add(doc);

            acumulado = acumulado.add(factura.getTotalComprobante());
        }

        // Respuesta final
        FacturaDesdeMontoResponse resp = new FacturaDesdeMontoResponse();
        resp.setDocumentos(docs);

        FacturaDesdeMontoResponse.Resumen resumen = new FacturaDesdeMontoResponse.Resumen();
        resumen.setTotalFacturado(acumulado);
        resumen.setDiferencia(acumulado.subtract(objetivo));
        resp.setResumen(resumen);

        return resp;
    }

    // ===================== Builders & Helpers =====================

    private CrearFacturaRequest buildCrearFacturaRequest(Producto prod, BigDecimal montoSinIVA) {
        // 1) Detalle (1 línea, cantidad 1, precio SIN IVA, IVA 13%)
        DetalleFacturaRequest detalle = buildDetalle(prod, montoSinIVA);

        // 2) Cabecera mínima obligatoria
        CrearFacturaRequest req = new CrearFacturaRequest();
        req.setTipoDocumento(TIPO_DOC);
        req.setTerminalId(getTerminalIdDefault());      // TODO: reemplazar por los reales
        req.setSesionCajaId(getSesionCajaIdDefault());  // TODO
        req.setUsuarioId(getUsuarioIdDefault());        // TODO
        req.setClienteId(null);                         // TE anónimo
        req.setCondicionVenta(CONDICION_VENTA);
        req.setMoneda(MONEDA);
        req.setTipoCambio(TIPO_CAMBIO);
        req.setSituacionComprobante(SITUACION_COMPROBANTE_NORMAL);
        req.setVersionCatalogos(VERSION_CATALOGOS);

        // 3) Detalles
        req.setDetalles(List.of(detalle));

        // 4) Medios de pago se asignan luego (prorrateo)
        // req.setMediosPago(...)

        // 5) Totales + Resumen de impuestos (v4.4)
        rellenarTotalesTE(req);
        rellenarResumenImpuestos(req);

        return req;
    }

    private DetalleFacturaRequest buildDetalle(Producto prod, BigDecimal montoSinIVA) {
        BigDecimal cantidad = BigDecimal.ONE;

        BigDecimal baseImponible = montoSinIVA.multiply(cantidad)
            .setScale(SCALE_MONEDA, RoundingMode.HALF_UP);

        BigDecimal montoImpuesto = baseImponible.multiply(IVA_FACTOR)
            .setScale(SCALE_MONEDA, RoundingMode.HALF_UP);

        BigDecimal totalLinea = baseImponible.add(montoImpuesto)
            .setScale(SCALE_MONEDA, RoundingMode.HALF_UP);

        DetalleFacturaRequest det = new DetalleFacturaRequest();
        det.setProductoId(prod.getId());
        det.setCantidad(cantidad);
        det.setPrecioUnitario(montoSinIVA);          // SIN IVA
        det.setMontoTotalLinea(totalLinea);          // si tu DTO lo tiene, ayuda al cuadre

        ImpuestoLineaRequest iva = new ImpuestoLineaRequest(
            TipoImpuesto.IVA.getCodigo(),                        // p.ej. "01"
            CodigoTarifaIVA.TARIFA_GENERAL_13.getCodigo(),       // p.ej. "08"
            IVA_PORC,                                            // 13 (%)
            montoImpuesto,                                       // monto del impuesto
            baseImponible,                                       // base imponible
            false,                                               // exonerado?
            BigDecimal.ZERO,                                     // % exoneración
            montoImpuesto,                                       // neto (sin exonerar)
            null,                                                // datos exoneración
            false,                                               // devolutivo?
            BigDecimal.ZERO                                      // IVA devuelto línea
        );

        det.setImpuestos(List.of(iva));
        return det;
    }

    private void rellenarTotalesTE(CrearFacturaRequest req) {
        BigDecimal totalBase = BigDecimal.ZERO;
        BigDecimal totalImpuesto = BigDecimal.ZERO;

        for (DetalleFacturaRequest d : req.getDetalles()) {
            BigDecimal base = d.getPrecioUnitario().multiply(d.getCantidad())
                .setScale(SCALE_MONEDA, RoundingMode.HALF_UP);
            totalBase = totalBase.add(base);

            if (d.getImpuestos() != null) {
                for (ImpuestoLineaRequest imp : d.getImpuestos()) {
                    totalImpuesto = totalImpuesto.add(imp.getMontoImpuesto());
                }
            }
        }

        // Si todos son gravados 13% y sin descuentos/otros cargos
        req.setTotalServiciosGravados(BigDecimal.ZERO);
        req.setTotalServiciosExentos(BigDecimal.ZERO);
        req.setTotalServiciosExonerados(BigDecimal.ZERO);
        req.setTotalServiciosNoSujetos(BigDecimal.ZERO);

        req.setTotalMercanciasGravadas(totalBase);
        req.setTotalMercanciasExentas(BigDecimal.ZERO);
        req.setTotalMercanciasExoneradas(BigDecimal.ZERO);
        req.setTotalMercanciasNoSujetas(BigDecimal.ZERO);

        req.setTotalGravado(totalBase);
        req.setTotalExento(BigDecimal.ZERO);
        req.setTotalExonerado(BigDecimal.ZERO);

        req.setTotalVenta(totalBase);
        req.setTotalDescuentos(BigDecimal.ZERO);
        req.setTotalVentaNeta(totalBase);

        req.setTotalImpuesto(totalImpuesto);
        req.setTotalIVADevuelto(BigDecimal.ZERO);
        req.setTotalOtrosCargos(BigDecimal.ZERO);
        req.setTotalNoSujeto(BigDecimal.ZERO);

        BigDecimal totalComprobante = totalBase.add(totalImpuesto)
            .setScale(SCALE_MONEDA, RoundingMode.HALF_UP);
        req.setTotalComprobante(totalComprobante);
    }

    private void rellenarResumenImpuestos(CrearFacturaRequest req) {
        BigDecimal totalBase = BigDecimal.ZERO;
        BigDecimal totalImpuesto = BigDecimal.ZERO;
        int lineas = 0;

        for (DetalleFacturaRequest d : req.getDetalles()) {
            BigDecimal base = d.getPrecioUnitario()
                .multiply(d.getCantidad())
                .setScale(SCALE_MONEDA, RoundingMode.HALF_UP);
            totalBase = totalBase.add(base);

            if (d.getImpuestos() != null) {
                for (ImpuestoLineaRequest imp : d.getImpuestos()) {
                    totalImpuesto = totalImpuesto.add(imp.getMontoImpuesto());
                }
            }
            lineas++;
        }

        ResumenImpuestoRequest resumen = ResumenImpuestoRequest.builder()
            .codigoImpuesto(TipoImpuesto.IVA.getCodigo())                   // ej. "01"
            .codigoTarifaIVA(CodigoTarifaIVA.TARIFA_GENERAL_13.getCodigo()) // ej. "08"
            .totalBaseImponible(totalBase)
            .totalMontoImpuesto(totalImpuesto)
            .totalMontoExoneracion(BigDecimal.ZERO)
            .totalImpuestoNeto(totalImpuesto)  // sin exoneración
            .cantidadLineas(lineas)
            .build();

        req.setResumenImpuestos(List.of(resumen));
    }

    // ===== Prorrateo de pagos por ticket =====
    private void distribuirPagosParaTicket(CrearFacturaRequest req, List<MedioPagoRequest> pagosPendientes) {
        BigDecimal porCubrir = req.getTotalComprobante();
        List<MedioPagoRequest> pagosDelTicket = new ArrayList<>();

        Iterator<MedioPagoRequest> it = pagosPendientes.iterator();
        while (it.hasNext() && porCubrir.compareTo(BigDecimal.ZERO) > 0) {
            MedioPagoRequest mp = it.next();
            BigDecimal disponible = mp.getMonto();

            if (disponible.compareTo(BigDecimal.ZERO) <= 0) {
                it.remove();
                continue;
            }

            BigDecimal usar = disponible.min(porCubrir).setScale(SCALE_MONEDA, RoundingMode.HALF_UP);

            // crear fracción para este ticket
            MedioPagoRequest fraccion = new MedioPagoRequest();
            fraccion.setMedioPago(mp.getMedioPago());
            fraccion.setMonto(usar);
            pagosDelTicket.add(fraccion);

            // actualizar pendiente
            mp.setMonto(disponible.subtract(usar).setScale(SCALE_MONEDA, RoundingMode.HALF_UP));
            porCubrir = porCubrir.subtract(usar).setScale(SCALE_MONEDA, RoundingMode.HALF_UP);

            if (mp.getMonto().compareTo(BigDecimal.ZERO) == 0) it.remove();
        }

        // En caso extremo de redondeos, si quedó porCubrir > 0, agregalo al primer medio por simplicidad
        if (porCubrir.compareTo(BigDecimal.ZERO) > 0 && !pagosDelTicket.isEmpty()) {
            MedioPagoRequest primero = pagosDelTicket.get(0);
            primero.setMonto(primero.getMonto().add(porCubrir).setScale(SCALE_MONEDA, RoundingMode.HALF_UP));
            porCubrir = BigDecimal.ZERO;
        }

        req.setMediosPago(pagosDelTicket);
    }

    private List<MedioPagoRequest> clonarPagos(List<MedioPagoRequest> pagos) {
        List<MedioPagoRequest> copia = new ArrayList<>();
        for (MedioPagoRequest p : pagos) {
            MedioPagoRequest c = new MedioPagoRequest();
            c.setMedioPago(p.getMedioPago());
            c.setMonto(p.getMonto() != null
                ? p.getMonto().setScale(SCALE_MONEDA, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
            copia.add(c);
        }
        return copia;
    }
}