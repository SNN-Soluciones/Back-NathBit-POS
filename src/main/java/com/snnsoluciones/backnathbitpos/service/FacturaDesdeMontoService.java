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
    private static final BigDecimal MARGEN = new BigDecimal("1000"); // ±100 mil
    private static final BigDecimal IVA_PORC = new BigDecimal("13");    // 13 (%)
    private static final BigDecimal IVA_FACTOR = new BigDecimal("0.13"); // 0.13
    private static final int SCALE_MONEDA = 2;

    // ===== Cabecera por defecto (colocá tus valores reales) =====
    private static final TipoDocumento TIPO_DOC = TipoDocumento.TIQUETE_ELECTRONICO;
    private static final Moneda MONEDA = Moneda.CRC;
    private static final BigDecimal TIPO_CAMBIO = BigDecimal.ONE; // CRC
    private static final String CONDICION_VENTA = CondicionVenta.CONTADO.getCodigo();
    private static final String VERSION_CATALOGOS = "MH-4.4-2025-08-21";

    // Si preferís inyectarlos, cambiá estos getters por servicio/config
    private Long getTerminalIdDefault() { return 1L; }        // TODO: tu terminal
    private Long getSesionCajaIdDefault() { return 738L; }      // TODO: tu sesión de caja vigente
    private Long getUsuarioIdDefault() { return 8L; }         // TODO: usuario que ejecuta

    // ===== Callbacks para modo asíncrono =====
    @FunctionalInterface public interface OnEach { void tick(); }
    @FunctionalInterface public interface ShouldStop { boolean get(); }
    @FunctionalInterface public interface Sleeper { void sleep(Random rnd) throws InterruptedException; }

    /**
     * NUEVO: Generación ticket-por-ticket, amigable para ejecución asíncrona.
     * No anotar @Transactional aquí. Cada factura se persiste en su propia transacción dentro de facturaService.crear(...).
     */
    public FacturaDesdeMontoResponse generarTicketPorTicket(
        FacturaDesdeMontoRequest request,
        OnEach onEach,
        ShouldStop shouldStop,
        Sleeper sleeper
    ) {
        BigDecimal objetivo = request.getMontoTotal().setScale(SCALE_MONEDA, RoundingMode.HALF_UP);
        BigDecimal acumulado = BigDecimal.ZERO;

        List<FacturaDesdeMontoResponse.DocumentoGenerado> docs = new ArrayList<>();
        Random rnd = new Random();

        // Productos disponibles - IMPORTANTE: Cargar con EAGER las relaciones necesarias
        List<Producto> productos = cargarProductosConRelaciones();
        if (productos.isEmpty()) {
            throw new IllegalStateException("No hay productos disponibles para generar tiquetes.");
        }

        // Cola de pagos a prorratear
        List<MedioPagoRequest> pagosPendientes = clonarPagos(request.getPagos());

        while (acumulado.compareTo(objetivo.add(MARGEN)) < 0) {
            if (shouldStop != null && shouldStop.get()) break;

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

            // 1 producto random. Precio línea = ticketMonto (base sin IVA).
            Producto prod = productos.get(rnd.nextInt(productos.size()));

            // Armar request
            CrearFacturaRequest cfReq = buildCrearFacturaRequest(prod, ticketMonto);

            // Prorratear pagos según total del comprobante
            distribuirPagosParaTicket(cfReq, pagosPendientes);

            // Crear la factura (IMPORTANTE: que este método sea @Transactional en su propio service)
            Factura factura = facturaService.crear(cfReq);

            // Resumen
            FacturaDesdeMontoResponse.DocumentoGenerado doc = new FacturaDesdeMontoResponse.DocumentoGenerado();
            doc.setClave(factura.getClave());
            doc.setTotal(factura.getTotalComprobante());
            doc.setEstado(factura.getEstado().name());
            docs.add(doc);

            // >>> SUMAR al acumulado <<<
            acumulado = acumulado.add(factura.getTotalComprobante()).setScale(SCALE_MONEDA, RoundingMode.HALF_UP);

            if (onEach != null) onEach.tick();

            // Dormir si aún falta
            if (acumulado.compareTo(objetivo.add(MARGEN)) < 0 && sleeper != null) {
                try { sleeper.sleep(rnd); }
                catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
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

    /**
     * Método auxiliar para cargar productos con sus relaciones EAGER
     * Esto evita LazyInitializationException en contexto asíncrono
     */
    @Transactional(readOnly = true)
    public List<Producto> cargarProductosConRelaciones() {
        // Opción 1: Query personalizada con JPQL
         return productoRepository.findAllWithRelaciones();

//        // Opción 2: Cargar y forzar inicialización
//        List<Producto> productos = productoRepository.findAll();
//        // Forzar carga de relaciones necesarias
//        for (Producto p : productos) {
//            if (p.getEmpresaCabys() != null && p.getEmpresaCabys().getCodigoCabys() != null) {
//                // Forzar carga del código CABYS
//                p.getEmpresaCabys().getCodigoCabys().getCodigo();
//            }
//            // Cargar cualquier otra relación lazy que necesites
//        }
//        return productos;
    }

    @Transactional
    public FacturaDesdeMontoResponse generar(FacturaDesdeMontoRequest request) {
        BigDecimal objetivo = request.getMontoTotal().setScale(SCALE_MONEDA, RoundingMode.HALF_UP);
        BigDecimal acumulado = BigDecimal.ZERO;

        List<FacturaDesdeMontoResponse.DocumentoGenerado> docs = new ArrayList<>();
        Random rnd = new Random();

        // Productos disponibles para "asociar" de forma aleatoria
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
        CrearFacturaRequest req = new CrearFacturaRequest();

        // Identificación
        req.setTipoDocumento(TIPO_DOC);
        req.setTerminalId(getTerminalIdDefault());
        req.setSesionCajaId(getSesionCajaIdDefault());
        req.setUsuarioId(getUsuarioIdDefault());

        // Cliente (null para tiquete anónimo)
        req.setClienteId(null);
        req.setNombreReceptor(null);
        req.setEmailReceptor(null);
        req.setActividadReceptor(null);

        // Datos comerciales
        req.setCondicionVenta(CONDICION_VENTA);
        req.setPlazoCredito(null); // Solo si es crédito
        req.setMoneda(MONEDA);
        req.setTipoCambio(TIPO_CAMBIO);
        req.setSituacionComprobante("NORMAL"); // o "1" según tu enum
        req.setObservaciones("Generado automáticamente");
        req.setVuelto(null); // Solo si hay vuelto

        // Descuento global (no aplicamos)
        req.setDescuentoGlobalPorcentaje(null);
        req.setMontoDescuentoGlobal(null);
        req.setMotivoDescuentoGlobal(null);

        // Detalles
        DetalleFacturaRequest detalle = buildDetalle(prod, montoSinIVA);
        req.setDetalles(Arrays.asList(detalle));

        // Otros cargos (vacío)
        req.setOtrosCargos(new ArrayList<>());

        // Medios de pago se asignan después

        // Versión catálogos IMPORTANTE
        req.setVersionCatalogos(VERSION_CATALOGOS);

        // Rellenar totales
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

        // Determinar si es servicio o mercancía por CABYS - MANEJO SEGURO
        String tipoServicio = "MERCANCIA";
        boolean esServicio = false;

        // Verificación null-safe para evitar LazyInitializationException
        if (prod.getEmpresaCabys() != null &&
            prod.getEmpresaCabys().getCodigoCabys() != null &&
            prod.getEmpresaCabys().getCodigoCabys().getCodigo() != null &&
            !prod.getEmpresaCabys().getCodigoCabys().getCodigo().isEmpty()) {

            char primerDigito = prod.getEmpresaCabys().getCodigoCabys().getCodigo().charAt(0);
            if (primerDigito >= '5' && primerDigito <= '9') {
                tipoServicio = "SERVICIO";
                esServicio = true;
            }
        }

        DetalleFacturaRequest det = new DetalleFacturaRequest();
        det.setNumeroLinea(1);
        det.setProductoId(prod.getId());
        det.setCantidad(cantidad);
        det.setPrecioUnitario(montoSinIVA);
        det.setUnidadMedida(prod.getUnidadMedida() != null ?
            prod.getUnidadMedida().name() : "Unid");

        // Manejo seguro del código CABYS
        String codigoCabys = "0000000000000"; // Default
        if (prod.getEmpresaCabys() != null &&
            prod.getEmpresaCabys().getCodigoCabys() != null &&
            prod.getEmpresaCabys().getCodigoCabys().getCodigo() != null) {
            codigoCabys = prod.getEmpresaCabys().getCodigoCabys().getCodigo();
        }
        det.setCodigoCabys(codigoCabys);

        det.setDescripcionPersonalizada(prod.getDescripcion() != null ?
            prod.getDescripcion() : prod.getNombre());

        // CAMPOS FALTANTES:
        det.setEsServicio(esServicio);
        det.setAplicaImpuestoServicio(false); // Por defecto false para tiquetes automáticos

        // Montos calculados
        det.setMontoTotal(baseImponible);
        det.setMontoDescuento(BigDecimal.ZERO);
        det.setSubtotal(baseImponible);
        det.setMontoImpuesto(montoImpuesto);
        det.setMontoTotalLinea(totalLinea);

        // Impuestos
        ImpuestoLineaRequest iva = new ImpuestoLineaRequest();
        iva.setCodigoImpuesto(TipoImpuesto.IVA.getCodigo());
        iva.setCodigoTarifaIVA(CodigoTarifaIVA.TARIFA_GENERAL_13.getCodigo());
        iva.setTarifa(IVA_PORC);
        iva.setBaseImponible(baseImponible);
        iva.setMontoImpuesto(montoImpuesto);
        iva.setTieneExoneracion(false);
        iva.setMontoExoneracion(BigDecimal.ZERO);
        iva.setImpuestoNeto(montoImpuesto);

        det.setImpuestos(List.of(iva));
        det.setDescuentos(new ArrayList<>()); // Lista vacía

        return det;
    }

    private void rellenarTotalesTE(CrearFacturaRequest req) {
        BigDecimal totalServiciosGravados = BigDecimal.ZERO;
        BigDecimal totalServiciosExentos = BigDecimal.ZERO;
        BigDecimal totalServiciosExonerados = BigDecimal.ZERO;
        BigDecimal totalServiciosNoSujetos = BigDecimal.ZERO;

        BigDecimal totalMercanciasGravadas = BigDecimal.ZERO;
        BigDecimal totalMercanciasExentas = BigDecimal.ZERO;
        BigDecimal totalMercanciasExoneradas = BigDecimal.ZERO;
        BigDecimal totalMercanciasNoSujetas = BigDecimal.ZERO;

        BigDecimal totalImpuesto = BigDecimal.ZERO;

        for (DetalleFacturaRequest d : req.getDetalles()) {
            BigDecimal base = d.getPrecioUnitario().multiply(d.getCantidad())
                .setScale(SCALE_MONEDA, RoundingMode.HALF_UP);

            // Obtener info del primer impuesto (si existe)
            BigDecimal montoExoneracion = BigDecimal.ZERO;
            String codigoTarifaIVA = "08"; // Default tarifa general

            if (d.getImpuestos() != null && !d.getImpuestos().isEmpty()) {
                ImpuestoLineaRequest imp = d.getImpuestos().get(0);
                codigoTarifaIVA = imp.getCodigoTarifaIVA();
                montoExoneracion = imp.getMontoExoneracion() != null ?
                    imp.getMontoExoneracion() : BigDecimal.ZERO;
                totalImpuesto = totalImpuesto.add(imp.getImpuestoNeto());
            }

            // Clasificar según lógica del frontend
            BigDecimal toGrav = BigDecimal.ZERO;
            BigDecimal toExento = BigDecimal.ZERO;
            BigDecimal toNoSuj = BigDecimal.ZERO;
            BigDecimal toExone = BigDecimal.ZERO;

            if (montoExoneracion.compareTo(BigDecimal.ZERO) > 0) {
                // Tiene exoneración
                toExone = base;
            } else {
                // No tiene exoneración, clasificar por código tarifa
                if ("10".equals(codigoTarifaIVA)) {
                    toExento = base;
                } else if ("11".equals(codigoTarifaIVA)) {
                    toNoSuj = base;
                } else {
                    // Cualquier otro código (08, 04, 02, 01, etc) es gravado
                    toGrav = base;
                }
            }

            // Acumular según si es servicio o mercancía
            if (d.getEsServicio()) {
                totalServiciosGravados = totalServiciosGravados.add(toGrav);
                totalServiciosExentos = totalServiciosExentos.add(toExento);
                totalServiciosExonerados = totalServiciosExonerados.add(toExone);
                totalServiciosNoSujetos = totalServiciosNoSujetos.add(toNoSuj);
            } else {
                totalMercanciasGravadas = totalMercanciasGravadas.add(toGrav);
                totalMercanciasExentas = totalMercanciasExentas.add(toExento);
                totalMercanciasExoneradas = totalMercanciasExoneradas.add(toExone);
                totalMercanciasNoSujetas = totalMercanciasNoSujetas.add(toNoSuj);
            }
        }

        // Setear todos los totales
        req.setTotalServiciosGravados(totalServiciosGravados);
        req.setTotalServiciosExentos(totalServiciosExentos);
        req.setTotalServiciosExonerados(totalServiciosExonerados);
        req.setTotalServiciosNoSujetos(totalServiciosNoSujetos);

        req.setTotalMercanciasGravadas(totalMercanciasGravadas);
        req.setTotalMercanciasExentas(totalMercanciasExentas);
        req.setTotalMercanciasExoneradas(totalMercanciasExoneradas);
        req.setTotalMercanciasNoSujetas(totalMercanciasNoSujetas);

        // Totales generales
        req.setTotalGravado(totalServiciosGravados.add(totalMercanciasGravadas));
        req.setTotalExento(totalServiciosExentos.add(totalMercanciasExentas));
        req.setTotalExonerado(totalServiciosExonerados.add(totalMercanciasExoneradas));
        req.setTotalNoSujeto(totalServiciosNoSujetos.add(totalMercanciasNoSujetas));

        BigDecimal totalVenta = req.getTotalGravado()
            .add(req.getTotalExento())
            .add(req.getTotalExonerado())
            .add(req.getTotalNoSujeto());

        req.setTotalVenta(totalVenta);
        req.setTotalDescuentos(BigDecimal.ZERO);
        req.setTotalVentaNeta(totalVenta);
        req.setTotalImpuesto(totalImpuesto);
        req.setTotalIVADevuelto(BigDecimal.ZERO);
        req.setTotalOtrosCargos(BigDecimal.ZERO);

        BigDecimal totalComprobante = req.getTotalVentaNeta()
            .add(totalImpuesto)
            .add(req.getTotalOtrosCargos())
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